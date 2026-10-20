package com.intellij.plugins.haxe.profiler.bridge.data;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.NavigatablePsiElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Every lime target generates its own ApplicationMain; a frame opens the copy from the capture's build. */
@DisplayName("Profiler: call stack element")
public class HaxeCallStackElementTest extends HaxeLightFixtureTestCase {

  private static final String APPLICATION_MAIN_SOURCE = "class ApplicationMain { public static function create():Void {} }";

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a symbol resolves to the generated class nearest the capture")
  public void testASymbolResolvesToTheGeneratedClassNearestTheCapture() {
    VirtualFile html5 = generatedMain("html5");
    VirtualFile windows = generatedMain("windows");
    Path windowsCapture = captureBeside(windows);

    NavigatablePsiElement declaration = HaxeCallStackElement.declarationOf(getProject(), "ApplicationMain.create", windowsCapture);

    assertNotNull(declaration);
    assertEquals(windows, declaration.getContainingFile().getVirtualFile(), "the windows export, not " + html5.getPath());
  }

  @Test
  @DisplayName("a relative frame path resolves to the file nearest the capture")
  public void testARelativeFramePathResolvesToTheFileNearestTheCapture() {
    generatedMain("windows");
    VirtualFile html5 = generatedMain("html5");
    HaxeCallStackElement frame = new HaxeCallStackElement("ApplicationMain.create", "ApplicationMain.hx", 1, captureBeside(html5));

    NavigatablePsiElement[] navigatables = frame.calcNavigatables(getProject());

    assertEquals(1, navigatables.length);
    assertEquals(html5, navigatables[0].getContainingFile().getVirtualFile());
  }

  private VirtualFile generatedMain(String target) {
    return myFixture.addFileToProject("export/" + target + "/haxe/ApplicationMain.hx", APPLICATION_MAIN_SOURCE).getVirtualFile();
  }

  /** The capture lands in {@code export/<target>/bin/captures/<stamp>/}, next to that target's executable. */
  private static Path captureBeside(VirtualFile generatedMain) {
    String targetDirectory = generatedMain.getParent().getParent().getPath();
    return Path.of(targetDirectory, "bin", "captures", "stamp", "hxcppprofile.hxtsession");
  }
}
