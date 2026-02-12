# Plan: Integration Test Bundle for Gecko Compatibility Layer

## Context
The Gecko compatibility wrappers (5 DS components) bridge old `org.gecko.emf.osgi` services to/from new `org.eclipse.fennec.emf.osgi` services. We need an OSGi integration test bundle to verify these wrappers correctly delegate method calls, forward service properties, and handle service lifecycle (register/unregister).

## New Module
**Name**: `org.eclipse.fennec.emf.gecko.compatibility.itest`

Following the pattern of `org.eclipse.fennec.emf.osgi.itest.minimal`.

## Files to Create

### 1. `org.eclipse.fennec.emf.gecko.compatibility.itest/bnd.bnd`
```
Bundle-Name: EMF Gecko Compatibility Integration Tests
Bundle-Description: Integration tests for the Eclipse Fennec EMF Gecko compatibility layer

-baseline:
-library: \
	enableOSGi-Test
-buildpath: \
	org.eclipse.fennec.emf.osgi.api;version=snapshot,\
	org.eclipse.fennec.emf.gecko.compatibility.api;version=snapshot,\
	org.eclipse.emf.common;version=latest,\
	org.eclipse.emf.ecore;version=latest,\
	org.osgi.framework;version=latest
```

- `enableOSGi-Test` library adds JUnit 5, AssertJ, OSGi Test Framework deps
- `org.eclipse.fennec.emf.gecko.compatibility.api` provides both Gecko interfaces and Fennec API transitively
- `org.eclipse.emf.ecore` for `ResourceSetImpl`, `URIHandlerImpl`, `EPackage.Registry`
- No `-dependson` needed (buildpath handles ordering)

### 2. `org.eclipse.fennec.emf.gecko.compatibility.itest/build.gradle`
Identical to the minimal itest pattern — resolves test.bndrun and feeds it to testOSGi.

### 3. `org.eclipse.fennec.emf.gecko.compatibility.itest/test.bndrun`
```
-library: enableOSGi-Test
-runrequires: \
	bnd.identity;id='org.eclipse.fennec.emf.gecko.compatibility.itest',\
	bnd.identity;id='org.eclipse.fennec.emf.gecko.compatibility.api'
-runfw: org.apache.felix.framework;version='[7.0.5,7.0.5]'
-runee: JavaSE-17
```

- Requires the test bundle + the compatibility bundle (which brings wrappers + Gecko interfaces)
- `-runbundles` left empty for the BND resolver to compute
- No blacklist needed — we want the full Fennec API available
- No `-runvm` needed — no external test resources

### 4. Test Class: `GeckoCompatibilityWrapperTest.java`

**Package**: `org.eclipse.fennec.emf.gecko.compatibility.test`
**Path**: `org.eclipse.fennec.emf.gecko.compatibility.itest/src/org/eclipse/fennec/emf/gecko/compatibility/test/GeckoCompatibilityWrapperTest.java`

**Annotations**: `@ExtendWith(ServiceExtension.class)`, `@ExtendWith(BundleContextExtension.class)`, `@SuppressWarnings("deprecation")`
**No `@RequireEMF`** — we only need the compatibility wrappers active, not the full EMF OSGi stack.

**Helpers**:
- `waitForServiceReference(Class<T>, String filter, long timeout)` — polls `BundleContext.getServiceReferences()` until a matching service appears or timeout. Returns `ServiceReference<T>` (to access both service and properties).
- `waitForNoServiceReference(Class<T>, String filter, long timeout)` — polls until no matching services exist.

Both use `BundleContext.getServiceReferences()` (available in Java 8, no extra deps needed).

**Test Methods** (7 tests):

| Test | Direction | What it verifies |
|------|-----------|------------------|
| `testUriMapProviderGeckoToFennec` | Gecko→Fennec | Register Gecko `UriMapProvider`, verify Fennec service appears and `getUriMap()` delegates correctly |
| `testUriHandlerProviderGeckoToFennec` | Gecko→Fennec | Register Gecko `UriHandlerProvider`, verify Fennec service appears and `getURIHandler()` returns same object |
| `testEPackageConfiguratorGeckoToFennec` | Gecko→Fennec | Register Gecko `EPackageConfigurator`, verify both `configureEPackage()` and `unconfigureEPackage()` delegate via AtomicBoolean flags |
| `testResourceSetConfiguratorGeckoToFennec` | Gecko→Fennec | Register Gecko `ResourceSetConfigurator`, verify `configureResourceSet()` receives the same ResourceSet instance |
| `testResourceSetFactoryFennecToGecko` | Fennec→Gecko | Register Fennec `ResourceSetFactory`, verify Gecko wrapper delegates `createResourceSet()` and `getResourceSetConfigurators()` returns empty |
| `testPropertyForwarding` | Gecko→Fennec | Register service with custom properties, verify they appear on the wrapped service's ServiceReference |
| `testServiceRemoval` | Gecko→Fennec | Register, verify wrapper appears, unregister, verify wrapper disappears |

**Test approach per method**:
1. Create a test implementation of the old/new interface (lambda or anonymous class)
2. Register it via `bundleContext.registerService()` with a unique `test.id` property
3. Use `waitForServiceReference()` with filter `(test.id=xxx)` to find the wrapped service
4. Get the service, invoke methods, verify delegation
5. Unregister in `finally` block

**Java 8 constraints** (project-wide `javac.source: 1.8`):
- Use `Collections.singletonMap()` not `Map.of()`
- Use `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))` not `Set.of()`
- Lambdas and method references are fine

## Verification
1. `./gradlew :org.eclipse.fennec.emf.gecko.compatibility.itest:build` — compiles, resolves bundles, runs OSGi tests
2. All 7 tests should pass, verifying every wrapper component works correctly
