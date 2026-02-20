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

import java.util.logging.Logger;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Declarative Services component that manages the lifecycle of the {@link EMFModelExtender}.
 * <p>
 * On activation, creates and starts an {@link EMFModelExtender} that begins
 * tracking bundles for EMF model content. On deactivation, the extender is
 * shut down and all model service registrations are cleaned up.
 *
 * @author Mark Hoffmann
 * @since 13.10.2022
 */
@Component
public class EMFModelExtenderComponent {

	private static final Logger logger = Logger.getLogger(EMFModelExtenderComponent.class.getName());

	private EMFModelExtender modelExtender;

	/**
	 * Activates the component by creating and starting the model extender.
	 *
	 * @param ctx the component context providing access to the bundle context
	 */
	@Activate
	public void activate(ComponentContext ctx) {
		modelExtender = new EMFModelExtender(ctx.getBundleContext());
		modelExtender.start();
		logger.info("Started EMF Model Extender");
	}

	/**
	 * Deactivates the component by shutting down the model extender
	 * and unregistering all discovered model services.
	 *
	 * @param ctx the component context
	 */
	@Deactivate
	public void deactivate(ComponentContext ctx) {
		modelExtender.shutdown();
		logger.info("Stopped EMF Model Extender");
	}
}
