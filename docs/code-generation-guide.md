# Code Generation Guide

This guide explains how to generate OSGi-compatible EMF model code with the Fennec EMF code generator (`fennecEMF`) — from the basic setup in a bnd workspace or Maven build to referencing models that live in **other bundles**.

The generator (`org.eclipse.fennec.emf.osgi.codegen`) extends the standard EMF generator. In addition to the plain model code it produces:

- an **`EPackageConfigurator`** that registers the EPackage in the appropriate registry
- a **`ConfigurationComponent`** — a DS component that registers all model services (`EPackage`, `EFactory`, `Resource.Factory`, `Condition`)
- an **`@EPackage` annotation** on the generated package interface, which bnd turns into an `org.eclipse.emf.ecore.generated_package` **Provide-Capability** on the bundle

The generator can be driven from a **bnd workspace** (via `-generate` in `bnd.bnd`) or from a **plain Maven build** (via the `bnd-generate-maven-plugin`). Both are covered below.

---

## Setup in a bnd Workspace

### Prerequisites

- A bnd workspace with the Fennec EMF library enabled (`-library: fennec` in `cnf/build.bnd`) — this makes the `fennecEMF` generator available. See the [README](../README.md) for the Getting Started steps.
- A `.ecore` / `.genmodel` pair, conventionally in the project's `model/` folder.
- The genmodel property **OSGi Compatible** set to `true` (attribute `oSGiCompatible="true"` in the `.genmodel` file). Without it, the OSGi-specific artifacts are not generated.

### `bnd.bnd`

```properties
# src-gen holds the generated code
src=${^src},src-gen

-generate: \
    model/mymodel.genmodel; \
        generate=fennecEMF; \
        genmodel=model/mymodel.genmodel; \
        output=src-gen; \
        logfile=codegen.log

# ship the model files inside the bundle
-includeresource.model: model=model
```

The first line of the instruction (`model/mymodel.genmodel`) is the **source** bnd watches for changes; the `genmodel` attribute tells the generator which genmodel to load. Both usually point to the same file.

### Instruction Attributes

