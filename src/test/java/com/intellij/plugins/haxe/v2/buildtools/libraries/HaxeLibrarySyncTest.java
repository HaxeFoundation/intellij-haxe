package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTestsBuildFileStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

  /** The only build file sits in tests/ (registered manually) with its -libs behind includes. */
  @NotNull
  private VirtualFile registerTestsBuildInSubfolder() {
    VirtualFile buildFile = myFixture.addFileToProject("tests/compile.hxml", TESTS_HXML).getVirtualFile();
    myFixture.addFileToProject("tests/compile-hl.hxml", HL_HXML);
    myFixture.addFileToProject("tests/compile-each.hxml", EACH_HXML);
    HaxeBuildFilesStore.getInstance(getProject()).addFile(myFixture.getModule().getName(), buildFile.getPath());
    return buildFile;
  }

  private void assertUtestCollected(@NotNull String reason) {
    Module module = myFixture.getModule();
    Map<Module, List<HaxeLibDependency>> dependencies =
      ReadAction.computeBlocking(() -> HaxeLibrarySync.collectDependencies(getProject()));

    List<HaxeLibDependency> moduleDependencies = dependencies.get(module);
    assertNotNull(moduleDependencies, "the module must have a dependency entry");
    boolean utestCollected = moduleDependencies.stream().anyMatch(dependency -> dependency.name().equals("utest"));
    assertTrue(utestCollected, reason + ", got: " + moduleDependencies);
  }
}
