# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Eclipse Fennec EMF OSGi project (formerly GeckoEMF) - a framework that enables Eclipse Modeling Framework (EMF) to work in pure OSGi environments without Eclipse PDE dependencies. The project provides OSGi service-based access to EMF ResourceSets, EPackages, and ResourceFactories.

**⚠️ Note: The project is currently undergoing refactoring and may not compile at this time.**

## Build System

The project uses a hybrid Gradle + BND workspace setup:

- **Primary build**: `./gradlew build` - Main build command
- **Test all modules**: `./gradlew test` - Runs JUnit tests across all submodules  
- **Clean**: `./gradlew clean`
- **Coverage report**: `./gradlew codeCoverageReport` - Generates consolidated Jacoco coverage
- **Quality analysis**: `./gradlew sonar` - Runs SonarQube analysis

Individual module builds use BND:
- Each module has a `bnd.bnd` file for OSGi bundle configuration
- Generated artifacts go to `generated/` directories
- Java source/target is 1.8

## Architecture Overview

The project consists of several key modules:

### Core API (`org.eclipse.fennec.emf.osgi.api`)
- Defines interfaces and configurators for EMF OSGi integration
- Key interfaces: `ResourceSetFactory`, `EPackageConfigurator`, `ResourceFactoryConfigurator`
- Service properties defined in `EMFNamespaces` constants
- Annotation-based configuration support

### Core Implementation (`org.eclipse.fennec.emf.osgi`)
- Default components for EPackage and ResourceFactory registries
- Service components with different configurations:
  - `component.bnd` - Full component set
  - `component.config.bnd` - Configuration-based components
  - `component.minimal.bnd` - Minimal component set
- Resource set factory implementations

### Code Generation (`org.eclipse.fennec.emf.osgi.codegen`)
- BND-based EMF code generator that creates OSGi-compatible model code
- Generates `EPackageConfigurator` and `ConfigurationComponent` classes
- Templates in `templates/model/` directory
- Triggered by setting GenModel "OSGi Compatible" to true

### Model Extender (`org.eclipse.fennec.emf.osgi.extender`)
- OSGi extender that auto-registers EMF models from bundle manifests
- Tracks bundles with `Require-Capability: osgi.extender; filter:="(osgi.extender=emf.model)"`
- Supports both single model files and folder scanning

## Service-Based Architecture

FennecEMF bridges EMF's static registry approach with OSGi's dynamic service model:

### EMF Static vs OSGi Dynamic
- **EMF Default**: Uses static global registries (EPackage.Registry.INSTANCE, Resource.Factory.Registry.INSTANCE)
- **FennecEMF Approach**: Replaces static registries with dynamic OSGi services that can appear/disappear at runtime

### Registry Whiteboard Pattern with Dynamic Property Propagation
FennecEMF provides OSGi services that act as registries, implementing a whiteboard pattern:
- Registry services track and aggregate configurators/factories as they come and go
- **Dynamic Service Properties**: Each registry component manually registers itself as a service and updates its service properties whenever configurators are added/removed
- This allows consumers to know what capabilities are available through service property inspection
- **ServicePropertyContext**: Used to aggregate and manage properties from multiple configurators
- **Property Propagation**: ResourceSetFactory services react to registry property changes and update their own properties with the collected capabilities

### EPackage Registry Components with Scopes
EPackageConfigurators can be registered with different scopes to target specific registry levels:

- **StaticEPackageRegistryComponent**: Replaces EMF's static EPackage.Registry.INSTANCE
  - Tracks `EPackageConfigurator` services with `emf.model.scope=static`
  - Implements `EPackage.Registry` interface and registers itself as an OSGi service
  - Provides service property `emf.default.epackage.registry=true`
  
- **DefaultEPackageRegistryComponent**: Provides ResourceSet-level EPackage registry
  - Tracks `EPackageConfigurator` services with `emf.model.scope=resourceset` 
  - Creates `EPackageRegistryImpl` delegate and registers it as `EPackage.Registry` service
  - Provides service property `default.resourceset.epackage.registry=true`

**Available Scopes**: `static`, `resourceset`, `generated` - allowing EPackages to be registered at different EMF registry levels

### ResourceSet Creation Pattern
- **Standard EMF**: `ResourceSet rs = new ResourceSetImpl()`
- **FennecEMF**: Inject `ResourceSetFactory` service or pre-configured `ResourceSet` service
- ResourceSets are created with proper registry configurations based on available OSGi services
- **Capability Advertisement**: ResourceSetFactory and ResourceSet services advertise their capabilities through service properties (e.g., `emf.name`, `emf.nsUri`) that are dynamically updated as the underlying registries change
- Consumers can use service filters to obtain ResourceSets with specific model support: `@Reference(target="(emf.name=mymodel)")`

### Service Registration
The framework registers EMF components as OSGi services:
- `ResourceSet` as prototype services with filtering support (`emf.name`, `emf.nsUri`, etc.)
- `ResourceSetFactory` services for creating configured ResourceSets
- `EPackage`/`EFactory` services with model metadata properties  
- `ResourceFactory` services for file extension/protocol handling
- `Condition` services for model lifecycle management

This allows EMF models to be dynamically added/removed as bundles are installed/uninstalled, unlike EMF's static approach.

## Development Workflow

1. **Model Development**: Place `.ecore` and `.genmodel` files in `model/` directories
2. **Code Generation**: Use the GeckoEMF generator or extender for OSGi service registration
3. **Service Properties**: Use EMF namespace constants for consistent property naming
4. **Testing**: Integration tests use `.bndrun` files for OSGi runtime testing

## Key Conventions

- All EMF models registered as OSGi services with standardized properties
- Generated code creates both service components and configurators
- Bundle manifests use OSGi requirements for model registration
- Service filtering via target properties (e.g., `target="(emf.name=mymodel)"`)