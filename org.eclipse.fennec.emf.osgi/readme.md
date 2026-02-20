# Fennec EMF OSGi Core Implementation

The core implementation module that bridges Eclipse Modeling Framework (EMF) with OSGi's dynamic service model. Provides Declarative Services components for EPackage registries, ResourceFactory registries, and ResourceSetFactory -- making EMF models dynamically available as OSGi services.

## Overview

This module implements the service interfaces defined in the API module (`org.eclipse.fennec.emf.osgi.api`) and produces two bundle variants:

| Bundle | Artifact | Description |
|--------|----------|-------------|
| **Full (All-In-One)** | `org.eclipse.fennec.emf.osgi.component` | All components including ConfigAdmin-driven isolated factories, dynamic ecore loading, and specialized ResourceSet implementations |
| **Minimal** | `org.eclipse.fennec.emf.osgi.component.minimal` | Core components only -- sufficient for most use cases |

Both bundles repackage the API module, so downstream consumers only need to depend on a single bundle at runtime.

## Architecture

### Component Dependency Chain

```
EPackageConfigurator services (scope=static)
        |
        v
StaticEPackageRegistryComponent
  registers: EPackage.Registry (emf.default.epackage.registry=true)
        |
        v  parent registry reference
EPackageConfigurator services (scope=resourceset)
        |
        v
DefaultEPackageRegistryComponent
  registers: EPackage.Registry (default.resourceset.epackage.registry=true)
        |
        v  constructor reference
DefaultResourceSetFactoryComponent ←── Resource.Factory services
        |                                      |
        |                                      v
        |                        DefaultResourceFactoryRegistryComponent
        |                          registers: Resource.Factory.Registry
        |
        v  registers 3 services:
   ResourceSetFactory   (this)
   ResourceSet          (prototype, via ResourceSetPrototypeFactory)
   Condition            (lifecycle signal)
```

### Dynamic Property Propagation

The framework ensures that service properties are always up-to-date with the current set of registered models:

1. When an `EPackageConfigurator` registers/unregisters, the registry component updates its own service properties (adding/removing `emf.name`, `emf.nsURI` entries)
2. `RegistryTrackingServiceComponent` detects these property changes and notifies registered `RegistryPropertyListener`s
3. `DefaultResourceSetFactoryComponent` (as a listener) propagates the changes to its own service properties
4. Consumers with service filters like `target="(emf.name=mymodel)"` automatically see the updated capabilities

This happens via the `ServicePropertyContext` mechanism from the API, which aggregates properties from multiple services and outputs merged `Dictionary`s for service re-registration.

## Components

### Core Components (both variants)

#### StaticEPackageRegistryComponent

Replaces EMF's global `EPackage.Registry.INSTANCE` with an OSGi-managed registry.

- Implements `EPackage.Registry` directly
- Backed by a `DelegatingHashMap` that fires `MapChangeListener` events on structural changes
- Tracks `EPackageConfigurator` services with `emf.model.scope=static`
- Service property: `emf.default.epackage.registry=true`

#### DefaultEPackageRegistryComponent

Provides the ResourceSet-level EPackage registry.

- Backed by `EPackageRegistryImpl` with `EPackage.Registry.INSTANCE` as delegate
- Tracks `EPackageConfigurator` services with `emf.model.scope=resourceset`
- References the `StaticEPackageRegistryComponent` as parent registry to propagate its properties
- Service property: `default.resourceset.epackage.registry=true`

#### DefaultResourceFactoryRegistryComponent

Manages the `Resource.Factory.Registry` for file extension, protocol, and content type mappings.

- Creates a `ResourceFactoryRegistryImpl` and registers it as an OSGi service
- Pre-installs `FennecXMLResourceFactory` for `xml` extension and `application/xml` content type
- Dynamically tracks `Resource.Factory` services using the filter:
  ```
  (|(emf.model.contentType=*)(emf.model.fileExtension=*)(emf.model.protocol=*))
  ```
- Routes factories to the correct map (content type, extension, or protocol) based on their service properties

#### DefaultResourceSetFactoryComponent

The main `ResourceSetFactory` implementation.

- Extends `DefaultResourceSetFactory` (from the `provider` package)
- Constructor-injects references to `EPackage.Registry`, `Resource.Factory.Registry`, and `RegistryTrackingService`
- Registers itself as listener on both registry services to receive property-change notifications
- Calls `EcorePackagesRegistrator.start()` on activation to bootstrap core EMF packages (EcorePackage, XMLTypePackage, XMLNamespacePackage) and standard resource factories (XMI, ecore, emof, binary)
- `createResourceSet()` returns a `ResourceSetImpl` with:
  - `DelegatingEPackageRegistry` wrapping the injected package registry
  - `DelegatingResourceFactoryRegistry` wrapping the injected factory registry
  - All registered `ResourceSetConfigurator`s applied

#### RegistryTrackingServiceComponent

Central event bus for registry property changes.

