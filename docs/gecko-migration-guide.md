# Migrating from GeckoEMF to Fennec EMF OSGi

GeckoEMF (last release: `org.geckoprojects.emf:*:6.3.1`) was donated to the
Eclipse Foundation and continues as Eclipse Fennec EMF OSGi
(`org.eclipse.fennec.emf:*`, first release 1.0.0). This guide covers the
mechanical migration: artifact coordinates, package renames, code generation
and the compatibility layer for incremental migration.

## Prerequisites

- Java 21 or newer (GeckoEMF 6.x ran on older JDKs; Fennec requires 21).
- Fennec artifacts are on Maven Central under the group id
  `org.eclipse.fennec.emf`.

## Artifact mapping

Old group id: `org.geckoprojects.emf` — new group id: `org.eclipse.fennec.emf`.
All Fennec artifacts are released as version **1.0.0**.

| GeckoEMF (6.3.1) | Fennec EMF OSGi (1.0.0) |
|---|---|
| `org.gecko.emf.osgi.api` | `org.eclipse.fennec.emf.osgi.api` |
| `org.gecko.emf.osgi.component` | `org.eclipse.fennec.emf.osgi.component` |
| `org.gecko.emf.osgi.component.config` | *absorbed into* `org.eclipse.fennec.emf.osgi.component` (all-in-one bundle, exports `…components.config`) |
| `org.gecko.emf.osgi.component.minimal` | `org.eclipse.fennec.emf.osgi.component.minimal` |
| `org.gecko.emf.osgi.codegen` | `org.eclipse.fennec.emf.osgi.codegen` |
| `org.gecko.emf.osgi.extender` | `org.eclipse.fennec.emf.osgi.extender` |
| `org.gecko.emf.osgi.bom` | `org.eclipse.fennec.emf.osgi.bom` |
| `org.gecko.emf.osgi.example.model.*` | `org.eclipse.fennec.emf.osgi.example.model.*` |
| `org.gecko.emf.osgi.bnd.library.workspace` / `.project` | `org.eclipse.fennec.emf.osgi.bnd.library.workspace` / `.project` |
| — (new) | `org.eclipse.fennec.emf.osgi.model.info` (runtime model introspection) |
| — (new) | `org.eclipse.fennec.emf.gecko.compatibility.api` (see below) |
| — (new) | `org.eclipse.fennec.emf.ecore.tool` (Ecore validation CLI) |

Not part of Fennec 1.0.0 (no counterpart yet): `org.gecko.emf.osgi.bson`,
`org.gecko.emf.collections`, `org.gecko.emf.osgi.doc.mermaid`,
`org.gecko.emf.osgi.doc.plantuml`. If you depend on one of these, open an
issue at <https://github.com/eclipse-fennec/emf.osgi/issues>.

BOM usage:

```xml
<dependency>
    <groupId>org.eclipse.fennec.emf</groupId>
    <artifactId>org.eclipse.fennec.emf.osgi.bom</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

## Package renames

All API packages move 1:1 from the `org.gecko.emf.osgi` namespace to
`org.eclipse.fennec.emf.osgi`:

| Old package | New package |
|---|---|
| `org.gecko.emf.osgi` | `org.eclipse.fennec.emf.osgi` |
| `org.gecko.emf.osgi.configurator` | `org.eclipse.fennec.emf.osgi.configurator` |
| `org.gecko.emf.osgi.annotation.*` | `org.eclipse.fennec.emf.osgi.annotation.*` |
| `org.gecko.emf.osgi.constants` | `org.eclipse.fennec.emf.osgi.constants` |
| `org.gecko.emf.osgi.model.info` | `org.eclipse.fennec.emf.osgi.model.info` |

In most code bases the migration is a search-and-replace of
`org.gecko.emf.osgi` → `org.eclipse.fennec.emf.osgi` in imports, plus the
artifact swap above. The service property names (`emf.name`, `emf.nsURI`,
`emf.fileExtension`, …) are unchanged, so existing `@Reference` target
filters keep working.

## Code generation

The generator's bnd external-plugin command was renamed `geckoEMF` →
`fennecEMF`.

**BND workspace** (`bnd.bnd`):

```properties
-generate: \
    model/mymodel.genmodel; \
        generate=fennecEMF; \
        genmodel=model/mymodel.genmodel; \
        output=src
```

**Maven** (`bnd-generate-maven-plugin`): swap the `externalPlugins`
dependency to `org.eclipse.fennec.emf:org.eclipse.fennec.emf.osgi.codegen:1.0.0`
and the `generateCommand` to `fennecEMF`. See the
[Maven code generation guide](maven-codegen.md) for a complete `pom.xml`
example.

Regenerate the model code after the switch — the generated
`EPackageConfigurator` / `ConfigurationComponent` classes reference the new
package namespace.

## The compatibility layer

`org.eclipse.fennec.emf:org.eclipse.fennec.emf.gecko.compatibility.api:1.0.0`
allows a runtime to mix migrated and unmigrated bundles. It contains:

- The most important legacy interfaces in their **old** `org.gecko.emf.osgi`
  packages (deprecated): `ResourceSetFactory`, `ResourceSetCache`,
  `UriHandlerProvider`, `UriMapProvider`, `Detachable`,
  `HughDataResourceSet`, the `org.gecko.emf.osgi.configurator.*` interfaces
  and `org.gecko.emf.osgi.model.info.EMFModelInfo`.
- Bridge components that translate services in **both directions**:
  - Legacy `EPackageConfigurator`, `ResourceFactoryConfigurator`,
    `ResourceSetConfigurator`, `UriHandlerProvider` and `UriMapProvider`
    services registered by unmigrated bundles are re-registered as their
    Fennec counterparts (with converted service properties), so old model
    bundles keep contributing to the Fennec registries.
  - Fennec `ResourceSetFactory` and `EMFModelInfo` services are re-registered
    under the legacy interfaces, so unmigrated consumers can still inject
    `org.gecko.emf.osgi.ResourceSetFactory`.

**When to use it:** you cannot migrate everything at once — e.g. third-party
model bundles compiled against GeckoEMF, or a large code base migrated module
by module. Install the compatibility bundle alongside the Fennec runtime and
migrate incrementally; remove it once nothing imports `org.gecko.emf.osgi.*`
anymore.

**When not to use it:** for a one-shot migration of a code base you control,
skip it — do the package/artifact rename and regenerate the model code.

## Migration checklist

1. Replace `org.geckoprojects.emf` artifacts with their
   `org.eclipse.fennec.emf` counterparts (table above), version 1.0.0.
2. Search-and-replace imports: `org.gecko.emf.osgi` →
   `org.eclipse.fennec.emf.osgi`.
3. Rename the codegen command `geckoEMF` → `fennecEMF` (bnd `-generate` or
   Maven `generateCommand`) and point it at the
   `org.eclipse.fennec.emf.osgi.codegen` artifact; regenerate the model code.
4. Rebuild; fix any leftover references (the old packages no longer resolve
   unless the compatibility bundle is on the classpath).
5. Runtime with unmigrated bundles? Add
   `org.eclipse.fennec.emf.gecko.compatibility.api` to the runtime and remove
   it once the last GeckoEMF consumer is gone.
