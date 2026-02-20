# EMF Delegate Registries — User Guide

This guide explains how to use Fennec EMF OSGi's delegate registry support to dynamically register custom behavior for EMF model elements via OSGi services.

---

## What Are EMF Delegates?

EMF provides four delegate mechanisms that allow you to attach custom behavior to model elements declaratively — without generating Java code for it. Instead of hard-coding logic in generated model classes, you annotate model elements in your `.ecore` file and provide delegate implementations at runtime.

| Delegate Type | What It Does | Annotated On |
|---|---|---|
| **Invocation Delegate** | Executes the body of an `EOperation` | `EOperation` |
| **Setting Delegate** | Computes the value of a derived/volatile `EStructuralFeature` | `EAttribute` / `EReference` |
| **Validation Delegate** | Evaluates validation constraints on an `EClassifier` | `EClass` / `EDataType` |
| **Conversion Delegate** | Converts `EDataType` values to/from their string representation | `EDataType` |

### The Problem in OSGi

EMF resolves delegates through static global registries (`Registry.INSTANCE`). In a traditional Eclipse/PDE environment, these are populated via extension points. In a pure OSGi environment, there is no extension point mechanism — delegates must be registered programmatically.

### The Fennec EMF OSGi Solution

Fennec EMF OSGi provides **whiteboard populator components** that watch for OSGi services implementing the delegate interfaces. When such a service appears, the populator automatically registers it in EMF's static registry. When the service disappears, the populator removes it. You just publish an OSGi service — the rest happens automatically.

---

## Step 1: Annotate Your Ecore Model

Every delegate type requires two things in your `.ecore` model:

1. **Package-level declaration** — tells EMF which delegate URI(s) are used in this package
2. **Element-level annotation** — tells EMF which specific element uses which delegate

The **delegate URI** is a string that connects your model annotation to the runtime delegate implementation. It can be any URI — it just has to match between the `.ecore` annotation and your OSGi service registration.

### Package-Level Declaration

Add an `EAnnotation` with source `http://www.eclipse.org/emf/2002/Ecore` to your `EPackage`. Use `details` entries to declare which delegate URIs are active:

```xml
<ecore:EPackage name="mymodel" nsURI="http://example.org/mymodel/1.0" ...>
  <eAnnotations source="http://www.eclipse.org/emf/2002/Ecore">
    <details key="invocationDelegates" value="http://example.org/mydelegate/1.0"/>
    <details key="settingDelegates"    value="http://example.org/mydelegate/1.0"/>
    <details key="validationDelegates" value="http://example.org/mydelegate/1.0"/>
    <details key="conversionDelegates" value="http://example.org/mydelegate/1.0"/>
  </eAnnotations>
  ...
</ecore:EPackage>
```

You only need to declare the delegate types you actually use. All four can share the same URI, or each can use a different one.

### Element-Level Annotations

Each model element that uses a delegate gets an `EAnnotation` whose `source` matches the delegate URI declared at the package level:

#### Invocation Delegate (on EOperation)

```xml
<eOperations name="greet" eType="ecore:EDataType ...#//EString">
  <eAnnotations source="http://example.org/mydelegate/1.0">
    <details key="body" value="'Hello, ' + self.name"/>
  </eAnnotations>
</eOperations>
```

The `body` detail contains an expression. Your delegate implementation receives this expression and evaluates it.

#### Setting Delegate (on EStructuralFeature)

The feature must be `volatile="true" transient="true" derived="true" changeable="false"`:

```xml
<eStructuralFeatures xsi:type="ecore:EAttribute" name="fullName"
    eType="ecore:EDataType ...#//EString"
    changeable="false" volatile="true" transient="true" derived="true">
  <eAnnotations source="http://example.org/mydelegate/1.0">
    <details key="derivation" value="firstName + ' ' + lastName"/>
  </eAnnotations>
</eStructuralFeatures>
```

The `derivation` detail contains the expression for computing the value.

#### Validation Delegate (on EClass)

Two annotations are needed — one declaring the constraint name, one providing the expression:

```xml
<eClassifiers xsi:type="ecore:EClass" name="Person">
  <!-- Declares which constraints exist -->
  <eAnnotations source="http://www.eclipse.org/emf/2002/Ecore">
    <details key="constraints" value="nameNotEmpty agePositive"/>
  </eAnnotations>
  <!-- Provides expressions for each constraint -->
  <eAnnotations source="http://example.org/mydelegate/1.0">
    <details key="nameNotEmpty" value="name <> null and name.size() > 0"/>
    <details key="agePositive"  value="age > 0"/>
  </eAnnotations>
  ...
</eClassifiers>
```

#### Conversion Delegate (on EDataType)

```xml
<eClassifiers xsi:type="ecore:EDataType" name="UpperString"
    instanceClassName="java.lang.String">
  <eAnnotations source="http://example.org/mydelegate/1.0">
    <details key="conversion" value="toUpperCase()"/>
  </eAnnotations>
</eClassifiers>
```

---

## Step 2: Implement the Delegate

Each delegate type has its own EMF interface. Your implementation is an OSGi DS component that Fennec EMF OSGi automatically picks up.

Three of the four delegate types use a **factory pattern**: you register a factory, and EMF calls the factory to create one delegate instance per model element. The exception is `ValidationDelegate`, which is registered directly (no factory).

### Invocation Delegate

**Purpose:** Provides the runtime implementation for `EOperations` that have a `body` annotation instead of generated Java code.

**When it is called:** When your code calls `eObject.eInvoke(operation, arguments)` on an EOperation carrying a delegate annotation.

Implement `EOperation.Internal.InvocationDelegate.Factory`. EMF calls your factory once per annotated `EOperation` to create a delegate, then caches it.

```java
@Component(service = EOperation.Internal.InvocationDelegate.Factory.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.OPERATION_INVOCATION_FACTORY,
    configuratorName = "http://example.org/mydelegate/1.0"
)
public class MyInvocationDelegateFactory
        implements EOperation.Internal.InvocationDelegate.Factory {

    @Override
    public EOperation.Internal.InvocationDelegate createInvocationDelegate(
            EOperation operation) {
        // Called once per EOperation, on first eInvoke(). The result is cached.
        // Read the expression from the annotation on this operation.
        String body = operation.getEAnnotation("http://example.org/mydelegate/1.0")
                .getDetails().get("body");
        return new MyInvocationDelegate(operation, body);
    }
}
```

The delegate has a single method:

```java
public class MyInvocationDelegate
        implements EOperation.Internal.InvocationDelegate {

    private final EOperation operation;
    private final String expression;

    public MyInvocationDelegate(EOperation operation, String expression) {
        this.operation = operation;
        this.expression = expression;
    }

    /**
     * Called every time eInvoke() is called on this operation.
     *
     * @param target    the EObject on which the operation is invoked (e.g. a Person instance)
     * @param arguments the arguments passed to eInvoke(), matching the operation's EParameters
     * @return the operation's return value (must match the operation's EType, or null for void)
     */
    @Override
    public Object dynamicInvoke(InternalEObject target, EList<?> arguments)
            throws InvocationTargetException {
        // Simple example: read a feature from the target and build a greeting.
        // The annotation says: body = "'Hello, ' + self.name"
        Object name = target.eGet(target.eClass().getEStructuralFeature("name"));
        return "Hello, " + name;
    }
}
```

**What happens at runtime:**

```
person.eInvoke(greetOperation, args)
  → EOperationImpl.getInvocationDelegate()          // lazy init, cached after first call
    → EcoreUtil looks up Factory in Registry for the annotation's source URI
      → factory.createInvocationDelegate(greetOperation)
  → delegate.dynamicInvoke(person, args)             // called on every eInvoke()
    → returns "Hello, Alice"
```

---

### Setting Delegate

**Purpose:** Computes the value of a derived/volatile feature at runtime. Instead of storing a value, the feature's value is calculated on every access.

