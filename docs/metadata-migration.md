# Metadata & Fingerprint migration — emf.osgi working document

> **Status:** open working document — **temporary, deleted when the migration finishes**
> (see §6). Companion to
> `emf.model.metadata/docs/migration-to-emf-osgi.md` (the agreed migration concept of
> 2026-07-27). That document defines *what* moves and *why*; this one tracks the points that
> have to be **decided or resolved on the emf.osgi side** before and during the port, each
> with context, options and a recommendation, so they can be closed one at a time.
>
> Written in English to match the rest of `emf.osgi/docs/`.
>
> **Work tracking:** parent issue
> [#51](https://github.com/eclipse-fennec/emf.osgi/issues/51) with sub-issues #52–#63 (one per
> work step). This document stays the decision record; the issues track execution.
>
> **How to use this document:** every open point has an ID (`M…`). Closing a point means
> filling in its *Decision* line with the outcome and the date, not deleting the section —
> the rationale stays readable. Points inherited from the source document keep a pointer to
> their original ID (`D1`…`D4`) so both documents stay cross-referenceable.

## 1. Verified state of emf.osgi (2026-07-28)

Facts checked against the working tree, not assumptions — they are the basis for several
points below.

| Fact | Anchor |
|---|---|
| The api and impl bundles are **already released and baselined**. Baselining against the release OBR is active and enforced. | `cnf/build.bnd:38` (`-plugin.baseline`), `cnf/build.bnd:46` (`-baselinerepo: Baseline`), `-baseline: *`; commits `a85249f`, `4981aa0`, `dc60abd` |
| The core impl bundle ships **two component profiles** via `-sub: *.bnd`. | `org.eclipse.fennec.emf.osgi/bnd.bnd`, `component.bnd`, `component.minimal.bnd` |
| `org.eclipse.fennec.emf.osgi.components` (the registry components) is a **private package of the minimal profile**. | `org.eclipse.fennec.emf.osgi/component.minimal.bnd` |
| `EMFNamespaces` lives in the api bundle, not in the impl bundle. | `org.eclipse.fennec.emf.osgi.api/src/org/eclipse/fennec/emf/osgi/constants/EMFNamespaces.java` |
| Model service properties are emitted from **at least four independent sites**, not one. | `components/StaticEPackageRegistryComponent.java:126`, `ecore/EcorePackagesRegistrator.java:92`, `components/dynamic/DynamicPackageLoader.java:162`, extender `ModelHelper.java:209` |
| The current branch `metadata-service` contains **no metadata code** — only codegen renames (Gecko→Fennec), IDE settings and baselining fixes. | `git diff --stat main...HEAD` |
| The source document plans a branch named `metadata-migration`. | source doc §5, Phase 1 |
| The **codegen already has both the api and the impl bundle on its buildpath** — a static helper is reachable from the generator without a new dependency. | `org.eclipse.fennec.emf.osgi.codegen/bnd.bnd:27,31` |
| Generated bundles have **exactly one injection point** for service properties: `getServiceProperties()` in the generated `*EPackageConfigurator`; the `ConfigurationComponent` copies it into all five registrations. | `example.model.basic/src-gen/…/BasicEPackageConfigurator.java:65-74`, `BasicConfigurationComponent.java:114-163` |
| Generated service properties are **already build-time constants** — `emf.version` is the literal `"1.5"`, `emf.contentType` is `"basic#1.0"`. | `BasicEPackageConfigurator.java:70-72` |
| The generator **does** reproduce custom annotation sources in `*PackageImpl` — `Version` and `ExtendedMetaData` both get a `createXxxAnnotations()` method. | `example.model.extended/src-gen/…/impl/ExtendedPackageImpl.java:465-492` |
| The project's **own bnd template writes build configuration into the `.ecore`** as a GenModel annotation (`complianceLevel`, `oSGiCompatible`, `basePackage`), because its genmodel sets `suppressGenModelAnnotations="false"`. | `bnd.templates.project/resources/templates/emf-model/model/{{EcoreFileName}}.ecore:7-10`, `…{{EcoreFileName}}.genmodel:5` |
| The workspace corpus contains a second GenModel namespace (UML2). | `codegen/test-resources/ws-1/org.w3.rdf.model/model/OWLLibrary.genmodel:3` |

Verified state of the donor repo (`emf.model.metadata`), relevant to effort and sequencing:

| Fact | Anchor |
|---|---|
| Golden fingerprint values **already exist** — three literals plus a scheme-registry path. | `Fp1CanonicalFormRegressionTest.java:49-54, 245-271` |
| `DefaultFingerprintService` is today a plain **OSGi service component**. | `DefaultFingerprintService.java:43` (`@Component(service = FingerprintService.class)`) |
| Hand-written code that actually moves: ~3.000 LOC src, ~4.100 LOC test. Of that, verbatim-portable ≈ 700 LOC src + 1.200 LOC test (fp1, scheme seam, ArtifactStore); the re-cut is `MetadataServiceImpl` (1.272) + `MapBasedMetadataIndex` (445) with ~2.500 LOC of tests. | `find`/`wc` over `src`/`test`, excluding `src-gen` |
| 81 of 102 Java files in the donor repo are `src-gen` — they are **regenerated, never ported**. | same |
| fp1 filters only the **detail key** `documentation`, not annotation **sources**; an annotation is dropped only if nothing remains after filtering. All other details of every source enter the hash. | `Fp1CanonicalizationScheme.java:357-377` |
| Classifier references are canonicalised as `nsURI#Name` — representation-independent. An **unresolved** proxy would degrade to `"#null"`. | `Fp1CanonicalizationScheme.java:397-405` |
| The kitchen-sink fixture's only GenModel annotation is **documentation-only**, so it is already dropped today. | `Fp1CanonicalFormRegressionTest.java:200-203` |

## 2. Inherited and undisputed

Taken over from the source document without change; listed so this document is self-contained
and so it is clear what is *not* up for discussion here:

1. **The rework happens directly in emf.osgi** on a feature branch; no rework branch in the
   donor repo (source §2).
2. **fp1 is frozen and ported verbatim** (package rename only), with golden-value regression
   tests as the acceptance criterion (source §4).
3. **`metadata-api.ecore` is dropped**; pure service contracts become plain Java interfaces
   with `@ProviderType` (source §3).
4. **Aspect attachment by composition** (`AspectEntry { typeId, content, diagnostics }`)
   instead of inheritance; codec vocabulary returns to the codec repo (source §1).
