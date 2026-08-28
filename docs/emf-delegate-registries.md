# EMF Delegate Registries — Analyse, Probleme und Lösungsansätze

**Status:** Entwurf
**Datum:** 2026-02-19
**Kontext:** Fennec M2M / emf.osgi / Model Atlas

---

## 1. Überblick: Die vier Delegate-Registries in EMF

EMF bietet vier Delegate-Registries, über die Verhalten deklarativ an Modell-Elemente (EOperations, EStructuralFeatures, EDataTypes, EClassifier-Constraints) gebunden wird. Die Zuweisung erfolgt über EAnnotations am EPackage und den jeweiligen Modell-Elementen.

| Registry | Zweck | Delegate-URI (Beispiel) |
|---|---|---|
| **ValidationDelegate.Registry** | Auswertung von `inv:`-Constraints | `http://www.eclipse.org/emf/2002/Ecore/OCL` |
| **InvocationDelegate.Factory.Registry** | Ausführung von `body:`-Annotationen an EOperations | `http://www.eclipse.org/emf/2002/Ecore/OCL` |
| **SettingDelegate.Factory.Registry** | Berechnung von `derive:`/`initial:`-Ausdrücken an EStructuralFeatures | `http://www.eclipse.org/emf/2002/Ecore/OCL` |
| **ConversionDelegate.Factory.Registry** | Konvertierung von EDataType-Werten (`createFromString`/`convertToString`) | z.B. `http://www.eclipse.org/emf/2002/Ecore/OCL` |

---

## 2. Ist-Zustand: Statische Singletons

### 2.1 Zugriff über `Registry.INSTANCE`

Alle vier Registries werden über ein statisches `INSTANCE`-Feld auf dem jeweiligen `Registry`-Interface bereitgestellt:

```java
// EValidator.java
interface ValidationDelegate.Registry extends Map<String, Object> {
    Registry INSTANCE = new ValidationDelegateRegistryImpl();
}

// EOperation.java
interface InvocationDelegate.Factory.Registry extends Map<String, Object> {
    Registry INSTANCE = new Impl();
}

// EStructuralFeature.java
interface SettingDelegate.Factory.Registry extends Map<String, Object> {
    Registry INSTANCE = new Impl();
}

// EDataType.java
interface ConversionDelegate.Factory.Registry extends Map<String, Object> {
    Registry INSTANCE = new Impl();
}
```

`INSTANCE` ist ein Interface-Feld und damit implizit `public static final` — die Referenz kann **nicht** ersetzt werden.

### 2.2 EMF-interner Zugriff: `EcoreUtil`

EMF löst Delegates über statische Methoden in `EcoreUtil` auf, die hart auf `Registry.INSTANCE` zugreifen:

```java
// EcoreUtil.java
public static SettingDelegate.Factory getSettingDelegateFactory(EStructuralFeature f) {
    for (String uri : getSettingDelegates(f.getEContainingClass().getEPackage())) {
        if (f.getEAnnotation(uri) != null)
            return SettingDelegate.Factory.Registry.INSTANCE.getFactory(uri);  // ← statisch
    }
    return null;
}
```

Die `EXxxImpl`-Klassen (`EOperationImpl`, `EStructuralFeatureImpl`, `EDataTypeImpl`) rufen diese `EcoreUtil`-Methoden auf, um den Delegate lazy zu initialisieren. Es gibt **keinen Parameter** für eine alternative Registry.

### 2.3 Impl-Klassen: Delegation nur bei ValidationDelegate

| Registry | Impl-Klasse | Parent-Delegation? |
|---|---|---|
| ValidationDelegate | `ValidationDelegateRegistryImpl` (separate Klasse in `o.e.e.ecore.impl`) | **Ja** — `delegateRegistry`-Feld, `delegatedGet()`, Konstruktor `(Registry parent)` |
| InvocationDelegate.Factory | `Registry.Impl` (inner class in `EOperation`) | **Nein** — nur `HashMap` |
| SettingDelegate.Factory | `Registry.Impl` (inner class in `EStructuralFeature`) | **Nein** — nur `HashMap` |
| ConversionDelegate.Factory | `Registry.Impl` (inner class in `EDataType`) | **Nein** — nur `HashMap` |

