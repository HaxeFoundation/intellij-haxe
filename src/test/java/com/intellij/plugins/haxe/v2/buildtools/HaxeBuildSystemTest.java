package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Each build system resolves its CURRENT selection: the haxe target it compiles to, and the debug compile additions that target takes. */
@DisplayName("Build tools: build system")
public class HaxeBuildSystemTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("hxml resolves the selected sections target")
  public void testHxmlResolvesTheSelectedSectionsTarget() {
    HaxeBuildFile hl = fixture("targets/hl.hxml", HaxeBuildFileType.HXML);
    assertEquals(HaxeTarget.HL, HaxeBuildSystem.of(HaxeBuildFileType.HXML).launchTarget(getProject(), hl));

    HaxeBuildFile interp = fixture("test.hxml", HaxeBuildFileType.HXML);
    assertEquals(HaxeTarget.INTERP, HaxeBuildSystem.of(HaxeBuildFileType.HXML).launchTarget(getProject(), interp));

    VirtualFile noTarget = myFixture.addFileToProject("no-target.hxml", "-cp src\n--main Main\n").getVirtualFile();
    HaxeTarget launchTarget = HaxeBuildSystem.of(HaxeBuildFileType.HXML)
      .launchTarget(getProject(), new HaxeBuildFile(noTarget, HaxeBuildFileType.HXML));
    assertNull(launchTarget, "an hxml without a target flag declares none - the caller decides interp semantics");
  }

  @Test
  @DisplayName("lime resolves the selected target flag")
  public void testLimeResolvesTheSelectedTargetFlag() {
    HaxeBuildFile lime = fixture("targets/lime-project.xml", HaxeBuildFileType.LIME);
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.LIME);

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Neko");
    assertEquals(HaxeTarget.NEKO, system.launchTarget(getProject(), lime));

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Windows");
    assertEquals(HaxeTarget.CPP, system.launchTarget(getProject(), lime));
  }

  @Test
  @DisplayName("nme resolves the selected target flag")
  public void testNmeResolvesTheSelectedTargetFlag() {
    HaxeBuildFile nme = fixture("targets/tests.nmml", HaxeBuildFileType.NMML);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(nme.file(), "Neko");
    assertEquals(HaxeTarget.NEKO, HaxeBuildSystem.of(HaxeBuildFileType.NMML).launchTarget(getProject(), nme));
  }

  @Test
  @DisplayName("hxp script declares no static target")
  public void testHxpScriptDeclaresNoStaticTarget() {
    HaxeBuildFile script = fixture("test.hxml", HaxeBuildFileType.HXP_SCRIPT);
    assertNull(HaxeBuildSystem.of(HaxeBuildFileType.HXP_SCRIPT).launchTarget(getProject(), script));
  }

  @Test
  @DisplayName("hxml debug additions follow the selected target")
  public void testHxmlDebugAdditionsFollowTheSelectedTarget() {
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.HXML);

    HaxeBuildFile hl = fixture("targets/hl.hxml", HaxeBuildFileType.HXML);
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), hl));

    HaxeBuildFile cpp = fixture("targets/cpp.hxml", HaxeBuildFileType.HXML);
    assertEquals(List.of("-debug", "-lib", "intellij-hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), cpp));

    HaxeBuildFile jvm = fixture("targets/jvm.hxml", HaxeBuildFileType.HXML);
    assertNull(system.debugCompileAdditions(getProject(), jvm), "targets without a debugger lane add nothing");
  }

  @Test
  @DisplayName("lime debug additions add the hxcpp server on desktop targets")
  public void testLimeDebugAdditionsAddTheHxcppServerOnDesktopTargets() {
    HaxeBuildFile lime = fixture("targets/lime-project.xml", HaxeBuildFileType.LIME);
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.LIME);

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Windows");
    assertEquals(List.of("-debug", "--haxelib=intellij-hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), lime));

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Neko");
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), lime));
  }

  @Test
  @DisplayName("nme debug additions use the single token library flag")
  public void testNmeDebugAdditionsUseTheSingleTokenLibraryFlag() {
    HaxeBuildFile nme = fixture("targets/tests.nmml", HaxeBuildFileType.NMML);
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.NMML);

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(nme.file(), "Windows");
    assertEquals(List.of("-debug", "--library intellij-hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), nme));

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(nme.file(), "Neko");
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), nme));
  }

  @Test
  @DisplayName("hxp script has no debug additions")
  public void testHxpScriptHasNoDebugAdditions() {
    HaxeBuildFile script = fixture("test.hxml", HaxeBuildFileType.HXP_SCRIPT);
    assertNull(HaxeBuildSystem.of(HaxeBuildFileType.HXP_SCRIPT).debugCompileAdditions(getProject(), script));
  }

  private HaxeBuildFile fixture(String relativePath, HaxeBuildFileType type) {
    VirtualFile file = myFixture.copyFileToProject(relativePath);
    return new HaxeBuildFile(file, type);
  }
}
