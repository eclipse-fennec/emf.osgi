# EMF Code Generation with Maven

The Fennec EMF code generator is a standard
[bnd external plugin](https://bnd.bndtools.org/chapters/880-plugins.html)
(command name `fennecEMF`), so it works with the stock
[`bnd-generate-maven-plugin`](https://github.com/bndtools/bnd/tree/master/maven-plugins/bnd-generate-maven-plugin)
— no Fennec-specific Maven machinery required. It generates OSGi-compatible
EMF model code (including the `EPackageConfigurator` and the DS
`ConfigurationComponent`) from a `.genmodel`.

## pom.xml

```xml
<build>
    <plugins>
        <plugin>
            <groupId>biz.aQute.bnd</groupId>
            <artifactId>bnd-generate-maven-plugin</artifactId>
            <version>7.1.0</version>
            <configuration>
                <externalPlugins>
                    <dependency>
                        <groupId>org.eclipse.fennec.emf</groupId>
                        <artifactId>org.eclipse.fennec.emf.osgi.codegen</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </externalPlugins>
                <steps>
                    <step>
                        <!-- Regenerate when the model changes (ant-style fileset) -->
                        <trigger>src/main/resources/model/mymodel.genmodel</trigger>
                        <generateCommand>fennecEMF</generateCommand>
                        <output>src/main/java</output>
                        <properties>
                            <genmodel>src/main/resources/model/mymodel.genmodel</genmodel>
                        </properties>
                    </step>
                </steps>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Generator properties

Passed via the step's `<properties>` map:

| Property | Required | Meaning |
|---|---|---|
| `genmodel` | yes | Path to the `.genmodel` to generate from |
| `output` | no | Output folder (the step's `<output>` element serves the same purpose) |
| `genmodelIncludeLocation` | no | Where the genmodel ends up in the built bundle, if not in the default `model/` folder |

## Notes

- The `.ecore` / `.genmodel` files must be resolvable relative to each other;
  keep them side by side in the model folder.
- Include the model folder in the bundle so the models are available at
  runtime (e.g. via `-includeresource: model=src/main/resources/model` in the
  bnd instructions of the `bnd-maven-plugin`).
- The generated code depends on `org.eclipse.fennec.emf.osgi.api` and the EMF
  runtime — the easiest way to get consistent versions is the BOM:
  `org.eclipse.fennec.emf:org.eclipse.fennec.emf.osgi.bom:1.0.0`.
- Migrating from GeckoEMF? The command was called `geckoEMF` and the external
  plugin lived in `org.geckoprojects.emf:org.gecko.emf.osgi.codegen` — see the
  [migration guide](gecko-migration-guide.md).
- In a BND workspace the same generator is wired via `-library: enable-emf`
  and `-generate` (see the [README](../README.md#1-code-generator)).