Alle vier `Impl`-Klassen erben von `CommonPlugin.SimpleTargetPlatformRegistryImpl<String, Object>`, das wiederum `HashMap<String, Object>` erweitert. Der einzige Mehrwert ist die `Descriptor`-Lazy-Auflösung (`Descriptor.getFactory()` beim ersten `get()`).

### 2.4 Vergleich mit EPackage.Registry

| Aspekt | EPackage.Registry | Delegate Registries |
|---|---|---|
| Instanzierung | `ResourceSetImpl` hat eigene `packageRegistry` | Nur globale `INSTANCE` |
| Delegation | `EPackageRegistryImpl(Registry parent)` | Nur `ValidationDelegateRegistryImpl` |
| Zugriff | `resourceSet.getPackageRegistry()` (instanz-basiert) | `Registry.INSTANCE` (statisch) |
| Pro-ResourceSet? | **Ja** | **Nein** |
| emf.osgi Unterstützung | Vollständig (Static, Default, Config, Isolated) | Nur InvocationDelegate (statischer Populator) |

---

## 3. Analyse: Brauchen wir Isolation pro Delegate-Registry?

### 3.1 Grundsätzliche Überlegung

In einer Multi-Registry-Umgebung (emf.osgi, Model Atlas) kann dasselbe EPackage in mehreren Package-Registries registriert sein. Die Frage ist: Braucht man für dasselbe Modell unterschiedliche Delegate-Implementierungen in unterschiedlichen Kontexten?

### 3.2 ValidationDelegate — Keine Isolation nötig

Ein `inv:`-Constraint ist ein OCL-Ausdruck, der am Modell annotiert ist. Die Semantik ist durch die Sprache (OCL) definiert. Zwei verschiedene OCL-Engines sollten denselben Constraint identisch auswerten. Unterschiedliche Validatoren für denselben Constraint wären inkonsistent.

**Fazit:** Global ist korrekt.

### 3.3 SettingDelegate — Keine Isolation nötig

Ein `derive:`-Ausdruck berechnet den Wert eines abgeleiteten Features. Der Ausdruck ist im Modell fixiert. Unterschiedliche Berechnungen für denselben Ableitungsausdruck wären ein semantischer Fehler.

**Fazit:** Global ist korrekt.

### 3.4 InvocationDelegate — Isolation potenziell sinnvoll

Ein `body:`-Ausdruck definiert die Implementierung einer EOperation. Szenarien für kontextspezifische Implementierungen:

- **Testumgebung:** Mock-Implementierung einer Operation vs. Produktiv-Implementierung
- **Versionierung:** Unterschiedliche Implementierungsversionen derselben Operation
- **Multi-Tenant:** Mandantenspezifisches Verhalten für dieselbe Modell-Operation

**Fazit:** Isolation wäre wünschenswert, ist aber ein Nischenbedarf.

### 3.5 ConversionDelegate — Isolation sinnvoll

Konvertierung von EDataType-Werten ist kontextabhängig. Konkretes Beispiel:

- **DateTime-Formatierung:** Ein Modell mit `EDataType "Timestamp"` soll in Kontext A als ISO-8601 (`2026-02-19T12:00:00Z`) serialisiert werden, in Kontext B als Unix-Epoch (`1771495200`).
- **Locale-abhängige Konvertierung:** Dezimaltrennzeichen, Datumsformate, Zahlenformatierung.
- **Backward Compatibility:** Legacy-Clients erwarten ein anderes Format als neue Clients.

**Fazit:** Isolation ist ein realer Bedarf, insbesondere in Multi-Client/Multi-Tenant-Szenarien.

---

## 4. Technische Hürden

### 4.1 `INSTANCE` ist `final`

```java
interface Registry extends Map<String, Object> {
    Registry INSTANCE = new Impl();  // implizit public static final
}
```

Die Referenz kann nicht durch eine andere Instanz (z.B. einen Proxy) ersetzt werden — jedenfalls nicht ohne Reflection-Hack auf `final`-Felder (fragil, nicht zukunftssicher).

### 4.2 Kein Kontext in der Aufrufkette

Wenn EMF intern `createInvocationDelegate(EOperation)` oder `createConversionDelegate(EDataType)` aufruft, gibt es keinen Kontext-Parameter:

```
eObject.eInvoke(op, args)
  → EOperationImpl.getInvocationDelegate()
    → EcoreUtil.getInvocationDelegateFactory(op)
      → Registry.INSTANCE.getFactory(uri)
        → factory.createInvocationDelegate(op)  // ← kein Kontext!
```

