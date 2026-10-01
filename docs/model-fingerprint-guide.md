# Model Fingerprints

A model fingerprint identifies **a version of a model by its content**. Fennec computes one
for every `EPackage` it registers and publishes it as the `emf.fingerprint` service property,
as a constant in generated code, and as an attribute in the bundle manifest.

This guide explains what the value means, what it guarantees, and how to use it.

## Why the nsURI is not enough

EMF identifies a model by its namespace URI. That works as long as one nsURI means one model —
and it stops working the moment it does not:

- Two bundles publish the same nsURI with **diverging content**, because a model went through
  a review stage, a branch, or a hotfix without a version bump.
- A document written years ago has to be read back today, and nobody recorded *which* version
  of the nsURI wrote it.
- A derived artifact — a mapping, a compiled expression, an index — was built for one model
  version and must not be reused for another.

In all three cases the nsURI is the wrong key: it does not change when the model changes. The
fingerprint does. It is derived from the model's content, so it changes exactly when the
content changes and stays the same when nothing relevant did.

```
emf.nsURI       = http://example.org/person/1.0   ← names the model
emf.fingerprint = fp1:14466a0b5de879a6…           ← names this version of it
```

## The value

```
<scheme>:<digest>
```

Currently one scheme exists: `fp1`, a SHA-256 over a canonical textual form of the package.
The prefix is not decoration — it is what makes the value future-proof. **A change to how the
canonical form is built is always a new scheme tag, never a changed `fp1`.** Values already
stored by consumers therefore keep their meaning forever; a fingerprint that silently changed
meaning would be worse than none at all.

### Guarantees

**Reproducible.** The same model content yields the same value on every node, in every JVM,
independent of object identity, registration order or where the model came from.

**Representation independent.** A model loaded from an `.ecore` file and the same model as
generated Java code produce the *same* fingerprint. This is enforced by a permanent test over
the workspace corpus (`FingerprintEquivalenceGateTest`), not merely intended — without it, the
same model would have two identities depending on how it happened to be loaded.

**Identifying.** Structurally different content yields a different value. Two packages under
one nsURI carry two distinct fingerprints, including when they differ only in a generic type
argument (`Box<String>` vs `Box<Integer>`).

**Computed, never trusted.** The framework always computes the value itself at registration
time. An `emf.fingerprint` property arriving with a service is treated as build context, never
adopted — otherwise a wrong or malicious value could make one model impersonate another.

### What goes into the hash

| Counts | Ignored |
|---|---|
| Classifiers, features, operations, parameters, supertypes, type parameters, enum literals | Order of classifiers within the package (sorted by name) |
| Declared order of features, operations, parameters, supertypes, type parameters, literals | `documentation` details, under every annotation source |
| Types, cardinalities, containment, defaults, IDs, generics | GenModel tooling configuration (`basePackage`, `complianceLevel`, …) |
| Behaviour-relevant annotations: `ExtendedMetaData`, delegates, mappings | Annotations left empty after filtering |

The rule behind the two columns: an annotation is fingerprint-relevant when it affects
structure, validation, serialization, mapping or delegates. It is irrelevant when it only
configures how Java is generated from the model — that is tooling, and the same model built
with a different base package is still the same model.

## Models that reference other models

Real models rarely come alone. A persistence model references an entity model, both reference
`XMLType`, and the Ecore editor writes those references as **relative paths** between the files:

```xml
<eStructuralFeatures xsi:type="ecore:EReference" name="owner"
    eType="ecore:EClass ../../com.example.person/model/person.ecore#//Person"/>
```

The same model as generated code has no files at all — `PersistencePackage` simply holds a Java
reference to `PersonPackage.Literals.PERSON`. Both representations must produce one fingerprint,
so the way a cross-package reference enters the hash has to be independent of how the file
happened to spell it.

### What enters the hash

A reference to a classifier in another package contributes exactly one token: the target's
**nsURI and name**, `http://example.org/person/1.0#Person`. Neither the path in the file nor the
content of the target enters. Two consequences follow:

- **A fingerprint identifies one package, not the closure.** Changing `Person` does not change
  the fingerprint of the persistence model; only renaming it, or moving it to another nsURI,
  does. Consumers that need "the whole model" identify each package separately.
- **The path is spelling, not identity.** `../person.ecore#//Person`,
  `platform:/resource/…/person.ecore#//Person` and `http://example.org/person/1.0#//Person`
  all denote the same classifier and yield the same token — *provided the reference could be
  resolved*.

### Resolved and unresolved references

While a reference is resolved, the token is read from the target: its package's nsURI and its
name. While it is still an unresolved proxy, there is no target to read from, so the token is
read from the **proxy URI** — the address the file gave, made absolute at load time. That is
where the two ways of addressing part:

| Reference in the `.ecore` | unresolved | resolved |
|---|---|---|
| by nsURI — `http://example.org/person/1.0#//Person` | `http://example.org/person/1.0#Person` | `http://example.org/person/1.0#Person` |
| by location — `../person.ecore#//Person` | `file:/home/ci/ws/person.ecore#Person` | `http://example.org/person/1.0#Person` |

A reference **by nsURI** hashes identically whether or not the target was available: the proxy
URI already *is* the nsURI. A reference **by location** hashes identically only once resolved.
Unresolved, it carries the absolute document URI — a filesystem path, or inside OSGi a
bundle-entry URL (`bundleentry://…`, `bundle://…`) that contains the bundle id — and the value
then depends on where the file was loaded from. It is still deterministic, still never `null`, but it is not the value the
generated code has, and it is not the value the next installation of the same bundle gets.

The fingerprint itself never loads anything. Whether a reference is resolved at the moment the
value is computed is decided by the path that loads the model — and the three paths below behave
differently.

### The code generator

