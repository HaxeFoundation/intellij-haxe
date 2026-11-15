package com.intellij.plugins.haxe.resolve;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.PsiTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** A qualified name two classpaths declare: the compiler takes the first classpath's, whether or not the indexes are ready. */
@DisplayName("Resolve: duplicate qualified names")
public class HaxeDuplicateQualifiedNameResolveTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a class two classpaths declare resolves to the first classpath's, with or without indexes")
  public void testAClassTwoClasspathsDeclareResolvesToTheFirstClasspathsWithOrWithoutIndexes() throws IOException {
    Module module = myFixture.getModule();
    VirtualFile laterClasspath = myFixture.getTempDirFixture().findOrCreateDir("lib");
    PsiTestUtil.addSourceRoot(module, laterClasspath);
    Disposer.register(getTestRootDisposable(), () -> PsiTestUtil.removeSourceRoot(module, laterClasspath));
    // the later classpath's copy is indexed first
    myFixture.addFileToProject("lib/pack/sub/Shape.hx", "package pack.sub;\nclass Shape { public static function make():Int return 2; }");
    myFixture.addFileToProject("pack/sub/Shape.hx", "package pack.sub;\nclass Shape { public static function make():Int return 1; }");
    myFixture.configureByText("Main.hx", "class Main { static function main() { pack.sub.Sha<caret>pe.make(); } }");
    HaxeReference reference = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), HaxeReference.class);
    assertNotNull(reference);

    String indexed = containingPath(reference.resolve());
    PsiManager.getInstance(getProject()).dropPsiCaches();
    String[] walked = new String[1];
    DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> walked[0] = containingPath(reference.resolve()));

    assertEquals("/src/pack/sub/Shape.hx", indexed);
    assertEquals(indexed, walked[0]);
  }

  private static String containingPath(PsiElement element) {
    return element == null ? null : element.getContainingFile().getVirtualFile().getPath();
  }
}