Vom `EOperation`-Objekt kann man zum `EPackage` navigieren (`op.getEContainingClass().getEPackage()`), aber das EPackage weiß nicht, in welcher Registry es registriert ist.

### 4.3 Thread-Kontext ist nicht zuverlässig

Man könnte theoretisch über den aktuellen Thread bestimmen, aus welchem Registry-Kontext ein Aufruf kommt (z.B. via `ThreadLocal`). Das ist jedoch:

- Fragil bei asynchroner Verarbeitung
- Nicht portabel
- Schwer zu debuggen

### 4.4 EPackage ist geteilt

Dasselbe `EPackage`-Objekt (gleiche nsURI) kann in mehreren Package-Registries gleichzeitig registriert sein. Es gibt keine 1:1-Zuordnung `EPackage → Registry`.

---

## 5. Lösungsansatz: Proxy-Delegate-Pattern

### 5.1 Grundidee

Statt die statische `Registry.INSTANCE` zu ersetzen, registrieren wir einen **Proxy-Delegate** darin. Der Proxy sitzt in der globalen Registry unter dem Delegate-URI und leitet intern an kontextspezifische Implementierungen weiter.

```
┌──────────────────────────────────────────────────────┐
│           Global Registry.INSTANCE                   │
│  URI "http://.../OCL" → ProxyDelegateFactory         │
└──────────────────┬───────────────────────────────────┘
                   │ createXxxDelegate(eOp / eDataType)
                   │
                   ▼
         ┌─────────────────────┐
         │  ProxyDelegateFactory│
         │  1. EPackage ermitteln│
         │  2. Scope/Context    │
         │     bestimmen        │
         │  3. Dispatch         │
         └────────┬────────────┘
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
  Context A   Context B   Default
  Factory     Factory     Factory
```

### 5.2 Kontext-Bestimmung

Der Proxy muss bestimmen, in welchem Kontext er sich befindet. Mögliche Mechanismen:

**a) EPackage → Scope-Lookup (emf.osgi):**
emf.osgi registriert EPackages als OSGi-Services mit Properties (`emf.model.scope`, `rsf.name`). Ein Reverse-Lookup-Service könnte eine Zuordnung `EPackage.nsURI → Scope` pflegen. Problem: Ein EPackage kann in mehreren Scopes sein.

**b) Client-seitiges Mapping (Model Atlas):**
Der Client (z.B. ein REST-Endpoint im Model Atlas) weiß, in welchem Registry-Kontext er arbeitet. Er teilt dem emf.osgi-System mit, welche kontextspezifischen Delegates zu verwenden sind, bevor er Modell-Operationen ausführt.

**c) Explizite Registrierung pro ResourceSet:**
Der Client registriert den konkreten Delegate direkt am EPackage/EOperation/EDataType-Objekt seiner ResourceSet-Instanz via `setInvocationDelegate()`/`setConversionDelegate()`. Das umgeht die Registry komplett.

### 5.3 Bewertung

| Mechanismus | Vorteile | Nachteile |
|---|---|---|
| EPackage → Scope | Automatisch, transparent | Mehrdeutig bei Multi-Registry |
| Client-Mapping | Explizit, eindeutig | Client muss aktiv konfigurieren |
| Direkt am EObject | Kein Registry-Problem | Skaliert nicht, muss pro Instanz gesetzt werden |

---

## 6. Model Atlas Szenario

### 6.1 Kontext

Der Fennec Model Atlas (`fennec-model.atlas`) nutzt die verteilten, konfigurierbaren und kaskadierbaren Package-Registries von emf.osgi. Er stellt EMF-Modelle über eine API (REST/GraphQL) bereit und bedient verschiedene Clients.

### 6.2 Anforderung

Ein Client des Model Atlas arbeitet in einem bestimmten Registry-Kontext:

- Client A nutzt Package-Registry "production" → DateTime als ISO-8601
- Client B nutzt Package-Registry "legacy" → DateTime als Unix-Epoch

Beide arbeiten mit demselben EPackage (gleiche nsURI), brauchen aber unterschiedliche ConversionDelegates.

### 6.3 Lösung über Client-Mapping

