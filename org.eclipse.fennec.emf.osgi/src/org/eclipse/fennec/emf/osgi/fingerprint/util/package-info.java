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
/**
 * Static access to the fingerprint computation and the shared model service property
 * core, for emitting bundles (registry components, dynamic loader, extender) and for
 * build-time use by the code generator.
 *
 * @author Mark Hoffmann
 */
@org.osgi.annotation.versioning.Version("1.1.0")
@org.osgi.annotation.bundle.Export
package org.eclipse.fennec.emf.osgi.fingerprint.util;
