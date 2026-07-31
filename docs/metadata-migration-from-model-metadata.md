# Porting from `emf.model.metadata` to `emf.osgi`

> **Internal working document, not published on GitHub Pages.** It exists for the repos that
> still build against the donor repo `/opt/git/emf.model.metadata` — codec, atlas, eorm,
> persistence-jpa. It can be deleted once the last consumer has moved and the donor repo is
> archived (migration phase 4).
>
> The permanent documentation is elsewhere: [Metadata Service](metadata-service-guide.md) for
> the wiring, [Model Fingerprints](model-fingerprint-guide.md) for the `emf.fingerprint`
> contract. The donor repo's `docs/migration-to-emf-osgi.md` is the frozen *concept*; this file
> is the *mapping* — where things actually landed, which differs from the concept in a few
> places.

## 1. Bundles and buildpath

| Donor bundle | Replacement |
|---|---|
| `org.eclipse.fennec.model.metadata.api` | split: `org.eclipse.fennec.emf.osgi.api` (fingerprint, artifact store) + `org.eclipse.fennec.emf.osgi.metadata` (metadata model and service API) |
| `org.eclipse.fennec.model.metadata` | `org.eclipse.fennec.emf.osgi` (fingerprint impl, internal) + `org.eclipse.fennec.emf.osgi.metadata` (metadata impl) |

A consumer that used both donor bundles typically needs, in `bnd.bnd`:

```
-buildpath: \
	org.eclipse.fennec.emf.osgi.api;version=snapshot,\
	org.eclipse.fennec.emf.osgi.metadata;version=snapshot,\
	...
```

Fennec artifacts use `version=snapshot` (or `version=project` inside this workspace), never
`version=latest`. `org.eclipse.fennec.emf.osgi` itself is only needed on the buildpath for the
non-OSGi bootstrap (see §5); the fingerprint implementation classes are internal and must not
be imported.

## 2. Java packages

| Donor package / type | New location |
|---|---|
| `…model.metadata.api.FingerprintService` | `org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService` (api bundle) |
| `…model.metadata.api.ArtifactStore` | `org.eclipse.fennec.emf.osgi.artifact.ArtifactStore` (api bundle) |
| `…model.metadata.service.CanonicalizationScheme` | `org.eclipse.fennec.emf.osgi.components.fingerprint.CanonicalizationScheme` — **internal**, not exported |
| `…model.metadata.service.DefaultFingerprintService`, `Fp1CanonicalizationScheme`, `InMemoryArtifactStore` | same internal package; obtain the service, do not construct it |
| `…model.metadata.api.MetadataService`, `MetadataIndex`, `AspectProvider` (generated from `metadata-api.ecore`) | plain Java interfaces in `org.eclipse.fennec.emf.osgi.metadata` |
| `…model.metadata.*` (generated mirror tree: `PackageMetadata`, `ClassMetadata`, …) | `org.eclipse.fennec.emf.osgi.model.metadata` |
| `…model.metadata.service.MetadataServiceImpl`, `MetadataServiceComponent`, `MapBasedMetadataIndex` | `org.eclipse.fennec.emf.osgi.metadata` / `…metadata.impl` |

Note the generated model package is `org.eclipse.fennec.emf.osgi.model.metadata` — the concept
document said `…metadata.model`; the implemented name is the one above.

## 3. Model identity

| | Donor | New |
|---|---|---|
| metadata model nsURI | `https://eclipse.org/fennec/metadata/1.0.0` | `https://eclipse.org/fennec/emf/osgi/metadata/1.0.0` |
| `metadata-api.ecore` nsURI | `https://eclipse.org/fennec/metadata/api/1.0.0` | **dropped** — the service contracts are no longer modeled |
| fingerprint service property | `fennec.model.fingerprint` | `emf.fingerprint` (`EMFNamespaces.EMF_MODEL_FINGERPRINT`) |

Persisted metadata resources written by the donor code will not load: both the nsURI and the
class structure changed. Regenerate them.

## 4. API changes a rename cannot cover

**Every lookup on `MetadataService` now returns `Optional`.** `getPackageMetadata`,
`getClassMetadata`, `getFeatureMetadata`, `getOperationMetadata`, all their `*ByURI` /
`*ByName` variants, `getIndexReader` and the four `get*Aspect` methods. Donor code that
null-checked the result has to be rewritten, not just re-imported.

**Aspects are composed, not inherited.** The donor had a type hierarchy — `Aspect`,
`PackageAspect`, `ClassAspect`, `FeatureAspect`, `OperationAspect` — that consumers extended in
their own Ecore. That is replaced by one opaque type, `AspectEntry { typeId, content: EObject
(containment), diagnostics }`. Attach any `EObject` you own as `content`; there is no longer an
Ecore dependency from your model to the metadata model. `getPackageAspect(ePackage, typeId)`
and friends now return `Optional<AspectEntry>`.

