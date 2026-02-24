/********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Data In Motion Consulting - initial implementation
 ********************************************************************/
package org.eclipse.fennec.emf.osgi.extender;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.extender.model.Model;
import org.eclipse.fennec.emf.osgi.helper.EcoreHelper;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * EMF ecore model extender that discovers and registers EMF models from OSGi bundles.
 * <p>
 * This class implements the OSGi extender pattern using a {@link BundleTracker} to
 * monitor bundles entering {@link Bundle#ACTIVE ACTIVE} or {@link Bundle#STARTING STARTING}
 * states. Bundles that declare the requirement:
 * <pre>
 * Require-Capability: osgi.extender; filter:="(osgi.extender=emf.model)"
 * </pre>
 * are scanned for {@code .ecore} model files. Each discovered {@link EPackage} is
 * registered as both an {@link EPackage} service and an {@link EPackageConfigurator}
 * service, enabling dynamic model availability in the OSGi service registry.
 * <p>
 * Service registrations are performed in the model bundle's own {@link BundleContext}
 * so that they are automatically cleaned up when the model bundle is stopped.
 *
 * @author Mark Hoffmann
 * @since 13.10.2022
 * @see ModelHelper
 * @see ModelExtenderConfigurator
 */
@Capability(namespace = ExtenderNamespace.EXTENDER_NAMESPACE, name = EMFNamespaces.EMF_MODEL_EXTENDER_NAME, version = "1.0.0")
public class EMFModelExtender {

	private static final Logger logger = Logger.getLogger(EMFModelExtender.class.getName());

	private final BundleContext bundleContext;
	private final BundleTracker<Bundle> tracker;

	/** Service registrations indexed by bundle ID, for cleanup on bundle removal. */
	private final Map<Long, List<ServiceRegistration<?>>> registrations = new ConcurrentHashMap<>();

	/**
	 * Creates a new model extender.
	 *
	 * @param bc the bundle context of the extender bundle itself
	 * @throws NullPointerException if {@code bc} is {@code null}
	 */
	public EMFModelExtender(BundleContext bc) {
		this.bundleContext = requireNonNull(bc, "BundleContext must not be null");
		this.tracker = new BundleTracker<>(bc,
				Bundle.ACTIVE | Bundle.STARTING,
				new BundleTrackerCustomizer<Bundle>() {

					@Override
					public Bundle addingBundle(Bundle bundle, BundleEvent event) {
						processAddBundle(bundle);
						return bundle;
					}

					@Override
					public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
						// no action needed - state transitions are handled by adding/removed
					}

					@Override
					public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
						processRemoveBundle(bundle.getBundleId());
					}
				});
	}

	/**
	 * Start tracking bundles. The tracker will scan all existing active
	 * bundles and then continue tracking new ones.
	 */
	public void start() {
		this.tracker.open();
	}

	/**
	 * Stop tracking and unregister all services.
	 */
	public void shutdown() {
		this.tracker.close();
		// Unregister any remaining services
		registrations.values().forEach(regs -> regs.forEach(reg -> {
			try {
				reg.unregister();
			} catch (IllegalStateException e) {
				// already unregistered
			}
		}));
		registrations.clear();
	}

	private void processAddBundle(Bundle bundle) {
		long bundleId = bundle.getBundleId();

		// Atomic check-then-put: skip if already processed (R1 fix)
		if (registrations.putIfAbsent(bundleId, List.of()) != null) {
			return;
		}

		try {
			Set<String> paths = ModelHelper.isModelBundle(bundle, bundleContext.getBundle().getBundleId());
			if (paths.isEmpty()) {
				registrations.remove(bundleId);
				return;
			}

			EcoreHelper ecoreHelper = new EcoreHelper();
			List<Model> models;
			try {
				ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();
				models = ModelHelper.readModelsFromBundle(bundle, ecoreHelper.getResourceSet(), paths, diagnostic);

				diagnostic.warnings.forEach(w -> logger.log(Level.WARNING, w));
				diagnostic.errors.forEach(e -> logger.log(Level.SEVERE, e));
			} finally {
				// M1 fix: release ResourceSet resources after loading
				ecoreHelper.releaseAll();
			}

			if (models.isEmpty()) {
				registrations.remove(bundleId);
				return;
			}

			List<ServiceRegistration<?>> bundleRegs = new ArrayList<>();
			try {
				for (Model model : models) {
					registerModel(bundle, model, bundleRegs);
				}
			} catch (RuntimeException e) {
				// M2 fix: on partial failure, unregister already-registered services
				for (ServiceRegistration<?> reg : bundleRegs) {
					try {
						reg.unregister();
					} catch (IllegalStateException ise) {
						// already unregistered
					}
				}
				registrations.remove(bundleId);
				throw e;
			}
			registrations.put(bundleId, bundleRegs);

			logger.fine(() -> "Registered " + models.size() + " EMF model(s) from " + getBundleIdentity(bundle));
		} catch (IllegalStateException e) {
			registrations.remove(bundleId);
			logger.log(Level.SEVERE, e, () -> "Error processing bundle " + getBundleIdentity(bundle));
		}
	}

	/**
	 * Registers a single model as both an {@link EPackageConfigurator} and
	 * an {@link EPackage} service in the model bundle's context.
	 */
	private void registerModel(Bundle modelBundle, Model model, List<ServiceRegistration<?>> regs) {
		EPackage ePackage = model.getEPackage();
		var properties = model.getProperties();

		ModelExtenderConfigurator configurator = new ModelExtenderConfigurator(ePackage);

		BundleContext modelBundleContext = getModelBundleContext(model.getBundleId());
		regs.add(modelBundleContext.registerService(EPackageConfigurator.class.getName(), configurator, properties));
		regs.add(modelBundleContext.registerService(EPackage.class, ePackage, properties));
	}

	/**
	 * Resolves the {@link BundleContext} for the bundle with the given ID.
	 * Falls back to the extender's own context if {@code bundleId} is {@code -1}
	 * or if the target bundle cannot be resolved.
	 *
	 * @param bundleId the target bundle ID
	 * @return the bundle context of the target bundle, or the extender's own context as fallback
	 */
	private BundleContext getModelBundleContext(long bundleId) {
		if (bundleId == -1) {
			return bundleContext;
		}
		// N1 fix: guard against NPE from system bundle or target bundle lookup
		Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		if (systemBundle == null) {
			return bundleContext;
		}
		BundleContext systemContext = systemBundle.getBundleContext();
		if (systemContext == null) {
			return bundleContext;
		}
		Bundle targetBundle = systemContext.getBundle(bundleId);
		if (targetBundle == null) {
			return bundleContext;
		}
		BundleContext targetContext = targetBundle.getBundleContext();
		return targetContext != null ? targetContext : bundleContext;
	}

	private void processRemoveBundle(long bundleId) {
		List<ServiceRegistration<?>> regs = registrations.remove(bundleId);
		if (regs == null) {
			return;
		}
		for (ServiceRegistration<?> reg : regs) {
			try {
				reg.unregister();
			} catch (IllegalStateException e) {
				logger.log(Level.FINE, e, () -> "Service already unregistered for bundle " + bundleId);
			}
		}
		logger.fine(() -> "Unregistered EMF model(s) for bundle " + bundleId);
	}

	/** Returns a human-readable identifier for a bundle (symbolic name, version, and ID). */
	private String getBundleIdentity(Bundle bundle) {
		if (bundle.getSymbolicName() == null) {
			return bundle.getBundleId() + " (" + bundle.getLocation() + ")";
		}
		return bundle.getSymbolicName() + ":" + bundle.getVersion() + " (" + bundle.getBundleId() + ")";
	}
}
