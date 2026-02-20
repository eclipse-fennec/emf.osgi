# Eclipse Fennec EMF - Multi-Runtime Concept

## Motivation

The core principle of Fennec EMF is **dynamic registration and deregistration** of EMF models as services. While this works well in OSGi, the same pattern is applicable in other runtime environments:

- **Plain Java** (e.g., CLI tools, serverless functions, unit tests)
- **Spring / Spring Boot** (enterprise applications)
- **Jakarta EE / CDI** (application servers)

This document analyzes the current codebase's OSGi coupling and outlines a strategy for multi-runtime support.

## Current State: OSGi Coupling Analysis

### API Module (`org.eclipse.fennec.emf.osgi.api`)

The API module is already well-separated. Most interfaces have **zero runtime OSGi dependency**:

| Class / Interface | Runtime OSGi Dependency | Notes |
|---|---|---|
| `EPackageConfigurator` | None | `@ProviderType` is compile-time only |
| `ResourceSetConfigurator` | None | `@ProviderType` is compile-time only |
| `ResourceFactoryConfigurator` | None | `@ProviderType` is compile-time only |
| `ResourceSetFactory` | None | `@ProviderType` is compile-time only |
| `EMFNamespaces` | None | Pure string constants |
| `DelegatingEPackageRegistry` | None | Pure EMF/Java |
| `DelegatingResourceFactoryRegistry` | None | Pure EMF/Java |
| `DelegatingHashMap` / `MapChangeListener` | None | Pure Java |
| `ServicePropertyContext` / `ServicePropertyContextImpl` | `org.osgi.framework` (API only) | `FrameworkUtil.asMap()`, `Constants.SERVICE_ID` |
| `ServicePropertiesHelper` | `org.osgi.framework` (API only) | `Constants.SERVICE_ID` |
| `RegistryPropertyListener` | None | Plain Java types (`Map`, `long`) |
| `RegistryTrackingService` | None | Plain Java types |
| OSGi annotations (`@EMFModel`, `@EMFConfigurator`, etc.) | Build-time only | `@ComponentPropertyType`, `@Capability` - processed by BND |

**Key insight**: The `org.osgi.framework` dependency in `ServicePropertyContextImpl` and `ServicePropertiesHelper` is the OSGi **specification API**, not an implementation. This is acceptable as a compile/runtime dependency - it is a lightweight API jar with no framework coupling.

### Core Implementation (`org.eclipse.fennec.emf.osgi`)

The implementation module is deeply OSGi-coupled. Every component uses DS annotations, `BundleContext`, and `ServiceRegistration`:

| Component | OSGi-Free Core Logic | OSGi-Coupled Mechanism |
|---|---|---|
| `StaticEPackageRegistryComponent` | EPackage.Registry map operations, DelegatingHashMap listener | `@Component`, `@Reference`, `registerService()`, `setProperties()` |
| `DefaultEPackageRegistryComponent` | EPackageConfigurator.configureEPackage() calls | `@Reference`, `ServiceReference` property tracking |
| `DefaultResourceFactoryRegistryComponent` | Map-put for extension/protocol/contentType | `@Reference`, `Converters.standardConverter()` |
| `DefaultResourceSetFactory` | `createResourceSet()` - pure EMF | Registration of 3 services, `PrototypeServiceFactory`, `Condition` |
| `ResourceSetCacheComponent` | `AtomicReference<ResourceSet>` caching | `@Component`, `@Reference` |
| `DynamicPackageLoader` | ecore loading, EPackage instantiation | `ConfigurationAdmin`, `ComponentServiceObjects` |
| `RegistryTrackingServiceComponent` | Listener notification dispatch | Entire purpose is OSGi service property observation |
| `EcorePackagesRegistrator` | Building property dictionaries | `registerService()` for built-in EMF packages |

### Extender (`org.eclipse.fennec.emf.osgi.extender`)

The extender is the most deeply OSGi-coupled module:

- `BundleTracker` / `BundleTrackerCustomizer` for watching bundles
- `BundleWiring` API for checking `Require-Capability` headers
- `Bundle.findEntries()` for classpath resource discovery
- Service registration using the **source bundle's** `BundleContext`

Only `ModelExtenderConfigurator` (implements `EPackageConfigurator`) is fully OSGi-free.

### Model Info (`org.eclipse.fennec.emf.osgi.model.info`)

