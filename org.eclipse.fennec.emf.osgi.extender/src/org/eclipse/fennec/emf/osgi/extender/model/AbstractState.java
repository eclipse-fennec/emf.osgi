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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * This object holds a sorted map of models
 */
public class AbstractState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Serialization version. */
    private static final int VERSION = 1;

    private Map<String, ModelNamespace> modelsByNamespace = new TreeMap<>();

    /**
     * Serialize the object
     * - write version id
     * - serialize fields
     * @param out Object output stream
     * @throws IOException
     */
    private void writeObject(final java.io.ObjectOutputStream out)
    throws IOException {
        out.writeInt(VERSION);
        out.writeObject(modelsByNamespace);
    }

    /**
     * Deserialize the object
     * - read version id
     * - deserialize fields
     */
    @SuppressWarnings("unchecked")
    private void readObject(final java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        final int version = in.readInt();
        if ( version < 1 || version > VERSION ) {
            throw new ClassNotFoundException(this.getClass().getName());
        }
        this.modelsByNamespace = (Map<String, ModelNamespace>) in.readObject();
    }

    public void add(final Model c) {
        ModelNamespace models = this.modelsByNamespace.get(c.getNamespace());
        if ( models == null ) {
            models = new ModelNamespace();
            this.modelsByNamespace.put(c.getNamespace(), models);
        }

        models.add(c);
    }

    public Map<String, ModelNamespace> getModels() {
        return this.modelsByNamespace;
    }

    public ModelNamespace getModels(final String ns) {
        return this.getModels().get(ns);
    }

    public Collection<String> getNamespaces() {
        return this.getModels().keySet();
    }
}
