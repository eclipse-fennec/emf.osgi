# Compatibility Layer Session Notes

## Goal
Create compatibility wrapper components in `org.eclipse.fennec.emf.gecko.compatibility.api` that track old GeckoEMF (`org.gecko.emf.osgi`) services and re-register them under the new Fennec (`org.eclipse.fennec.emf.osgi`) interfaces with converted properties.

## What Was Done

### 1. UriHandlerProvider Wrapper Component (DONE)
**File**: `org.eclipse.fennec.emf.gecko.compatibility.api/src/org/eclipse/fennec/emf/gecko/compatibility/GeckoUriHandlerProviderWrapperComponent.java`

- DS Component that tracks `org.gecko.emf.osgi.UriHandlerProvider` (old, deprecated)
- Re-registers each as `org.eclipse.fennec.emf.osgi.UriHandlerProvider` (new Fennec API)
- Handles add/updated/remove lifecycle (dynamic policy, multiple cardinality)
- Forwards all service properties except framework-internal ones (service.id, objectClass, component.name, etc.)
- Uses `ConcurrentHashMap<old provider, ServiceRegistration<new provider>>` for tracking

### 2. bnd.bnd Updated (DONE)
- Added `org.eclipse.fennec.emf.osgi.api;version=snapshot` to `-buildpath`
- Added `-privatepackage: org.eclipse.fennec.emf.gecko.compatibility`

## What Could Be Done Next
Similar wrapper components for other old GeckoEMF interfaces:
- `org.gecko.emf.osgi.UriMapProvider` -> `org.eclipse.fennec.emf.osgi.UriMapProvider`
- `org.gecko.emf.osgi.configurator.EPackageConfigurator` -> `org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator`
- `org.gecko.emf.osgi.configurator.ResourceSetConfigurator` -> `org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator`
- `org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator` -> (annotation-based in new API, may need different approach)
- `org.gecko.emf.osgi.ResourceSetFactory` -> `org.eclipse.fennec.emf.osgi.ResourceSetFactory`

## Key Architecture Notes
- Both old and new `UriHandlerProvider` have identical method: `URIHandler getURIHandler()`
- Old interfaces are in package `org.gecko.emf.osgi`, marked `@Deprecated`
- New interfaces are in package `org.eclipse.fennec.emf.osgi`
- The project uses BND workspace with Gradle build
- Existing components use patterns like `SelfRegisteringServiceComponent` and `ServicePropertyContext` for dynamic property propagation
- Property prefix in new API: `emf.` (see `EMFNamespaces` constants)