```
┌──────────────────────────────────────────────┐
│                Model Atlas                    │
│                                              │
│  API Request (Thread/Session) ──────────┐    │
│                                         │    │
│  ┌──────────────────────────────────┐   │    │
│  │ Registry-Kontext bestimmen       │◄──┘    │
│  │ (aus Request-Header, Tenant-ID,  │        │
│  │  Session, API-Key, ...)          │        │
│  └──────────┬───────────────────────┘        │
│             │                                │
│             ▼                                │
│  ┌──────────────────────────────────┐        │
│  │ Client-Konfiguration             │        │
│  │ "Für Modell X nutze Converter Y" │        │
│  │ → einfaches Mapping              │        │
│  └──────────┬───────────────────────┘        │
│             │                                │
│             ▼                                │
│  ┌──────────────────────────────────┐        │
│  │ emf.osgi Client (ResourceSet)    │        │
│  │ → registriert den spezifischen   │        │
│  │   Converter in seinem Kontext    │        │
│  └──────────────────────────────────┘        │
└──────────────────────────────────────────────┘
```

Der entscheidende Punkt: Ein Client nutzt in der Regel **eine** Registry gleichzeitig, nicht mehrere. Das Mapping `(Modell, Kontext) → Converter` ist daher eindeutig und kann vom Client selbst aufgelöst werden, ohne dass EMF-intern eine Kontext-Bestimmung stattfinden muss.

### 6.4 Implikation für emf.osgi

emf.osgi muss nicht selbst die Isolation der Delegate-Registries implementieren. Stattdessen:

1. **emf.osgi** stellt die Delegate-Factories als OSGi-Services bereit (Whiteboard-Pattern, mit Properties für Scope/Name)
2. **Model Atlas** (oder ein anderer Client) wählt basierend auf seinem Kontext die passende Factory
3. **Der Client** registriert die gewählte Factory entweder:
   - Direkt am EDataType/EOperation-Objekt (`setConversionDelegate()`, `setInvocationDelegate()`)
   - Oder über den Proxy in der globalen Registry (wenn mehrere Clients im selben Prozess laufen)

---

## 7. Implementierungsvorschlag: Whiteboard-Populatoren für alle vier Registries

### 7.1 Übersicht

Das bestehende `DefaultEOperationInvocationDelegateRegistryComponent` in emf.osgi dient als Referenz-Pattern. Es fehlen drei analoge Components für Validation, Setting und Conversion. Zusätzlich muss das `ConfiguratorType`-Enum um drei Werte erweitert werden.

Alle vier Components folgen dem identischen Pattern:
1. `@Component` ohne eigenen Service (Populator, kein Service-Provider)
2. `@Reference(MULTIPLE, DYNAMIC)` für Factory und Descriptor (Whiteboard)
3. Target-Filter auf `configuratorType` im `@EMFConfigurator`
4. `put()`/`remove()` auf die statische `Registry.INSTANCE`

### 7.2 Änderung 1: `ConfiguratorType` erweitern

**Datei:** `org.eclipse.fennec.emf.osgi.api/src/org/eclipse/fennec/emf/osgi/annotation/ConfiguratorType.java`

Aktueller Zustand:
```java
public enum ConfiguratorType {
    EPACKAGE,
    RESOURCE_SET,
    RESOURCE_FACTORY,
    OPERATION_INVOCATION_FACTORY
}
```

Zielzustand — drei neue Werte:
```java
public enum ConfiguratorType {
    EPACKAGE,
    RESOURCE_SET,
    RESOURCE_FACTORY,
    OPERATION_INVOCATION_FACTORY,
    VALIDATION_DELEGATE,
    SETTING_DELEGATE_FACTORY,
    CONVERSION_DELEGATE_FACTORY
}
```

### 7.3 Änderung 2: `DefaultValidationDelegateRegistryComponent` (NEU)

**Datei:** `org.eclipse.fennec.emf.osgi/src/org/eclipse/fennec/emf/osgi/components/DefaultValidationDelegateRegistryComponent.java`

