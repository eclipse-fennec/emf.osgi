[![Build Status](https://github.com/eclipse-fennec/emf.osgi/actions/workflows/snapshot.yml/badge.svg)](https://github.com/eclipse-fennec/emf.osgi/actions/workflows/snapshot.yml)

# Eclipse Fennec EMF OSGi

Eclipse Fennec EMF OSGi enables the [Eclipse Modeling Framework (EMF)](https://eclipse.dev/modeling/emf/) in pure OSGi environments without any Eclipse PDE or Equinox dependencies. EMF models, packages, and factories are registered and consumed as standard OSGi services.

> **Note:** This project was formerly known as *GeckoEMF*. It has been donated to the Eclipse Foundation and transitioned to the [Eclipse Fennec](https://projects.eclipse.org/projects/technology.fennec) project. A [compatibility layer](#gecko-emf-compatibility) is provided for migration.

## Overview

EMF relies on static global registries (`EPackage.Registry.INSTANCE`, `Resource.Factory.Registry.INSTANCE`) and Eclipse extension points. This makes it difficult to use in non-Equinox OSGi runtimes and prevents dynamic, service-oriented architectures.

Fennec EMF OSGi replaces these static registries with dynamic OSGi services:

- **`ResourceSet`** is available as a prototype-scoped OSGi service, injectable via `@Reference`
- **`ResourceSetFactory`** creates pre-configured `ResourceSet` instances on demand
- **`EPackage`** and **`EFactory`** instances are registered as services with metadata properties
- **`Resource.Factory`** registrations are managed through the service registry
- Service properties are **dynamically updated** as models appear and disappear at runtime

```java
// Inject a ResourceSet that has the "mymodel" EPackage registered
@Reference(target = "(emf.name=mymodel)")
private ResourceSet resourceSet;

// Or inject the factory for programmatic creation
@Reference(target = "(emf.name=mymodel)")
private ResourceSetFactory resourceSetFactory;
```

## Getting Started

### BND Library (Recommended)

Add the Fennec EMF library to your BND workspace (`cnf/build.bnd`):

```properties
-library: fennec
```

This provides the required dependencies and the code generator. For individual modules, add to your project's `bnd.bnd`:

```properties
-library: enable-emf
```

### Gradle/Maven

Maven coordinates (group ID: `org.eclipse.fennec.emf`):

```
org.eclipse.fennec.emf:org.eclipse.fennec.emf.osgi.bom:${fennec.version}
```

## Model Registration

There are three ways to register EMF models with Fennec EMF OSGi:

### 1. Code Generator

The Fennec EMF code generator extends the standard EMF generator to produce OSGi-compatible code. Enable it by setting **GenModel > All > OSGi Compatible** to `true`.

The generator creates:

- **`EPackageConfigurator`** -- registers the EPackage in the appropriate registry
- **`ConfigurationComponent`** -- DS component that registers all model services

The following services are registered per model:

| Service | Properties |
|---------|------------|
| `EPackage` | `emf.name`, `emf.nsURI`, `emf.fileExtension`, `emf.contentType`, `emf.protocol` |
| `EFactory` | same as EPackage |
| `Resource.Factory` (if generated) | same as EPackage |
| `Condition` | all model-related properties |

In a BND workspace, code generation is configured in `bnd.bnd`:

```properties
-generate: \
    model/mymodel.genmodel; \
        generate=geckoEMF; \
        genmodel=model/mymodel.genmodel; \
        output=src

-includeresource: model=model
```

### 2. Model Extender

The extender (`org.eclipse.fennec.emf.osgi.extender`) automatically discovers and registers `.ecore` models from bundles at runtime -- no code generation required.

Add this to your bundle's `bnd.bnd`:

```properties
Require-Capability: \
    osgi.extender; \
    filter:="(osgi.extender=emf.model)"

-includeresource: model=model
```

Place `.ecore` files in the `model/` folder and the extender will register them as `EPackage` and `EPackageConfigurator` services automatically.

See the full [Extender Documentation](org.eclipse.fennec.emf.osgi.extender/readme.md) for custom paths, inline properties, and annotations.

### 3. Dynamic Package Registration

Register models dynamically via OSGi Configuration Admin using the `DynamicPackageLoader` factory PID:

```json
{
    "DynamicPackageLoader~demo": {
        "emf.dynamicEcoreUri": "https://example.org/demo/demo.ecore",
        "emf.feature": ["foo", "bar"],
        "emf.feature.my": "own"
    }
}
```

The model is loaded from the given URI and registered with the following properties derived from the `EPackage`:

| Property | Source |
|----------|--------|
| `emf.name` | `EPackage.getName()` |
| `emf.nsURI` | `EPackage.getNsURI()` |
| `emf.feature` | forwarded from configuration |
| `emf.feature.*` | prefix-stripped and forwarded (e.g., `emf.feature.my=own` becomes `my=own`) |

Changing `dynamicEcoreUri` triggers unregistration of the old model and re-registration from the new URI. Changing other properties updates the service properties without re-loading.

## Configurators

Configurators are services that customize the EMF setup. Register your own implementations with the `emf.configuratorName` property:

| Interface | Purpose |
|-----------|---------|
| `EPackageConfigurator` | Registers an EPackage in the EPackage registry |
| `ResourceFactoryConfigurator` | Registers resource factories in the ResourceFactory registry |
| `ResourceSetConfigurator` | Configures a ResourceSet before it is handed to consumers |

## Service Properties

All EMF services use standardized properties defined in `EMFNamespaces`:

| Property | Type | Description |
|----------|------|-------------|
| `emf.name` | `String+` | Model name(s) |
| `emf.nsURI` | `String+` | Model namespace URI(s) |
| `emf.fileExtension` | `String+` | File extensions for resource factories |
| `emf.protocol` | `String+` | Protocol schemes for resource factories |
| `emf.contentType` | `String+` | Content type identifiers |
| `emf.version` | `String` | Model version |
| `emf.feature` | `String+` | Feature tags for filtering |
| `emf.configuratorName` | `String` | Configurator name |
| `emf.dynamicEcoreUri` | `String` | URI for dynamic model loading |

Properties are automatically propagated: when a configurator is added or removed, the `ResourceSet` and `ResourceSetFactory` service properties are updated to reflect the current set of available models.

### The `emf.feature.*` Prefix

Custom properties can be forwarded through the Fennec EMF stack using the `emf.feature.` prefix. For example, setting `emf.feature.foo=bar` on a configurator results in `foo=bar` appearing on the `ResourceSet` and `ResourceSetFactory` service properties. This works for all configurators and dynamic model registration.

## Configuration Admin Components

For advanced scenarios, Fennec EMF provides configurable DS components that create dedicated, isolated EMF stacks via OSGi Configuration Admin. This is useful for multi-tenant applications or when multiple independent model sets are needed.

See the [Configuration Guide](docs/configuration-guide.md) for full details, factory PIDs, and JSON examples.

## Project Modules

| Module | Description |
|--------|-------------|
| `org.eclipse.fennec.emf.osgi.api` | Public API: interfaces, configurators, constants, annotations |
| `org.eclipse.fennec.emf.osgi` | Core implementation: DS components, registries, ResourceSet factories |
| `org.eclipse.fennec.emf.osgi.codegen` | BND-based EMF code generator for OSGi-compatible model code |
| `org.eclipse.fennec.emf.osgi.extender` | OSGi extender for automatic `.ecore` model registration |
| `org.eclipse.fennec.emf.osgi.model.info` | Runtime model introspection (EClassifier lookup by Java class) |
| `org.eclipse.fennec.emf.gecko.compatibility.api` | Compatibility layer for migrating from GeckoEMF |

## Documentation

- [Configuration Guide](docs/configuration-guide.md) -- Configuring EMF components via OSGi Configuration Admin
- [Extender Documentation](org.eclipse.fennec.emf.osgi.extender/readme.md) -- Automatic model registration from bundles
- [EMF Delegate Registries](docs/emf-delegate-registries.md) -- Analysis of EMF's four delegate registries
- [EMF Delegate User Guide](docs/emf-delegate-user-guide.md) -- Using invocation, setting, validation, and conversion delegates

## Building

The project uses a Gradle + BND workspace:

```bash
./gradlew build       # Full build
./gradlew test        # Run unit tests
./gradlew clean       # Clean build artifacts
```

Requires Java 21.

## Gecko EMF Compatibility

The module `org.eclipse.fennec.emf.gecko.compatibility.api` provides wrapper interfaces that map the old `org.gecko.emf.osgi` package names to the new `org.eclipse.fennec.emf.osgi` packages. This allows existing GeckoEMF consumers to migrate incrementally.

## Links

- [Eclipse Fennec Project](https://projects.eclipse.org/projects/technology.fennec)
- [Source Code](https://github.com/eclipse-fennec/emf.osgi)
- [Issue Tracker](https://github.com/eclipse-fennec/emf.osgi/issues)

## Contributors

- **Juergen Albert** (jalbert) / [j.albert@data-in-motion.biz](mailto:j.albert@data-in-motion.biz) @ [Data In Motion](https://www.datainmotion.de) -- architect, developer
- **Mark Hoffmann** (mhoffmann) / [m.hoffmann@data-in-motion.biz](mailto:m.hoffmann@data-in-motion.biz) @ [Data In Motion](https://www.datainmotion.de) -- developer, architect
- **Stefan Bischof** (bipolis) / [stbischof@bipolis.org](mailto:stbischof@bipolis.org) -- developer

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/)

## Copyright

Copyright (c) Contributors to the Eclipse Foundation.

---
Data In Motion Consulting GmbH - [info@data-in-motion.biz](mailto:info@data-in-motion.biz)