**When it is called:** When your code calls `eObject.eGet(feature)`, `eObject.eIsSet(feature)`, etc. on a feature with a delegate annotation.

Implement `EStructuralFeature.Internal.SettingDelegate.Factory`:

```java
@Component(service = EStructuralFeature.Internal.SettingDelegate.Factory.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.SETTING_DELEGATE_FACTORY,
    configuratorName = "http://example.org/mydelegate/1.0"
)
public class MySettingDelegateFactory
        implements EStructuralFeature.Internal.SettingDelegate.Factory {

    @Override
    public EStructuralFeature.Internal.SettingDelegate createSettingDelegate(
            EStructuralFeature feature) {
        // Called once per feature, on first eGet(). The result is cached.
        return new MySettingDelegate(feature);
    }
}
```

The delegate has seven methods. For a typical derived attribute, only `dynamicGet` and `dynamicIsSet` need real implementations — the rest should throw because derived features are read-only:

```java
public class MySettingDelegate
        implements EStructuralFeature.Internal.SettingDelegate {

    private final EStructuralFeature feature;

    public MySettingDelegate(EStructuralFeature feature) {
        this.feature = feature;
    }

    /**
     * Called on every eGet(feature) — computes and returns the derived value.
     * EMF does NOT cache the return value, so this is called on every access.
     *
     * @param owner            the EObject that owns this feature (e.g. a Person)
     * @param settings         internal storage array (unused for derived features)
     * @param dynamicFeatureID internal index of this feature
     * @param resolve          true if proxies should be resolved
     * @param coreType         true if the core EMF type should be returned (relevant for maps/feature maps)
     * @return the computed value for this feature
     */
    @Override
    public Object dynamicGet(InternalEObject owner,
            EStructuralFeature.Internal.DynamicValueHolder settings,
            int dynamicFeatureID, boolean resolve, boolean coreType) {
        // Simple example: the annotation says derivation = "name.toUpperCase()"
        // We read the "name" feature and return it uppercased.
        Object name = owner.eGet(owner.eClass().getEStructuralFeature("name"));
        if (name == null) {
            return null;
        }
        return name.toString().toUpperCase();
    }

    /**
     * Called on eSet(feature, value). For derived features declared as changeable="false",
     * EMF rejects the eSet() call with IllegalArgumentException before this method is reached.
     * If the feature is changeable, this method should store the value.
     */
    @Override
    public void dynamicSet(InternalEObject owner,
            EStructuralFeature.Internal.DynamicValueHolder settings,
            int dynamicFeatureID, Object newValue) {
        throw new UnsupportedOperationException("Derived feature is read-only");
    }

    /**
     * Called on eIsSet(feature) — returns whether the feature has a value.
     * For derived features, this typically checks if dynamicGet would return non-null.
     */
    @Override
    public boolean dynamicIsSet(InternalEObject owner,
            EStructuralFeature.Internal.DynamicValueHolder settings,
            int dynamicFeatureID) {
        return dynamicGet(owner, settings, dynamicFeatureID, false, false) != null;
    }

    /**
     * Called on eUnset(feature). Derived features are typically not unsettable.
     */
    @Override
    public void dynamicUnset(InternalEObject owner,
            EStructuralFeature.Internal.DynamicValueHolder settings,
            int dynamicFeatureID) {
        throw new UnsupportedOperationException("Derived feature is read-only");
    }

    /**
     * Returns an EStructuralFeature.Setting view for this feature.
     * Called by eObject.eSetting(feature). Rarely needed — can throw for simple cases.
     */
    @Override
    public EStructuralFeature.Setting dynamicSetting(InternalEObject owner,
            EStructuralFeature.Internal.DynamicValueHolder settings,
            int dynamicFeatureID) {
        throw new UnsupportedOperationException();
    }

    /**
     * Called when an object is added at the other end of a bidirectional reference.
     * Only relevant for EReferences with an eOpposite. Not needed for EAttributes.
     */
    @Override
    public NotificationChain dynamicInverseAdd(InternalEObject owner,
            EStructuralFeature.Internal.DynamicValueHolder settings,
            int dynamicFeatureID, InternalEObject otherEnd,
            NotificationChain notifications) {
        throw new UnsupportedOperationException();
    }

    /**
     * Called when an object is removed at the other end of a bidirectional reference.
     * Only relevant for EReferences with an eOpposite. Not needed for EAttributes.
     */
    @Override
    public NotificationChain dynamicInverseRemove(InternalEObject owner,
            EStructuralFeature.Internal.DynamicValueHolder settings,
            int dynamicFeatureID, InternalEObject otherEnd,
            NotificationChain notifications) {
        throw new UnsupportedOperationException();
    }
}
```