- Implements `RegistryTrackingService`
- Tracks all `EPackage.Registry` and `Resource.Factory.Registry` services
- When a tracked service's properties change, notifies all registered `RegistryPropertyListener`s
- Used by `DefaultResourceSetFactoryComponent` to react to registry changes

#### Other Core Components

| Component | Service | Purpose |
|-----------|---------|---------|
| `DefaultEOperationInvocationDelegateRegistryComponent` | -- | Manages `EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE` |
| `ResourceSetCacheComponent` | `ResourceSetCache` | Provides a cached (non-thread-safe) `ResourceSet`; requires ConfigAdmin |
| `ResourceSetUriHandlerConfiguratorComponent` | `ResourceSetConfigurator` | Configures URIConverter with custom `UriHandlerProvider`s and `UriMapProvider`s |
| `UriMapProviderComponent` | `UriMapProvider` | ConfigAdmin-driven URI-to-URI redirection maps |

### Configuration-Driven Components (full variant only)

These components enable creation of isolated EMF stacks via ConfigurationAdmin, where multiple independent `ResourceSetFactory` instances can coexist with separate registries.

#### IsolatedResourceFactoryConfiguration

Factory orchestrator: when activated with a `resourceSetFactoryName`, it programmatically creates three linked ConfigAdmin factory configurations:

```
IsolatedResourceFactoryConfiguration
  creates:
    ├── ConfigurationEPackageRegistryComponent        (EPackage.Registry)
    ├── ConfigurationResourceFactoryRegistryComponent  (Resource.Factory.Registry)
    └── ConfigurationResourceSetFactoryComponent       (ResourceSetFactory)
```

All three are wired together by the shared factory name, forming a complete isolated EMF stack.

#### ConfigurationEPackageRegistryComponent

ConfigAdmin-driven variant of `DefaultEPackageRegistryComponent`. Activated with a PID of `EPackageRegistry`.

#### ConfigurationResourceFactoryRegistryComponent

ConfigAdmin-driven variant that simply extends `ResourceFactoryRegistryImpl` and registers as `Resource.Factory.Registry`.

#### ConfigurationResourceSetFactoryComponent

ConfigAdmin-driven variant of the resource set factory. Merges ConfigAdmin properties into the service dictionary.

### Dynamic Model Loading (full variant only)

#### DynamicPackageLoader

Loads `.ecore` files at runtime from a configurable URI (via ConfigAdmin).

- `@Designate(ocd = DynamicEMFModel.class, factory = true)` -- metatype-driven
- On activation: loads the ecore file, registers `EPackage` and `EPackageConfigurator` as services
- On modification: re-registers if URI changed, updates properties otherwise
- On deactivation: unregisters and removes from `EPackage.Registry.INSTANCE`

Configuration attributes:
- `dynamicEcoreUri` (required) -- URL pointing to the `.ecore` file
- `feature` (optional) -- feature names for service properties
- `version` (optional) -- model version

## Internal Packages

### `org.eclipse.fennec.emf.osgi.ecore` -- EMF Bootstrap

**`EcorePackagesRegistrator`** -- Reference-counted static utility that bootstraps core EMF packages and resource factories as OSGi services:

| Service Type | Instances |
|-------------|-----------|
| `EPackage` | `EcorePackage`, `XMLTypePackage`, `XMLNamespacePackage` |
| `Resource.Factory` | XMI (`*`, `xmi`), Ecore (`ecore`), EMOF (`emof`), Binary (`bin`) |

**`FennecXMLResourceFactory`** -- Custom `XMLResourceFactoryImpl` pre-configured with `ExtendedMetaData`, schema location support, encoded attribute style, and lexical handler. Registered for `xml` extension and `application/xml` content type.

### `org.eclipse.fennec.emf.osgi.factory` -- Prototype Factory

**`ResourceSetPrototypeFactory`** -- Implements `PrototypeServiceFactory<ResourceSet>` so that each `getService()` call returns a fresh `ResourceSet` via `ResourceSetFactory.createResourceSet()`. On `ungetService()`, clears all resource contents.

### `org.eclipse.fennec.emf.osgi.provider` -- ResourceSetFactory Base

**`DefaultResourceSetFactory`** -- Abstract base class implementing `ResourceSetFactory`. Manages:
- `ServicePropertyContext` for property propagation
- Three `ServiceRegistration`s (ResourceSetFactory, ResourceSet prototype, Condition)
- `Set<ResourceSetConfigurator>` applied to each created ResourceSet
- `DelegatingEPackageRegistry` and `DelegatingResourceFactoryRegistry` wrapping

### `org.eclipse.fennec.emf.osgi.resourceset` -- Specialized ResourceSets (full variant only)

