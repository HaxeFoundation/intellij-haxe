package com.intellij.plugins.haxe.buildsystem.hxml;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.hxml.psi.HXMLMain;
import com.intellij.plugins.haxe.hxml.psi.HXMLQualifiedName;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A {@code -main} type path refers to the class the build starts from, found through that build's own class paths. */
@DisplayName("Navigation: hxml main class reference")
public class HxmlMainClassReferenceTest extends HaxeCodeInsightFixtureTestCase {
  private static final String MAIN_HX_SOURCE = "class Main { static function main() {} }\n";
  private static final String START_HX_SOURCE = "package app;\nclass Start { static function main() {} }\n";

  @Override
  protected String getBasePath() {
    // every fixture is created in-project by the test itself
    return "";
  }

  @Test
  @DisplayName("main resolves through the builds class paths")
  public void testMainResolvesThroughTheBuildsClassPaths() {
    myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);
    myFixture.addFileToProject("other/Main.hx", MAIN_HX_SOURCE);
    PsiFile hxml = myFixture.addFileToProject("build.hxml", "-cp src\n-main Main\n--interp\n");

    PsiElement target = mainTarget(hxml, 0);

    assertEquals("src/Main.hx", relativePathOf(target, hxml));
  }

  @Test
  @DisplayName("each section resolves through its own class paths")
  public void testEachSectionResolvesThroughItsOwnClassPaths() {
    myFixture.addFileToProject("first/Main.hx", MAIN_HX_SOURCE);
    myFixture.addFileToProject("second/Main.hx", MAIN_HX_SOURCE);
    PsiFile hxml = myFixture.addFileToProject("build.hxml", """
      -cp first
      -main Main
      --interp
      --next
      -cp second
      -main Main
      --interp
      """);

    PsiElement inFirst = mainTarget(hxml, 0);
    PsiElement inSecond = mainTarget(hxml, 1);

    assertEquals("first/Main.hx", relativePathOf(inFirst, hxml));
    assertEquals("second/Main.hx", relativePathOf(inSecond, hxml));
  }

  @Test
  @DisplayName("the last class path wins as in the compiler")
  public void testTheLastClassPathWinsAsInTheCompiler() {
    myFixture.addFileToProject("a/Main.hx", MAIN_HX_SOURCE);
    myFixture.addFileToProject("b/Main.hx", MAIN_HX_SOURCE);
    PsiFile hxml = myFixture.addFileToProject("build.hxml", "-cp a\n-cp b\n-main Main\n--interp\n");

    PsiElement target = mainTarget(hxml, 0);

    assertEquals("b/Main.hx", relativePathOf(target, hxml));
  }

  @Test
  @DisplayName("a packaged main resolves to its class")
  public void testAPackagedMainResolvesToItsClass() {
    myFixture.addFileToProject("src/app/Start.hx", START_HX_SOURCE);
    PsiFile hxml = myFixture.addFileToProject("build.hxml", "-cp src\n-main app.Start\n--interp\n");

    PsiElement target = mainTarget(hxml, 0);

    HaxeClass startClass = assertInstanceOf(HaxeClass.class, target);
    assertEquals("Start", startClass.getName());
  }

  @Test
  @DisplayName("a main outside the class paths stays unresolved")
  public void testAMainOutsideTheClassPathsStaysUnresolved() {
    myFixture.addFileToProject("other/Main.hx", MAIN_HX_SOURCE);
    PsiFile hxml = myFixture.addFileToProject("build.hxml", "-cp src\n-main Main\n--interp\n");

    PsiElement target = mainTarget(hxml, 0);

    assertNull(target, "a same-named class outside the build's class paths is no target");
  }

  @Test
  @DisplayName("renaming the class rewrites the main type path")
  public void testRenamingTheClassRewritesTheMainTypePath() {
    PsiFile start = myFixture.addFileToProject("src/app/Start.hx", START_HX_SOURCE);
    PsiFile hxml = myFixture.addFileToProject("build.hxml", "-cp src\n-main app.Start\n--interp\n");
    HaxeClass startClass = PsiTreeUtil.findChildOfType(start, HaxeClass.class);
    assertNotNull(startClass);

    myFixture.renameElement(startClass, "Launch");

    String hxmlText = hxml.getText();
    assertTrue(hxmlText.contains("-main app.Launch"), hxmlText);
  }

  /** What the {@code index}-th {@code -main} line of the hxml file resolves to. */
  @Nullable
  private static PsiElement mainTarget(PsiFile hxml, int index) {
    List<HXMLMain> mains = List.copyOf(PsiTreeUtil.findChildrenOfType(hxml, HXMLMain.class));
    HXMLQualifiedName typePath = mains.get(index).getQualifiedName();
    assertNotNull(typePath);
    PsiReference reference = typePath.getReference();
    assertNotNull(reference, "a -main type path is a reference");
    return reference.resolve();
  }

  /** The resolved element's file, relative to the hxml file's folder. */
  private static String relativePathOf(@Nullable PsiElement target, PsiFile hxml) {
    assertNotNull(target, "the -main line resolves");
    VirtualFile root = hxml.getVirtualFile().getParent();
    return VfsUtilCore.getRelativePath(target.getContainingFile().getVirtualFile(), root);
  }
}