**What happens at runtime:**

```
person.eGet(upperNameFeature)
  → EStructuralFeatureImpl.getSettingDelegate()      // lazy init, cached after first call
    → EcoreUtil looks up Factory in Registry for the annotation's source URI
      → factory.createSettingDelegate(upperNameFeature)
  → delegate.dynamicGet(person, ...)                 // called on every eGet()
    → reads person.name ("alice"), returns "ALICE"
```

> **Note:** EMF rejects `eSet()` calls on non-changeable features with `IllegalArgumentException` before the delegate's `dynamicSet` is even reached. The feature must be declared `changeable="false"` in the ecore model.

**When to implement which methods:**

| Method | Called by | Implement for |
|---|---|---|
| `dynamicGet` | `eGet(feature)` | Always — this is the core method |
| `dynamicSet` | `eSet(feature, value)` | Writable computed features only |
| `dynamicIsSet` | `eIsSet(feature)` | Always — serialization uses this |
| `dynamicUnset` | `eUnset(feature)` | Writable computed features only |
| `dynamicSetting` | `eSetting(feature)` | Rarely needed |
| `dynamicInverseAdd` | Bidirectional reference add | Derived EReferences with eOpposite |
| `dynamicInverseRemove` | Bidirectional reference remove | Derived EReferences with eOpposite |

---

### Validation Delegate

**Purpose:** Evaluates validation constraints declared on EClasses or EDataTypes. Unlike the other three, this is **not** a factory — you register the delegate directly.

**When it is called:** When `Diagnostician.INSTANCE.validate(eObject)` is called and the object's EClass (or a feature's EDataType) has constraints with a matching delegate annotation.

The delegate has three `validate` overloads. EMF calls a specific one depending on **where** the constraint annotation is placed:

```java
@Component(service = EValidator.ValidationDelegate.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.VALIDATION_DELEGATE,
    configuratorName = "http://example.org/mydelegate/1.0"
)
public class MyValidationDelegate implements EValidator.ValidationDelegate {

    /**
     * Validates an EOperation-based invariant on an EClass.
     *
     * Called when the EClass has an EOperation annotated as an invariant
     * (an operation returning EBoolean with a delegate annotation).
     * This is less common — most models use constraint-based validation instead.
     *
     * @param eClass     the EClass being validated
     * @param eObject    the instance being validated (e.g. a Person)
     * @param context    shared validation context (for caching between validators)
     * @param invariant  the EOperation representing the invariant
     * @param expression the expression from the delegate annotation on the EOperation
     * @return true if valid, false if the constraint is violated
     */
    @Override
    public boolean validate(EClass eClass, EObject eObject,
            Map<Object, Object> context, EOperation invariant,
            String expression) {
        // Example: the invariant operation "isAdult" has body "self.age >= 18"
        // Simple implementation: just read the feature and check
        return doValidate(eObject, expression);
    }

    /**
     * Validates a named constraint on an EClass.
     *
     * This is the most common case. Called when the EClass has:
     *   1. An Ecore annotation with key "constraints" listing constraint names
     *   2. A delegate annotation with a detail entry for each constraint name
     *
     * @param eClass     the EClass being validated
     * @param eObject    the instance being validated
     * @param context    shared validation context
     * @param constraint the constraint name (e.g. "nameNotEmpty")
     * @param expression the expression from the delegate annotation (e.g. "name <> null")
     * @return true if valid, false if the constraint is violated
     */
    @Override
    public boolean validate(EClass eClass, EObject eObject,
            Map<Object, Object> context, String constraint,
            String expression) {
        // Example: constraint "nameNotEmpty" with expression "name <> null and name.size() > 0"
        // Simple implementation: check that the "name" feature is not null/empty
        return doValidate(eObject, expression);
    }

    /**
     * Validates a constraint on an EDataType value.
     *
     * Called when an EDataType (not EClass) has a constraint annotation.
     * The value is a plain Java object, not an EObject.
     *
     * @param eDataType  the EDataType being validated (e.g. "PositiveInt")
     * @param value      the Java value to validate (e.g. Integer 42)
     * @param context    shared validation context
     * @param constraint the constraint name
     * @param expression the expression from the delegate annotation
     * @return true if valid, false if the constraint is violated
     */
    @Override
    public boolean validate(EDataType eDataType, Object value,
            Map<Object, Object> context, String constraint,
            String expression) {
        // Example: EDataType "PositiveInt" with constraint "isPositive", expression "self > 0"
        if (value instanceof Number number) {
            return number.intValue() > 0;
        }
        return false;
    }

    private boolean doValidate(EObject eObject, String expression) {
        // Simple example implementation: check that "name" is not empty.
        // A real implementation would parse and evaluate the expression.
        EStructuralFeature nameFeature = eObject.eClass().getEStructuralFeature("name");
        if (nameFeature == null) {
            return true;
        }
        Object value = eObject.eGet(nameFeature);
        return value instanceof String s && !s.isEmpty();
    }
}
```