- `EMFModelInfo` interface: OSGi-free at runtime
- `EMFModelInfoImpl`: Core hierarchy analysis logic is pure EMF/Java, only `@Reference` injection is OSGi-specific

## Proposed Architecture: SPI Abstraction Layer

```
┌─────────────────────────────────────────────────────────┐
│  fennec.emf.api (existing, already mostly OSGi-free)    │
│  EPackageConfigurator, ResourceSetFactory,               │
│  DelegatingEPackageRegistry, EMFNamespaces, ...          │
├─────────────────────────────────────────────────────────┤
│  fennec.emf.spi (NEW - runtime abstraction)             │
│  ServicePublisher, ConfiguratorTracker, ModelDiscoverer  │
├─────────────────────────────────────────────────────────┤
│  fennec.emf.core (NEW - extracted pure logic)           │
│  EPackageRegistryManager, ResourceFactoryRegistryMgr,    │
│  CoreResourceSetFactory, ModelInfoAnalyzer               │
├───────────┬───────────────┬─────────────────────────────┤
│  OSGi     │  Spring       │  Plain Java                 │
│  (DS)     │  (Beans)      │  (ServiceLoader)            │
│  existing │  new module   │  new module                 │
└───────────┴───────────────┴─────────────────────────────┘
```

### SPI Interfaces

```java
/**
 * Abstracts OSGi ServiceRegistration / Spring Bean registration.
 * Allows the core to publish services without knowing the runtime.
 */
public interface ServicePublisher<T> {
    void register(Class<T> type, T service, Map<String, Object> properties);
    void updateProperties(Map<String, Object> properties);
    void unregister();
}

/**
 * Abstracts OSGi @Reference(cardinality=MULTIPLE, policy=DYNAMIC).
 * Receives callbacks when configurators appear/disappear.
 */
public interface ConfiguratorTracker<T> {
    void onAdd(T configurator, Map<String, Object> properties);
    void onRemove(T configurator);
    void onModified(T configurator, Map<String, Object> properties);
}

/**
 * Abstracts the OSGi extender pattern / ServiceLoader discovery.
 * Discovers EMF models from the runtime environment.
 */
public interface ModelDiscoverer {
    void start(ModelCallback callback);
    void stop();

    interface ModelCallback {
        void onModelFound(EPackageConfigurator configurator, Map<String, Object> properties);
        void onModelRemoved(EPackageConfigurator configurator);
    }
}
```

## Property Handling Across Runtimes

The dynamic property propagation is the most OSGi-specific feature. Each runtime handles it differently:

### OSGi (current implementation)

Properties flow dynamically through the service registry:

```
EPackageConfigurator registered with { emf.name=basic, emf.nsUri=... }
  → StaticEPackageRegistryComponent.setProperties({ emf.name=[basic,...] })
    → ResourceSetFactory.setProperties({ emf.name=[basic,...] })
      → Consumer filters: @Reference(target="(emf.name=basic)")
```

- Properties are **live-updated** via `ServiceRegistration.setProperties()`
- Consumers use **LDAP filter expressions** for selection
- `ServicePropertyContext` aggregates properties from multiple sources
- The `RegistryTrackingService` notifies when registry properties change

### Spring

Spring does not have dynamic service properties. Alternative approaches:

**Static approach (sufficient for most cases):**

In Spring, all beans are known at startup. The dynamic property propagation simplifies to a one-time aggregation:

```java
@Configuration
public class FennecEMFAutoConfiguration {

    @Bean
    public EPackageRegistryManager ePackageRegistry(
            List<EPackageConfigurator> configurators) {
        // All configurators are known at startup
        var manager = new EPackageRegistryManager();
        configurators.forEach(c -> manager.addConfigurator(c));
        return manager;
    }

    @Bean
    @Scope("prototype")
    public ResourceSet resourceSet(ResourceSetFactory factory) {
        return factory.createResourceSet();
    }
}
```

**Qualifier-based selection (replaces LDAP filters):**

```java
// Registration via custom annotation
@EPackageModel(name = "basic", nsURI = "http://example.org/basic")
@Component
public class BasicEPackageConfigurator implements EPackageConfigurator { ... }

// Consumption with qualifier
@Autowired
@ForModel(name = "basic")
private ResourceSet resourceSet;
```

**Event-based dynamic approach (for hot-reload scenarios):**

