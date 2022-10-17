/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.gecko.emf.osgi.extender.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Dictionary;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.framework.ServiceRegistration;

public class Model implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Serialization version. */
    private static final int VERSION = 1;

    /** The model namespace */
    private String ns;

    /** The bundle id. */
    private long bundleId;
    
    /** The model URL */
    private URL url;

    /** The model properties. */
    private Dictionary<String, Object> properties;
    
    private transient EPackage ePackage = null;
    private transient ServiceRegistration<?> modelRegistration = null;

    /** The model state. */
    private volatile ModelState state = ModelState.INSTALL;

    private volatile List<File> files;

    public Model(final EPackage ePackage,
    		final URL url,
            final Dictionary<String, Object> properties,
            final long bundleId) {
        this.ePackage = ePackage;
        this.ns = ePackage.getNsURI();
        this.url = url;
        this.bundleId = bundleId;
        this.properties = properties;
    }

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
        out.writeObject(ns);
        out.writeObject(url.toExternalForm());
        out.writeObject(properties);
        out.writeLong(bundleId);
        out.writeObject(state.name());
        out.writeObject(files);
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
        this.ns = (String) in.readObject();
        this.url = new URL((String) in.readObject());
        this.properties = (Dictionary<String, Object>) in.readObject();
        this.bundleId = in.readLong();
        this.state = ModelState.valueOf((String)in.readObject());
        this.files = (List<File>) in.readObject();
    }

    /**
     * Get the model namespace
     * @return The model namespace.
     */
    public String getNamespace() {
        return this.ns;
    }

    /**
     * The bundle id
     * @return The bundle id
     */
    public long getBundleId() {
        return this.bundleId;
    }

    /**
     * Get the model state
     * @return The state
     */
    public ModelState getState() {
        return this.state;
    }

    /**
     * Set the model state
     * @param value The new state
     */
    public void setState(final ModelState value) {
        this.state = value;
    }

    /**
     * Get all properties
     * @return The model properties
     */
    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    public void setFiles(final List<File> f) {
        this.files = f;
    }

    public List<File> getFiles() {
        return this.files;
    }

    /**
	 * Returns the modelReference.
	 * @return the modelReference
	 */
	public EPackage getEPackage() {
		return ePackage;
	}

	/**
	 * Returns the modelREgistration.
	 * @return the modelREgistration
	 */
	public ServiceRegistration<?> getModelRegistration() {
		return modelRegistration;
	}

	/**
	 * Sets the modelREgistration.
	 * @param modelREgistration the modelREgistration to set
	 */
	public void setModelRegistration(ServiceRegistration<?> modelREgistration) {
		this.modelRegistration = modelREgistration;
	}

    @Override
    public String toString() {
        return "Model [namespace=" + ns
                + ", url=" + url.toExternalForm()
                + ", bundleId=" + bundleId
                + ", properties=" + properties
                + ", state=" + state + "]";
    }
}
