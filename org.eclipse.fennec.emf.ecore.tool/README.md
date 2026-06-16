# Fennec EMF Ecore Tool

A standalone, **plain-Java** command line tool that loads an Ecore/XMI file with a
reference EMF runtime and runs one of several modes. It is intended for
**cross-language EMF conformance / round-trip testing**: EMF implementations
written in other languages can be checked against this tool to ensure they
produce valid Ecore files and valid XMI.

The build (`bnd`) produces a self-contained, runnable "fat" jar — all referenced
EMF classes (`org.eclipse.emf.common`, `org.eclipse.emf.ecore`,
`org.eclipse.emf.ecore.xmi`) are inlined, so it runs anywhere with just a JRE,
no OSGi framework or Eclipse required.

## Build

```sh
./gradlew :org.eclipse.fennec.emf.ecore.tool:build
```

The runnable jar is written to
`generated/org.eclipse.fennec.emf.ecore.tool.jar`.

## Usage

```sh
java -jar org.eclipse.fennec.emf.ecore.tool.jar [mode] [options] <input-file>
```

### Modes (default: `--print`)

| Mode          | Description                                                                 |
|---------------|-----------------------------------------------------------------------------|
| `--validate`  | Runs the EMF validation mechanism (`Diagnostician`) and prints the findings. Exits with code `1` if validation errors are found. |
| `--roundtrip` | Loads the model and serializes it again (load → save round-trip test).      |
| `--print`     | Prints the structure of the loaded `EPackage`(s): classifiers, attributes, references, operations, enum literals. |

### Options

| Option                | Description                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------|
| `-o`, `--output <file>` | Write the result to `<file>` instead of the console. In `--roundtrip` mode this is the re-serialized model (default: `<input>.roundtrip.<ext>` next to the input). |
| `-h`, `--help`        | Show help.                                                                                    |

### Exit codes

| Code | Meaning                                                        |
|------|----------------------------------------------------------------|
| `0`  | Success (model loaded; in `--validate` mode also: no errors).  |
| `1`  | Validation reported at least one error.                        |
| `2`  | Usage error, or the model could not be loaded.                 |

## Examples

```sh
# Print the structure of a model
java -jar org.eclipse.fennec.emf.ecore.tool.jar model.ecore

# Validate and fail the build on errors (CI-friendly exit code)
java -jar org.eclipse.fennec.emf.ecore.tool.jar --validate model.ecore

# Round-trip: re-serialize to a known file, then diff against the input
java -jar org.eclipse.fennec.emf.ecore.tool.jar --roundtrip -o out.ecore model.ecore
diff model.ecore out.ecore
```

## Note on message resources

EMF resolves `Diagnostician`/validation message strings from a root
`plugin.properties`. Because a flat fat jar collapses all EMF bundles into one
root, this module ships a single merged `resources/plugin.properties`
(concatenated from the `common`, `ecore.xmi` and `ecore` bundles). Regenerate it
if the EMF dependency version changes.
