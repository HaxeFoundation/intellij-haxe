package com.intellij.plugins.haxe.lang.psi.stubs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.stubs.index.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeConstructorStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeClassInheritanceStubIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Collection;
import java.util.List;

/**
 * Tests for Haxe PSI stub indexes.
 *
 * Each fixture file is named after its primary class so the module name matches the class
 * name, giving flat FQNs of the form "package.ClassName" (no extra module segment).
 * For example, SimpleClass.hx in package com.example → FQN = com.example.SimpleClass.
 *
 * This matches the behaviour of {@link com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass#getFullyQualifiedName()},
 * which includes the module name only when the class name differs from the file name.
 */
@DisplayName("Indexing: stub index")
public class HaxeStubIndexTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/stubs/";
  }

  // ── HaxeClassNameStubIndex ────────────────────────────────────────────

  // (fixture files, class name) - every kind of type is indexed by its simple name
  static final List<Arguments> CLASS_NAME_LOOKUPS = List.of(
    // simple class
    arguments(List.of("SimpleClass.hx"), "SimpleClass"),
    // interface
    arguments(List.of("IBase.hx"), "IBase"),
    // extended interface
    arguments(List.of("IExtended.hx", "IBase.hx"), "IExtended"),
    // enum
    arguments(List.of("Color.hx"), "Color"),
    // abstract
    arguments(List.of("MyAbstract.hx"), "MyAbstract"),
    // typedef
    arguments(List.of("MyTypedef.hx", "SimpleClass.hx"), "MyTypedef"),
    // extern class
    arguments(List.of("ExternClass.hx"), "ExternClass"),
    // child class
    arguments(List.of("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx"), "ChildClass"),
    // private class: the ancillary private class of PrivateClassModule.hx is still indexed by simple name
    arguments(List.of("PrivateClassModule.hx"), "PrivateClass"),
    // module class
    arguments(List.of("ModuleClass.hx"), "ModuleClass"),
    // deep package class
    arguments(List.of("DeepClass.hx"), "DeepClass"),
    arguments(List.of("AnotherClass.hx", "DeepClass.hx"), "AnotherClass"));

  @ParameterizedTest(name = "{1}")
  @FieldSource("CLASS_NAME_LOOKUPS")
  @DisplayName("class name index")
  public void testClassNameIndex(List<String> files, String name) {
    myFixture.configureByFiles(files.toArray(String[]::new));

    Collection<HaxeClass> results = getByName(name);
    assertFalse(results.isEmpty(), "Expected result for '" + name + "'");
    assertEquals(name, results.iterator().next().getName());
  }

  // ── HaxeFullyQualifiedNameStubIndex ──────────────────────────────────

  // (fixture files, fully qualified name) - the file name equals the primary class name, so
  // the FQN carries no module segment; an ancillary class carries its module's
  static final List<Arguments> FQN_LOOKUPS = List.of(
    // simple class
    arguments(List.of("SimpleClass.hx"), "com.example.SimpleClass"),
    // interface
    arguments(List.of("IBase.hx"), "com.example.IBase"),
    // enum
    arguments(List.of("Color.hx"), "com.example.Color"),
    // abstract
    arguments(List.of("MyAbstract.hx"), "com.example.MyAbstract"),
    // typedef
    arguments(List.of("MyTypedef.hx", "SimpleClass.hx"), "com.example.MyTypedef"),
    // deep package
    arguments(List.of("DeepClass.hx"), "com.example.deep.pkg.DeepClass"),
    // module class
    arguments(List.of("ModuleClass.hx"), "com.example.module.ModuleClass"),
    // ancillary class
    arguments(List.of("PrivateClassModule.hx"), "com.example.PrivateClassModule.PrivateClass"));

  @ParameterizedTest(name = "{1}")
  @FieldSource("FQN_LOOKUPS")
  @DisplayName("fqn index")
  public void testFqnIndex(List<String> files, String fqn) {
    myFixture.configureByFiles(files.toArray(String[]::new));

    Collection<HaxeClass> results = getByFqn(fqn);
    assertFalse(results.isEmpty(), "Expected result for FQN '" + fqn + "'");
    String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
    assertEquals(simpleName, results.iterator().next().getName());
  }

  // ── HaxeSuperClassStubIndex ───────────────────────────────────────────

  @Test
  @DisplayName("super class index direct extends")
  public void testSuperClassIndex_directExtends() throws Throwable {
    myFixture.configureByFiles("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx");
    Collection<HaxeClass> subclasses = getBySuper("SimpleClass");
    assertFalse(subclasses.isEmpty(), "Expected at least one subclass of 'SimpleClass'");
    boolean foundChild = subclasses.stream().anyMatch(c -> "ChildClass".equals(c.getName()));
    assertTrue(foundChild, "Expected 'ChildClass' to appear as subclass of 'SimpleClass'");
  }

  @Test
  @DisplayName("super class index interface implementation")
  public void testSuperClassIndex_interfaceImplementation() throws Throwable {
    myFixture.configureByFiles("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx");
    Collection<HaxeClass> implementors = getBySuper("IBase");
    assertFalse(implementors.isEmpty(), "Expected at least one implementor of 'IBase'");
    boolean foundChild = implementors.stream().anyMatch(c -> "ChildClass".equals(c.getName()));
    assertTrue(foundChild, "Expected 'ChildClass' to appear as implementor of 'IBase'");
  }

  @Test
  @DisplayName("super class index interface extends")
  public void testSuperClassIndex_interfaceExtends() throws Throwable {
    myFixture.configureByFiles("IExtended.hx", "IBase.hx");
    Collection<HaxeClass> subInterfaces = getBySuper("IBase");
    assertFalse(subInterfaces.isEmpty(), "Expected at least one sub-interface of 'IBase'");
    boolean foundExtended = subInterfaces.stream().anyMatch(c -> "IExtended".equals(c.getName()));
    assertTrue(foundExtended, "Expected 'IExtended' to appear as sub-interface of 'IBase'");
  }

  @Test
  @DisplayName("super class index deep package extends")
  public void testSuperClassIndex_deepPackageExtends() throws Throwable {
    myFixture.configureByFiles("AnotherClass.hx", "DeepClass.hx");
    Collection<HaxeClass> subclasses = getBySuper("DeepClass");
    assertFalse(subclasses.isEmpty(), "Expected at least one subclass of 'DeepClass'");
    boolean foundAnother = subclasses.stream().anyMatch(c -> "AnotherClass".equals(c.getName()));
    assertTrue(foundAnother, "Expected 'AnotherClass' to appear as subclass of 'DeepClass'");
  }

  // ── HaxeMethodNameStubIndex ───────────────────────────────────────────

  // (fixture file, method index, index key) - a constructor ("new") is keyed by its class's FQN
  static final List<Arguments> METHOD_LOOKUPS = List.of(
    // constructor
    arguments("SimpleClass.hx", HaxeConstructorStubIndex.KEY, "com.example.SimpleClass"),
    // instance method
    arguments("SimpleClass.hx", HaxeClassMethodNameStubIndex.KEY, "method"),
    // static method
    arguments("SimpleClass.hx", HaxeStaticMethodNameStubIndex.KEY, "staticMethod"),
    // module function
    arguments("ModuleClass.hx", HaxeModuleMethodNameStubIndex.KEY, "moduleFunction"));

  @ParameterizedTest(name = "{1}: {2}")
  @FieldSource("METHOD_LOOKUPS")
  @DisplayName("method name index")
  public void testMethodNameIndex(String file, StubIndexKey<String, HaxeMethod> key, String name) {
    myFixture.configureByFiles(file);

    Collection<HaxeMethod> methods = getMethods(name, key);
    assertFalse(methods.isEmpty(), "Expected result for '" + name + "' in " + key);
  }

  // ── HaxeFieldNameStubIndex ────────────────────────────────────────────

  // (fixture file, field index, field name)
  static final List<Arguments> FIELD_LOOKUPS = List.of(
    // instance field
    arguments("SimpleClass.hx", HaxeClassFieldNameStubIndex.KEY, "field"),
    // static field
    arguments("SimpleClass.hx", HaxeStaticFieldNameStubIndex.KEY, "staticField"),
    // module var
    arguments("ModuleClass.hx", HaxeModuleFieldNameStubIndex.KEY, "moduleVar"));

  @ParameterizedTest(name = "{1}: {2}")
  @FieldSource("FIELD_LOOKUPS")
  @DisplayName("field name index")
  public void testFieldNameIndex(String file, StubIndexKey<String, HaxePsiField> key, String name) {
    myFixture.configureByFiles(file);

    Collection<HaxePsiField> fields = getFields(name, key);
    assertFalse(fields.isEmpty(), "Expected result for '" + name + "' in " + key);
  }

  // ── Multiple files ─────────────────────────────────────────────────────

  @Test
  @DisplayName("multiple files fqn unique")
  public void testMultipleFiles_fqnUnique() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx", "DeepClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.SimpleClass");
    assertEquals(1, results.size(), "FQN index should have exactly one entry for 'com.example.SimpleClass'");
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  private GlobalSearchScope projectScope() {
    return GlobalSearchScope.allScope(myFixture.getProject());
  }

  private Collection<HaxeClass> getByName(String name) {
    return StubIndex.getElements(HaxeClassNameStubIndex.KEY, name,
                                 myFixture.getProject(), projectScope(), HaxeClass.class);
  }

  private Collection<HaxeClass> getByFqn(String fqn) {
    return StubIndex.getElements(HaxeFullyQualifiedClassNameStubIndex.KEY, fqn,
                                 myFixture.getProject(), projectScope(), HaxeClass.class);
  }

  private Collection<HaxeClass> getBySuper(String superName) {
    return StubIndex.getElements(HaxeClassInheritanceStubIndex.KEY, superName,
                                 myFixture.getProject(), projectScope(), HaxeClass.class);
  }

  private Collection<HaxeMethod> getMethods(String name, StubIndexKey<String, HaxeMethod> key) {
    return StubIndex.getElements(key, name, myFixture.getProject(), projectScope(), HaxeMethod.class);
  }

  private Collection<HaxePsiField> getFields(String name, StubIndexKey<String, HaxePsiField> key) {
    return StubIndex.getElements(key, name, myFixture.getProject(), projectScope(), HaxePsiField.class);
  }
}
