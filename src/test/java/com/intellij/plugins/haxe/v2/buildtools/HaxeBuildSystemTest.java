package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions.Debugger;
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
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), hl, Debugger.DEFAULT));

    HaxeBuildFile cpp = fixture("targets/cpp.hxml", HaxeBuildFileType.HXML);
    assertEquals(List.of("-debug", "-lib", "intellij-hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), cpp, Debugger.DEFAULT));

    HaxeBuildFile jvm = fixture("targets/jvm.hxml", HaxeBuildFileType.HXML);
    assertNull(system.debugCompileAdditions(getProject(), jvm, Debugger.DEFAULT), "targets without a debugger lane add nothing");
  }

  @Test
  @DisplayName("hxml cpp additions compile in the launched debuggers server")
  public void testHxmlCppAdditionsCompileInTheLaunchedDebuggersServer() {
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.HXML);
    HaxeBuildFile cpp = fixture("targets/cpp.hxml", HaxeBuildFileType.HXML);

    assertEquals(List.of("-debug", "-lib", "hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), cpp, Debugger.HXCPP_VSHAXE));
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), cpp, Debugger.HXCPP_LEGACY),
                 "the legacy build keeps its own debugger lib");
  }

  @Test
  @DisplayName("hxml flash additions tag the swf for the player debugger only")
  public void testHxmlFlashAdditionsTagTheSwfForThePlayerDebuggerOnly() {
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.HXML);
    HaxeBuildFile swf = fixture("targets/swf.hxml", HaxeBuildFileType.HXML);

    assertEquals(List.of("-debug", "-D", "fdb"), system.debugCompileAdditions(getProject(), swf, Debugger.FLASH_PLAYER));
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), swf, Debugger.DEFAULT),
                 "AIR under adl needs no debugger tag");
  }

  @Test
  @DisplayName("lime debug additions add the hxcpp server on desktop targets")
  public void testLimeDebugAdditionsAddTheHxcppServerOnDesktopTargets() {
    HaxeBuildFile lime = fixture("targets/lime-project.xml", HaxeBuildFileType.LIME);
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.LIME);

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Windows");
    assertEquals(List.of("-debug", "--haxelib=intellij-hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), lime, Debugger.DEFAULT));
    assertEquals(List.of("-debug", "--haxelib=hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), lime, Debugger.HXCPP_VSHAXE));
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), lime, Debugger.HXCPP_LEGACY));

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Neko");
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), lime, Debugger.DEFAULT));
  }

  @Test
  @DisplayName("lime flash additions define fdb for the player debugger only")
  public void testLimeFlashAdditionsDefineFdbForThePlayerDebuggerOnly() {
    HaxeBuildFile lime = fixture("targets/lime-project.xml", HaxeBuildFileType.LIME);
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.LIME);

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Flash");
    assertEquals(List.of("-debug", "-Dfdb"), system.debugCompileAdditions(getProject(), lime, Debugger.FLASH_PLAYER));
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), lime, Debugger.DEFAULT));

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(lime.file(), "Windows");
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), lime, Debugger.FLASH_PLAYER),
                 "the tag belongs to swf output only");
  }

  @Test
  @DisplayName("nme debug additions use the single token library flag")
  public void testNmeDebugAdditionsUseTheSingleTokenLibraryFlag() {
    HaxeBuildFile nme = fixture("targets/tests.nmml", HaxeBuildFileType.NMML);
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.NMML);

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(nme.file(), "Windows");
    assertEquals(List.of("-debug", "--library intellij-hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), nme, Debugger.DEFAULT));
    assertEquals(List.of("-debug", "--library hxcpp-debug-server"),
                 system.debugCompileAdditions(getProject(), nme, Debugger.HXCPP_VSHAXE));
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), nme, Debugger.HXCPP_LEGACY));

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(nme.file(), "Neko");
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), nme, Debugger.DEFAULT));
  }

  @Test
  @DisplayName("nme flash additions define fdb for the player debugger only")
  public void testNmeFlashAdditionsDefineFdbForThePlayerDebuggerOnly() {
    HaxeBuildFile nme = fixture("targets/tests.nmml", HaxeBuildFileType.NMML);
    HaxeBuildSystem system = HaxeBuildSystem.of(HaxeBuildFileType.NMML);

    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(nme.file(), "Flash");
    assertEquals(List.of("-debug", "-Dfdb"), system.debugCompileAdditions(getProject(), nme, Debugger.FLASH_PLAYER));
    assertEquals(List.of("-debug"), system.debugCompileAdditions(getProject(), nme, Debugger.DEFAULT));
  }

  @Test
  @DisplayName("hxp script has no debug additions")
  public void testHxpScriptHasNoDebugAdditions() {
    HaxeBuildFile script = fixture("test.hxml", HaxeBuildFileType.HXP_SCRIPT);
    assertNull(HaxeBuildSystem.of(HaxeBuildFileType.HXP_SCRIPT).debugCompileAdditions(getProject(), script, Debugger.DEFAULT));
  }

  private HaxeBuildFile fixture(String relativePath, HaxeBuildFileType type) {
    VirtualFile file = myFixture.copyFileToProject(relativePath);
    return new HaxeBuildFile(file, type);
  }
}
