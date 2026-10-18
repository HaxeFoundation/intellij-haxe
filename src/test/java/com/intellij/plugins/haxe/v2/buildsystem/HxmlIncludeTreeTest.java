package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Includes become entries with their own declarations, resolved next to the root; a loop ends where it closes. */
@DisplayName("Build system: hxml include tree")
public class HxmlIncludeTreeTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("an included file keeps the libraries and defines its own lines declare")
  public void testAnIncludedFileKeepsTheLibrariesAndDefinesItsOwnLinesDeclare() {
    myFixture.addFileToProject("common.hxml", "-lib hxparse\n-D shared\n");
    VirtualFile root = myFixture.addFileToProject("compile.hxml", "common.hxml\n-lib utest:1.13.2\n-main Main\n").getVirtualFile();

    HxmlIncludeTree tree = HxmlIncludeTree.of(root);

    assertEquals(List.of("compile.hxml", "common.hxml"), tree.files().stream().map(entry -> entry.file().getName()).toList());
    assertEquals(List.of(new HaxeLibDependency("utest", "1.13.2")), tree.own().libraries());
    HxmlIncludeTree common = tree.includes().getFirst();
    assertEquals(List.of(new HaxeLibDependency("hxparse", null)), common.own().libraries());
    assertEquals("shared", common.own().defines().getFirst().name());
    assertEquals(List.of(new HaxeLibDependency("utest", "1.13.2"), new HaxeLibDependency("hxparse", null)), tree.declaredLibraries());
  }

  @Test
  @DisplayName("an include loop ends where it closes")
  public void testAnIncludeLoopEndsWhereItCloses() {
    myFixture.addFileToProject("b.hxml", "a.hxml\n-lib b\n");
    VirtualFile a = myFixture.addFileToProject("a.hxml", "b.hxml\n-lib a\n").getVirtualFile();

    HxmlIncludeTree tree = HxmlIncludeTree.of(a);

    assertEquals(List.of("a.hxml", "b.hxml"), tree.files().stream().map(entry -> entry.file().getName()).toList());
    assertTrue(tree.includes().getFirst().includes().isEmpty(), "b's reference back to a is dropped");
    assertEquals(List.of(new HaxeLibDependency("a", null), new HaxeLibDependency("b", null)), tree.declaredLibraries());
  }
}