```java
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.nonNull;

import java.util.logging.Logger;

import org.eclipse.emf.ecore.EValidator.ValidationDelegate;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Descriptor;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Whiteboard populator that registers {@link ValidationDelegate} services
 * into the global {@link Registry#INSTANCE}.
 */
@Component(name = DefaultValidationDelegateRegistryComponent.NAME)
@ProviderType
public class DefaultValidationDelegateRegistryComponent {

    private static final Logger LOG = Logger.getLogger(
        DefaultValidationDelegateRegistryComponent.class.getName());

    public static final String NAME = "DefaultValidationDelegateRegistry";
    public static final String TARGET = "(configuratorType=VALIDATION_DELEGATE)";

    private final Registry registry = Registry.INSTANCE;

    @Reference(policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
    public void addValidationDelegate(ValidationDelegate delegate,
            final EMFConfigurator properties) {
        Object recent = registry.put(properties.configuratorName(), delegate);
        if (nonNull(recent)) {
            LOG.info(() -> String.format(
                "A validation delegate '%s' for '%s' was already registered "
                + "and is now replaced by a new one",
                recent.toString(), properties.configuratorName()));
        }
    }

    public void removeValidationDelegate(ValidationDelegate delegate,
            EMFConfigurator properties) {
        registry.remove(properties.configuratorName());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
    public void addValidationDelegateDescriptor(Descriptor descriptor,
            final EMFConfigurator properties) {
        Object recent = registry.put(properties.configuratorName(), descriptor);
        if (nonNull(recent)) {
            LOG.info(() -> String.format(
                "A validation delegate descriptor '%s' for '%s' was already "
                + "registered and is now replaced by a new one",
                recent.toString(), properties.configuratorName()));
        }
    }

    public void removeValidationDelegateDescriptor(Descriptor descriptor,
            EMFConfigurator properties) {
        registry.remove(properties.configuratorName());
    }
}
```

### 7.4 Änderung 3: `DefaultSettingDelegateRegistryComponent` (NEU)

**Datei:** `org.eclipse.fennec.emf.osgi/src/org/eclipse/fennec/emf/osgi/components/DefaultSettingDelegateRegistryComponent.java`

```java
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.nonNull;

import java.util.logging.Logger;

import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Whiteboard populator that registers {@link Factory} services
 * into the global {@link Registry#INSTANCE}.
 */
@Component(name = DefaultSettingDelegateRegistryComponent.NAME)
@ProviderType
public class DefaultSettingDelegateRegistryComponent {

    private static final Logger LOG = Logger.getLogger(
        DefaultSettingDelegateRegistryComponent.class.getName());

    public static final String NAME = "DefaultSettingDelegateRegistry";
    public static final String TARGET = "(configuratorType=SETTING_DELEGATE_FACTORY)";

    private final Registry registry = Registry.INSTANCE;

    @Reference(policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
    public void addSettingDelegateFactory(Factory factory,
            final EMFConfigurator properties) {
        Object recent = registry.put(properties.configuratorName(), factory);
        if (nonNull(recent)) {
            LOG.info(() -> String.format(
                "A setting delegate factory '%s' for '%s' was already "
                + "registered and is now replaced by a new one",
                recent.toString(), properties.configuratorName()));
        }
    }

    public void removeSettingDelegateFactory(Factory factory,
            EMFConfigurator properties) {
        registry.remove(properties.configuratorName());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
    public void addSettingDelegateDescriptor(Descriptor descriptor,
            final EMFConfigurator properties) {
        Object recent = registry.put(properties.configuratorName(), descriptor);
        if (nonNull(recent)) {
            LOG.info(() -> String.format(
                "A setting delegate factory descriptor '%s' for '%s' was "
                + "already registered and is now replaced by a new one",
                recent.toString(), properties.configuratorName()));
        }
    }

    public void removeSettingDelegateDescriptor(Descriptor descriptor,
            EMFConfigurator properties) {
        registry.remove(properties.configuratorName());
    }
}
```

### 7.5 Änderung 4: `DefaultConversionDelegateRegistryComponent` (NEU)

**Datei:** `org.eclipse.fennec.emf.osgi/src/org/eclipse/fennec/emf/osgi/components/DefaultConversionDelegateRegistryComponent.java`

