package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions.Debugger;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

  /** (hxml fixture, debugger, debug compile additions - null when the target has no debugger lane). */
  static final List<Arguments> HXML_DEBUG_ADDITIONS = List.of(
    // hxml debug additions follow the selected target
    arguments("targets/hl.hxml", Debugger.DEFAULT, List.of("-debug")),
    arguments("targets/cpp.hxml", Debugger.DEFAULT, List.of("-debug", "-lib", "intellij-hxcpp-debug-server")),
    // targets without a debugger lane add nothing
    arguments("targets/jvm.hxml", Debugger.DEFAULT, null),
    // hxml cpp additions compile in the launched debuggers server
    arguments("targets/cpp.hxml", Debugger.HXCPP_VSHAXE, List.of("-debug", "-lib", "hxcpp-debug-server")),
    // the legacy build keeps its own debugger lib
    arguments("targets/cpp.hxml", Debugger.HXCPP_LEGACY, List.of("-debug")),
    // hxml flash additions tag the swf for the player debugger only
    arguments("targets/swf.hxml", Debugger.FLASH_PLAYER, List.of("-debug", "-D", "fdb")),
    // AIR under adl needs no debugger tag
    arguments("targets/swf.hxml", Debugger.DEFAULT, List.of("-debug")));

  @ParameterizedTest(name = "{0} / {1}")
  @FieldSource("HXML_DEBUG_ADDITIONS")
  @DisplayName("hxml debug additions follow the target and debugger")
  public void testHxmlDebugAdditionsFollowTheTargetAndDebugger(String fixturePath, Debugger debugger, List<String> additions) {
    HaxeBuildFile hxml = fixture(fixturePath, HaxeBuildFileType.HXML);

    assertEquals(additions, HaxeBuildSystem.of(HaxeBuildFileType.HXML).debugCompileAdditions(getProject(), hxml, debugger));
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

  /** (build file type, its own action names - the names a custom action may not take). */
  static final List<Arguments> DEFAULT_ACTION_NAMES = List.of(
    arguments(HaxeBuildFileType.HXML, Set.of("Build")),
    arguments(HaxeBuildFileType.LIME, Set.of("test", "run", "build", "clean")),
    arguments(HaxeBuildFileType.NMML, Set.of("test", "run", "build", "clean")),
    arguments(HaxeBuildFileType.HXP_SCRIPT, Set.of("build")));

  @ParameterizedTest(name = "{0}")
  @FieldSource("DEFAULT_ACTION_NAMES")
  @DisplayName("default action names all resolve as built ins")
  public void testDefaultActionNamesAllResolveAsBuiltIns(HaxeBuildFileType type, Set<String> names) {
    HaxeBuildSystem buildSystem = HaxeBuildSystem.of(type);
    VirtualFile file = myFixture.addFileToProject("reserved." + extensionOf(type), "").getVirtualFile();
    HaxeBuildFile buildFile = new HaxeBuildFile(file, type);

    assertEquals(names, Set.copyOf(buildSystem.defaultActionNames()));
    for (String name : names) {
      boolean answered = buildSystem.actionCommand(getProject(), null, buildFile, name) != null;
      assertTrue(answered, name + " must resolve as a built-in");
    }
  }

  @Test
  @DisplayName("a C++ hxml gets a clean action that runs in its output folder")
  public void testACppHxmlGetsACleanActionThatRunsInItsOutputFolder() {
    VirtualFile file = myFixture.addFileToProject("native/cpp.hxml", "-main Main\n-cpp out/cpp\n").getVirtualFile();
    HaxeBuildFile buildFile = new HaxeBuildFile(file, HaxeBuildFileType.HXML);
    HaxeBuildSystem buildSystem = HaxeBuildSystem.of(HaxeBuildFileType.HXML);

    List<String> clean = buildSystem.actionCommand(getProject(), null, buildFile, "Clean");
    String workDirectory = buildSystem.actionWorkDirectory(getProject(), buildFile, "Clean");

    assertEquals(List.of("Build", "Clean"), buildSystem.actionNames(getProject(), buildFile));
    assertEquals(List.of("run", "hxcpp", "Build.xml", "clean"), clean.subList(1, clean.size()));
    assertEquals(HaxeBuildWorkDirectories.resolve(getProject(), file, "out/cpp").toString(), workDirectory);
    assertEquals("hxcpp clean out/cpp", buildSystem.presentableCommand(getProject(), buildFile, "Clean"));
  }

  @Test
  @DisplayName("an hxml building another target has no clean action")
  public void testAnHxmlBuildingAnotherTargetHasNoCleanAction() {
    VirtualFile file = myFixture.addFileToProject("web/js.hxml", "-main Main\n-js out/main.js\n").getVirtualFile();
    HaxeBuildFile buildFile = new HaxeBuildFile(file, HaxeBuildFileType.HXML);
    HaxeBuildSystem buildSystem = HaxeBuildSystem.of(HaxeBuildFileType.HXML);

    assertEquals(List.of("Build"), buildSystem.actionNames(getProject(), buildFile));
    assertNull(buildSystem.actionCommand(getProject(), null, buildFile, "Clean"));
  }

  private static String extensionOf(HaxeBuildFileType type) {
    return switch (type) {
      case HXML -> "hxml";
      case NMML -> "nmml";
      case HXP_SCRIPT -> "hxp";
      default -> "xml";
    };
  }

  private HaxeBuildFile fixture(String relativePath, HaxeBuildFileType type) {
    VirtualFile file = myFixture.copyFileToProject(relativePath);
    return new HaxeBuildFile(file, type);
  }
}
