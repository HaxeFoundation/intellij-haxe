package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.haxelib.HaxelibNameUtil;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync.DeclaredDependency;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTestsBuildFileStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The dependency-collection half of the library sync, against a layout whose
 * only build file lives in a tests/ subfolder (so the top-level scan finds
 * nothing and it is registered manually), with its -lib declarations behind
 * a chain of hxml includes.
 */
@DisplayName("Build tools: library sync")
public class HaxeLibrarySyncTest extends HaxeCodeInsightFixtureTestCase {

  private static final String TESTS_HXML = """
    -cp src
    -cp ../src
    compile-hl.hxml
    """;
  private static final String HL_HXML = """
    compile-each.hxml
    --main unit.TestMain
    -hl bin/unit.hl
    """;
  private static final String EACH_HXML = """
    -p src
    -p ../src
    -lib utest
    """;

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("manually added active build file in a subfolder feeds the module dependencies")
  public void testManuallyAddedActiveBuildFileInASubfolderFeedsTheModuleDependencies() {
    VirtualFile buildFile = registerTestsBuildInSubfolder();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(buildFile.getPath());

    assertUtestCollected("-lib utest behind two include levels must be collected");
  }

  @Test
  @DisplayName("sole build file is the implicit active file for the sync")
  public void testSoleBuildFileIsTheImplicitActiveFileForTheSync() {
    // nothing stored: the tree shows the only build file as (Active), and the
    // sync must follow the same rule instead of seeing no active file at all
    registerTestsBuildInSubfolder();

    assertUtestCollected("the sole known build file must feed the module dependencies without an explicit activation");
  }

  @Test
  @DisplayName("tests build file libraries join the active build's dependencies")
  public void testTestsBuildFileLibrariesJoinTheActiveBuildsDependencies() {
    VirtualFile mainBuild = myFixture.addFileToProject("build.hxml", "-cp src\n--main Main\n").getVirtualFile();
    VirtualFile testsBuild = registerTestsBuildInSubfolder();

    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(mainBuild.getPath());
    HaxeTestsBuildFileStore.getInstance(getProject()).markTestsFile(myFixture.getModule().getName(), testsBuild.getPath());

    assertUtestCollected("the module holds the test sources too - the tests build's libraries must resolve");
  }

  @Test
  @DisplayName("every build file of the module feeds its dependencies, not only the active one")
  public void testEveryBuildFileOfTheModuleFeedsItsDependenciesNotOnlyTheActiveOne() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", "-cp src\n-main WebMain\n-lib tink_core\n-js bin/web.js\n").getVirtualFile();
    myFixture.addFileToProject("node.hxml", "-cp src\n-main NodeMain\n-lib hxnodejs\n-js bin/node.js\n");
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());

    List<String> names = collectedDependencyNames();

    assertTrue(names.contains("tink_core") && names.contains("hxnodejs"),
               "a file resolves under its own build, so both builds' libraries attach; got: " + names);
  }

  /** The fixture's module has the V1 module type; once it carries the legacy engine's entries, that engine owns its libraries and the sync adds none, or they would appear twice. */
  @Test
  @DisplayName("a module the legacy engine equips gets no dependencies from the sync")
  public void testAModuleTheLegacyEngineEquipsGetsNoDependenciesFromTheSync() {
    registerTestsBuildInSubfolder();
    String legacyEntry = HaxelibNameUtil.stringifyHaxelib("utest:1.0.0");
    ModuleRootModificationUtil.addModuleLibrary(myFixture.getModule(), legacyEntry, List.of(), List.of());

    List<String> names = collectedDependencyNames();

    assertTrue(names.isEmpty(), "the legacy engine owns the module's libraries, got: " + names);
  }

  /** haxelib runs where the declaring build compiles, so a .haxelib repository local to that build is the one it resolves from. */
  @Test
  @DisplayName("a dependency resolves from its declaring builds work directory")
  public void testADependencyResolvesFromItsDeclaringBuildsWorkDirectory() {
    VirtualFile buildFile = registerTestsBuildInSubfolder();

    DeclaredDependency utest = collectedDependency("utest");

    assertEquals(Path.of(HaxeBuildWorkDirectories.workDirectory(getProject(), buildFile)), utest.workDirectory());
  }

  /** The only build file sits in tests/ (registered manually) with its -libs behind includes. */
  @NotNull
  private VirtualFile registerTestsBuildInSubfolder() {
    VirtualFile buildFile = myFixture.addFileToProject("tests/compile.hxml", TESTS_HXML).getVirtualFile();
    myFixture.addFileToProject("tests/compile-hl.hxml", HL_HXML);
    myFixture.addFileToProject("tests/compile-each.hxml", EACH_HXML);
    HaxeBuildFilesStore.getInstance(getProject()).addFile(myFixture.getModule().getName(), buildFile.getPath());
    return buildFile;
  }

  /** The fixture module's collected dependencies; the module must have an entry. */
  @NotNull
  private List<DeclaredDependency> collectedDependencies() {
    Map<Module, List<DeclaredDependency>> dependencies =
      ReadAction.computeBlocking(() -> HaxeLibrarySync.collect(getProject())).dependencies();
    List<DeclaredDependency> moduleDependencies = dependencies.get(myFixture.getModule());
    assertNotNull(moduleDependencies, "the module must have a dependency entry");
    return moduleDependencies;
  }

  @NotNull
  private List<String> collectedDependencyNames() {
    return collectedDependencies().stream()
      .map(declared -> declared.dependency().name())
      .toList();
  }

  @NotNull
  private DeclaredDependency collectedDependency(@NotNull String name) {
    List<DeclaredDependency> dependencies = collectedDependencies();
    DeclaredDependency declared = dependencies.stream()
      .filter(candidate -> candidate.dependency().name().equals(name))
      .findFirst()
      .orElse(null);
    assertNotNull(declared, name + " must be collected, got: " + dependencies);
    return declared;
  }

  private void assertUtestCollected(@NotNull String reason) {
    List<String> names = collectedDependencyNames();

    assertTrue(names.contains("utest"), reason + ", got: " + names);
  }
}
