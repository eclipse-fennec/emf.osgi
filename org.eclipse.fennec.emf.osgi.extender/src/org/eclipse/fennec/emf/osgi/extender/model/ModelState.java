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
package org.eclipse.fennec.emf.osgi.extender.model;

/**
 * The state of a EMF model.
 *
 * The state represents the EMF model extenders view. It might not
 * reflect the current state of the system. For example if a
 * configuration is installed through the EMF model extender, it gets
 * the state "INSTALLED". However if an administrator now removes the service
 * through any other way like e.g. the web console,
 * the model still has the state "INSTALLED".
 *
 */
public enum ModelState {

    INSTALL,        // the configuration should be installed
    UNINSTALL,      // the configuration should be uninstalled
    INSTALLED,      // the configuration is installed
    UNINSTALLED,    // the configuration is uninstalled
    IGNORED         // the configuration is ignored
}