```java
@Component
public class SpringPropertyRegistry {
    private final ApplicationEventPublisher events;
    private final Map<String, Set<String>> aggregatedProperties = new ConcurrentHashMap<>();

    public void onConfiguratorAdded(EPackageConfigurator c, Map<String, Object> props) {
        // Aggregate properties
        mergeProperties(props);
        events.publishEvent(new ModelRegistryChangedEvent(aggregatedProperties));
    }
}
```

### Plain Java

In plain Java without a service registry, properties are typically static configuration:

```java
// Manual wiring
var registry = new EPackageRegistryManager();
registry.addConfigurator(new BasicEPackageConfigurator(),
    Map.of("emf.name", "basic", "emf.nsUri", "http://example.org/basic"));

var factory = new CoreResourceSetFactory(registry);
ResourceSet rs = factory.createResourceSet();
```

Properties are passed explicitly at registration time. There is no dynamic propagation because there is no service registry to propagate to.

## ServiceLoader as Extender Alternative

A `ServiceLoader`-based model discoverer makes sense as the plain-Java equivalent of the OSGi extender:

### How It Would Work

```
META-INF/services/org.eclipse.fennec.emf.osgi.api.EPackageConfigurator
  com.example.model.basic.BasicEPackageConfigurator
  com.example.model.extended.ExtendedEPackageConfigurator
```

```java
public class ServiceLoaderModelDiscoverer implements ModelDiscoverer {

    @Override
    public void start(ModelCallback callback) {
        ServiceLoader.load(EPackageConfigurator.class).forEach(configurator -> {
            Map<String, Object> properties = extractProperties(configurator);
            callback.onModelFound(configurator, properties);
        });
    }

    private Map<String, Object> extractProperties(EPackageConfigurator configurator) {
        // Extract from @EMFModel annotation if present
        EMFModel annotation = configurator.getClass().getAnnotation(EMFModel.class);
        if (annotation != null) {
            return Map.of(
                EMFNamespaces.EMF_MODEL_NAME, annotation.name(),
                EMFNamespaces.EMF_MODEL_NSURI, annotation.nsURI()
            );
        }
        return Map.of();
    }
}
```

### Comparison: OSGi Extender vs. ServiceLoader Discoverer

| Aspect | OSGi Extender | ServiceLoader Discoverer |
|---|---|---|
| Discovery mechanism | `BundleTracker` + `Require-Capability` header | `META-INF/services` files |
| Dynamic add/remove | Yes (bundle install/uninstall) | No (classpath is fixed) |
| Ecore file scanning | Yes (`Bundle.findEntries("model/", "*.ecore")`) | No (only compiled configurators) |
| Property source | Bundle manifest headers | `@EMFModel` annotation on configurator class |
| Dependencies | `org.osgi.framework`, `org.osgi.util.tracker` | None (pure Java SE) |
| Hot deploy | Yes | No |
| Suitable for | OSGi runtimes, dynamic environments | CLI, tests, serverless, embedded |

### Code Generator Changes

The existing codegen (`org.eclipse.fennec.emf.osgi.codegen`) generates OSGi DS component XML. For ServiceLoader support, it would additionally generate:

- `META-INF/services/org.eclipse.fennec.emf.osgi.api.EPackageConfigurator` entries
- Annotated configurator classes with `@EMFModel(name=..., nsURI=...)` for property extraction

Both outputs can coexist - a generated model bundle would work in OSGi (via DS) AND plain Java (via ServiceLoader) simultaneously.

## Implementation Roadmap

### Phase 1: SPI Definition (non-breaking)

- Define `ServicePublisher`, `ConfiguratorTracker`, `ModelDiscoverer` interfaces in a new `fennec.emf.spi` module
- No changes to existing code

### Phase 2: Core Logic Extraction (refactoring)

- Extract OSGi-free logic from registry components into `fennec.emf.core`
- Refactor existing OSGi components to delegate to core classes
- Existing OSGi behavior remains identical

### Phase 3: ServiceLoader Module (new module)

- Implement `ServiceLoaderModelDiscoverer`
- Implement plain-Java wiring (manual `EPackageRegistryManager` setup)
- Extend codegen to generate `META-INF/services` files

### Phase 4: Spring Module (new module)

- Implement Spring Auto-Configuration
- `@Conditional` support for selective model loading
- Spring Boot starter artifact

### Phase 5: Property Handling Unification

- Define a `PropertyProvider` interface that works across runtimes
- OSGi: backed by `ServiceReference` properties
- Spring: backed by `@EMFModel` annotation + `Environment`
- Plain Java: backed by `Map<String, Object>` passed at registration
