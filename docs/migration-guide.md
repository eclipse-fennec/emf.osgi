<!--
  Copyright (c) Contributors to the Eclipse Foundation.
  This program and the accompanying materials are made available under the
  terms of the Eclipse Public License 2.0 AND the Creative Commons CC0-1.0
  which are available at https://www.eclipse.org/legal/epl-2.0/ and
  https://creativecommons.org/publicdomain/zero/1.0/ .
  SPDX-License-Identifier: EPL-2.0 AND CC0-1.0

  This document was drafted with the assistance of an AI agent (Claude Code)
  and reviewed and verified by a human project committer before merge.
-->

# Migrating from GeckoEMF to Fennec EMF OSGi

GeckoEMF has been donated to the Eclipse Foundation and continues as
**Eclipse Fennec EMF OSGi**. The functionality is unchanged — the migration is
almost entirely a mechanical rename of coordinates, packages, and a handful of
build instructions. Service properties and filters do **not** change, and a
[compatibility layer](#the-compatibility-layer) lets existing consumers migrate
incrementally instead of in one step.

## At a glance

| What | GeckoEMF (old) | Fennec EMF OSGi (new) |
|---|---|---|
| Maven groupId | `org.geckoprojects.emf` | `org.eclipse.fennec.emf` |
| Java packages | `org.gecko.emf.osgi.*` | `org.eclipse.fennec.emf.osgi.*` |
| bnd workspace library | `-library: geckoEMF` | `-library: fennecEMF` |
| bnd project library | `-library: enable-emf` | `-library: enableEMF` |
| Code generator directive | `generate="geckoEMF"` | `generate="fennecEMF"` |
| Service properties & filters | `emf.name`, `emf.nsURI`, … | **unchanged** |

If you only remember one thing: rename `org.gecko.emf.osgi` → `org.eclipse.fennec.emf.osgi`
and `gecko`/`geckoEMF` → `fennec`/`fennecEMF` throughout, and leave your
`emf.*` service filters alone.

---

## 1. Dependencies

The Maven groupId changed from `org.geckoprojects.emf` to
`org.eclipse.fennec.emf`. Released artifacts are published to
[Maven Central](https://repo1.maven.org/maven2/org/eclipse/fennec/emf/);
snapshots from the `snapshot` branch go to
[Sonatype Central snapshots](https://central.sonatype.com/repository/maven-snapshots/org/eclipse/fennec/emf/).

The simplest way to line up a consistent set of versions is the **BOM**:

```
org.eclipse.fennec.emf:org.eclipse.fennec.emf.osgi.bom:${fennec.version}
```

The BOM aggregates the Fennec EMF API together with the required EMF
(`org.eclipse.emf.common`, `org.eclipse.emf.ecore`, `org.eclipse.emf.ecore.xmi`)
and OSGi annotation/service bundles, so you no longer have to pin those
versions individually.

Replace any direct dependency on the old `org.geckoprojects.emf:*` artifacts
with the corresponding `org.eclipse.fennec.emf:*` artifact (the artifact IDs
follow the bundle renames in the [next section](#2-bnd-workspace-setup)).

---

## 2. bnd workspace setup

If you consume Fennec EMF from a bnd/Bndtools workspace, two `-library`
instructions were renamed:

- **Workspace library** (in `cnf/build.bnd`) — registers the Maven repository
  with the EMF dependencies:

  ```properties
  # old
  -library: geckoEMF
  # new
  -library: fennecEMF
  ```

  The generated repository index was likewise renamed from `geckoEMF.maven` to
  `fennecEMF.maven`; if you referenced it directly, update the name.

- **Project library** (in a module's `bnd.bnd`) — puts the Fennec EMF API on the
  build path and enables the model project template:

  ```properties
  # old
  -library: enable-emf
  # new
  -library: enableEMF
  ```

Bundle symbolic names followed the package rename — for example
`org.gecko.emf.osgi.api` → `org.eclipse.fennec.emf.osgi.api`,
`org.gecko.emf.osgi` → `org.eclipse.fennec.emf.osgi`. Update any explicit
`-buildpath` or `-runbundles` entries accordingly.

---

## 3. Java code

Rename the package prefix everywhere in your imports:

```
org.gecko.emf.osgi.*   →   org.eclipse.fennec.emf.osgi.*
```

The public API kept the same type names, so this is a pure package move:

| Type | New fully-qualified name |
|---|---|
| `ResourceSetFactory` | `org.eclipse.fennec.emf.osgi.ResourceSetFactory` |
| `EPackageConfigurator` | `org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator` |
| `ResourceSetConfigurator` | `org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator` |
| `ResourceFactoryConfigurator` | `org.eclipse.fennec.emf.osgi.configurator.ResourceFactoryConfigurator` |
| `UriHandlerProvider` / `UriMapProvider` | `org.eclipse.fennec.emf.osgi.UriHandlerProvider` / `…UriMapProvider` |
| `@RequireEMF` | `org.eclipse.fennec.emf.osgi.annotation.require.RequireEMF` |
| `EMFNamespaces` (constants) | `org.eclipse.fennec.emf.osgi.constants.EMFNamespaces` |
| `EMFModelInfo` | `org.eclipse.fennec.emf.osgi.model.info.EMFModelInfo` |

### What does *not* change

The OSGi **service property and capability namespaces are unchanged**. The
`EMFNamespaces` constants still use the `emf.` prefix and the `emf.core`
capability namespace — for example `emf.name`, `emf.nsURI`, `emf.model.scope`,
and the model-scope values `static` / `resourceset` / `generated`. Consumer
service filters therefore need **no** edits:

```java
// still valid, before and after the migration
@Reference(target = "(emf.name=mymodel)")
private ResourceSet resourceSet;
```

The extender capability filter is also unchanged:

```properties
Require-Capability: osgi.extender; filter:="(osgi.extender=emf.model)"
```

---

## 4. Code generator

Model projects that generate OSGi-compatible code with the BND external plugin
only need the generator directive renamed in their `bnd.bnd`:

```properties
-generate: \
    model/mymodel.genmodel; \
        generate="fennecEMF"; \
        genmodel=model/mymodel.genmodel; \
        output=src-gen
```

`generate="geckoEMF"` becomes `generate="fennecEMF"`. The generator itself is
now provided by the `org.eclipse.fennec.emf.osgi.codegen` bundle, and the
GenModel toggle that switches OSGi code generation on is unchanged — set
**GenModel → All → OSGi Compatible** to `true`.

Nothing inside your `.ecore` / `.genmodel` needs to change: model namespace
URIs (`nsURI`) are your own and are left as-is. Regenerate the model after the
rename so the generated `src-gen/` code references the new packages.

---

## The compatibility layer

For a code base that cannot be renamed in one pass, the
`org.eclipse.fennec.emf.gecko.compatibility.api` bundle lets old and new code
coexist. It works in two ways:

1. **Old API packages are re-exported.** The bundle exports the deprecated
   `org.gecko.emf.osgi`, `org.gecko.emf.osgi.configurator`, and
   `org.gecko.emf.osgi.model.info` packages, so code still importing the old
   package names continues to compile and resolve against it.
2. **Runtime service bridging.** Declarative Services wrapper components track
   services published under the old `org.gecko.emf.osgi` interfaces and
   re-register them under the corresponding `org.eclipse.fennec.emf.osgi`
   interfaces (and vice versa), forwarding the service properties. This lets a
   partially-migrated system wire old providers to new consumers and back while
   you migrate module by module.

To use it, add the compatibility bundle to your runtime alongside the Fennec EMF
bundles.

### Limitations

The compatibility layer is a **migration aid, not a permanent API**. Everything
it exposes is marked `@Deprecated`, and it advertises the old `emf.core`
capability as `deprecated=true`. Some bridges are not perfect one-to-one
mappings — notably `ResourceFactoryConfigurator` is annotation-based in the new
API, and the `ResourceSetFactory` bridge does not reconstruct the full set of
configurators. Treat the layer as scaffolding: add it to get a large code base
building and running quickly, then remove it once you have completed the
package rename described above.

---

## Migration checklist

1. Change the Maven groupId `org.geckoprojects.emf` → `org.eclipse.fennec.emf`
   (ideally via the `org.eclipse.fennec.emf.osgi.bom`).
2. In bnd workspaces, rename `-library: geckoEMF` → `fennecEMF` (workspace) and
   `-library: enable-emf` → `enableEMF` (project).
3. Rename Java imports `org.gecko.emf.osgi.*` → `org.eclipse.fennec.emf.osgi.*`
   — or add `org.eclipse.fennec.emf.gecko.compatibility.api` to defer this.
4. Rename the code generator directive `generate="geckoEMF"` →
   `generate="fennecEMF"` and regenerate.
5. Leave `emf.*` service properties and filters unchanged.
6. Build, resolve your `.bndrun`, and run your integration tests to confirm the
   migrated bundles wire up as before.

## See also

- [Configuration Guide](configuration-guide.md) — configuring EMF components via OSGi Configuration Admin
- [Model Extender](../org.eclipse.fennec.emf.osgi.extender/readme.md) — automatic model registration from bundles
