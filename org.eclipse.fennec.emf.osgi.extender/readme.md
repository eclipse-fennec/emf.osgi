# Fennec EMF Model Extender

An OSGi extender that automatically discovers and registers EMF ecore models from bundles at runtime -- no code generation or manual service registration required.

## Overview

The EMF Model Extender implements the [OSGi Extender Pattern](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html) using a `BundleTracker` to monitor bundles as they become active. When a bundle declares the `emf.model` extender requirement, the extender:

1. Scans the declared model paths for `.ecore` files
2. Loads each ecore file into an `EPackage`
3. Registers each `EPackage` as an OSGi service
4. Registers a corresponding `EPackageConfigurator` service

This makes the models available to the Fennec EMF OSGi infrastructure without any generated code.

## How It Works

### Architecture

```
+---------------------+       tracks        +-------------------+
| EMFModelExtender    | ------------------> | Model Bundles     |
| (BundleTracker)     |                     | (with .ecore)     |
+---------------------+                     +-------------------+
         |                                           |
         |  reads Require-Capability                 |  contains .ecore files
         |  extracts model paths                     |  at declared paths
         v                                           v
+---------------------+       loads         +-------------------+
| ModelHelper         | ------------------> | EcoreHelper       |
| (path resolution,   |                     | (ResourceSet,     |
|  bundle scanning)   |                     |  ecore loading)   |
+---------------------+                     +-------------------+
         |
         |  registers services in model bundle's context
         v
+---------------------+
| OSGi Service        |
| Registry            |
|  - EPackage         |
|  - EPackageConfig.  |
+---------------------+
```

### Component Lifecycle

The `EMFModelExtenderComponent` (Declarative Services) manages the lifecycle:

- **Activate**: Creates an `EMFModelExtender` and starts the `BundleTracker`
- **Deactivate**: Stops the tracker and unregisters all model services

### Service Registration

For each discovered `.ecore` model, two services are registered in the **model bundle's own `BundleContext`** (not the extender's), ensuring automatic cleanup when the model bundle stops:

| Service Type | Implementation | Purpose |
|-------------|----------------|---------|
| `EPackageConfigurator` | `ModelExtenderConfigurator` | Registers/unregisters the `EPackage` in EMF package registries |
| `EPackage` | The loaded `EPackage` instance | Direct access to the EMF package for consumers |

### Service Properties

Each registered service carries these standard properties:

| Property | Value | Description |
|----------|-------|-------------|
| `emf.name` | `ePackage.getName()` | The EPackage name |
| `emf.nsURI` | `ePackage.getNsURI()` | The EPackage namespace URI |
| `emf.registration` | `extender` | Indicates model was registered by the extender |
| `emf.model.scope` | `static` (default) | The registry scope for the EPackageConfigurator |

Additional custom properties can be specified inline (see below).

## Usage

### Basic: Default Model Folder

Place your `.ecore` files in a `model/` folder inside your bundle and add this requirement to your `bnd.bnd`:

```properties
Require-Capability: \
    osgi.extender;\
    filter:="(osgi.extender=emf.model)"
```

The extender will scan the default path `model/` and register all `.ecore` files found there.

### Using the Annotation

Instead of manually writing the `Require-Capability` header, use the `@ProvideExtenderModel` annotation on any class or `package-info.java`:

```java
@ProvideExtenderModel
package com.example.mymodel;

import org.eclipse.fennec.emf.osgi.annotation.extender.ProvideExtenderModel;
```

With custom model locations:

```java
@ProvideExtenderModel({"/mymodels", "OSGI-INF/ecore/special.ecore"})
package com.example.mymodel;

import org.eclipse.fennec.emf.osgi.annotation.extender.ProvideExtenderModel;
```

### Custom Model Paths

Specify one or more paths using the `models` attribute:

```properties
Require-Capability: \
    osgi.extender;\
    filter:="(osgi.extender=emf.model)";\
    models:List<String>="OSGI-INF/model,/custom/path"
```

### Registering a Single Model File

Point directly to a specific `.ecore` file:

```properties
Require-Capability: \
    osgi.extender;\
    filter:="(osgi.extender=emf.model)";\
    models:List<String>="/model/mymodel.ecore"
```

### Inline Properties

Append additional service properties to any path using semicolons. These properties are added to the `EPackage` and `EPackageConfigurator` service registrations:

```
path;key1=value1;key2=value2;flagKey
```

- `key=value` -- sets a named property
- `flagKey` (no `=`) -- sets the key with a `null` value

#### Examples

Single model with properties:

```properties
models:List<String>="/model/mymodel.ecore;foo=bar;test=me"
```

Multiple paths with different properties:

```properties
models:List<String>="/model;foo=bar,OSGI-INF/model;env=staging,/special/special.ecore;toast=me"
```

Override the default scope:

```properties
models:List<String>="/model;emf.model.scope=resourceset"
```

The `emf.model.scope` property controls which EMF registry level the `EPackageConfigurator` targets. If not specified, it defaults to `static`. Available scopes:

| Scope | Description |
|-------|-------------|
| `static` | Replaces entries in `EPackage.Registry.INSTANCE` (global) |
| `resourceset` | Registered at ResourceSet level |
| `generated` | For generated model code |

### Full Example

A bundle's `bnd.bnd` with multiple model locations and properties:

```properties
-includeresource.model: \
    model/manual.ecore=model/manual.ecore,\
    OSGI-INF/model/test.ecore=model/test.ecore

Require-Capability: \
    osgi.extender;\
    filter:="(osgi.extender=emf.model)";\
    models:List<String>="/model;foo=bar;test=me,OSGI-INF/model;env=production"

-buildpath: org.eclipse.fennec.emf.osgi.api;version=snapshot
```

## Module Structure

```
org.eclipse.fennec.emf.osgi.extender/
  src/
    org/eclipse/fennec/emf/osgi/extender/
      EMFModelExtenderComponent.java  -- DS component (lifecycle)
      EMFModelExtender.java           -- BundleTracker + service registration
      ModelExtenderConfigurator.java   -- EPackageConfigurator implementation
      ModelHelper.java                -- Bundle scanning + ecore loading utility
      model/
        Model.java                    -- Immutable data holder (EPackage + properties + bundleId)
  test/
    org/gecko/emf/osgi/extender/
      ModelUtilsTest.java             -- extractProperties unit tests
      ModelExtenderConfiguratorTest.java -- Configurator unit tests
      ModelHelperTest.java            -- Model loading unit tests
      ModelTest.java                  -- Model data holder unit tests
```

## Testing

### Unit Tests

```bash
./gradlew :org.eclipse.fennec.emf.osgi.extender:test
```

Unit tests cover: `Model`, `ModelExtenderConfigurator`, `ModelHelper` (property extraction, model loading with real `.ecore` files).

### OSGi Integration Tests

```bash
./gradlew :org.eclipse.fennec.emf.osgi.extender.itest:testOSGi
```

Integration tests run in a full OSGi container (Felix) and verify:
- Bundle tracking and model discovery
- Service registration and property propagation
- Bundle restart and re-registration
- End-to-end lifecycle with real model bundles

### Full Build

```bash
./gradlew build
```

A full build is required when changing the extender or API, because the component bundle (`org.eclipse.fennec.emf.osgi`) repackages the API. Without a full build, integration tests may fail with `NoClassDefFoundError`.

## Dependencies

| Dependency | Purpose |
|-----------|---------|
| `org.eclipse.fennec.emf.osgi.api` | Interfaces (`EPackageConfigurator`), constants (`EMFNamespaces`), `EcoreHelper` |
| `org.eclipse.emf.ecore` | EMF core (EPackage, EClass, Resource) |
| `org.eclipse.emf.ecore.xmi` | XMI resource factory for loading `.ecore` files |
| `org.osgi.framework` | OSGi framework API (Bundle, BundleContext, ServiceRegistration) |
| `org.osgi.util.tracker` | BundleTracker |
| `org.osgi.namespace.extender` | OSGi extender namespace constants |
| `org.osgi.service.component` | Declarative Services annotations |
