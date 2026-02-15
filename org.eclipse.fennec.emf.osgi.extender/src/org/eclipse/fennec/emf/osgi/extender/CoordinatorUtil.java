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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;

/**
 * Utility class for coordinations
 */
public class CoordinatorUtil {
	
	private static Logger logger = Logger.getLogger(CoordinatorUtil.class.getName());
	
	private CoordinatorUtil() {
	}

    public static Object getCoordination(final Object object) {
        final Coordinator coordinator = (Coordinator) object;
        final Coordination threadCoordination = coordinator.peek();
        if ( threadCoordination == null ) {
            try {
                return coordinator.create("org.apache.felix.configurator", 0L);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, e, ()->"Unable to create new coordination with coordinator " + coordinator);
            }
        }
        return null;
    }

    public static void endCoordination(final Object object) {
        final Coordination coordination = (Coordination) object;
        try {
            coordination.end();
        } catch (final Exception e) {
        	logger.log(Level.SEVERE, e, ()->"Error ending coordination " + coordination);
        }
    }
}