**`AspectProvider` is gone; the SPI is `MetadataHandler`.** The per-element callbacks
(`buildPackageAspect`, `buildClassAspect`, `buildFeatureAspect`, `buildAttributeAspect`,
`buildReferenceAspect`, `buildOperationAspect`, `buildProfiles`, `getAspectTypeId`) are replaced
by a single coarse hook:

```java
public interface MetadataHandler {
	void onPackageRegistered(PackageMetadata packageMetadata);
	default void onPackageUnregistered(PackageMetadata packageMetadata) {}
	default void clear() {}
}
```

The handler walks the mirror tree itself and attaches `AspectEntry` instances where it wants
them. Consumers that relied on the framework's traversal now own that loop.

**Profiles are gone.** `PackageProfile`, `ClassProfile`, `buildProfiles` and
`getPackageProfile*` have no replacement. The pre-merged annotation hierarchy they carried was
codec-specific; a consumer that needs it computes it in its own `MetadataHandler`.

**Codec vocabulary did not move.** `Base*Config`, `SerializationFormat`, `TypeStrategy`,
`IdKeyMode` and the JSON key defaults are not part of the new metadata model — they belong to
the codec repo and stay there.

**Multi-version support is new.** One nsURI can hold several fingerprint-keyed versions:
`getPackageMetadataVersions(nsURI)` returns all of them, `getPackageMetadataByFingerprint`
selects one. Donor code assuming one metadata tree per nsURI still compiles but may silently
pick the wrong version — check callers of `getPackageMetadata(String nsURI)`.

## 5. Getting a handle

In OSGi, inject the services — `MetadataService` for reading, `MetadataWhiteboard` for
registering packages and handlers; contribute a `MetadataHandler` as a service and it is picked
up. On a flat classpath there is no `new MetadataServiceImpl(...)` any more:

```java
MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(myHandler);
```

The parameterless and single-argument forms take the default `FingerprintService`; the
`createWhiteboard(FingerprintService, MetadataHandler...)` overload is for an explicit one.
Details and the persistence of the registry are in the
[Metadata Service guide](metadata-service-guide.md).

## 6. Order of work for a consumer repo

1. Swap the `-buildpath` entries (§1) and let the compiler list the breaks.
2. Fix imports mechanically (§2), then the nsURI and the property name (§3).
3. Rewrite the null-checks into `Optional` handling (§4) — this is the bulk of the work.
4. Replace the `AspectProvider` implementation with a `MetadataHandler` that traverses the tree
   and emits `AspectEntry` (§4). Any consumer Ecore that extended the donor aspect types loses
   its supertypes here and becomes a standalone model.
5. Re-check every `getPackageMetadata(nsURI)` call for the multi-version case.
6. Audit the consumer's *own* registries for nsURI keying — see below.

## 7. The bug this migration exists to fix

Worth knowing while porting, because most consumers have their own copy of it. The failure was
observed in [model.atlas#156](https://github.com/eclipse-fennec/model.atlas/issues/156): one
nsURI, `http://example.org/person/1.0`, published twice from two branches with diverging content
(`draft` has `Person.name`, `approved` has `Person.fullName`), both legitimately live. The donor
`MetadataServiceImpl` kept a flat `Map<String, PackageMetadata>` keyed by nsURI, which produced
three defects in sequence:

| Code fact | Effect |
|---|---|
| flat map keyed by nsURI | the derived side can only represent **one** version per nsURI |
| `registerPackage` was **first-wins** | the second version's metadata was never built; its objects silently got the *first* version's metadata |
| `unregisterPackage` did an unconditional `remove(nsURI)` | one version's unbind deleted the entry every other live version still needed — persistent HTTP 500 afterwards |

The generalization is what matters for a port: **any consumer that whiteboard-tracks `EPackage`
services and keys them by nsURI is broken by multi-version registration.** In the donor's own
neighbourhood that pattern also existed in the Atlas (`DynamicEPackageRegistrationService`,
`DynamicEPackageConfigurator`) and in the codec (`TypeDiscriminatorService`'s
`EPackage.Registry.INSTANCE` fallback). If the repo you are porting has such a map, fix it in the
same pass — key it by `emf.fingerprint` and make removal per-version — or the port merely moves
the bug.

The new implementation is keyed by fingerprint with nsURI as a secondary, explicitly best-effort
index; unregistration is a per-version refcount that can never remove another version's entry.