```java
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.nonNull;

import java.util.logging.Logger;

import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Whiteboard populator that registers {@link Factory} services
 * into the global {@link Registry#INSTANCE}.
 */
@Component(name = DefaultConversionDelegateRegistryComponent.NAME)
@ProviderType
public class DefaultConversionDelegateRegistryComponent {

    private static final Logger LOG = Logger.getLogger(
        DefaultConversionDelegateRegistryComponent.class.getName());

    public static final String NAME = "DefaultConversionDelegateRegistry";
    public static final String TARGET = "(configuratorType=CONVERSION_DELEGATE_FACTORY)";

    private final Registry registry = Registry.INSTANCE;

    @Reference(policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
    public void addConversionDelegateFactory(Factory factory,
            final EMFConfigurator properties) {
        Object recent = registry.put(properties.configuratorName(), factory);
        if (nonNull(recent)) {
            LOG.info(() -> String.format(
                "A conversion delegate factory '%s' for '%s' was already "
                + "registered and is now replaced by a new one",
                recent.toString(), properties.configuratorName()));
        }
    }

    public void removeConversionDelegateFactory(Factory factory,
            EMFConfigurator properties) {
        registry.remove(properties.configuratorName());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
    public void addConversionDelegateDescriptor(Descriptor descriptor,
            final EMFConfigurator properties) {
        Object recent = registry.put(properties.configuratorName(), descriptor);
        if (nonNull(recent)) {
            LOG.info(() -> String.format(
                "A conversion delegate factory descriptor '%s' for '%s' was "
                + "already registered and is now replaced by a new one",
                recent.toString(), properties.configuratorName()));
        }
    }

    public void removeConversionDelegateDescriptor(Descriptor descriptor,
            EMFConfigurator properties) {
        registry.remove(properties.configuratorName());
    }
}
```

### 7.6 Änderung 5: `component.bnd` — neue Components aktivieren

Die neuen Components müssen in der passenden `.bnd`-Sub-Konfiguration aktiviert werden, damit sie als DS-Components erkannt werden. Die bestehende `DefaultEOperationInvocationDelegateRegistryComponent` ist bereits in `component.bnd` enthalten — die drei neuen Components müssen dort ebenfalls eingetragen werden.

### 7.7 Nutzung durch die OCL Engine (Fennec M2M)

Die OCL Engine registriert ihre Delegate-Factories als OSGi-Services mit `@EMFConfigurator`.

Die Delegate-URI ist `http://www.eclipse.org/fennec/m2m/ocl/1.0` (unser eigener Namespace, nicht der Eclipse OCL URI `http://www.eclipse.org/emf/2002/Ecore/OCL`). Damit können beide OCL-Implementierungen koexistieren.

```java
@Component(service = EValidator.ValidationDelegate.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.VALIDATION_DELEGATE,
    configuratorName = "http://www.eclipse.org/fennec/m2m/ocl/1.0")
public class OclValidationDelegate implements EValidator.ValidationDelegate {
    // ...
}

@Component(service = EStructuralFeature.Internal.SettingDelegate.Factory.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.SETTING_DELEGATE_FACTORY,
    configuratorName = "http://www.eclipse.org/fennec/m2m/ocl/1.0")
public class OclSettingDelegateFactory
    implements EStructuralFeature.Internal.SettingDelegate.Factory {
    // ...
}

@Component(service = EOperation.Internal.InvocationDelegate.Factory.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.OPERATION_INVOCATION_FACTORY,
    configuratorName = "http://www.eclipse.org/fennec/m2m/ocl/1.0")
public class OclInvocationDelegateFactory
    implements EOperation.Internal.InvocationDelegate.Factory {
    // ...
}

@Component(service = EDataType.Internal.ConversionDelegate.Factory.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.CONVERSION_DELEGATE_FACTORY,
    configuratorName = "http://www.eclipse.org/fennec/m2m/ocl/1.0")
public class OclConversionDelegateFactory
    implements EDataType.Internal.ConversionDelegate.Factory {
    // ...
}
```

In plain Java (ohne OSGi) registriert die Engine die Delegates direkt:

```java
String DELEGATE_URI = "http://www.eclipse.org/fennec/m2m/ocl/1.0";
ValidationDelegate.Registry.INSTANCE.put(DELEGATE_URI, new OclValidationDelegate(engine));
SettingDelegate.Factory.Registry.INSTANCE.put(DELEGATE_URI, new OclSettingDelegateFactory(engine));
InvocationDelegate.Factory.Registry.INSTANCE.put(DELEGATE_URI, new OclInvocationDelegateFactory(engine));
ConversionDelegate.Factory.Registry.INSTANCE.put(DELEGATE_URI, new OclConversionDelegateFactory(engine));
```

**Hinweis zur Kompatibilität:** Ecore-Modelle, die den Eclipse OCL Delegate-URI (`http://www.eclipse.org/emf/2002/Ecore/OCL`) in ihren Annotationen verwenden, benötigen eine zusätzliche Registrierung unter dieser URI. Die Engine kann optional auch unter der Eclipse-URI registriert werden, um bestehende Modelle ohne Annotation-Änderung zu unterstützen.

