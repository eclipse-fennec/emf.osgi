---
layout: home

hero:
  name: Fennec EMF OSGi
  text: EMF for pure OSGi — models as services
  tagline: The Eclipse Modeling Framework in pure OSGi environments, without Eclipse PDE or Equinox — ResourceSets, EPackages and ResourceFactories as dynamic OSGi services.
  image:
    src: /fennec-logo.png
    alt: Eclipse Fennec logo
  actions:
    - theme: brand
      text: Configuration Guide
      link: /guides/configuration-guide
    - theme: alt
      text: Model Extender
      link: /guides/model-extender
    - theme: alt
      text: View on GitHub
      link: https://github.com/eclipse-fennec/emf.osgi

features:
  - icon: 🧩
    title: Dynamic service registries
    details: "Replaces EMF's static global registries (EPackage.Registry.INSTANCE, Resource.Factory.Registry.INSTANCE) with OSGi services that appear and disappear at runtime — a whiteboard of configurators with dynamically propagated service properties."
  - icon: 📦
    title: ResourceSet as a service
    details: "ResourceSet is a prototype-scoped OSGi service and ResourceSetFactory creates pre-configured instances on demand. Consumers filter by capability: @Reference(target = \"(emf.name=mymodel)\")."
  - icon: ⚙️
    title: OSGi-aware code generator
    details: "A BND-based EMF generator that produces OSGi-compatible model code — EPackageConfigurator and DS ConfigurationComponent classes — straight from your .genmodel, wired into the workspace build."
    link: https://github.com/eclipse-fennec/emf.osgi/tree/snapshot/org.eclipse.fennec.emf.osgi.codegen
    linkText: Code generator
  - icon: 🔍
    title: Model extender
    details: "No code generation required — the extender discovers .ecore files in bundles that require the emf.model extender capability and registers them as EPackage and EPackageConfigurator services automatically."
    link: /guides/model-extender
    linkText: Extender guide
  - icon: 🗂️
    title: Isolated EMF stacks
    details: "Configurable DS components create dedicated, isolated EMF stacks via OSGi Configuration Admin — factory PIDs for multi-tenant applications or independent model sets, including dynamic model loading from a URI."
    link: /guides/configuration-guide
    linkText: Configuration guide
  - icon: ✅
    title: EMF delegates
    details: "Validation, invocation, setting and conversion delegates as OSGi whiteboard services — declare them in Ecore annotations and implement them as components, isolated per ResourceSet instead of global singletons."
    link: /guides/emf-delegate-user-guide
    linkText: Delegate guide
  - icon: 🔁
    title: Gecko EMF compatibility
    details: "Formerly GeckoEMF: a compatibility layer maps the old org.gecko.emf.osgi package names to org.eclipse.fennec.emf.osgi, so existing consumers migrate incrementally."
    link: /guides/gecko-migration-guide
    linkText: Migration guide
---

## Getting started

Fennec EMF OSGi (`org.eclipse.fennec.emf.osgi`) enables the
[Eclipse Modeling Framework](https://eclipse.dev/modeling/emf/) in pure OSGi
environments in the [Eclipse Fennec](https://github.com/eclipse-fennec)
ecosystem. Models are plain Ecore `EPackage`s, registered and consumed as
standard OSGi services.

In a **BND workspace**, add the Fennec EMF library to `cnf/build.bnd` and
enable it per project:

```properties
# cnf/build.bnd
-library: fennec

# your bundle's bnd.bnd
-library: enable-emf
```

For **Gradle/Maven** builds, use the BOM (group ID `org.eclipse.fennec.emf`,
current release 1.0.0); the implementation ships as
`org.eclipse.fennec.emf.osgi.component` (all-in-one) or
`org.eclipse.fennec.emf.osgi.component.minimal`:

```
org.eclipse.fennec.emf:org.eclipse.fennec.emf.osgi.bom:1.0.0
```

Then inject a `ResourceSet` that has your model registered:

```java
@Reference(target = "(emf.name=mymodel)")
private ResourceSet resourceSet;
```

The documentation here is the user-facing manual. Internal development notes
(agent prompts, CI details, design analyses) live in the
[`docs/` folder on GitHub](https://github.com/eclipse-fennec/emf.osgi/tree/snapshot/docs).
