package com.intellij.plugins.haxe.lang.psi.indexes.filebased;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedClassNameIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedMemberNameIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedModuleNameIndex;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.junit5.RunInEdt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

@DisplayName("Indexing: fully qualified name file-based indexes")
public class HaxeFqnFileBasedIndexTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/filebasedIndexes/";
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("class name index")
  class ClassNameIndex {

    @Test
    @DisplayName("main class by FQN")
    public void testMainClassByFqn() {
      myFixture.configureByFiles("CondClass.hx");
      Collection<HaxeClass> results = classesByFqn("com.example.CondClass");
      assertFalse(results.isEmpty(), "Expected file-based FQN entry for 'com.example.CondClass'");
      assertEquals("CondClass", results.iterator().next().getName());
    }

    @Test
    @DisplayName("ancillary class by module qualified FQN")
    public void testAncillaryClassByModuleQualifiedFqn() {
      myFixture.configureByFiles("CondModule.hx");
      Collection<HaxeClass> results = classesByFqn("com.example.CondModule.CondExtra");
      assertFalse(results.isEmpty(), "Expected entry for module-qualified 'com.example.CondModule.CondExtra'");
      assertEquals("CondExtra", results.iterator().next().getName());
    }

    @Test
    @DisplayName("ancillary class by short FQN")
    public void testAncillaryClassByShortFqn() {
      // the indexer emits BOTH the module-qualified and the short form
      myFixture.configureByFiles("CondModule.hx");
      Collection<HaxeClass> results = classesByFqn("com.example.CondExtra");
      assertFalse(results.isEmpty(), "Expected entry for short-form 'com.example.CondExtra'");
      assertEquals("CondExtra", results.iterator().next().getName());
    }

    @Test
    @DisplayName("skips stubable files")
    public void testSkipsStubableFiles() {
      // no #if in the file -> stub-indexed -> the file-based index must NOT hold it
      myFixture.configureByFiles("StubbedPlain.hx");
      Collection<HaxeClass> results = classesByFqn("com.example.StubbedPlain");
      assertTrue(results.isEmpty(), "Stubable file must not appear in the file-based FQN index");
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("module name index")
  class ModuleNameIndex {

    @Test
    @DisplayName("module file by FQN")
    public void testModuleFileByFqn() {
      myFixture.configureByFiles("CondModule.hx");
      Collection<HaxeModule> results = modulesByFqn("com.example.CondModule");
      assertFalse(results.isEmpty(), "Expected module entry for 'com.example.CondModule'");
    }

    @Test
    @DisplayName("class file by FQN")
    public void testClassFileByFqn() {
      // every haxe file is a module; a plain class file indexes under its own name
      myFixture.configureByFiles("CondClass.hx");
      Collection<HaxeModule> results = modulesByFqn("com.example.CondClass");
      assertFalse(results.isEmpty(), "Expected module entry for 'com.example.CondClass'");
    }

    @Test
    @DisplayName("skips stubable files")
    public void testSkipsStubableFiles() {
      myFixture.configureByFiles("StubbedPlain.hx");
      Collection<HaxeModule> results = modulesByFqn("com.example.StubbedPlain");
      assertTrue(results.isEmpty(), "Stubable file must not appear in the file-based module index");
    }
  }

  // Member keys use getQualifiedName(true): the module segment is included even
  // when it equals the class name, so main-class members read package.Module.Class.member.
  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("member name index")
  class MemberNameIndex {

    @Test
    @DisplayName("instance method by FQN")
    public void testInstanceMethodByFqn() {
      myFixture.configureByFiles("CondClass.hx");
      Collection<PsiElement> results = membersByFqn("com.example.CondClass.CondClass.condMethod");
      assertFalse(results.isEmpty(), "Expected member entry for 'com.example.CondClass.CondClass.condMethod'");
    }

    @Test
    @DisplayName("instance field by FQN")
    public void testInstanceFieldByFqn() {
      myFixture.configureByFiles("CondClass.hx");
      Collection<PsiElement> results = membersByFqn("com.example.CondClass.CondClass.condField");
      assertFalse(results.isEmpty(), "Expected member entry for 'com.example.CondClass.CondClass.condField'");
    }

    @Test
    @DisplayName("static field by FQN")
    public void testStaticFieldByFqn() {
      myFixture.configureByFiles("CondClass.hx");
      Collection<PsiElement> results = membersByFqn("com.example.CondClass.CondClass.condStaticField");
      assertFalse(results.isEmpty(), "Expected member entry for 'com.example.CondClass.CondClass.condStaticField'");
    }

    @Test
    @DisplayName("ancillary class method by FQN")
    public void testAncillaryClassMethodByFqn() {
      myFixture.configureByFiles("CondModule.hx");
      Collection<PsiElement> results = membersByFqn("com.example.CondModule.CondExtra.extraMethod");
      assertFalse(results.isEmpty(), "Expected member entry for 'com.example.CondModule.CondExtra.extraMethod'");
    }

    @Test
    @DisplayName("module function by FQN")
    public void testModuleFunctionByFqn() {
      myFixture.configureByFiles("CondModule.hx");
      Collection<PsiElement> results = membersByFqn("com.example.CondModule.condModuleFunc");
      assertFalse(results.isEmpty(), "Expected member entry for module function 'com.example.CondModule.condModuleFunc'");
    }

    @Test
    @DisplayName("module var by FQN")
    public void testModuleVarByFqn() {
      myFixture.configureByFiles("CondModule.hx");
      Collection<PsiElement> results = membersByFqn("com.example.CondModule.condModuleVar");
      assertFalse(results.isEmpty(), "Expected member entry for module var 'com.example.CondModule.condModuleVar'");
    }

    @Test
    @DisplayName("skips stubable files")
    public void testSkipsStubableFiles() {
      myFixture.configureByFiles("StubbedPlain.hx");
      Collection<PsiElement> results = membersByFqn("com.example.StubbedPlain.StubbedPlain.plainMethod");
      assertTrue(results.isEmpty(), "Stubable file must not appear in the file-based member index");
    }
  }

  private GlobalSearchScope projectScope() {
    return GlobalSearchScope.allScope(myFixture.getProject());
  }

  private Collection<HaxeClass> classesByFqn(String fqn) {
    return HaxeFullyQualifiedClassNameIndex.getByFqn(fqn, myFixture.getProject(), projectScope());
  }

  private Collection<HaxeModule> modulesByFqn(String fqn) {
    return HaxeFullyQualifiedModuleNameIndex.getByFqn(fqn, myFixture.getProject(), projectScope());
  }

  private Collection<PsiElement> membersByFqn(String fqn) {
    return HaxeFullyQualifiedMemberNameIndex.getByFqn(fqn, myFixture.getProject(), projectScope());
  }
}
