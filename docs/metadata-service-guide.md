# Metadata Service

The metadata service keeps a pre-computed mirror tree for every EMF model it sees: one
`PackageMetadata` per model version, with `ClassMetadata`, `FeatureMetadata` and
`OperationMetadata` below it, plus whatever contributors attach. Consumers that would
otherwise walk `EPackage` structures on every request — codecs, mappers, validators, UI
builders — read from that tree instead and hang their own derived content off it.

This guide covers why you would use it, how to get a handle on it in OSGi and on a flat
classpath, and how to contribute to it. The interfaces themselves are documented in their
Javadoc; what follows is the wiring.

---

## Why derive anything at all

An Ecore model says what the data *is*. Almost every real system needs something else about
that same model: how to serialize it, how to map it to tables, which constraints hold, how to
expose it over a protocol. Encoding that into the domain model pollutes it, costs annotation
parsing on every call, and is impossible when the Ecore belongs to someone else.

So the information is attached *beside* the model instead of inside it. When a model arrives,
a contributor derives what it needs — once — and hangs the result on the mirror tree. Two
properties make that worth doing:

* **The derived content is orthogonal.** It adds a dimension without the domain model knowing,
  and each concern is independent of the others. A new one never touches an existing one.
* **The expensive part happens once.** Annotation parsing, constraint compilation, mapping
  inference run at registration, not on every serialization or database write. The result is a
  plain `EObject`, so it is type-safe, navigable, serializable and cacheable.

The concerns this was built for: a **constraint cache** that holds pre-compiled OCL instead of
re-parsing annotation strings on every validation; an **ORM mapping** for `emf.persistence-jpa`
that resolves classes to tables and references to joins without putting database concerns in
the Ecore; a **protocol mapping** such as the SensiNact one in `emf.util`, which consumes a
resolved structure rather than re-interpreting Ecore at runtime.

