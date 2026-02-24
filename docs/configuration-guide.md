# Configuration Guide — EMF OSGi Components

This guide explains how to configure Fennec EMF OSGi components via OSGi Configuration Admin. Configuration-based setup is one of three ways to register EMF models in OSGi — alongside the code generator and the extender.

---

## When to Use Configuration-Based Components

| Approach | Best For |
|---|---|
| **Code Generator** | Generated model code with compile-time EPackage/EFactory registration |
| **Extender** | Auto-discovery of models from bundle manifests without code changes |
| **Configuration Admin** | Runtime provisioning of isolated EMF stacks; multi-tenant or dynamically scoped ResourceSets |

Configuration-based components are particularly useful when you need **multiple isolated EMF stacks** at runtime — each with its own EPackage registry, ResourceFactory registry, and ResourceSetFactory — without writing any Java code.

---

## Quick Start: Isolated ResourceSet Factory

The most common use case is creating a fully isolated, named EMF stack with a single configuration. The `IsolatedResourceSetFactory` is a meta-configurator that automatically creates and wires three sub-components for you.

### Example: OSGi Configurator JSON

```json
{
    ":configurator:resource-version": 1,
    ":configurator:symbolicname": "com.example.myapp",
    ":configurator:version": "1.0.0",
    "IsolatedResourceSetFactory~myapp": {
        "rsf.name": "myapp"
    }
}
```

This single configuration creates:

1. An **EPackageRegistry** named `myapp` — tracks all `EPackageConfigurator` services (except Ecore)
2. A **ResourceFactoryRegistry** named `myapp` — provides resource factories
3. A **ResourceSetFactory** named `myapp` — wired to the two registries above

You can now inject the isolated factory by targeting the name:

```java
@Reference(target = "(rsf.name=myapp)")
private ResourceSetFactory resourceSetFactory;
```

### Filtering Models

To restrict which models the isolated stack sees, set the `rsf.model.target.filter` property:

```json
"IsolatedResourceSetFactory~myapp": {
    "rsf.name": "myapp",
    "rsf.model.target.filter": "(emf.name=mymodel)"
}
```

The Ecore model is always excluded automatically from the filter.

---

## Component Reference

### IsolatedResourceSetFactory

Meta-configurator that creates a complete isolated EMF stack (EPackageRegistry + ResourceFactoryRegistry + ResourceSetFactory) from a single configuration.

| | |
|---|---|
| **Factory PID** | `IsolatedResourceSetFactory` |
| **OCD Name** | EMF Isolated ResourceSet Factory |
| **Service** | *(none — delegates to sub-configurations)* |

#### Properties

| Property | Type | Required | Default | Description |
|---|---|---|---|---|
| `rsf.name` | `String` | **Yes** | — | Name of the isolated factory. Links all three sub-components via target filters. |
| `rsf.model.target.filter` | `String` | No | `(emf.name=*)` | LDAP filter selecting which `EPackageConfigurator` services the EPackage registry tracks. Ecore is always excluded. |

#### Example

```json
"IsolatedResourceSetFactory~inventory": {
    "rsf.name": "inventory",
    "rsf.model.target.filter": "(emf.name=inventory)"
}
```

---

### EPackageRegistry

A dedicated `EPackage.Registry` instance that tracks `EPackageConfigurator` services and delegates failed lookups to a parent registry.

| | |
|---|---|
| **Factory PID** | `EPackageRegistry` |
| **OCD Name** | EMF EPackage Registry |
| **Service** | `org.eclipse.emf.ecore.EPackage.Registry` |

#### Properties

| Property | Type | Required | Default | Description |
|---|---|---|---|---|
| `rsf.name` | `String` | **Yes** | — | Name of the resource set factory this registry belongs to. Used for target filter matching. |
| `ePackageConfigurator.target` | `String` | No | `(emf.model.scope=resourceset)` | LDAP target filter for the `EPackageConfigurator` services to track. |
| `parentRegistry.target` | `String` | No | `(default.resourceset.epackage.registry=true)` | LDAP target filter for the parent `EPackage.Registry` used as fallback for failed lookups. |

#### Example

```json
"EPackageRegistry~myapp": {
    "rsf.name": "myapp",
    "ePackageConfigurator.target": "(emf.model.scope=resourceset)"
}
```

---

### ResourceFactoryRegistry

A dedicated `Resource.Factory.Registry` instance for a specific resource set factory.

| | |
|---|---|
| **Factory PID** | `ResourceFactoryRegistry` |
| **OCD Name** | EMF Resource Factory Registry |
| **Service** | `org.eclipse.emf.ecore.resource.Resource.Factory.Registry` |

#### Properties

| Property | Type | Required | Default | Description |
|---|---|---|---|---|
| `rsf.name` | `String` | No | `""` | Name of the resource set factory this registry belongs to. Used for target filter matching. |