5. **The fingerprint is computed, never trusted** — a fingerprint arriving as an input
   property is not adopted (source §3, atlas#156).
6. **`src-gen` is generated by the project owner** from the new ecore/genmodel and never
   hand-edited (project `CLAUDE.md`).
7. **Genericity gate before API freeze**: eorm-style visitor spike + OCL-cache-style
   artifact spike, both without inheriting from the metadata model (source §5, Phase 2).

## 3. Open points

### M1 — Where does the metadata API live, given that the api bundle is already released?

*Origin: new (interacts with source §3, §7 and risk 2).*

**Context.** Source §3 places the plain-Java metadata API in `org.eclipse.fennec.emf.osgi.api`,
package `org.eclipse.fennec.emf.osgi.metadata`. Source §7 says the first release tag comes only
after the Phase 2 genericity gate. Those two are in tension with the verified state: the api
bundle is *already* released and baselined (§1). Adding a package to it ships the new API as a
minor bump of a released bundle — it is then under semantic-versioning governance and visible
to consumers **before** the gate has confirmed its shape. That is exactly risk 2 of the source
document ("API frozen against one consumer's shape again").

The asymmetry matters: the **fingerprint** contract *wants* to be frozen early — it is the
whole point of porting it verbatim. The **metadata** API does not; the gate exists because its
shape is still in question (see also M7).

**Options.**

- (a) Both in the released api bundle, as source §3 states.
- (b) Fingerprint API in the released api bundle; metadata API in its own new bundle
  `org.eclipse.fennec.emf.osgi.metadata` with its own package version, promoted into the api
  bundle after the gate (or left where it is).
- (c) Both in the new metadata bundle until the gate, then split.

**Recommendation:** (b). It keeps the frozen thing frozen and the unfrozen thing cheap to
change, and it needs no baseline exception. Promotion after the gate is a package move plus one
minor bump, which is a smaller cost than a breaking change on a released api bundle.

**Consequence if (b):** the split-package check in source §3 has to be redone — API package
`…osgi.metadata` and impl `…osgi.metadata.impl` / generated `…osgi.metadata.model` would then
live in the *same* bundle, which is allowed but means the bundle exports one package and keeps
two private. Note in the bnd file which is which.

**Decision (2026-07-28):** **(b)** — fingerprint API (`FingerprintService`, `ArtifactStore`;
**not** `CanonicalizationScheme`, which stays internal per M11) and `EMF_MODEL_FINGERPRINT` into
the released api bundle (that contract wants freezing). **Each API concern gets its own package**
(added 2026-07-28): `org.eclipse.fennec.emf.osgi.fingerprint` (`FingerprintService`) and
`org.eclipse.fennec.emf.osgi.artifact` (`ArtifactStore`) — independent package versioning per
concern. The metadata API into its own bundle
`org.eclipse.fennec.emf.osgi.metadata` with its own package version until the genericity gate.
Promotion into the api bundle after the gate stays optional.

---

### M2 — Fingerprint impl placement, re-evaluated against `component.minimal.bnd`

*Origin: source D1, extended.*

**Context.** D1 weighs "fingerprint impl in the core impl bundle (zero extra deps)" against "own
`…osgi.fingerprint` bundle (cleaner for the multi-runtime core extraction)". The source
document does not account for the fact that the core impl bundle has **two component profiles**,
and that the registry components — the intended emission site — are a private package of the
**minimal** profile (§1). So "in the core impl bundle" implies: canonicalization and SHA-256
land in the minimal profile too, which grows by ~600 LOC of code that a minimal deployment may
not want.

Second consideration: `multi-runtime-concept.md` in this repo plans an SPI abstraction layer and
a core extraction. A separate fingerprint bundle is the shape that extraction would want anyway.

Third consideration (added with M13): the canonicalisation has a **build-time** caller, the
generator. That gives it three consumers — build time, registration time, and the service facade
— which argues for a unit of its own rather than for code that happens to sit next to the
registry components. The codegen can reach either placement today (§1).

**Options.**

- (a) Core impl bundle, both profiles (D1's proposal, minimal grows).
- (b) Core impl bundle, but emission only in the full profile — minimal registers without the
  property. Rejected on sight unless someone wants it: it makes the property's presence a
  function of the deployment profile, which consumers cannot see.
- (c) Own bundle `org.eclipse.fennec.emf.osgi.fingerprint`, core impl depends on it.

**Recommendation:** (c), unless keeping the dependency graph flat outweighs it. (a) is
acceptable if the minimal profile is not treated as a hard size budget — then say so explicitly,
so the growth is a decision and not an accident.

**Depends on:** M3 (if emission must be static, the code must be *in* or *required by* whatever
bundle emits).

**Decision (2026-07-28):** **(a)** — core impl bundle, both profiles, i.e. D1 as originally
proposed. The recommendation above is reversed, and M3 is the reason: property presence is
structural, so every emission site calls the helper unconditionally — a separate bundle would be
a **hard wire dependency** of the core impl that a minimal deployment must install anyway. It
would save nothing and add a dependency edge and a release artifact. The growth of the minimal
profile (~600 LOC canonicalization + SHA-256) is hereby accepted **as a conscious decision**: the
minimal profile is not a hard size budget. The multi-runtime extraction stays cheap regardless —
the canonicalization core is pure EMF with no OSGi dependency ("extraction falls out of the
multi-runtime work", D1).

---

### M3 — Emission must not depend on a dynamic service reference

*Origin: new.*

**Context.** Service properties are fixed at registration time. `DefaultFingerprintService` is
today an ordinary OSGi component (§1). If the registry components reference it as a service,
then any `EPackage` registered before the fingerprint component is active is registered
**without** `emf.fingerprint` — nondeterministically, depending on start order and on whether
the deployment contains the component at all. A property that is sometimes absent cannot be used
in a service filter, which is most of its value.

**Options.**

- (a) Use the canonicalization **internally and statically** from the emitting code; register
  `FingerprintService` additionally as a service facade for external consumers. Presence of the
  property is then structural.
- (b) Static `@Reference` with mandatory cardinality, so the registry components do not activate
  without a fingerprint service. Correct too, but it makes the *entire* EMF registry
  unavailable when the fingerprint component is missing — a large blast radius for a property.
- (c) Optional reference, property best-effort. Rejected: see above.

**Recommendation:** (a). It is also the strongest argument in favour of D1/M2 keeping the code
reachable from the core impl bundle without a service round-trip.

**Reinforced by M13:** the generator runs in the bnd build classloader, where an OSGi service
reference does not exist at all. A static entry point is therefore not merely preferable, it is
a precondition for build-time use. The service registration becomes the facade, not the
primary API.

**Decision (2026-07-28):** **(a)** — static helper used internally by all emission sites;
`FingerprintService` registered additionally as a facade for external consumers. Settled
together with M13(a), which requires the static entry point anyway.

---

### M4 — Cost of fingerprinting on the bind path, and caching

*Origin: new.*

**Context.** The registry components bind and unbind configurators dynamically and re-propagate
service properties on every change; `ServicePropertyContext` aggregates. Computing a canonical
form plus SHA-256 per bind puts the cost on the whiteboard's critical path. Because the
fingerprint is *computed, never trusted* (§2.5), there is no shortcut via an incoming property —
so the answer has to be caching.

**Points to settle.**

- Cache keyed by `EPackage` **instance identity** (weak/identity map) — sound for generated
  packages, whose content does not change after registration.
- Dynamic packages (`DynamicPackageLoader`) can in principle be mutated after load. Decide
  whether the cache is invalidated, or whether mutation-after-registration is declared out of
  contract (the fingerprint identifies a *model version*; a mutated package is a different
  version and arguably a bug).
- Whether a fingerprint is computed **eagerly at bind** (property is present, cost is paid) or
  **lazily on first read** (property cannot then be a service property at all — this decides
  itself: eager, if the property is the goal).
- Measure once on a realistic model set before Phase 1 exit; record the number in this document
  so later regressions are visible.

**Recommendation:** eager at bind, identity-keyed cache, mutation-after-registration out of
contract and documented as such.

**Scope reduced by M13:** if the build-time constant is adopted, generated packages carry their
fingerprint as a literal and cost nothing at bind. This point then only covers the paths that
have no generator step — `DynamicPackageLoader`, `EcorePackagesRegistrator`, and hand-written
packages such as `example.model.manual`. It does not disappear: those paths remain, and the
measurement is still worth taking.

**Decision (2026-07-28):** as recommended — eager at bind; **in-memory** weak identity map,
scoped to the JVM run (rebuilds itself through normal re-registration after a restart, no
warm-up); the cache is internal and **not a seam** — the only pluggable persistence point is the
`ArtifactStore`; mutation after registration is out of contract, no invalidation machinery.
Identity semantics underlying this are recorded in §4 (presence-at-registration by ordering,
benign races, keying rule).

**Measured (2026-07-28, #56, via `FingerprintCostMeasurement`, warmup 100 / 500 runs, local
dev machine):**

| Model | uncached median | p95 |
|---|---|---|
| Ecore (`EcorePackage`, incl. generics) | 127 µs | 179 µs |
| XMLType (~55 data types) | 91 µs | 108 µs |
| synthetic 200 classes × 10 attributes | 507 µs | 755 µs |
| **cached lookup** (the re-aggregation hot path) | **0.2 µs** | 0.2 µs |

Conclusion: the uncached computation is a one-time cost per registration in the 0.1–0.5 ms
range (50 large models ≈ 25 ms once); the cached lookup makes the whiteboard re-aggregation
path effectively free. No further optimization warranted. The measurement class stays in the
repo `@Disabled` for re-runs.

---

### M5 — Which of the four emission sites emit the property, and how divergence is prevented

*Origin: new (refines source §3 and Phase 1, which say "registry components/extender").*

**Context.** `emf.nsURI` / `emf.version` are emitted from at least four independent places
(§1): `StaticEPackageRegistryComponent`, `EcorePackagesRegistrator`, `DynamicPackageLoader`, and
the extender's `ModelHelper`. Adding `emf.fingerprint` to "the registry components and the
extender" is therefore not one change but ≥4, and the sites will drift.

**Points to settle.**

- Full list of sites that must carry the property (including `DefaultEPackageRegistryComponent`
  and `FennecXMLResourceFactory` — check each; the latter emits a hard-coded version today,
  `FennecXMLResourceFactory.java:50`, which suggests it is not a model-registration site at all).
- Whether emission is centralised in **one helper** that all sites call, so that a fifth site
  added later cannot silently omit the property.
- Whether the itest asserts the property on **every** registration path or only the static one.
  The source document's Phase 1 itest ("registered `EPackage` service carries the property")
  covers one path; the multi-version assertion needs at least two.

**Recommendation:** one helper, and an itest per emission path. This is cheap now and
unmaintainable later.

**Decision (2026-07-28):** as recommended — (1) **one shared helper** builds the model property
set (nsURI, version, fingerprint, …) and all emission sites call it; per §4 this helper is also
where the presence-at-registration ordering guarantee lives. (2) Site list: the four known sites
(`StaticEPackageRegistryComponent`, `EcorePackagesRegistrator`, `DynamicPackageLoader`, extender
`ModelHelper`); `FennecXMLResourceFactory` is **not** a model-registration site (registers a
resource factory, hard-coded version); `DefaultEPackageRegistryComponent` to be verified during
implementation. (3) **One itest per emission path**, including the two-same-nsURI/distinct-
fingerprints assertion on more than one path.

---

### M6 — Property name

*Origin: source D2, unchanged.*

`emf.fingerprint` (emf.osgi convention, follows `emf.nsURI` / `emf.version` — see
`EMFNamespaces.java:69,77`) vs. `fennec.model.fingerprint` (atlas draft). One name, agreed with
the atlas team in the weekly, **before Phase 1 ships** — after that it is a published contract.

**Recommendation:** `emf.fingerprint`, as `EMFNamespaces.EMF_MODEL_FINGERPRINT`, consistent with
the existing `EMF_PREFIX` constants.

**Decision (2026-07-28):** **`emf.fingerprint`**, as
`EMFNamespaces.EMF_MODEL_FINGERPRINT = EMF_PREFIX + "fingerprint"`, in the semantics of the
existing `emf.*` model properties. D2 is thereby resolved on the emf.osgi side; to be
communicated to the atlas team in the weekly (atlas aligns its draft name
`fennec.model.fingerprint`).

---

### M7 — `AspectEntry.content` type: decide at the model cut, not in the spikes

*Origin: source D3, position changed.*

**Context.** D3 proposes `EObject` containment and defers the alternative ("possibly both") to
the Phase 2 spikes. The objection: this choice determines the structure of the model. Deciding
it after the gate means a model change, a new nsURI or a version bump, and regeneration at every
consumer.

**Premise correction (2026-07-28):** D3's stated test case — "the OCL cache (parsed constraints
are not EObjects)" — is **outdated**. The parsed constraints are held as `OCLExpression`
**EObjects**: parsed once, handed to the evaluator as EObjects, and the explicit goal is to
*serialize* them so the cache becomes transferable between nodes. Serializability is a general
goal of the metadata concept, not an optional nicety — which strengthens `EObject` containment
as the primary slot. This correction flows back to the source document (see §6).

Second, subtler point: with `EObject` containment, the consumer's aspect content is only
loadable if its `EPackage` is resolvable in the *same* `ResourceSet` — under a multi-version
registry that is exactly the situation that hurts. Serialisability of `AspectEntry` is therefore
conditional either way.

**Options.**

- (a) `content : EObject` containment only (D3 as written), revisit after the spikes.
- (b) `content : EObject` containment **plus** a transient `EJavaObject` slot from the start.
  Costs one feature; removes the post-gate model bump.
- (c) `EJavaObject` only — gives up in-tree serialisation.

**Recommendation:** (b), fixed during the Phase 2 model cut. The spikes then *validate* the
choice instead of triggering a rework.

**Decision (2026-07-28):** **(b)** — `content : EObject` (containment) as the primary slot; all
known payloads are EObjects (eorm mapping, codec profile, `OCLExpression`) and serializability
is the point. The transient `EJavaObject` slot is kept anyway as a cheap escape hatch for
genuinely non-EMF payloads — it costs one feature ("frisst kein Brot") and removes the
post-gate model bump if such a payload ever appears. Fixed at the model cut, not revisited in
the spikes.

---

### M8 — Operation-level aspect entries

*Origin: source D4, unchanged.*

Keep operation-level attachment in the slim model, or defer until a consumer needs it (the
codec's `buildOperationAspect` may return null today)? Decide during the Phase 2 model cut,
together with M7 — both are model-shape questions with the same "cheap now, expensive later"
asymmetry.

**Recommendation:** decide together with M7; if the cost is one containment feature, keep it.

**Decision (2026-07-28):** **keep** the operation-level attachment point. Cost is one containment
feature on `OperationMetadata`; with M7's corrected premise there is even a plausible first user
(`OCLExpression` constraints on operations). Same cheap-now/model-bump-later asymmetry as M7.

---

### M9 — Golden values are only meaningful together with their fixtures

*Origin: new (port rule, not a decision).*

The golden literals are anchored to the `kitchenSink()` / `empty()` fixtures in
`Fp1CanonicalFormRegressionTest`. Porting the test while "adapting" the fixtures to the new
model would keep the literals and destroy their meaning. **Port rule:** fixture construction
moves verbatim; if a fixture must change, the golden value has to be re-derived from the donor
implementation and the change recorded here with a reason.

Consequence for Phase 0: "record golden fingerprint values" is effectively **already done**
(§1). What remains is the port rule above.

**Interaction with M14 — verified golden-neutral.** M14 changes the canonicalisation before the
freeze, which would normally invalidate the golden values. It does not here: the kitchen-sink
fixture's only GenModel annotation carries nothing but `documentation` and is therefore already
dropped by today's filter (§1). Excluding the whole GenModel source leaves all three literals
unchanged. Re-verify this after implementing M14 rather than trusting the argument.

**Decision:** rule accepted unless objected — no decision needed.

---

### M10 — *Withdrawn:* "codegen must not emit the fingerprint"

*Origin: new. **Withdrawn 2026-07-28**, superseded by M13.*

The original claim was that a fingerprint emitted by the codegen would be a "trusted" value and a
second source of truth, contradicting §2.5. **The reasoning was wrong** and is recorded here so
it is not re-raised:

- `emf.version` and `emf.contentType` are **already** build-time constants in generated code
  (§1). A fingerprint constant is not a new trust class; it is the same class as `emf.version`.
- "Computed, never trusted" (atlas#156) is about a fingerprint **supplied from outside** by a
  third party, not about the project's own build output inside the very bundle that contains the
  model.

What remains of the concern is a real question, but a different one — whether the value computed
at build time from the `.ecore` equals the value computed at runtime from the generated
`EPackage`. That is M13/M14.

**Decision:** withdrawn; no action.

---

### M11 — `CanonicalizationScheme` seam check (Phase 0 gate)

*Origin: source §5 Phase 0, still outstanding.*

Verify the seam against the Merkle/composite requirements of the unified-persistence concept
(§6.4, §17.2): a structured scheme (subtree hashes, composite root over several packages) must
be addable as a **second scheme** without breaking the API. Cheaper to adjust in the donor repo
before the freeze than afterwards. The seam is 96 LOC (`CanonicalizationScheme.java`) and is
already tag-addressed, which is a good sign but not the proof.

**Open question to answer explicitly:** does a composite scheme need a fingerprint over *several*
`EPackage`s — i.e. an entry point that the current single-package signature does not have? If
yes, the seam has to grow **before** Phase 1.

**Decision (2026-07-28): seam confirmed, no signature change before the freeze — with one
condition that reverses a source-doc detail.** Findings of the check against the
unified-persistence concept (§6.2, §6.4, §17 #2/#3 there):

- The **composite root needs no multi-package entry point**: it is defined as a hash over the
  ordered list of `(packageUri, packageFingerprint)` *pairs* — each pair fingerprint comes from
  the existing single-package call; the composite itself hashes a small string list. A later
  convenience method is **additive** (`@ProviderType`: consumers call, never implement — a new
  method is a minor bump, not a break).
- The **Merkle root fits the value format**: a single digest, `fpm1:<hex>`, service-property
  capable. Subtree hashes are resolution data for the snapshot store — the concept itself states
  the fingerprint *identifies, never describes*, so they are not part of the value.
- A different digest length (8-byte truncation, considered there) is a new scheme's business —
  the tag versions the whole algorithm, digest included. Already provided for.
- **Condition:** `CanonicalizationScheme` **stays internal**. The source document plans to
  promote it to public API during the move — that promotion is **dropped**: the interface's
  single-package/single-string shape would then be frozen, and a Merkle scheme does not fit it.
  As long as it is package-private (its own Javadoc argues exactly this), reshaping it is free.
  This deviation flows back to the source document (§6 checklist).

---

### M12 — Branch hygiene

*Origin: new.*

The current branch `metadata-service` carries unrelated work (codegen renames, IDE settings,
baselining fixes) and no metadata code (§1). The source document names the migration branch
`metadata-migration`.

**Recommendation:** land the existing codegen/baselining work on `main` first, then start the
migration on a clean branch; align the name with the source document or update the source
document. Do not mix two subjects in the branch that is going to be reviewed for a frozen
contract.

**Decision (2026-07-28):** **no branch split — recommendation not adopted.** The project's
workflow lands everything together in `snapshot`, and the generator is testable without a
release; a separate clean branch buys nothing here. Migration work continues on
`metadata-service`; the source document's branch name `metadata-migration` is superseded.

---

### M13 — Static helper, build-time constant, manifest capability

*Origin: new, proposed by the project owner 2026-07-28. Supersedes M10.*

**Context.** Three connected proposals, in ascending order of risk.

**(a) The canonicalisation is a static helper.** Uncontested. The generator runs in the bnd build
classloader, where an OSGi service reference does not exist — a static entry point is a
precondition, not a preference (see M3). No new dependency is needed: the codegen already has
both the api and the impl bundle on its buildpath (§1). The `FingerprintService` component
becomes a facade over the helper for external consumers.

**(b) The generator emits the fingerprint as a constant.** The generator holds the `.ecore`-loaded
`EPackage` and can compute the value at build time, so the generated
`*EPackageConfigurator.getServiceProperties()` — the single injection point per generated bundle
(§1) — puts a literal into the property instead of computing at bind. Gains:

- zero cost on the bind path for generated packages (reduces M4 to the non-generated paths);
- the value exists **without a running framework** — readable from the JAR for atlas ingestion,
  OBR indexing, offline dependency analysis.

**(c) The fingerprint becomes a bundle capability.** This is the part that a runtime-computed value
cannot deliver at all, and arguably the strongest reason for (b): with a build-time value the
generator can write the fingerprint into the manifest, making it **matchable at resolve time**
via `Require-Capability`. The generator already emits `@Capability(...)` annotations
(`BasicConfigurationComponent.java:51-55`) — same mechanism, no new tooling.

**Precondition — the equivalence gate.** (b) and (c) are only sound if the fingerprint of the
`.ecore`-loaded `EPackage` equals that of the generated `EPackageImpl`. This is **required**
anyway, not merely convenient: runtime computation does not disappear (dynamic packages, Ecore
itself, hand-written packages), so both derivations coexist. If they diverge, the same model gets
two identities depending on how it was loaded — the WP6 dedupe invariant breaks and
`getPackageMetadataVersions` reports phantom versions. See M14 for the guarantee this rests on.

The gate is a test over the workspace corpus — `example.model.basic`, `.extended`, `.manual`,
`delegates-test.ecore`, the multi-ecore `rdf`/`rdfs`/`OWLLibrary` test workspaces, and Ecore
itself — comparing `.ecore`-derived against generated-`eINSTANCE`-derived fingerprints. Known
things it must clear:

- annotation sources of every kind (M14 decides which are relevant at all);
- **unresolved proxies**: `typeKey` degrades to `"#null"` for an unresolved classifier (§1), so a
  generator environment that does not fully resolve cross-model references diverges. The
  multi-ecore test workspaces are the case;
- the UML2 generator path (`org.eclipse.uml2.codegen.ecore` is on the codegen buildpath).

**Validation: build time, not runtime.** Runtime validation of the constant is rejected — it means
computing *and* comparing, so it costs exactly what the constant saves and puts two values into
circulation. Instead the generator emits a **pin test** next to the constant:
`assertEquals(FINGERPRINT, FingerprintHelper.fingerprint(XPackage.eINSTANCE))`. Runtime cost
zero, drift fails the build, and the equivalence is then proven per model instead of assumed
once. Optionally a dev-mode check behind a system property, off in production.

Drift cases the pin test covers: stale `src-gen` (the constant is then exactly as stale as
`emf.version`), hand-edited `src-gen`, and a residual equivalence defect.

**Recommendation.** Split by risk: **(a) unconditionally, in Phase 1**, together with runtime
computation — it unblocks everything and carries no risk. Then the equivalence gate. **(b) and
(c) only once the gate is green**, with the pin test, and with runtime computation retained for
the non-generated paths.

**Decision:** **(a) decided 2026-07-28** (with M3) — static helper in the core impl bundle,
runtime computation in Phase 1. **The equivalence gate is green (2026-07-28, #57,
`FingerprintEquivalenceGateTest`)**: basic, extended (cross-references to basic and Ecore,
ExtendedMetaData/Version annotations), Ecore itself and XMLType all yield identical fingerprints
for `.ecore` and generated code. The predicted proxy risk materialized and is handled: the
workspace-relative references (`../../org.eclipse.emf.ecore/model/Ecore.ecore`) degrade to
`"#null"` without codegen-style URI resolution — conservative (false-different), and the gate
asserts full resolution before comparing. **(b)/(c) are unblocked** and proceed as #58/#59.

**(b) implemented 2026-07-29 (#58).** `GeneratorHelper.getFingerprint(GenPackage)` computes the
value over the `.ecore`-loaded package at generation time; the JET template emits it as a named
constant `FINGERPRINT` on the generated `*EPackageConfigurator` and puts it into
`getServiceProperties()`. Named rather than inlined because the manifest capability (#59) needs
the same value, and because it is then readable from the class without a running framework.

Two things the implementation settled beyond the decision text:

- **The proxy guard is a skip, not an abort.** The generator runs `resolveAll` and then
  `UnresolvedProxyCrossReferencer.find(ePackage)`; on a remaining proxy it emits **no** constant
  and logs a warning (`FennecEmfGenerator.warn`, added next to `info`/`error`), leaving that model
  on the runtime path. Rationale: a missing fingerprint is recoverable, a wrong one propagates
  into every downstream consumer as a false identity. Generation is not failed, because the
  multi-ecore workspaces are exactly the case that would then break.
- **The pin test is one sweep in the itest, not a generated test per model.** `-generate` writes
  to `src-gen` only, and `src-gen` is bundle source — a generated JUnit test would force JUnit
  onto the `-buildpath` of every model bundle. `FingerprintConstantDriftTest` instead compares the
  advertised property of *every* registered `EPackage` service against
  `FingerprintHelper.fingerprint(…)`, plus explicit assertions for the two generated example
  models. No wiring in the model bundles, and models added later are covered the moment they
  register. Verified to have teeth: with a deliberately wrong constant both new tests fail while
  the other 66 itests stay green. A generated per-model test for *external* consumers remains
  possible as a separate opt-in (a `testOutput` parameter); it is not part of #58.

**(c) implemented 2026-07-29 (#59), with one deviation from this document.** The capability does
**not** get a new namespace. `org.eclipse.emf.ecore.generated_package` already exists in the
workspace and is already emitted per generated model by the `@EPackage` provide-annotation, with
`uri`, `class`, `ecore`, `genModel` and the Fennec-added source locations. One model is one
capability: the fingerprint became the attribute `emf.fingerprint` on *that* capability rather
than a second, competing `fennec.emf.model` clause describing the same thing. The attribute name
is deliberately identical to the service property, so one filter expression works against a
running framework and against a JAR on disk.

The annotation gained an optional `fingerprint()` element (package version bumped to 1.1.0 —
baselining flagged the MINOR change) and the generator fills it from the same
`GeneratorHelper.getFingerprint(…)` the constant uses, so the two emissions cannot diverge by
construction. When there is no fingerprint the attribute is **omitted entirely** rather than
written empty — `${if;${#fingerprint};…}` in the capability declaration — because
`emf.fingerprint=""` would match a presence filter and read as "declared, but blank". `manual`,
the hand-written model, is the case that proves it.

`FingerprintConstantDriftTest` covers this from the manifest side via `BundleWiring`, and pins
namespace and attribute names as literals: they are what foreign tooling filters on, so a rename
must fail the build rather than silently break consumers. Verified with a deliberately wrong
manifest value — the capability test fails, the constant test stays green, so the two emissions
are independently pinned.

Not in scope, worth a decision later: a `version:Version` attribute would allow range matching
(`Require-Capability: …;filter:="(&(uri=…)(version>=1.5))"`), but the model version is a free-form
string from the `Version` annotation and is not guaranteed to parse as an OSGi version.

A precondition surfaced while implementing: the example models must compile against the
**workspace** api bundle, otherwise generated code referencing a new constant silently builds
against the released one. `basic` and `manual` listed it without a version attribute and were
fixed to `version=snapshot`.

---

### M14 — fp1 must be representation-independent; annotation ignorelist

*Origin: new, from the M13 discussion 2026-07-28. Second Phase 0 gate next to M11.*

**The requirement.** The `.ecore` and the code generated from it are the same model version in two
representations. A content-derived fingerprint must therefore yield the same value for both.
Stated as a contract guarantee: **representation independence** — `.ecore`, generated code, and
serialised XMI of the same model version produce the same fingerprint. This is also what the
unified-persistence concept needs if the fingerprint is to be the join key across storage forms.

**It does not hold today.** fp1 filters only the detail key `documentation`, not annotation
sources (§1). At the same time this project's own bnd template writes build configuration into the
`.ecore` as a GenModel annotation — `complianceLevel`, `oSGiCompatible`, `basePackage` (§1).
Consequences, independent of any equivalence question:

- renaming the Java base package from `com.foo` to `com.bar` changes the fingerprint;
- moving `complianceLevel` from 17 to 21 changes the fingerprint.

Both are semantically identical models with different identities. This is the atlas#156 failure
class with the sign reversed: there two models looked alike, here one model looks like two.
Derived artifacts are not reused although they are valid, and `getPackageMetadataVersions` reports
versions that do not exist.

**The criterion** — so future sources are decided by rule instead of case by case:

> An annotation is fingerprint-relevant if it affects the **observable behaviour of the model** —
> structure, validation, serialisation, mapping, delegates. It is ignorable only if it affects
> solely **how the Java artifact is generated**.

Applied to the local corpus:

| Source / detail | Verdict | Reason |
|---|---|---|
| `ExtendedMetaData` (`kind`, `name`, `namespace`, …) | **in** | Determines the XML wire format; changing it changes the serialised document and breaks consumers of the old shape. |
| Delegates under the Ecore source (`validationDelegates`, `settingDelegates`, `invocationDelegates`, `conversionDelegates`), OCL `body` | **in** | Behaviour. |
| ORM mapping, constraints | **in** | Behaviour — a model whose mapping annotation is missing does not map to the database, so it is not the same model. |
| `Version`, `SuppressWarnings`, unknown/custom sources | **in** | Default is inclusion; an unknown source is domain content until shown otherwise. |
| `basePackage`, `complianceLevel`, `oSGiCompatible` (GenModel source) | **out** | Shape of the Java artifact only. nsURI, structure and serialisation are unchanged. That axis is covered by the bundle version — the fingerprint identifies the model version, not the artifact. |

**Reference direction.** Because domain annotations are identity-relevant, the **`.ecore` is the
semantic reference**. If a fingerprint-relevant annotation is missing at runtime, that is a gap in
the generator — the annotation has to reach the generated package. Only for tooling configuration
is fp1 the thing that gets adjusted. (This reverses an earlier position in the discussion that
took the generated code as the reference; taking it as the reference would canonicalise away
exactly the ORM/OCL case.)

**Shape of the ignorelist.** Two tiers from the start, even though the key tier begins empty —
`Set<String> ignoredSources` plus `Map<String, Set<String>> ignoredKeysBySource`, plus the global
`documentation` key. Costs nothing now, avoids a later rebuild.

Initial fp1 content:

- source `http://www.eclipse.org/emf/2002/GenModel`
- source `http://www.eclipse.org/uml2/2.2.0/GenModel` (a second GenModel namespace exists in the
  workspace, §1 — hence a list, not a single constant)
- key `documentation`, globally (it also occurs under domain sources)

**Rule: every content change to the list is a new scheme tag.** Adding an entry changes the
fingerprint of every model carrying that annotation, and from Phase 1 on those values live in
service properties, manifests and persisted logs. "Maybe key-based later" therefore means `fp2`,
not an edit to fp1 — which is exactly what the tag-addressed seam exists for: both schemes stay
computable side by side.

**Timing.** Before Phase 1, i.e. **before the freeze**. Afterwards the identical change is a break
of a published contract. Cost is low and verified: golden-neutral (see M9).

**Decision (2026-07-28):** **confirmed** — criterion, two-tier list shape and initial content as
stated above (both GenModel sources out, global `documentation` key out, everything else in;
`.ecore` is the semantic reference; any content change to the list = new scheme tag).

**Execution order inverted (2026-07-28): implemented in emf.osgi, not in the donor repo.** The
donor state stays untouched — the new version simply moves. The proof chain works in either
order and stays two separate steps: the verbatim port first (goldens prove the port, #53), the
ignorelist on top (goldens prove neutrality, #52). Precondition holds: no fp1 values circulate
yet — service properties, manifests and persisted logs all start with Phase 1 in emf.osgi. The
ignorelist must land **before Phase 1 exit**; after values circulate it is a scheme bump.

## 4. Clarified understanding — identity semantics (discussion 2026-07-28)

Not open points: these were worked out in discussion and are recorded so they don't get
re-litigated. They inform M4, M9, M13 and M14 but decide none of them.

**Consumers call the fingerprint service, they never implement it.** There is exactly one
implementation, in emf.osgi. eorm, codec, OCL cache etc. call
`fingerprint(ePackage, derivationInputs…)` and key their own artifacts by the result. The
`derivationInputs` are **provider-owned knowledge** — the provider passes its own bundle version,
its OSGi config (e.g. `dialect=postgres`), or the fingerprint of its *own metamodel* (see below).
They are not conjured by the infrastructure.

**Derivation identity vs. result identity.** Two different identities of a derived artifact,
only one of which can be a lookup key:

| | computable | serves as |
|---|---|---|
| `fp(package)` | always | model version identity, the join key |
| `fp(package, derivationInputs…)` | **before** building | address of the derived artifact — resolve-or-build depends on this |
| content hash of the artifact itself | only **after** building | integrity / dedup — atlas-domain, and it does not need fp1 (the artifact owner hashes its own canonical serialization; fp1's canonicalization exists only because EPackages arrive in many representations) |

The lookup key must be computable from the *inputs*, never from the *output* — otherwise
resolve-or-build is a chicken-and-egg.

**The metamodel fingerprint is a good derivation input.** `fp(eorm-metamodel)` versions the
artifact *format* content-derived: metamodel changes → different key → automatic rebuild. It does
not capture changes to the derivation *logic* (generator bugfix, same metamodel) — the provider's
tool/bundle version stays a useful second input.

**Assigned IDs (UUIDs) never go into keys.** They don't exist before the build, and they are not
reproducible across nodes — two nodes deriving the same artifact must arrive at the same key,
which is the entire point of content-derived keys. Assigned IDs live on the **value side** of the
store.

**documentId vs. fingerprint (atlas storage model).** The atlas is not write-once — drafts are
overwritten. The clean split: `documentId` = lineage, stable across overwrites; `fingerprint` =
state, changes with every stand (git analogy: branch ref vs. commit SHA). Immutability is not a
storage discipline but a property of the identity — a state cannot be changed, only a new one
produced, because any change yields a new fingerprint by itself.

**The `ArtifactStore` contract requires no history.** An overwriting store is a valid
implementation: `resolve(fp, typeId)` answers matching-state-or-nothing; a stale fingerprint gets
a miss → the provider rebuilds. Wrong data is impossible, only reuse can be missed. Keeping old
states later is a pure extension (fingerprint property becomes a version selector), no schema
change.

**Draft churn creates no artifacts.** Fingerprinting fires at *registration* in a runtime; atlas
drafts are not registered anywhere, so intermediate states never populate the store.

**Presence-at-registration is guaranteed by ordering, not by locking.** The fingerprint is
computed while building the property dictionary that is *passed to* `registerService(…)` — the
service becomes visible only afterwards, so no consumer can ever observe a registered package
without the property. This holds identically for generated code (`getServiceProperties()`),
extender (`ModelHelper`) and `DynamicPackageLoader`, and is the reason M5 insists on one shared
helper. The cache rebuilds itself after a restart as a side effect of normal re-registration —
no warm-up. Thread safety is benign: the computation is deterministic, so a race costs duplicate
work, never a wrong value.

**Keying rule.** Identity-keyed ⇒ volatile and internal (the M4 cache). Fingerprint-keyed ⇒
stable, shareable, persistable (the `ArtifactStore`). The two compose in sequence
(`instance → fingerprint → artifacts`); neither inherits from the other, and the fingerprint can
never live in the store that is addressed by it.

**EObject widening withdrawn.** All known consumers fingerprint *packages*; the fingerprint API
stays `EPackage`-typed. Instance-data fingerprinting, should it ever become real, is a new scheme
tag with its own entry point. M11 remains what the source document made it: the Merkle/composite
check.

**Sharpened rationale for M9/M14:** once fingerprints are primary keys in a foreign persistence
(content-addressed atlas storage), canonicalization drift no longer produces an inconsistency —
it **corrupts the identity space of another system**. The scheme-tag prefix (`fp1:`) is what
keeps key generations collision-free side by side.

## 5. Sequencing

Ordering follows the dependencies between the points above, not the phase numbers of the source
document.

**Before any code moves**

1. ~~**M11** (seam check)~~ — **decided**: seam confirmed, no pre-freeze signature change;
   `CanonicalizationScheme` stays internal (source-doc promotion dropped). Remaining work: the
   **M14 implementation in emf.osgi, after the verbatim port** (#53 → #52; golden-neutral per
   M9; donor repo stays untouched).
2. ~~**M6** (property name)~~ — **decided**: `emf.fingerprint`; communicate to the atlas team in
   the weekly.
3. ~~**M12** (branch hygiene)~~ — **decided**: no branch split, work continues on
   `metadata-service`.
4. ~~**M1 / M2 / M3 / M13(a)** as one bundle of decisions~~ — **decided 2026-07-28**: fingerprint
   API + constants in the released api bundle, impl as static helper in the core impl bundle
   (both profiles), service registration as facade; metadata API + impl in their own bundle
   `…osgi.metadata` until the genericity gate.

**Phase 1 — fingerprint (unchanged in substance from source §5)**

- Verbatim port with golden tests under the M9 port rule.
- `EMFNamespaces.EMF_MODEL_FINGERPRINT` per M6; emission per M3/M5; caching per M4.
- itests: property present on every emission path (M5); two same-nsURI packages carry distinct
  fingerprints.
- **Exit:** golden tests green, itests green, measured bind cost recorded in M4, snapshot
  published.

**Phase 2 — metadata bundle**

- Model cut with **M7 and M8 decided up front**.
- API per M1; service/whiteboard/index re-cut; WP6 acceptance suite ported.
- Genericity gate (§2.7) — the spikes now validate M7 rather than reopening it.
- **Exit:** WP6 suite green, both spikes pass, API reviewed for semantic versioning.

**Model cut executed 2026-07-29 (#60).** 29 classifiers down to 12. Removed: the codec
vocabulary (`SerializationFormat`, `TypeStrategy`, `IdStrategy`, `IdKeyMode`,
`SuperTypeSelection`, `EnumSerializationStrategy`, `Base*Config`) and the `*Profile` hierarchy,
both returning to the codec repository. Replaced: the five typed aspect classes by one
`AspectEntry` with `typeId`, `content` (containment `EObject`, serializable) and
`transientContent` (`EJavaObject`, transient) per M7. Kept: the mirror tree with its caches,
supertype closure and id features, the operation level per M8, `modelFingerprint` as the primary
key, and the transient `properties` build context.

Three points the cut decided that the decision record did not cover:

- **`AspectEntry` has no typed back-reference.** The donor's aspects each had an `eOpposite` to
  their owner; one entry type contained at four levels cannot, since an `eOpposite` names exactly
  one container type. The owner comes from `eContainer()`.
- **`allDiagnostics` was carried over unchanged, including two gaps** — aspect diagnostics are not
  aggregated (deliberate in the donor), and operation/parameter diagnostics do not reach
  `ClassMetadata` although both are `DiagnosticContainer`. The second looks like a donor defect
  but was *not* silently fixed: the WP6 suite has expectations on it. Decide when porting the
  suite (#62).
- **Base package is `org.eclipse.fennec.emf.osgi.model`, not `…osgi.metadata.model`** as the issue
  proposed. The Java package is `basePackage` + EPackage name, so the issue's layout would have
  required naming the EPackage `model` and shipping `emf.name=model` — too generic a value for a
  global service property. The other direction (`basePackage=…osgi`, name `metadata`) collides
  with the API package, because the generator writes an `@Export`ed `package-info.java` for every
  model package and a hand-written one would clash. Result: generated code in
  `org.eclipse.fennec.emf.osgi.model.metadata` — matching the existing
  `org.eclipse.fennec.emf.osgi.model.info` — and `org.eclipse.fennec.emf.osgi.metadata` left free
  for the API of #61. nsURI is unchanged from the issue.

The new bundle carries `-generate`, and its generated code is committed rather than ignored. It
therefore dogfoods #58/#59 on the first build: `MetadataEPackageConfigurator.FINGERPRINT` and the
`emf.fingerprint` capability attribute are both present, with no generator warnings.

Phases 3 (codec) and 4 (decommission) stay as described in the source document; nothing in this
document changes them — except for one added item: **removing this document** (§6).

## 6. Lifecycle of this document

This is scaffolding, not documentation. It exists to get M1–M12 closed and has no value
afterwards — a repo full of stale migration notes is worse than one without them.

**Removal is part of Phase 4** (decommission, source §5). Checklist:

1. Every point in §3 carries a filled-in *Decision* line.
2. Anything with **lasting** value has been moved to its permanent home *before* deletion —
   this is the part that must not be skipped:
   - the fingerprint contract and the `emf.fingerprint` property → the emf.osgi specification /
     `docs/configuration-guide.md`, wherever the other `EMFNamespaces` properties are described;
   - the caching and bind-cost behaviour decided in M4, and the mutation-after-registration
     contract → Javadoc at the emitting code plus the user-facing docs;
   - the **M14 guarantee** (representation independence) and the ignorelist with its scheme-tag
     rule → into the fp1 contract documentation itself, i.e. the Javadoc of the scheme plus the
     user-facing fingerprint docs. This is the single most important item in this list: it is a
     published guarantee, not a migration note;
   - the **M13 equivalence gate** → a permanent test, not a one-off check. It guards M14 for every
     model added later;
   - decisions that changed the concept (currently M1, M7 — premise correction on D3 —, M11 — no
     API promotion of `CanonicalizationScheme` —, M12 — branch name superseded —, M13 and M14
     deviate from or extend the source document; M10 was withdrawn) → back into
     `emf.model.metadata/docs/migration-to-emf-osgi.md`, which is the permanent record, before
     that repo is archived.
3. Delete `docs/metadata-migration.md`.
4. Delete the **Metadata Migration** entry from the `Key Documents` list in `CLAUDE.md`.
5. Delete the pointer paragraph in the header of
   `emf.model.metadata/docs/migration-to-emf-osgi.md` (or, if that repo is already archived,
   leave it — a dangling link in an archived repo is harmless, a dangling link in a live one is
   not).

Cross-check before deleting: `grep -rn "metadata-migration" .` in both repos, and the
`docs-site/` VitePress config in case the document was picked up by the published site.

## 7. Change log

| Date | Change |
|---|---|
| 2026-07-28 | Document created. M1–M12 opened from the review of `migration-to-emf-osgi.md` against the verified emf.osgi state. |
| 2026-07-28 | **M13** opened (static helper, build-time constant, manifest capability, equivalence gate, pin test). **M14** opened (representation independence, annotation ignorelist, scheme-tag rule) as a second Phase 0 gate. **M10 withdrawn** — its rationale was wrong, superseded by M13. M2/M3 gained the build-time caller argument; M4 reduced in scope to the non-generated paths; M9 gained the golden-neutrality check for M14. §1 extended with the verified facts these rest on. Sequencing and §5 updated. |
| 2026-07-28 | **M1 extended**: one api package per concern (`…osgi.fingerprint`, `…osgi.artifact`). **Work tracking created**: parent issue #51, sub-issues #52–#63. |
| 2026-07-28 | **M14 execution order inverted**: implemented in emf.osgi on top of the verbatim port (#53 → #52) instead of in the donor repo — the donor state stays untouched, the proof chain (goldens) covers both steps in either order. |
| 2026-07-28 | **M11 decided** after the seam check: no signature change needed — composite root hashes `(uri, fp)` pairs from single-package calls, Merkle root fits the value format, extensions are additive under `@ProviderType`. Condition: `CanonicalizationScheme` stays internal; the source doc's API promotion is dropped. |
| 2026-07-28 | **M14 confirmed** (criterion, two-tier ignorelist, initial content; implementation in donor repo pending). **M12 decided**: no branch split — snapshot workflow, recommendation not adopted. **M6 decided**: `emf.fingerprint` in the semantics of the existing `emf.*` properties; atlas informed via weekly. |
| 2026-07-28 | **M8 decided**: operation-level attachment point stays in the slim model. |
| 2026-07-28 | **M7 decided**: (b) — EObject containment as primary slot plus transient EJavaObject escape hatch, fixed at the model cut. Premise of source-doc D3 corrected: OCL constraints are held as `OCLExpression` EObjects with serialization as the goal (transferable cache). |
| 2026-07-28 | **M5 decided** as recommended: one shared property helper, four emission sites (`FennecXMLResourceFactory` excluded, `DefaultEPackageRegistryComponent` to verify), itest per emission path. |
| 2026-07-28 | **M4 decided** as recommended: eager at bind, in-memory identity cache per JVM run, internal/not pluggable (persistence seam is the `ArtifactStore` alone), mutation after registration out of contract; measurement stays a Phase 1 exit criterion. |
| 2026-07-28 | **§4 added** (clarified identity semantics): consumers call, never implement; derivation vs. result identity; metamodel fp as derivation input; no assigned IDs in keys; documentId vs. fingerprint; `ArtifactStore` needs no history; presence-at-registration by ordering; keying rule; EObject widening withdrawn; sharpened M9/M14 rationale. Later sections renumbered (Sequencing §5, Lifecycle §6, Change log §7). |
| 2026-07-28 | **M1, M2, M3 and M13(a) decided.** M1 = (b): fingerprint API into the released api bundle, metadata API into its own bundle until the gate. M2 = (a), reversing the document's own recommendation: M3's structural-presence requirement makes a separate fingerprint bundle a hard wire dependency with no gain — impl goes into the core impl bundle, both profiles, minimal-profile growth accepted consciously. M3 = (a): static helper internally, service registration as facade. M13(b)/(c) stay open pending the equivalence gate. |
