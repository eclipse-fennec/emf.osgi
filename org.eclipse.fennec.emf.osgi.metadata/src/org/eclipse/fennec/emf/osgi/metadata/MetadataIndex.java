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
package org.eclipse.fennec.emf.osgi.metadata;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Query and maintenance in one interface - the shape an index implementation registers
 * itself under. The whiteboard binds it through
 * {@link MetadataWhiteboard#setMetadataIndex(MetadataIndex)} and hands only its read side
 * to consumers.
 * <p>
 * Splitting read from write is what allows a different implementation (a persistent or
 * full-text index, say) to be dropped in without consumers noticing: they only ever hold
 * a {@link MetadataIndexReader}.
 *
 * @author Data In Motion Consulting
 */
@ProviderType
public interface MetadataIndex extends MetadataIndexReader, MetadataIndexWriter {
}