**What happens at runtime:**

```
Diagnostician.INSTANCE.validate(person)
  → EObjectValidator checks constraints declared on Person's EClass
    → finds "constraints" annotation listing "nameNotEmpty"
    → finds delegate annotation with key "nameNotEmpty" → expression = "name <> null ..."
    → looks up ValidationDelegate in Registry for the delegate URI
      → delegate.validate(personClass, person, context, "nameNotEmpty", "name <> null ...")
        → your code checks the constraint, returns true/false
```

**Which `validate` overload is called:**

| Annotation placement | Overload called |
|---|---|
| `EClass` with `constraints` detail + expression per constraint name | `validate(EClass, EObject, ..., String constraint, String expression)` |
| `EClass` with invariant `EOperation` carrying a delegate annotation | `validate(EClass, EObject, ..., EOperation invariant, String expression)` |
| `EDataType` with `constraints` detail + expression | `validate(EDataType, Object, ..., String constraint, String expression)` |

Unlike the other three delegates, `ValidationDelegate` is **not** cached on the model element. It is looked up from the registry on every validation call.

---

### Conversion Delegate

**Purpose:** Controls how an `EDataType` is serialized to a string and deserialized from a string. EMF calls this when saving/loading model instances (e.g. XMI serialization) or when you call `eFactory.convertToString()`/`createFromString()` explicitly.

**When it is called:** When `EFactory.convertToString(dataType, value)` or `EFactory.createFromString(dataType, literal)` is called on an EDataType carrying a delegate annotation.

Implement `EDataType.Internal.ConversionDelegate.Factory`:

```java
@Component(service = EDataType.Internal.ConversionDelegate.Factory.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.CONVERSION_DELEGATE_FACTORY,
    configuratorName = "http://example.org/mydelegate/1.0"
)
public class MyConversionDelegateFactory
        implements EDataType.Internal.ConversionDelegate.Factory {

    @Override
    public EDataType.Internal.ConversionDelegate createConversionDelegate(
            EDataType eDataType) {
        // Called once per EDataType, on first convert/create call. The result is cached.
        return new MyConversionDelegate(eDataType);
    }
}
```

The delegate has two methods — one for each direction of conversion:

