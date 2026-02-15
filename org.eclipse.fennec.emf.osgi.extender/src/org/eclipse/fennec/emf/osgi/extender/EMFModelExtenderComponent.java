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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.logging.Logger;

import org.osgi.framework.Constants;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.ComponentContext;

/**
 * 
 * @author mark
 * @since 13.10.2022
 */
@Component
public class EMFModelExtenderComponent {
	
	private static Logger logger = Logger.getLogger(EMFModelExtenderComponent.class.getName());

    /** The current EMF extender. */
    private EMFModelExtender modelExtender;
	
	@Activate
	public void activate(ComponentContext ctx) {
		modelExtender = new EMFModelExtender(ctx.getBundleContext());
		modelExtender.start();
		logger.info("Start EMF Model Extender");
	}
	@Deactivate
	public void deactivate(ComponentContext ctx) {
		modelExtender.shutdown();
		logger.info("Shutdown EMF Model Extender");
		
	}
	
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, 
			policy = ReferencePolicy.DYNAMIC, 
			service = AnyService.class, 
			target = "(" + Constants.OBJECTCLASS + "=org.osgi.service.coordinator.Coordinator)")
	public void addCoordinator(Object coordinator) {
		if (modelExtender != null) {
			modelExtender.setCoordinator(coordinator);
		}
	}
	
	public void removeCoordinator(Object coordinator) {
		if (modelExtender != null) {
			modelExtender.setCoordinator(null);
		}
		
	}

}