| Attribute | Default | Description |
|---|---|---|
| `generate` | — | Must be `fennecEMF` — selects the Fennec EMF external plugin |
| `genmodel` | — | Project-relative path of the genmodel to generate (required) |
| `output` | `src-gen` | Output folder for the generated sources |
| `logfile` | — | Project-relative file the generator writes its log to. **Set this** — it is the main debugging aid, especially for reference-resolution problems (see [Troubleshooting](#troubleshooting)) |
| `lineEndings` | `system` | Line endings of the generated files: `system` (platform default; an existing file keeps its current delimiter), `lf`, or `crlf`. Set a fixed value to avoid line-ending churn when the code is generated on different operating systems. Resolution order: this attribute always wins; without it, an Eclipse project-specific line delimiter (`line.separator` in `.settings/org.eclipse.core.runtime.prefs`, set via Project Properties → Resource → *New text file line delimiter*) is used; otherwise the system default applies |
| `genmodelIncludeLocation` | — | Folder the genmodel is shipped under inside the bundle, when it is **not** included at its source path (used to compute the paths in the generated `@EPackage` annotation) |
| `includeGenModelAttr` | `true` | Include the `genModel` attribute in the generated `@EPackage` annotation / capability |
| `includeGenModelSourceLocationsAttr` | `true` | Include the `genModelSourceLocations` attribute |
| `includeEcoreAttr` | `true` | Include the `ecore` attribute |
| `includeEcoreSourceLocationsAttr` | `true` | Include the `ecoreSourceLocations` attribute |

In addition, bnd's own `-generate` attributes (e.g. `clean`) apply as usual — see the [bnd documentation](https://bnd.bndtools.org/instructions/generate.html).

---

## Setup in a Maven Build

In a Maven build the generator is invoked through bnd's [`bnd-generate-maven-plugin`](https://github.com/bndtools/bnd/tree/master/maven-plugins/bnd-generate-maven-plugin), which loads `fennecEMF` as an external plugin from the codegen artifact:

```xml
<dependencies>
    <dependency>
        <groupId>org.eclipse.fennec.emf</groupId>
        <artifactId>org.eclipse.fennec.emf.osgi.api</artifactId>
        <version>${fennec.emf.version}</version>
    </dependency>
    <!-- provider bundles whose models you reference (see next section) -->
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>biz.aQute.bnd</groupId>
            <artifactId>bnd-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>biz.aQute.bnd</groupId>
            <artifactId>bnd-generate-maven-plugin</artifactId>
            <version>${bnd.version}</version>
            <configuration>
                <externalPlugins>
                    <dependency>
                        <groupId>org.eclipse.fennec.emf</groupId>
                        <artifactId>org.eclipse.fennec.emf.osgi.codegen</artifactId>
                        <version>${fennec.emf.version}</version>
                    </dependency>
                </externalPlugins>
                <steps>
                    <step>
                        <trigger>src/main/resources/model/mymodel.genmodel</trigger>
                        <generateCommand>fennecEMF</generateCommand>
                        <output>src/main/java</output>
                        <clear>false</clear>
                        <properties>
                            <genmodel>src/main/resources/model/mymodel.genmodel</genmodel>
                            <logfile>codegen.log</logfile>
                        </properties>
                    </step>
                </steps>
            </configuration>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Notes:

- `<trigger>` plays the role of the instruction source (the generator re-runs when it changes), `<generateCommand>` selects the `fennecEMF` plugin, and everything from the [attribute table](#instruction-attributes) above goes into `<properties>`.
- The module needs a **`bnd.bnd` file in its root** (an empty one is fine) so the generate plugin treats it as a valid bnd project.
- With `<output>src/main/java</output>` the generated code lands in the regular source folder; models placed under `src/main/resources/model/` end up at `model/` inside the jar automatically, so no extra include instruction is needed.
- The module's **Maven `<dependencies>` play the role of the `-buildpath`** when the generator resolves references to models of other bundles (next section).

A complete real-world example is the [sensiNact `testdata` module](https://github.com/eclipse-sensinact/org.eclipse.sensinact.gateway/tree/master/core/models/testdata), whose genmodel additionally references the model of its `provider` dependency (it still uses the generator's former GeckoEMF coordinates — `org.geckoprojects.emf:org.gecko.emf.osgi.codegen` with `generateCommand=geckoEMF`; for Fennec use the coordinates shown above).

---

## The `generated_package` Capability

The generated package interface carries the `@EPackage` annotation from `org.eclipse.fennec.emf.osgi.annotation.provide`. bnd translates it into a bundle capability like:

```
Provide-Capability: org.eclipse.emf.ecore.generated_package; \
    class="com.example.model.MyModelPackage"; \
    uri="http://example.com/mymodel"; \
    genModel="model/mymodel.genmodel"; \
    genModelSourceLocations:List<String>="model/mymodel.genmodel,my.project.name/model/mymodel.genmodel"
```

This capability advertises where the model files live **inside the jar**. It is what makes the bundle referencable by genmodels of other bundles (next section), and it lets the Eclipse ecore editor find the model in a repository.

---

## Referencing Models from Other Bundles

A common scenario: your model extends or uses types from another model whose code is **already generated** and shipped in another bundle. In that case the other model must not be regenerated — your genmodel references the provider's genmodel via `usedGenPackages`, and the generated code simply imports the provider's packages.

This works both when the provider is a **sibling project in the same workspace** and when it is a **jar from a repository** on your `-buildpath`.

### Supported URI Forms

References in the `.ecore` / `.genmodel` may use any of these forms:

| Form | Typically produced by |
|---|---|
| `../../<name>/model/other.genmodel#//<pkg>` | The Eclipse genmodel editor (relative path between workspace projects) |
| `platform:/resource/<name>/model/other.genmodel#//<pkg>` | Eclipse PDE tooling |
| `platform:/plugin/<name>/model/other.genmodel#//<pkg>` | Eclipse PDE tooling |
| `resource://<name>/model/other.genmodel#//<pkg>` | Manual editing (the generator's native scheme) |

All forms are normalized to `resource://<name>/<path>`, and **`<name>` — the first path segment — is treated as a Bundle-SymbolicName (BSN)**. This is why the common relative idiom `../../<projectName>/...` works in both setups, as long as the project **directory name equals the BSN** (the bnd convention).

### How a Reference Is Resolved

For each referenced model URI the generator tries, in order:

1. **The current project** — if `<name>` equals the project's own BSN, the file is read from the project directory.
2. **The `-buildpath`** (in Maven builds: the module's dependencies) — every buildpath container whose manifest `Bundle-SymbolicName` equals `<name>` is checked, and the referenced file is read **from inside that jar**. The candidate model files per jar are collected from:
   - the `org.eclipse.emf.ecore.generated_package` capability (`genModel`, `genModelSourceLocations`, `ecore`, `ecoreSourceLocations`, `uri` attributes), and
   - a scan of the jar for `*.ecore`, `*.genmodel`, and `*.uml` resources.
3. **A relative filesystem path** as a last resort — the original URI is resolved relative to the project directory, so `../../<siblingProject>/model/other.genmodel` finds the checked-out sibling project in the same workspace directly on disk.

### Provider-Side Requirements

For a bundle consumed from a repository (Maven, OBR, …) to be referencable, it must:

1. **Ship its model files inside the jar**, e.g.:

   ```properties
   -includeresource.model: model=model
   ```

   The path part of the reference URI (after the BSN segment) must match either the location **inside the jar** — a reference `../../<bsn>/model/other.genmodel` expects `model/other.genmodel` in the jar — or one of the **source locations** advertised by the capability (see below).

2. Ideally, **provide the `generated_package` capability** — automatic when the provider's code was generated with `fennecEMF`. The jar scan in step 2 above finds `*.genmodel`/`*.ecore` files even without the capability, but the capability makes the contract explicit and adds the **source-location mappings**: `genModelSourceLocations` / `ecoreSourceLocations` map source-tree paths to the actual jar location. This matters for Maven-built providers, where the genmodel lives at `src/main/resources/model/other.genmodel` in the source tree but at `model/other.genmodel` inside the jar — the capability lets a reference using the source-tree path (as the Eclipse editor writes it) resolve against the jar anyway.

### Consumer-Side Setup

1. Put the provider bundle on the `-buildpath`:

   ```properties
   -buildpath: \
       com.example.provider.model;version=latest,\
       ...
   ```

2. Reference the provider's genmodel in your `.genmodel` via `usedGenPackages`, and its ecore in your `.ecore` where types are used:

   ```xml
   <genmodel:GenModel ...
       usedGenPackages="../../com.example.provider.model/model/provider.genmodel#//provider">
   ```

   ```xml
   <eClassifiers xsi:type="ecore:EClass" name="MyClass"
       eSuperTypes="../../com.example.provider.model/model/provider.ecore#//BaseClass"/>
   ```

The generated code then imports the provider's packages instead of regenerating them, and the generated `ConfigurationComponent` requires the provider's `EPackage` service at runtime.

### Working Example

The workspace of this repository contains a complete consumer/provider pair:

- Provider: [`org.eclipse.fennec.emf.osgi.example.model.basic`](https://github.com/eclipse-fennec/emf.osgi/tree/main/org.eclipse.fennec.emf.osgi.example.model.basic)
- Consumer: [`org.eclipse.fennec.emf.osgi.example.model.extended`](https://github.com/eclipse-fennec/emf.osgi/tree/main/org.eclipse.fennec.emf.osgi.example.model.extended)

The consumer's genmodel references **both** a sibling workspace project (`basic`, resolved via the relative filesystem path) and a repository jar (`Ecore.genmodel` from the `org.eclipse.emf.ecore` bundle, resolved via the BSN match against the buildpath):

```xml
usedGenPackages="../../org.eclipse.fennec.emf.osgi.example.model.basic/other/main/resources/model/basic.genmodel#//basic
                 ../../org.eclipse.emf.ecore/model/Ecore.genmodel#//ecore"
```

For a Maven-based pair, see the sensiNact [`testdata` module](https://github.com/eclipse-sensinact/org.eclipse.sensinact.gateway/tree/master/core/models/testdata) (consumer) referencing the model of its `provider` Maven dependency using a source-tree path that resolves through the capability's source-location mapping:

```xml
usedGenPackages="../../../../../org.eclipse.sensinact.gateway.core.models.provider/src/main/resources/model/sensinact.genmodel#//provider"
```

---

## Troubleshooting

Set the `logfile` attribute on the `-generate` instruction (in Maven: the `<logfile>` property of the step). The generator logs its complete setup (BSN, project directory, all buildpath containers with their candidate model files) and **every resolution attempt** for every referenced URI — which containers were compared, which paths were tried, and where it gave up. When a reference does not resolve, this log is the place to look.

Common pitfalls:

| Symptom | Likely Cause |
|---|---|
| Reference to a repo jar does not resolve | The first path segment of the reference does not match the provider's BSN, or the provider bundle is missing from `-buildpath` |
| Reference resolves in the workspace but not from the repo jar | The provider jar does not ship the model files (missing `-includeresource.model`), or ships them at a different path than the reference expects |
| `NullPointerException` in `setImportManager` during generation | A referenced genmodel could not be loaded — check the log for the failed URI resolution |
| Generated code embeds the referenced packages instead of importing them | `usedGenPackages` is missing in the consuming genmodel, or points at the wrong `#//<pkg>` fragment |
| Workspace project reference silently resolves to an **old released** artifact | A baseline/release repository also offers the provider bundle; qualify workspace `-buildpath` entries with `;version=project` (or `;version=snapshot`) so the workspace project wins |
