package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.stubs.index.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeSuperClassStubIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import org.junit.Test;

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
public class HaxeStubIndexTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/stubs/";
  }

  // ── HaxeClassNameStubIndex ────────────────────────────────────────────

  @Test
  public void testClassNameIndex_simpleClass() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeClass> results = getByName("SimpleClass");
    assertFalse("Expected at least one result for 'SimpleClass'", results.isEmpty());
    assertEquals("SimpleClass", results.iterator().next().getName());
  }

  @Test
  public void testClassNameIndex_interface() throws Throwable {
    myFixture.configureByFiles("IBase.hx");
    Collection<HaxeClass> results = getByName("IBase");
    assertFalse("Expected result for 'IBase'", results.isEmpty());
    assertEquals("IBase", results.iterator().next().getName());
  }

  @Test
  public void testClassNameIndex_extendedInterface() throws Throwable {
    myFixture.configureByFiles("IExtended.hx", "IBase.hx");
    Collection<HaxeClass> results = getByName("IExtended");
    assertFalse("Expected result for 'IExtended'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_enum() throws Throwable {
    myFixture.configureByFiles("Color.hx");
    Collection<HaxeClass> results = getByName("Color");
    assertFalse("Expected result for 'Color'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_abstract() throws Throwable {
    myFixture.configureByFiles("MyAbstract.hx");
    Collection<HaxeClass> results = getByName("MyAbstract");
    assertFalse("Expected result for 'MyAbstract'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_typedef() throws Throwable {
    myFixture.configureByFiles("MyTypedef.hx", "SimpleClass.hx");
    Collection<HaxeClass> results = getByName("MyTypedef");
    assertFalse("Expected result for 'MyTypedef'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_externClass() throws Throwable {
    myFixture.configureByFiles("ExternClass.hx");
    Collection<HaxeClass> results = getByName("ExternClass");
    assertFalse("Expected result for 'ExternClass'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_childClass() throws Throwable {
    myFixture.configureByFiles("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx");
    Collection<HaxeClass> results = getByName("ChildClass");
    assertFalse("Expected result for 'ChildClass'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_privateClass() throws Throwable {
    // PrivateClassModule.hx contains both PrivateClassModule (primary) and private PrivateClass
    myFixture.configureByFiles("PrivateClassModule.hx");
    // Private classes are still indexed by simple name
    Collection<HaxeClass> results = getByName("PrivateClass");
    assertFalse("Expected result for 'PrivateClass'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_moduleClass() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxeClass> results = getByName("ModuleClass");
    assertFalse("Expected result for 'ModuleClass'", results.isEmpty());
  }

  @Test
  public void testClassNameIndex_deepPackageClass() throws Throwable {
    myFixture.configureByFiles("DeepClass.hx");
    Collection<HaxeClass> results = getByName("DeepClass");
    assertFalse("Expected result for 'DeepClass'", results.isEmpty());
  }

  // ── HaxeFullyQualifiedNameStubIndex ──────────────────────────────────
  // Each fixture file is named after its primary class, so FQN = "package.ClassName"
  // with no additional module segment (file name == class name → not ancillary).

  @Test
  public void testFqnIndex_simpleClass() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.SimpleClass");
    assertFalse("Expected result for FQN 'com.example.SimpleClass'", results.isEmpty());
    assertEquals("SimpleClass", results.iterator().next().getName());
  }

  @Test
  public void testFqnIndex_interface() throws Throwable {
    myFixture.configureByFiles("IBase.hx");
    Collection<HaxeClass> results = getByFqn("com.example.IBase");
    assertFalse("Expected result for FQN 'com.example.IBase'", results.isEmpty());
  }

  @Test
  public void testFqnIndex_enum() throws Throwable {
    myFixture.configureByFiles("Color.hx");
    Collection<HaxeClass> results = getByFqn("com.example.Color");
    assertFalse("Expected result for FQN 'com.example.Color'", results.isEmpty());
  }

  @Test
  public void testFqnIndex_abstract() throws Throwable {
    myFixture.configureByFiles("MyAbstract.hx");
    Collection<HaxeClass> results = getByFqn("com.example.MyAbstract");
    assertFalse("Expected result for FQN 'com.example.MyAbstract'", results.isEmpty());
  }

  @Test
  public void testFqnIndex_typedef() throws Throwable {
    myFixture.configureByFiles("MyTypedef.hx", "SimpleClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.MyTypedef");
    assertFalse("Expected result for FQN 'com.example.MyTypedef'", results.isEmpty());
  }

  @Test
  public void testFqnIndex_deepPackage() throws Throwable {
    myFixture.configureByFiles("DeepClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.deep.pkg.DeepClass");
    assertFalse("Expected result for FQN 'com.example.deep.pkg.DeepClass'", results.isEmpty());
  }

  @Test
  public void testFqnIndex_moduleClass() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.module.ModuleClass");
    assertFalse("Expected result for FQN 'com.example.module.ModuleClass'", results.isEmpty());
  }

  @Test
  public void testFqnIndex_ancillaryClass() throws Throwable {
    // PrivateClass is ancillary inside PrivateClassModule.hx:
    // FQN = com.example.PrivateClassModule.PrivateClass
    myFixture.configureByFiles("PrivateClassModule.hx");
    Collection<HaxeClass> results = getByFqn("com.example.PrivateClassModule.PrivateClass");
    assertFalse("Expected FQN index entry for ancillary 'PrivateClass'", results.isEmpty());
  }

  // ── HaxeSuperClassStubIndex ───────────────────────────────────────────

  @Test
  public void testSuperClassIndex_directExtends() throws Throwable {
    myFixture.configureByFiles("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx");
    Collection<HaxeClass> subclasses = getBySuper("SimpleClass");
    assertFalse("Expected at least one subclass of 'SimpleClass'", subclasses.isEmpty());
    boolean foundChild = subclasses.stream().anyMatch(c -> "ChildClass".equals(c.getName()));
    assertTrue("Expected 'ChildClass' to appear as subclass of 'SimpleClass'", foundChild);
  }

  @Test
  public void testSuperClassIndex_interfaceImplementation() throws Throwable {
    myFixture.configureByFiles("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx");
    Collection<HaxeClass> implementors = getBySuper("IBase");
    assertFalse("Expected at least one implementor of 'IBase'", implementors.isEmpty());
    boolean foundChild = implementors.stream().anyMatch(c -> "ChildClass".equals(c.getName()));
    assertTrue("Expected 'ChildClass' to appear as implementor of 'IBase'", foundChild);
  }

  @Test
  public void testSuperClassIndex_interfaceExtends() throws Throwable {
    myFixture.configureByFiles("IExtended.hx", "IBase.hx");
    Collection<HaxeClass> subInterfaces = getBySuper("IBase");
    assertFalse("Expected at least one sub-interface of 'IBase'", subInterfaces.isEmpty());
    boolean foundExtended = subInterfaces.stream().anyMatch(c -> "IExtended".equals(c.getName()));
    assertTrue("Expected 'IExtended' to appear as sub-interface of 'IBase'", foundExtended);
  }

  @Test
  public void testSuperClassIndex_deepPackageExtends() throws Throwable {
    myFixture.configureByFiles("AnotherClass.hx", "DeepClass.hx");
    Collection<HaxeClass> subclasses = getBySuper("DeepClass");
    assertFalse("Expected at least one subclass of 'DeepClass'", subclasses.isEmpty());
    boolean foundAnother = subclasses.stream().anyMatch(c -> "AnotherClass".equals(c.getName()));
    assertTrue("Expected 'AnotherClass' to appear as subclass of 'DeepClass'", foundAnother);
  }

  // ── HaxeMethodNameStubIndex ───────────────────────────────────────────

  @Test
  public void testMethodNameIndex_instanceMethod() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeMethod> methods = getMethods("method");
    assertFalse("Expected result for method named 'method'", methods.isEmpty());
  }

  @Test
  public void testMethodNameIndex_staticMethod() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeMethod> methods = getMethods("staticMethod");
    assertFalse("Expected result for static method 'staticMethod'", methods.isEmpty());
  }

  @Test
  public void testMethodNameIndex_constructor() throws Throwable {
    // Constructors are named "new" in Haxe
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxeMethod> methods = getMethods("new");
    assertFalse("Expected at least one constructor ('new') in index", methods.isEmpty());
  }

  @Test
  public void testMethodNameIndex_moduleFunction() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxeMethod> methods = getMethods("moduleFunction");
    assertFalse("Expected result for module-level function 'moduleFunction'", methods.isEmpty());
  }

  // ── HaxeFieldNameStubIndex ────────────────────────────────────────────

  @Test
  public void testFieldNameIndex_instanceField() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxePsiField> fields = getFields("field");
    assertFalse("Expected result for field named 'field'", fields.isEmpty());
  }

  @Test
  public void testFieldNameIndex_staticField() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx");
    Collection<HaxePsiField> fields = getFields("staticField");
    assertFalse("Expected result for static field 'staticField'", fields.isEmpty());
  }

  @Test
  public void testFieldNameIndex_moduleVar() throws Throwable {
    myFixture.configureByFiles("ModuleClass.hx");
    Collection<HaxePsiField> fields = getFields("moduleVar");
    assertFalse("Expected result for module-level var 'moduleVar'", fields.isEmpty());
  }

  // ── Multiple files ─────────────────────────────────────────────────────

  @Test
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
  public void testMultipleFiles_fqnUnique() throws Throwable {
    myFixture.configureByFiles("SimpleClass.hx", "DeepClass.hx");
    Collection<HaxeClass> results = getByFqn("com.example.SimpleClass");
    assertEquals("FQN index should have exactly one entry for 'com.example.SimpleClass'", 1, results.size());
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
    return StubIndex.getElements(HaxeFullyQualifiedNameStubIndex.KEY, fqn,
                                 myFixture.getProject(), projectScope(), HaxeClass.class);
  }

  private Collection<HaxeClass> getBySuper(String superName) {
    return StubIndex.getElements(HaxeSuperClassStubIndex.KEY, superName,
                                 myFixture.getProject(), projectScope(), HaxeClass.class);
  }

  private Collection<HaxeMethod> getMethods(String name) {
    return StubIndex.getElements(HaxeMethodNameStubIndex.KEY, name,
                                 myFixture.getProject(), projectScope(), HaxeMethod.class);
  }

  private Collection<HaxePsiField> getFields(String name) {
    return StubIndex.getElements(HaxeFieldNameStubIndex.KEY, name,
                                 myFixture.getProject(), projectScope(), HaxePsiField.class);
  }
}
