# Metadata Service

The metadata service keeps a pre-computed mirror tree for every EMF model it sees: one
`PackageMetadata` per model version, with `ClassMetadata`, `FeatureMetadata` and
`OperationMetadata` below it, plus whatever contributors attach. Consumers that would
otherwise walk `EPackage` structures on every request — codecs, mappers, validators, UI
builders — read from that tree instead and hang their own derived content off it.

This guide covers how to get a handle on it, in OSGi and on a flat classpath, and how to
contribute to it. The interfaces themselves are documented in their Javadoc; what follows is
the wiring.

---

## The two handles

| Interface | For | Comes from |
|---|---|---|
| `MetadataService` | reading | inject the service, or a whiteboard is one |
| `MetadataWhiteboard` | reading **and** lifecycle | inject the service, or `MetadataServices` |

`MetadataWhiteboard extends MetadataService`, and the implementation registers itself under
both. Inject the narrow one unless you actually register packages — a consumer that only reads
should not hold a handle that can unregister someone else's model.

## Identity is the fingerprint, not the nsURI

Everything below assumes this, so it is worth stating once: a model version is identified by
its [fingerprint](model-fingerprint-guide.md), and two packages published under one nsURI with
diverging content are **two** model versions with two separate trees. Lookups that start from
an `EPackage` instance are therefore exact; `getPackageMetadata(String nsURI)` cannot be and
answers with the most recently registered version. Use `getPackageMetadataVersions(nsURI)` when
that ambiguity matters.

This is also why a `FingerprintService` is a mandatory collaborator rather than a nicety: it
computes the key everything else is filed under.

---

## In OSGi

Nothing to wire. Declarative Services binds the mandatory `FingerprintService`, tracks every
`EPackage` service as it appears, and hands the whiteboard the index and handlers that are
registered as services:

```java
@Component
public class MyConsumer {

    @Reference
    private MetadataService metadata;   // read-only handle

    public void useIt(EPackage ePackage) {
        metadata.getPackageMetadata(ePackage)
                .ifPresent(tree -> ...);
    }
}
```

Registering a model is not something you do here — publishing the `EPackage` as a service is
enough, which is what the [code generator](code-generation-guide.md) and the
[model extender](../org.eclipse.fennec.emf.osgi.extender/readme.md) already arrange.

Two consequences of DS wiring worth knowing:

* **The component does not start without a `FingerprintService`.** The reference is mandatory
  and static. The only implementation shipped here is in the core implementation bundle, so one
  of `org.eclipse.fennec.emf.osgi.component` or `…component.minimal` has to be in the runtime.
* **Late arrivals lose nothing.** An index bound after packages are registered is populated
  from the registry; a handler added later gets `onPackageRegistered` replayed for every known
  model version, so its entries still land on trees that were built before it appeared.

---

## Outside OSGi

On a flat classpath — unit tests, a build-time tool, a plain-Java service — the same wiring has
to happen by hand. `MetadataServices` is where that knowledge lives, so callers never touch the
implementation class (it is in a private package and deliberately not exported):

```java
MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard();

whiteboard.registerPackage(MyPackage.eINSTANCE);

PackageMetadata tree = whiteboard.getPackageMetadata(MyPackage.eINSTANCE).orElseThrow();
```

What you get back is fully wired: the default `FingerprintService` is in place — the same
identity scheme the registry components emit their `emf.fingerprint` service property with, so
a whiteboard built here keys models exactly as the OSGi path would — and an in-memory index is
already bound, so URI and name lookups answer from the first registration.

Handlers passed to the factory are registered *before* the whiteboard is handed back, so they
see the very first `registerPackage` rather than only a replay:

```java
MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(new CodecHandler());
```

### Being explicit about model identity

Two overloads, same wiring, different amount of opinion:

```java
// the shipped default
MetadataServices.createWhiteboard(handlers);

// an explicit service, from the start
MetadataServices.createWhiteboard(myFingerprintService, handlers);
```

The default comes from `FingerprintHelper.getDefaultFingerprintService()` (see
[Model Fingerprints](model-fingerprint-guide.md#using-it-from-code)). It can also be replaced
afterwards, which is useful when the decision is made later than the construction:

```java
whiteboard.setFingerprintService(myFingerprintService);
```

Replacing discards fingerprints memoized from the previous service — which may have used a
different scheme — while trees already registered keep the identity they were filed under.
There is no unset counterpart: the collaborator is mandatory, so it is replaced, never
withdrawn. A `null` is ignored.

### A custom index

The default index is in-memory and per-whiteboard. To use a different one — persistent,
full-text, shared — bind it; it is populated from what is already registered:

```java
whiteboard.setMetadataIndex(new MyMetadataIndex());
```

### Two things to watch out for

**A bare `null` does not compile.** Both factory methods are varargs, so a lone `null` is
ambiguous between them. Say which one you mean:

```java
MetadataServices.createWhiteboard((FingerprintService) null);   // NullPointerException, as intended
```

**The metadata bundle imports `org.eclipse.fennec.emf.osgi.fingerprint.util`.** That is where
the default service comes from, and the package is exported by both core implementation
bundles. For a plain classpath it means the implementation jar has to be on it; inside OSGi it
resolves against either artifact with no manifest workaround. What it does *not* need is any
private package — a downstream bundle compiles against the metadata and API bundles only.

---

## Contributing to the tree

`MetadataHandler` is the single extension point. In OSGi, register one as a service; outside,
pass it to the factory or add it later. A contributor attaches its own content as an
`AspectEntry` under a type id of its own:

```java
public class CodecHandler implements MetadataHandler {

    @Override
    public void onPackageRegistered(PackageMetadata packageMetadata) {
        AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
        entry.setTypeId("codec");
        entry.setContent(myCodecPlan(packageMetadata));   // any EObject, your own model
        packageMetadata.getAspects().add(entry);
    }
}
```

Read it back through the aspect accessors, which exist at all four levels:

```java
metadata.getPackageAspect(ePackage, "codec")
        .map(AspectEntry::getContent)
        .ifPresent(...);
```

Because the content slot is a plain `EObject`, a contributor ships its content model in its own
bundle and the metadata model never learns the type.

**Timing.** `onPackageRegistered` runs while the tree is being built, *before* it is published
to lookups, the index and other readers — which is what makes contribution safe: nobody can
observe a model version whose entries are still missing. The price is that the callback must not
call back into `MetadataService` for the package being registered; it is not visible yet, and
the tree it is handed already holds everything. Handlers run inside the registration of an
`EPackage` service, so they must be quick and must not block.

---

## Persisting the state

`getRegistry()` returns the `MetadataRegistry` — the serializable root holding every tree,
keyed by fingerprint. It is an ordinary EMF resource root, so the whole state can be written
out and read back:

```java
Resource resource = resourceSet.createResource(URI.createFileURI("metadata.xmi"));
resource.getContents().add(metadata.getRegistry());
resource.save(null);
```

Useful for caching a build-time computation, replicating state across nodes, or inspecting what
a runtime actually knows.

---

## Related

- [Model Fingerprints](model-fingerprint-guide.md) — the identity everything here is keyed by,
  and the `FingerprintHelper` entry points
- [Configuration Guide](configuration-guide.md) — the model service properties the handlers
  receive as build context
- [Code Generation](code-generation-guide.md) — how models end up registered as services in the
  first place
