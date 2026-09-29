package com.intellij.plugins.haxe.lang.psi.stubs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import java.util.Collection;

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

  @Test
  @DisplayName("class name index simple class")
  public void testClassNameIndex_simpleClass() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeClass> results = getByName("SimpleClass");
    assertFalse(results.isEmpty(), "Expected at least one result for 'SimpleClass'");
    assertEquals("SimpleClass", results.iterator().next().getName());
  }

  @Test
  @DisplayName("class name index interface")
  public void testClassNameIndex_interface() throws Throwable {
    myFixture.configureByFiles("IBase.hx");
    Collection<HaxeClass> results = getByName("IBase");
    assertFalse(results.isEmpty(), "Expected result for 'IBase'");
    assertEquals("IBase", results.iterator().next().getName());
  }

  @Test
  @DisplayName("class name index extended interface")
  public void testClassNameIndex_extendedInterface() throws Throwable {
    myFixture.configureByFiles("IExtended.hx", "IBase.hx");
    Collection<HaxeClass> results = getByName("IExtended");
    assertFalse(results.isEmpty(), "Expected result for 'IExtended'");
  }

  @Test
  @DisplayName("class name index enum")
  public void testClassNameIndex_enum() throws Throwable {
    myFixture.configureByFiles("Color.hx");
    Collection<HaxeClass> results = getByName("Color");
    assertFalse(results.isEmpty(), "Expected result for 'Color'");
  }

  @Test
  @DisplayName("class name index abstract")
  public void testClassNameIndex_abstract() throws Throwable {
    myFixture.configureByFiles("MyAbstract.hx");
    Collection<HaxeClass> results = getByName("MyAbstract");
    assertFalse(results.isEmpty(), "Expected result for 'MyAbstract'");
  }

  @Test
  @DisplayName("class name index typedef")
  public void testClassNameIndex_typedef() throws Throwable {
    myFixture.configureByFiles("MyTypedef.hx", "SimpleClass.hx");
    Collection<HaxeClass> results = getByName("MyTypedef");
    assertFalse(results.isEmpty(), "Expected result for 'MyTypedef'");
  }

  @Test
  @DisplayName("class name index extern class")
  public void testClassNameIndex_externClass() throws Throwable {
    myFixture.configureByFiles("ExternClass.hx");
    Collection<HaxeClass> results = getByName("ExternClass");
    assertFalse(results.isEmpty(), "Expected result for 'ExternClass'");
  }

  @Test
  @DisplayName("class name index child class")
  public void testClassNameIndex_childClass() throws Throwable {
    myFixture.configureByFiles("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx");
    Collection<HaxeClass> results = getByName("ChildClass");
    assertFalse(results.isEmpty(), "Expected result for 'ChildClass'");
  }

  @Test
  @DisplayName("class name index private class")
  public void testClassNameIndex_privateClass() throws Throwable {
    // PrivateClassModule.hx contains both PrivateClassModule (primary) and private PrivateClass
    myFixture.configureByFiles("PrivateClassModule.hx");
    // Private classes are still indexed by simple name
    Collection<HaxeClass> results = getByName("PrivateClass");
    assertFalse(results.isEmpty(), "Expected result for 'PrivateClass'");
  }

  @Test
  @DisplayName("class name index module class")
  public void testClassNameIndex_moduleClass() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxeClass> results = getByName("ModuleClass");
    assertFalse(results.isEmpty(), "Expected result for 'ModuleClass'");
  }

  @Test
  @DisplayName("class name index deep package class")
  public void testClassNameIndex_deepPackageClass() throws Throwable {
    myFixture.configureByFiles("DeepClass.hx");
    Collection<HaxeClass> results = getByName("DeepClass");
    assertFalse(results.isEmpty(), "Expected result for 'DeepClass'");
  }

  // ── HaxeFullyQualifiedNameStubIndex ──────────────────────────────────
  // Each fixture file is named after its primary class, so FQN = "package.ClassName"
  // with no additional module segment (file name == class name → not ancillary).

  @Test
  @DisplayName("fqn index simple class")
  public void testFqnIndex_simpleClass() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.SimpleClass");
    assertFalse(results.isEmpty(), "Expected result for FQN 'com.example.SimpleClass'");
    assertEquals("SimpleClass", results.iterator().next().getName());
  }

  @Test
  @DisplayName("fqn index interface")
  public void testFqnIndex_interface() throws Throwable {
    myFixture.configureByFiles("IBase.hx");
    Collection<HaxeClass> results = getByFqn("com.example.IBase");
    assertFalse(results.isEmpty(), "Expected result for FQN 'com.example.IBase'");
  }

  @Test
  @DisplayName("fqn index enum")
  public void testFqnIndex_enum() throws Throwable {
    myFixture.configureByFiles("Color.hx");
    Collection<HaxeClass> results = getByFqn("com.example.Color");
    assertFalse(results.isEmpty(), "Expected result for FQN 'com.example.Color'");
  }

  @Test
  @DisplayName("fqn index abstract")
  public void testFqnIndex_abstract() throws Throwable {
    myFixture.configureByFiles("MyAbstract.hx");
    Collection<HaxeClass> results = getByFqn("com.example.MyAbstract");
    assertFalse(results.isEmpty(), "Expected result for FQN 'com.example.MyAbstract'");
  }

  @Test
  @DisplayName("fqn index typedef")
  public void testFqnIndex_typedef() throws Throwable {
    myFixture.configureByFiles("MyTypedef.hx", "SimpleClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.MyTypedef");
    assertFalse(results.isEmpty(), "Expected result for FQN 'com.example.MyTypedef'");
  }

  @Test
  @DisplayName("fqn index deep package")
  public void testFqnIndex_deepPackage() throws Throwable {
    myFixture.configureByFiles("DeepClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.deep.pkg.DeepClass");
    assertFalse(results.isEmpty(), "Expected result for FQN 'com.example.deep.pkg.DeepClass'");
  }

  @Test
  @DisplayName("fqn index module class")
  public void testFqnIndex_moduleClass() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.module.ModuleClass");
    assertFalse(results.isEmpty(), "Expected result for FQN 'com.example.module.ModuleClass'");
  }

  @Test
  @DisplayName("fqn index ancillary class")
  public void testFqnIndex_ancillaryClass() throws Throwable {
    // PrivateClass is ancillary inside PrivateClassModule.hx:
    // FQN = com.example.PrivateClassModule.PrivateClass
    myFixture.configureByFiles("PrivateClassModule.hx");
    Collection<HaxeClass> results = getByFqn("com.example.PrivateClassModule.PrivateClass");
    assertFalse(results.isEmpty(), "Expected FQN index entry for ancillary 'PrivateClass'");
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

  @Test
  @DisplayName("method name index constructor")
  public void testMethodNameIndex_constructor() throws Throwable {
    // Constructors are named "new" in Haxe
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeMethod> methods = getMethods("com.example.SimpleClass", HaxeConstructorStubIndex.KEY);
    assertFalse(methods.isEmpty(), "Expected at least one constructor ('new') in index");
  }

  @Test
  @DisplayName("method name index instance method")
  public void testMethodNameIndex_instanceMethod() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeMethod> methods = getMethods("method", HaxeClassMethodNameStubIndex.KEY);
    assertFalse(methods.isEmpty(), "Expected result for method named 'method'");
  }

  @Test
  @DisplayName("method name index static method")
  public void testMethodNameIndex_staticMethod() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeMethod> methods = getMethods("staticMethod", HaxeStaticMethodNameStubIndex.KEY);
    assertFalse(methods.isEmpty(), "Expected result for static method 'staticMethod'");
  }

  @Test
  @DisplayName("method name index module function")
  public void testMethodNameIndex_moduleFunction() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxeMethod> methods = getMethods("moduleFunction", HaxeModuleMethodNameStubIndex.KEY);
    assertFalse(methods.isEmpty(), "Expected result for module-level function 'moduleFunction'");
  }

  // ── HaxeFieldNameStubIndex ────────────────────────────────────────────

  @Test
  @DisplayName("field name index instance field")
  public void testFieldNameIndex_instanceField() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxePsiField> fields = getFields("field", HaxeClassFieldNameStubIndex.KEY);
    assertFalse(fields.isEmpty(), "Expected result for field named 'field'");
  }

  @Test
  @DisplayName("field name index static field")
  public void testFieldNameIndex_staticField() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxePsiField> fields = getFields("staticField", HaxeStaticFieldNameStubIndex.KEY);
    assertFalse(fields.isEmpty(), "Expected result for static field 'staticField'");
  }

  @Test
  @DisplayName("field name index module var")
  public void testFieldNameIndex_moduleVar() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxePsiField> fields = getFields("moduleVar", HaxeModuleFieldNameStubIndex.KEY);
    assertFalse(fields.isEmpty(), "Expected result for module-level var 'moduleVar'");
  }

  // ── Multiple files ─────────────────────────────────────────────────────

  @Test
  @DisplayName("multiple files all classes indexed")
  public void testMultipleFiles_allClassesIndexed() throws Throwable {
    myFixture.configureByFiles(
      "SimpleClass.hx", "IBase.hx", "IExtended.hx", "ChildClass.hx",
      "Color.hx", "MyAbstract.hx", "MyTypedef.hx", "ExternClass.hx",
      "ModuleClass.hx", "DeepClass.hx", "AnotherClass.hx"
    );

    assertFalse(getByName("SimpleClass").isEmpty());
    assertFalse(getByName("IBase").isEmpty());
    assertFalse(getByName("IExtended").isEmpty());
    assertFalse(getByName("ChildClass").isEmpty());
    assertFalse(getByName("Color").isEmpty());
    assertFalse(getByName("MyAbstract").isEmpty());
    assertFalse(getByName("MyTypedef").isEmpty());
    assertFalse(getByName("ExternClass").isEmpty());
    assertFalse(getByName("ModuleClass").isEmpty());
    assertFalse(getByName("DeepClass").isEmpty());
    assertFalse(getByName("AnotherClass").isEmpty());
  }

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