The generator resolves the complete model before hashing: it calls `EcoreUtil.resolveAll` on the
ResourceSet that loaded the genmodel, where referenced files are found via `usedGenPackages`, the
`-buildpath` and the workspace (see
[Referencing Models from Other Bundles](code-generation-guide.md#referencing-models-from-other-bundles)).
Then it checks for proxies that are still unresolved. If there are none, the value is computed
over the resolved model — every cross-package reference keyed by nsURI — and burnt into the
`FINGERPRINT` constant and the manifest capability. If there is at least one, **no constant is
emitted** and the generator log says which targets could not be resolved:

```
Model http://example.org/persistence/1.0 has 1 unresolved proxy target(s) - no fingerprint
constant is emitted, because the value would differ from the one computed over the generated code.
```

A missing constant is recoverable — the model falls back to computing at runtime, where the
generated Java code is resolved by construction. A location-keyed value in the constant would not
be: it would disagree with the runtime value forever. Relative paths in the `.ecore` are therefore
perfectly fine for generated models; the only requirement is that they resolve at build time,
which the generator verifies for you.

### The model extender

The extender registers `.ecore` files straight from a bundle, without generated code. It loads each
file from its bundle URL and computes the fingerprint immediately. Cross-package references by
relative path are resolved **on demand while hashing**: the target file is read from the same
bundle, at the location the relative path names. When that succeeds — the referenced file is in
the bundle, in the expected folder — the value equals the one the generator would produce for the
same model, independent of the order in which the files are loaded.

When it does not succeed, the extender logs a warning naming the unresolved targets — the
runtime counterpart of the generator's message — and registers the model anyway. The reference
stays a proxy keyed by its bundle-entry URL, so the registered package carries an
`emf.fingerprint` that differs from the generated one and, because the URL contains the bundle
id, from every other installation of the same bundle. Two situations lead there:

- The referenced file is **not shipped**, or shipped under a different path than the reference
  names. Ship every referenced `.ecore` with the folder layout the references assume.
- The referenced model lives in **another bundle**. A relative path cannot cross a bundle
  boundary at runtime. Reference it by nsURI in the `.ecore`, or generate the consuming model.

A reference by nsURI hashes the same value whether or not the extender could resolve it, so
for an extender-deployed model that references models outside its own bundle the nsURI form is
the only stable one.

### The dynamic package loader

The configuration-driven loader (`dynamicEcoreUri`) loads into a framework ResourceSet and calls
`EcoreUtil.resolveAll` before registering. The same rules apply: relative references resolve
against files next to the loaded one, nsURI references against the packages already registered in
the framework, and whatever remains unresolved is keyed by its absolute URI.

### A single file, nowhere else

A model uploaded or indexed on its own — one `.ecore`, no neighbours, no framework — can only
resolve what it does not need to resolve. Every reference by nsURI yields the published value;
every reference by location yields a value nobody else will reproduce. This is the case the
"resolution state is not identity" rule below is written for.

### Checking

The generated `FINGERPRINT` constant, the manifest attribute and the `emf.fingerprint` service
property of the same model must agree. If an extender-registered package shows a different value
from the generated bundle of the same `.ecore`, a reference did not resolve at load time — the
extender's warning names the file. At build time, the generator log names unresolved targets
explicitly; a bundle whose capability lacks the `emf.fingerprint` attribute is a bundle whose
model did not resolve at build.

## Where you find it

### 1. As a service property, at runtime

Every `EPackage` registered through a framework path — static registry, Ecore packages,
dynamic loader, model extender — carries the property from the first instant the service is
visible. It is aggregated up to the registry, `ResourceSetFactory` and `ResourceSet` services,
so you can bind to an exact model version:

```java
@Reference(target = "(emf.fingerprint=fp1:14466a0b5de879a6…)")
ResourceSetFactory factory;
```

### 2. As a constant, in generated code

Bundles generated with the Fennec code generator carry the fingerprint as a literal, computed
at build time from the `.ecore`:

```java
public class BasicEPackageConfigurator implements EPackageConfigurator {

    public static final String FINGERPRINT = "fp1:14466a0b5de879a6…";
```

Nothing is hashed at bind time for those models, and the value can be read without a running
framework. It is generated only when the model resolved completely at build time; if a
cross-reference could not be resolved, the constant is omitted — with a warning in the
generator log — and the model falls back to runtime computation. A missing fingerprint is
recoverable, a wrong one is not.

### 3. As a bundle capability, without a framework at all

The same value goes into the manifest, as an attribute on the capability that already
describes the generated package:

```
Provide-Capability: org.eclipse.emf.ecore.generated_package;
  class="org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage";
  uri="http://fennec.eclipse.org/example/model/basic";
  emf.fingerprint="fp1:14466a0b5de879a6…"
```

The attribute is named exactly like the service property, so **one filter expression works
against a running framework and against a JAR on disk**. This is what makes offline use
possible — repository indexing, dependency analysis, catalogue ingestion — and it allows
resolve-time matching:

```
Require-Capability: org.eclipse.emf.ecore.generated_package;
  filter:="(emf.fingerprint=fp1:14466a0b5de879a6…)"
```

A model that cannot state its fingerprint omits the attribute rather than declaring it empty,
so `(emf.fingerprint=*)` means "this bundle knows its model version".

## Using it from code

```java
@Reference
FingerprintService fingerprintService;   // org.eclipse.fennec.emf.osgi.fingerprint

String fingerprint = fingerprintService.fingerprint(ePackage);
```

Where a service reference is not available — inside a static initializer, a build-time
generator, or a component that computes service properties before registering — use the static
entry point instead:

```java
String fingerprint = FingerprintHelper.fingerprint(ePackage);
```

Both are in the released API bundle (`FingerprintService`) and the core implementation bundle
(`FingerprintHelper`, package `org.eclipse.fennec.emf.osgi.fingerprint.util`).

### Getting the service itself, without OSGi

Some collaborators take a `FingerprintService` as a mandatory parameter — the
[metadata service](metadata-service-guide.md#outside-osgi) is one. Inside OSGi that arrives as a
service reference; on a flat classpath, ask the helper for the one it computes with:

```java
FingerprintService service = FingerprintHelper.getDefaultFingerprintService();
```

This is the supported way to reach the shipped implementation. The class itself sits in a
private package that is deliberately not exported, so compiling against it would leave a bundle
with an `Import-Package` nobody can satisfy — the accessor is in the exported
`…fingerprint.util` package and resolves against both the full and the minimal implementation
bundle.

It hands out the same singleton the static methods above use, so callers share the per-package
cache. The computation is deterministic and thread-safe, which is what makes one shared instance
the right default rather than a convenience.

### Derivation inputs

Consumers that derive artifacts from a model often need a key for *the artifact*, not for the
model — one that also covers the settings the derivation used:

```java
String artifactKey = fingerprintService.fingerprint(ePackage, "codec", "v3", "strict");
```

The result is a fingerprint over the model **plus** those tokens. Use the plain model
fingerprint to answer "which model version is this?", and a derived key to answer "can I reuse
this artifact?".

The two are not interchangeable, and mixing them up is the one mistake worth naming:

| | Answers | Role |
|---|---|---|
| `fingerprint(ePackage)` | which model version is this? | the **join key** — what metadata trees and anything else about the model are filed under |
| `fingerprint(ePackage, inputs…)` | can I reuse this artifact? | the **store key** of one consumer's derived artifact |

The join key stays purely content-derived, always. A fingerprint that arrived from somewhere
else — a service property, a manifest, a document header — may legitimately be folded into a
derived key as just another input token, but it never replaces the locally computed one. That is
the same rule as "computed, never trusted" above, seen from the consumer's side.

### Addressing a scheme explicitly

The tag in front of the digest versions the *algorithm*. Most consumers never need to think
about it: they compute with the current one, or they resolve a value they read elsewhere and
react to the result. Where it does matter, the seam is addressable:

```java
fingerprintService.currentScheme();                       // "fp1" — the tag new values carry
fingerprintService.supportedSchemes();                    // every tag that can be computed
fingerprintService.fingerprintInScheme("fp1", ePackage);  // compute in a named scheme
```

Several schemes stay computable side by side, which is what makes a future bump additive: a new
tag *adds* an implementation instead of editing one whose values are already in circulation.
Consequently **a published scheme is frozen, and no two schemes share canonicalization code** —
a helper refactored for a newer scheme would silently change an older one's values, and a
fingerprint that changed meaning is worse than none.

Values with different tags are not comparable, even for the same model. A bump therefore never
makes a stored value unreadable — it only costs *precision* where several versions share an
nsURI, because an exact-match lookup misses and the consumer falls back to the nsURI.

### Cost

The computation walks the model once and is cached per package instance, so re-reading a
property costs nothing. Measured on a developer machine:

| Model | uncached median | p95 |
|---|---|---|
| Ecore itself, including generics | 127 µs | 179 µs |
| XMLType, ~55 data types | 91 µs | 108 µs |
| synthetic, 200 classes × 10 attributes | 507 µs | 755 µs |
| **cached lookup** | **0.2 µs** | 0.2 µs |

In other words: a one-time cost per registration in the 0.1–0.5 ms range, and free afterwards.
Generated models skip even that, since their value is a constant.

## Contract and limits

**Do not mutate an `EPackage` after registering it.** The fingerprint identifies a model
version; mutating a registered package makes it a different version while every consumer still
holds the old identity. The per-instance cache is deliberately never invalidated, because
there is no correct behaviour to fall back to — the model is simply out of contract.

**The fingerprint is not a version number.** It is not ordered, not human-readable, and says
nothing about compatibility. Two fingerprints are either equal or not; whether a newer model
can read an older document is a question the fingerprint does not answer.

**Resolution state is not identity — for references by nsURI.** A reference addressed by
nsURI yields the same fingerprint whether or not the target package was resolvable when the value
was computed. A reference addressed by document location does so only once resolved; unresolved,
it is keyed by that location. Which loading paths resolve what, and what to do about it, is in
[Models that reference other models](#models-that-reference-other-models).

## Related

- [Configuration Guide](configuration-guide.md) — all model service properties
- [Code Generation](code-generation-guide.md) — the generator that emits constant and
  capability
- [Metadata Service](metadata-service-guide.md) — the main consumer: it files every metadata
  tree under the fingerprint