#### Example

```json
"ResourceFactoryRegistry~myapp": {
    "rsf.name": "myapp"
}
```

---

### ResourceSetFactory

A dedicated `ResourceSetFactory` instance wired to specific EPackage and Resource.Factory registries.

| | |
|---|---|
| **Factory PID** | `ResourceSetFactory` |
| **OCD Name** | EMF ResourceSet Factory |
| **Service** | `ResourceSetFactory`, `ResourceSet` (prototype) |

#### Properties

| Property | Type | Required | Default | Description |
|---|---|---|---|---|
| `ePackageRegistry.target` | `String` | No | `(emf.model.scope=resourceset)` | LDAP target filter for the `EPackage.Registry` service to use. |
| `resourceFactoryRegistry.target` | `String` | No | `""` | LDAP target filter for the `Resource.Factory.Registry` service to use. |

#### Example

```json
"ResourceSetFactory~myapp": {
    "ePackageRegistry.target": "(rsf.name=myapp)",
    "resourceFactoryRegistry.target": "(rsf.name=myapp)"
}
```

---

### ResourceSetCache

Caches a single `ResourceSet` instance created by a targeted `ResourceSetFactory`. Useful when multiple consumers need the same pre-configured `ResourceSet`.

| | |
|---|---|
| **Factory PID** | `ResourceSetCache` |
| **OCD Name** | EMF ResourceSet Cache |
| **Service** | `ResourceSetCache` |

#### Properties

| Property | Type | Required | Default | Description |
|---|---|---|---|---|
| `resourceSetFactory.target` | `String` | No | `""` | LDAP target filter for the `ResourceSetFactory` service to use for creating the cached `ResourceSet`. |

#### Example

```json
"ResourceSetCache~myapp": {
    "resourceSetFactory.target": "(rsf.name=myapp)"
}
```

Usage:

```java
@Reference(target = "(resourceSetFactory.target=*myapp*)")
private ResourceSetCache cache;

ResourceSet rs = cache.getResourceSet(); // always the same instance
```

---

### UriMapProvider

Provides URI-to-URI mappings for EMF resource resolution. Source URIs are redirected to destination URIs when resolving resources.

| | |
|---|---|
| **Factory PID** | `DefaultUriMapProvider` |
| **OCD Name** | EMF URI Map Provider |
| **Service** | `UriMapProvider` |

#### Properties

| Property | Type | Required | Default | Description |
|---|---|---|---|---|
| `uri.map.src` | `String` | **Yes** | — | Comma-separated list of source URIs to map from. |
| `uri.map.dest` | `String` | **Yes** | — | Comma-separated list of destination URIs to map to. Must have the same number of entries as source URIs. |

#### Example

```json
"DefaultUriMapProvider~platform-redirect": {
    "uri.map.src": "platform:/plugin/com.example/model/example.ecore",
    "uri.map.dest": "http://example.org/model/example.ecore"
}
```

Multiple mappings:

```json
"DefaultUriMapProvider~multi": {
    "uri.map.src": "platform:/plugin/a/model/a.ecore,platform:/plugin/b/model/b.ecore",
    "uri.map.dest": "http://example.org/a.ecore,http://example.org/b.ecore"
}
```

---

## How They Wire Together

The configuration-based components use a **naming convention** to link together. The key property is `rsf.name` — a simple string that acts as a correlation identifier.

### The `rsf.name` Linking Pattern

```
                     rsf.name = "myapp"
                           |
           +---------------+---------------+
           |               |               |
    EPackageRegistry  ResourceFactory   ResourceSet
      ~myapp          Registry~myapp    Factory~myapp
           |               |               |
           |               |    ePackageRegistry.target =
           |               |      "(rsf.name=myapp)"
           |               |    resourceFactoryRegistry.target =
           |               |      "(rsf.name=myapp)"
           +-------+-------+---------------+
                   |
            ResourceSetFactory
              finds registries
              by rsf.name match
```

1. **EPackageRegistry** and **ResourceFactoryRegistry** each publish a service with `rsf.name=<name>` in their service properties.
2. **ResourceSetFactory** uses target filters on its registry references — `(rsf.name=<name>)` — to bind to the correct registries.
3. **IsolatedResourceSetFactory** automates all of this: given a name, it creates the three sub-configurations with matching `rsf.name` values and wires the target filters automatically.

### Manual vs. Automated Wiring

| Approach | Configs Needed | Use Case |
|---|---|---|
| `IsolatedResourceSetFactory` | **1** | Standard isolated stack — one config does it all |
| Manual (EPackageRegistry + ResourceFactoryRegistry + ResourceSetFactory) | **3** | Advanced: custom target filters, shared registries, or non-standard wiring |

For most use cases, `IsolatedResourceSetFactory` is the recommended entry point. Use the individual components only when you need fine-grained control over registry sharing or model scoping.
