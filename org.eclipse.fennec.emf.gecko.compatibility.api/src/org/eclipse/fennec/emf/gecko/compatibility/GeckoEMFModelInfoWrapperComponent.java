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
package org.eclipse.fennec.emf.gecko.compatibility;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.fennec.emf.osgi.model.info.EMFModelInfo;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Compatibility component that tracks new Fennec {@link EMFModelInfo}
 * services and re-registers them as legacy {@link org.gecko.emf.osgi.model.info.EMFModelInfo}
 * services with converted properties.
 * <p>
 * This wrapper exposes new Fennec services under the old Gecko interface so that existing
 * consumers using the old API continue to work.
 *
 * @author Data In Motion
 * @since 1.0
 */
@Capability(namespace = org.gecko.emf.osgi.model.info.EMFModelInfo.NAMESPACE, name = org.gecko.emf.osgi.model.info.EMFModelInfo.NAME)
@Component(name = "GeckoEMFModelInfoWrapper")
@SuppressWarnings("deprecation")
public class GeckoEMFModelInfoWrapperComponent {

	private static final Set<String> EXCLUDED_PROPERTIES = Set.of(
			Constants.SERVICE_ID,
			Constants.SERVICE_BUNDLEID,
			Constants.SERVICE_SCOPE,
			Constants.OBJECTCLASS,
			Constants.SERVICE_PID,
			ComponentConstants.COMPONENT_NAME,
			ComponentConstants.COMPONENT_ID
	);

	private final BundleContext ctx;
	private final Map<EMFModelInfo, ServiceRegistration<org.gecko.emf.osgi.model.info.EMFModelInfo>> registrations = new ConcurrentHashMap<>();

	@Activate
	public GeckoEMFModelInfoWrapperComponent(BundleContext ctx) {
		this.ctx = ctx;
	}

	@Deactivate
	public void deactivate() {
		registrations.values().forEach(ServiceRegistration::unregister);
		registrations.clear();
	}

	@Reference(name = "fennecEMFModelInfo", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeFennecEMFModelInfo", updated = "updatedFennecEMFModelInfo")
	public void addFennecEMFModelInfo(EMFModelInfo modelInfo, Map<String, Object> properties) {
		org.gecko.emf.osgi.model.info.EMFModelInfo wrapper = new org.gecko.emf.osgi.model.info.EMFModelInfo() {
			@Override
			public Optional<EClassifier> getEClassifierForClass(Class<?> clazz) {
				return modelInfo.getEClassifierForClass(clazz);
			}

			@Override
			public Optional<EClassifier> getEClassifierForClass(String fullQualifiedClassName) {
				return modelInfo.getEClassifierForClass(fullQualifiedClassName);
			}

			@Override
			public List<EClass> getUpperTypeHierarchyForEClass(EClass eClass) {
				return modelInfo.getUpperTypeHierarchyForEClass(eClass);
			}
		};
		Dictionary<String, Object> converted = convertProperties(properties);
		ServiceRegistration<org.gecko.emf.osgi.model.info.EMFModelInfo> reg = ctx.registerService(org.gecko.emf.osgi.model.info.EMFModelInfo.class, wrapper, converted);
		registrations.put(modelInfo, reg);
	}

	public void updatedFennecEMFModelInfo(EMFModelInfo modelInfo, Map<String, Object> properties) {
		ServiceRegistration<org.gecko.emf.osgi.model.info.EMFModelInfo> reg = registrations.get(modelInfo);
		if (reg != null) {
			reg.setProperties(convertProperties(properties));
		}
	}

	public void removeFennecEMFModelInfo(EMFModelInfo modelInfo) {
		ServiceRegistration<org.gecko.emf.osgi.model.info.EMFModelInfo> reg = registrations.remove(modelInfo);
		if (reg != null) {
			reg.unregister();
		}
	}

	private Dictionary<String, Object> convertProperties(Map<String, Object> properties) {
		Hashtable<String, Object> converted = new Hashtable<>();
		properties.forEach((key, value) -> {
			if (value != null && !EXCLUDED_PROPERTIES.contains(key)) {
				converted.put(key, value);
			}
		});
		return converted;
	}
}