```java
public class MyConversionDelegate
        implements EDataType.Internal.ConversionDelegate {

    private final EDataType dataType;

    public MyConversionDelegate(EDataType dataType) {
        this.dataType = dataType;
    }

    /**
     * Converts a Java value to its string representation.
     * Called by EFactory.convertToString(dataType, value).
     *
     * This is used when EMF serializes model instances (e.g. saving to XMI).
     * The returned string must be parseable by createFromString().
     *
     * @param value the Java object to serialize (matches the dataType's instanceClass)
     * @return the string representation
     */
    @Override
    public String convertToString(Object value) {
        // Example: EDataType "UpperString" stores strings as uppercase
        if (value == null) {
            return null;
        }
        return value.toString().toUpperCase();
    }

    /**
     * Creates a Java value from its string representation.
     * Called by EFactory.createFromString(dataType, literal).
     *
     * This is used when EMF deserializes model instances (e.g. loading from XMI).
     * Must be the inverse of convertToString().
     *
     * @param literal the string from the serialized model
     * @return the Java object (must be assignable to the dataType's instanceClass)
     */
    @Override
    public Object createFromString(String literal) {
        // Example: just return the literal as-is (it's already a String)
        return literal;
    }
}
```

**What happens at runtime:**

```
eFactory.convertToString(upperStringType, "hello")
  → EFactoryImpl → EDataTypeImpl.getConversionDelegate()   // lazy init, cached
    → EcoreUtil looks up Factory in Registry for the annotation's source URI
      → factory.createConversionDelegate(upperStringType)
  → delegate.convertToString("hello")                      // called on every convert
    → returns "HELLO"

eFactory.createFromString(upperStringType, "HELLO")
  → same delegate (already cached)
  → delegate.createFromString("HELLO")
    → returns "HELLO"
```

---

## Step 3: Deploy

No additional configuration is needed. When your bundle starts:

1. OSGi DS activates your component and registers it as a service
2. Fennec EMF OSGi's whiteboard populator detects the service (matched by `configuratorType`)
3. The populator calls `Registry.INSTANCE.put(configuratorName, yourService)`
4. EMF can now resolve your delegate when model elements are accessed

When your bundle stops:

1. OSGi DS deactivates your component
2. The populator calls `Registry.INSTANCE.remove(configuratorName)`
3. EMF can no longer resolve the delegate (subsequent access will fail or use a fallback)

---

## Quick Reference

### Annotation → Service → Registry Mapping

| `@EMFConfigurator` configuratorType | Service Interface | EMF Registry |
|---|---|---|
| `OPERATION_INVOCATION_FACTORY` | `EOperation.Internal.InvocationDelegate.Factory` | `InvocationDelegate.Factory.Registry.INSTANCE` |
| `SETTING_DELEGATE_FACTORY` | `EStructuralFeature.Internal.SettingDelegate.Factory` | `SettingDelegate.Factory.Registry.INSTANCE` |
| `VALIDATION_DELEGATE` | `EValidator.ValidationDelegate` | `ValidationDelegate.Registry.INSTANCE` |
| `CONVERSION_DELEGATE_FACTORY` | `EDataType.Internal.ConversionDelegate.Factory` | `ConversionDelegate.Factory.Registry.INSTANCE` |

### Ecore Annotation Keys

| Delegate Type | Package-Level Key | Element-Level Annotation Key |
|---|---|---|
| Invocation | `invocationDelegates` | `body` |
| Setting | `settingDelegates` | `derivation` |
| Validation | `validationDelegates` | constraint name (e.g. `nameNotEmpty`) |
| Conversion | `conversionDelegates` | `conversion` |

### Delegate Caching Behavior

| Delegate Type | Cached? | Where? | Implication |
|---|---|---|---|
| Invocation | Yes | On `EOperation` | Factory must be registered before first `eInvoke()` call |
| Setting | Yes | On `EStructuralFeature` | Factory must be registered before first `eGet()` call |
| Validation | No | — | Delegate is looked up on every `validate()` call |
| Conversion | Yes | On `EDataType` | Factory must be registered before first `convertToString()`/`createFromString()` call |

> **Important:** For the three cached delegate types, EMF creates the delegate lazily on first access and caches it on the model element object. If your factory is not yet registered when the first access happens, the delegate will be `null` and subsequent accesses will fail — even if the factory is registered later. Make sure your delegate service is available before any code accesses the annotated model elements.