| Class | Description |
|-------|-------------|
| `HughDataResourceSetImpl` | Optimized for large datasets: suppresses EMF notifications, uses `ResourceLocator` cache |
| `HughDataResourceLocator` | Efficient cache for resource lookup, with bulk `clear()` |
| `HughDataResourceSetFactory` | Extends `DefaultResourceSetFactory`, creates `HughDataResourceSetImpl` |
| `SynchronizedResourceSetImpl` | Thread-safe `ResourceSet` using `ReentrantReadWriteLock` and `CopyOnWriteArrayList`; implements `Detachable` |

### `org.eclipse.fennec.emf.osgi.urihandler` -- HTTP URI Handler

**`RestfulURIHandlerImpl`** -- Handles `http://` and `https://` URIs with full CRUD:
- `createOutputStream` -- PUT (configurable via `OPTION_HTTP_METHOD`)
- `createInputStream` -- GET
- `delete` -- DELETE
- `exists` -- HEAD
- `getAttributes` -- OPTIONS + HEAD

Supports custom headers, response body loading, response logging, and configurable timeouts.

## Bundle Variant Comparison

| Feature | Full (`component`) | Minimal (`component.minimal`) |
|---------|-------------------|------------------------------|
| Core registries (EPackage, ResourceFactory) | Yes | Yes |
| DefaultResourceSetFactory | Yes | Yes |
| RegistryTrackingService | Yes | Yes |
| URI handler/map configuration | Yes | Yes |
| EcorePackagesRegistrator (bootstrap) | Yes | Yes |
| ConfigAdmin isolated factories | Yes | No |
| Dynamic ecore loading | Yes | No |
| HughDataResourceSet | Exported | Not available |
| SynchronizedResourceSet | Exported | Not available |

## Package Structure

```
org.eclipse.fennec.emf.osgi/
  src/
    org/eclipse/fennec/emf/osgi/
      components/                          -- Core DS components
        StaticEPackageRegistryComponent
        DefaultEPackageRegistryComponent
        DefaultResourceFactoryRegistryComponent
        DefaultResourceSetFactoryComponent
        RegistryTrackingServiceComponent
        DefaultEOperationInvocationDelegateRegistryComponent
        ResourceSetCacheComponent
        ResourceSetUriHandlerConfiguratorComponent
        RestUriHandlerProvider
        UriMapProviderComponent
        config/                            -- ConfigAdmin-driven variants (full only)
          IsolatedResourceFactoryConfiguration
          ConfigurationEPackageRegistryComponent
          ConfigurationResourceFactoryRegistryComponent
          ConfigurationResourceSetFactoryComponent
        dynamic/                           -- Runtime ecore loading (full only)
          DynamicPackageLoader
          DynamicPackageConfiguratorImpl
          DynamicEMFModel (annotation)
      ecore/                               -- EMF bootstrap
        EcorePackagesRegistrator
        FennecXMLResourceFactory
      factory/                             -- Prototype factory
        ResourceSetPrototypeFactory
      provider/                            -- ResourceSetFactory base (exported)
        DefaultResourceSetFactory
      resourceset/                         -- Specialized ResourceSets (full only)
        HughDataResourceSetImpl
        HughDataResourceLocator
        HughDataResourceSetFactory
        SynchronizedResourceSetImpl
      urihandler/                          -- HTTP URI handler
        RestfulURIHandlerImpl
  test/
    org/eclipse/fennec/emf/osgi/
      components/
        RegistryTrackingServiceComponentTest.java
        RegistryTrackingIntegrationTest.java
      helper/
        ServicePropertyContextTest.java
        SystemPropertyHelperTest.java
      provider/
        DelegatingEPackageRegistryTest.java
        DelegatingHashMapTest.java
```

## Testing

### Unit Tests

```bash
./gradlew :org.eclipse.fennec.emf.osgi:test
```

Tests cover: `RegistryTrackingServiceComponent` (with Mockito), `ServicePropertyContext`, `ServicePropertiesHelper`, `DelegatingEPackageRegistry`, `DelegatingHashMap`.

### Full Build

```bash
./gradlew build
```

Required when changing this module or the API, since both bundle variants repackage the API.

## Dependencies

| Dependency | Purpose |
|-----------|---------|
| `org.eclipse.fennec.emf.osgi.api` | Interfaces, constants, annotations, EcoreHelper |
| `org.eclipse.emf.ecore` | EMF core (EPackage, Resource, ResourceSet) |
| `org.eclipse.emf.ecore.xmi` | XMI/XML resource factories |
| `org.eclipse.emf.common` | EMF common (URI, etc.) |
| `org.osgi.framework` | OSGi framework API |
| `org.osgi.service.component` | Declarative Services |
| `org.osgi.service.cm` | ConfigurationAdmin (for config-driven components) |
| `org.osgi.service.condition` | OSGi Condition service |
| `org.osgi.util.converter` | OSGi Converter for property handling |
| `org.osgi.namespace.service` | Service namespace for capabilities |
| `org.osgi.namespace.extender` | Extender namespace constants |
| `org.osgi.namespace.implementation` | Implementation namespace |
| `biz.aQute.bnd.annotation` | BND annotations |