Because every derived artifact is an ordinary EMF model, it can also be *held* elsewhere —
persisted and versioned in an EObject registry such as the
[Fennec Model Atlas](https://github.com/eclipse-fennec/model.atlas), then re-loaded by any
runtime that needs it. Deriving and holding are separate jobs; this service does the first and
gives you the fingerprint that makes the second safe.

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

**One model version can arrive as several Java instances.** A generated `EPackage` and the
same model loaded from its `.ecore` produce the same fingerprint by design — the equivalence
gate asserts it — so registering both deduplicates onto one tree. Element lookups
(`getClassMetadata`, `getFeatureMetadata`, `getOperationMetadata`, and the `get…Aspect` calls
on top of them) are keyed by instance for speed, but instance identity is only a cache key:
an `EClass` of a second, structurally equal instance resolves to the shared tree instead of to
nothing. The correspondence is exact, not a guess — classifier and feature names are unique
within their scope, and operations correspond by declared position, which equal fingerprints
guarantee. Diverging instances are unaffected: they are separate versions and each keeps
resolving to its own.

### Push and pull are both first-class

There are two ways a tree comes into existence, and the difference matters for anything that
holds one:

**Push** — `registerPackage`, which is what the whiteboard does when an `EPackage` service
appears. It takes a per-version liveness count, so registering identical content twice
deduplicates onto one tree with a count of two, and `unregisterPackage` drops the tree only on
the last withdrawal. Unbinding one version of an nsURI therefore never removes another live
version of it.

**Pull** — `getPackageMetadata(EPackage)` builds the tree on the spot if that content is
unknown. **No prior registration is needed.** A consumer that is handed a concrete `EPackage`
instance — a serializer invoked with a model, a validator, a mapper — does not have to care
whether anyone registered it; it asks with the instance it holds and gets exactly that version.
Trees created this way carry no liveness count and no unbind ever evicts them: they are cached
reads, not registrations.

Which means the whiteboard is a warm-up optimization, not a precondition. It pre-builds so the
first real call pays nothing.

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

### Build context

The OSGi service properties of the `EPackage` service reach the handler on the tree itself,
stringified:

```java
if ("false".equals(packageMetadata.getProperties().get("codec.enabled"))) {
    return;                                    // this model is not ours to derive for
}
```

Everything the [configuration guide](configuration-guide.md) lists is available here, which is
what lets a contributor decide whether a model is relevant to it at all, or pick up a
deployment-specific setting. Two limits: the map is **transient** — it is build context, not
part of the model version, so it is not written out when the registry is saved (see below) — and
an `emf.fingerprint` among those properties is context too, never identity. The key the tree is
filed under is always computed locally.

**Timing.** `onPackageRegistered` runs while the tree is being built, *before* it is published
to lookups, the index and other readers — which is what makes contribution safe: nobody can
observe a model version whose entries are still missing. The price is that the callback must not
call back into `MetadataService` for the package being registered; it is not visible yet, and
the tree it is handed already holds everything. Handlers run inside the registration of an
`EPackage` service, so they must be quick and must not block.

---

## Reusing an expensive derivation

Some contributions are cheap enough to redo on every start. Compiling constraints, inferring a
mapping or building an index is not, and nobody wants to repeat it on every node. `ArtifactStore`
is the seam for that — a two-method durable store, keyed by fingerprint and type id:

```java
Optional<EObject> resolve(String fingerprint, String typeId);
void put(String fingerprint, String typeId, EObject artifact);
```

The handler does *resolve-or-build* against it. On a hit nothing is derived; on a miss the work
runs once and the result is stored:

```java
public class OclCacheHandler implements MetadataHandler {

    private final ArtifactStore store;   // injected, or InMemoryArtifactStore

    @Override
    public void onPackageRegistered(PackageMetadata packageMetadata) {
        String fingerprint = packageMetadata.getModelFingerprint();
        EObject constraints = store.resolve(fingerprint, "ocl")
                .orElseGet(() -> {
                    EObject built = compileConstraints(packageMetadata);   // the expensive part
                    store.put(fingerprint, "ocl", built);
                    return built;
                });

        AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
        entry.setTypeId("ocl");
        entry.setContent(constraints);
        packageMetadata.getAspects().add(entry);
    }
}
```

**The fingerprint is what makes the reuse safe.** It changes exactly when the model content
changes, so an artifact is never served to a version it was not derived from — two diverging
versions of one nsURI get two artifacts, and identical content on two nodes shares one. Keying
by nsURI here would reintroduce the very bug fingerprints exist to prevent.

Three things to know about the arrangement:

* **The store is the contributor's, not the framework's.** Nothing in the metadata service binds
  an `ArtifactStore` or calls it for you; the handler owns the decision, which is what keeps the
  service independent of any particular backend. `InMemoryArtifactStore` is shipped as the
  default; a durable one can sit on the Model Atlas, a filesystem, or anything else.
* **Unregistering does not evict.** Dropping a model version is local to the node. The stored
  artifact stays, because another node may still be using it — cleanup is the store's business,
  not the whiteboard's.
* **Derivation settings belong in the key.** If *how* you derive can change — an engine version,
  a configuration — key by
  `fingerprintService.fingerprint(ePackage, "ocl", "engine-1.2")` instead, so a changed
  derivation produces a new key for unchanged Ecore. See
  [derivation inputs](model-fingerprint-guide.md#derivation-inputs).

---

## Persisting and restoring the state

`getRegistry()` returns the `MetadataRegistry` — the serializable root holding every tree, keyed
by fingerprint. It is an ordinary EMF resource root, so the whole state can be written out:

```java
Resource resource = resourceSet.createResource(URI.createFileURI("metadata.xmi"));
resource.getContents().add(metadata.getRegistry());
resource.save(null);
```

and taken over again on the next start, or on another node:

```java
Resource resource = resourceSet.getResource(URI.createFileURI("metadata.xmi"), true);
List<PackageMetadata> adopted =
        whiteboard.loadRegistry((MetadataRegistry) resource.getContents().get(0));
```

Whereas `ArtifactStore` caches one contributor's artifact, this carries the whole state —
mirror trees and every contribution on them — so a build-time computation can be shipped and
adopted instead of repeated.

`loadRegistry` adopts rather than trusts, and the return value is the list of what it actually
took over:

* **A stale tree is refused.** If the saved `modelFingerprint` contradicts what the referenced
  `EPackage` hashes to now, the model moved on since the file was written and the tree is left
  behind. Verification needs a resolvable package and a bound `FingerprintService`; where either
  is missing — an offline inspection, say — the stored key is taken as stated.
* **A live tree wins.** A model version already built in this runtime is not replaced by a
  stored one.
* **Adoption is a move**, so the argument is left holding only the entries that were skipped, and
  adopted trees take no liveness count — no unbind evicts them.
* **Handlers are not re-run**, since their output is part of what was saved. A handler added
  *after* a load does get replayed over the adopted trees like over any other, so a contributor
  that cannot tolerate a tree already carrying its entries should check for its own type id
  first.

Two things do not survive the round trip: the transient build-context properties described
above, and any reference into a model the reading `ResourceSet` cannot resolve. An adopted tree
whose `EPackage` did not resolve stays reachable by fingerprint, nsURI and through the index, but
not by `EClass` or `EStructuralFeature` instance — there is no live instance to key it under.

---

## Related

- [Model Fingerprints](model-fingerprint-guide.md) — the identity everything here is keyed by,
  and the `FingerprintHelper` entry points
- [Configuration Guide](configuration-guide.md) — the model service properties the handlers
  receive as build context
- [Code Generation](code-generation-guide.md) — how models end up registered as services in the
  first place