### Lazy Loading with Descriptors

For each delegate type, you can alternatively register a `Descriptor` instead of the actual factory/delegate. The descriptor's `getFactory()` method is called on first use, allowing deferred class loading:

```java
@Component(service = EOperation.Internal.InvocationDelegate.Factory.Descriptor.class)
@EMFConfigurator(
    configuratorType = ConfiguratorType.OPERATION_INVOCATION_FACTORY,
    configuratorName = "http://example.org/mydelegate/1.0"
)
public class MyInvocationDelegateDescriptor
        implements EOperation.Internal.InvocationDelegate.Factory.Descriptor {

    @Override
    public EOperation.Internal.InvocationDelegate.Factory getFactory() {
        return new MyInvocationDelegateFactory();
    }
}
```

---

## Plain Java Usage (Without OSGi)

If you are not running in an OSGi environment, register delegates directly in the static registries:

```java
String DELEGATE_URI = "http://example.org/mydelegate/1.0";

// Register all four delegate types
EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE
    .put(DELEGATE_URI, new MyInvocationDelegateFactory());

EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE
    .put(DELEGATE_URI, new MySettingDelegateFactory());

EValidator.ValidationDelegate.Registry.INSTANCE
    .put(DELEGATE_URI, new MyValidationDelegate());

EDataType.Internal.ConversionDelegate.Factory.Registry.INSTANCE
    .put(DELEGATE_URI, new MyConversionDelegateFactory());
```

---

## Complete Ecore Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ecore:EPackage xmi:version="2.0"
    xmlns:xmi="http://www.omg.org/XMI"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
    name="mymodel" nsURI="http://example.org/mymodel/1.0" nsPrefix="mymodel">

  <!-- Declare delegate URIs at package level -->
  <eAnnotations source="http://www.eclipse.org/emf/2002/Ecore">
    <details key="invocationDelegates" value="http://example.org/mydelegate/1.0"/>
    <details key="settingDelegates"    value="http://example.org/mydelegate/1.0"/>
    <details key="validationDelegates" value="http://example.org/mydelegate/1.0"/>
    <details key="conversionDelegates" value="http://example.org/mydelegate/1.0"/>
  </eAnnotations>

  <eClassifiers xsi:type="ecore:EClass" name="Person">
    <!-- Validation: declare constraint names -->
    <eAnnotations source="http://www.eclipse.org/emf/2002/Ecore">
      <details key="constraints" value="nameNotEmpty"/>
    </eAnnotations>
    <!-- Validation: provide constraint expressions -->
    <eAnnotations source="http://example.org/mydelegate/1.0">
      <details key="nameNotEmpty" value="name &lt;> null and name.size() > 0"/>
    </eAnnotations>

    <!-- Invocation: operation with body expression -->
    <eOperations name="greet"
        eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString">
      <eAnnotations source="http://example.org/mydelegate/1.0">
        <details key="body" value="'Hello, ' + self.name"/>
      </eAnnotations>
    </eOperations>

    <!-- Normal attribute -->
    <eStructuralFeatures xsi:type="ecore:EAttribute" name="name"
        eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>

    <!-- Setting: derived attribute with derivation expression -->
    <eStructuralFeatures xsi:type="ecore:EAttribute" name="upperName"
        eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"
        changeable="false" volatile="true" transient="true" derived="true">
      <eAnnotations source="http://example.org/mydelegate/1.0">
        <details key="derivation" value="name.toUpperCase()"/>
      </eAnnotations>
    </eStructuralFeatures>
  </eClassifiers>

  <!-- Conversion: custom data type with conversion expression -->
  <eClassifiers xsi:type="ecore:EDataType" name="UpperString"
      instanceClassName="java.lang.String">
    <eAnnotations source="http://example.org/mydelegate/1.0">
      <details key="conversion" value="toUpperCase()"/>
    </eAnnotations>
  </eClassifiers>

</ecore:EPackage>
```