### 7.8 Zusammenfassung der Änderungen in emf.osgi

| Artefakt | Aktion | Modul |
|---|---|---|
| `ConfiguratorType.java` | 3 Enum-Werte hinzufügen | `emf.osgi.api` |
| `DefaultValidationDelegateRegistryComponent.java` | Neu erstellen | `emf.osgi` |
| `DefaultSettingDelegateRegistryComponent.java` | Neu erstellen | `emf.osgi` |
| `DefaultConversionDelegateRegistryComponent.java` | Neu erstellen | `emf.osgi` |
| `component.bnd` | 3 neue Components registrieren | `emf.osgi` |

---

## 8. Mittelfristig: Proxy-Delegate für Conversion/Invocation

Für ConversionDelegate und InvocationDelegate einen optionalen **Proxy** bereitstellen, der kontextspezifisches Dispatch ermöglicht. Dieser Proxy wird als regulärer Delegate in `Registry.INSTANCE` registriert und delegiert intern basierend auf Client-Konfiguration.

## 9. Langfristig: EMF Enhancement Request

EMF-Contribution vorschlagen: Instanz-basierte Delegate-Registries analog zu `ResourceSet.getPackageRegistry()`:

```java
// Vorschlag für EMF
public interface ResourceSet {
    // bestehend:
    EPackage.Registry getPackageRegistry();
    Resource.Factory.Registry getResourceFactoryRegistry();

    // neu:
    EValidator.ValidationDelegate.Registry getValidationDelegateRegistry();
    InvocationDelegate.Factory.Registry getInvocationDelegateRegistry();
    SettingDelegate.Factory.Registry getSettingDelegateRegistry();
    ConversionDelegate.Factory.Registry getConversionDelegateRegistry();
}
```

Dies würde den statischen `INSTANCE`-Zugriff in `EcoreUtil` durch einen ResourceSet-basierten Lookup ersetzen und echte Isolation ohne Proxy-Workarounds ermöglichen.

---

## 8. Zusammenfassung

| Frage | Antwort |
|---|---|
| Brauchen wir Isolation für ValidationDelegate? | **Nein** — Constraint-Semantik ist fix |
| Brauchen wir Isolation für SettingDelegate? | **Nein** — Ableitungslogik ist fix |
| Brauchen wir Isolation für InvocationDelegate? | **Potenziell** — Test-Mocks, Versionierung |
| Brauchen wir Isolation für ConversionDelegate? | **Ja** — DateTime-Formate, Locale, Legacy-Kompatibilität |
| Ist Isolation mit Standard-EMF möglich? | **Nein** — `Registry.INSTANCE` ist `final`, `EcoreUtil` greift statisch zu |
| Workaround ohne EMF-Patch? | **Proxy-Delegate** in globaler Registry + Client-seitiges Mapping |
| Relevanz für Model Atlas? | **Hoch** — Multi-Client/Multi-Tenant mit unterschiedlichen Konvertierungsregeln |
| Was muss emf.osgi jetzt liefern? | Whiteboard-Populatoren für alle 4 Registries (3 fehlen) |
| Was muss emf.osgi mittelfristig liefern? | Proxy-Delegate + Service-basierte Delegate-Factories mit Scope-Properties |

---

## Anhang A: EMF-Quellen

- `EValidator.java` → `ValidationDelegate.Registry`
- `EOperation.java` → `InvocationDelegate.Factory.Registry`
- `EStructuralFeature.java` → `SettingDelegate.Factory.Registry`
- `EDataType.java` → `ConversionDelegate.Factory.Registry`
- `ValidationDelegateRegistryImpl.java` → einzige mit Parent-Delegation
- `EcoreUtil.java` → `getXxxDelegateFactory()` — statischer Zugriff auf `INSTANCE`
- `EOperationImpl.java:1078` → `getInvocationDelegate()` lazy via `EcoreUtil`
- `EStructuralFeatureImpl.java:844` → `getSettingDelegate()` lazy via `EcoreUtil`
- `EDataTypeImpl.java:342` → `getConversionDelegate()` lazy via `EcoreUtil`
- `EFactoryImpl.java:304,485` → nutzt `getConversionDelegate()` in `createFromString`/`convertToString`
