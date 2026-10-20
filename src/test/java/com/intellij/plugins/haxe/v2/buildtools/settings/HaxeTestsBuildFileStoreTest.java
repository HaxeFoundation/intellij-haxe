package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Tool window: tests build file store")
public class HaxeTestsBuildFileStoreTest {

  private static final String MODULE = "app";
  private static final String ROOT = "/p";
  private static final List<String> PLAIN_FILES = List.of("/p/build.hxml", "/p/project.xml");

  @Test
  @DisplayName("defaults are empty")
  public void defaultsAreEmpty() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    assertTrue(store.getTestsFilePaths(MODULE).isEmpty());
  }

  @Test
  @DisplayName("tests files can be marked and unmarked")
  public void testsFilesCanBeMarkedAndUnmarked() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    store.markTestsFile(MODULE, "/p/tests.hxml");
    store.markTestsFile(MODULE, "/p/tests.hxml");
    assertEquals(List.of("/p/tests.hxml"), store.getTestsFilePaths(MODULE));

    store.unmarkTestsFile(MODULE, "/p/tests.hxml");
    assertTrue(store.getTestsFilePaths(MODULE).isEmpty());
  }

  @Test
  @DisplayName("a container may hold several tests files")
  public void aContainerMayHoldSeveralTestsFiles() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    store.markTestsFile(MODULE, "/p/utest/test.hxml");
    store.markTestsFile(MODULE, "/p/nme/tests/tests.nmml");
    assertEquals(List.of("/p/utest/test.hxml", "/p/nme/tests/tests.nmml"), store.getTestsFilePaths(MODULE));
  }

  @Test
  @DisplayName("containers are independent")
  public void containersAreIndependent() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    store.markTestsFile("app", "/app/test.hxml");
    store.markTestsFile("lib", "/lib/tests.hxml");

    assertEquals(List.of("/app/test.hxml"), store.getTestsFilePaths("app"));
    assertEquals(List.of("/lib/tests.hxml"), store.getTestsFilePaths("lib"));
  }

  @Test
  @DisplayName("marked files join the conventional candidates")
  public void markedFilesJoinTheConventionalCandidates() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    store.markTestsFile(MODULE, "/p/project.xml");
    assertEquals(List.of("/p/project.xml"), store.resolveTestsFiles(MODULE, ROOT, PLAIN_FILES));
    assertEquals(List.of("/p/test.hxml", "/p/project.xml"),
                 store.resolveTestsFiles(MODULE, ROOT, List.of("/p/test.hxml", "/p/project.xml")),
                 "a mark does not hide the sibling conventional candidate");
  }

  @Test
  @DisplayName("stale marked paths drop out")
  public void staleMarkedPathsDropOut() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    store.markTestsFile(MODULE, "/p/deleted.hxml");
    assertTrue(store.resolveTestsFiles(MODULE, ROOT, PLAIN_FILES).isEmpty());
  }

  @Test
  @DisplayName("unmarking a conventional file excludes it")
  public void unmarkingAConventionalFileExcludesIt() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    List<String> candidates = List.of("/p/build.hxml", "/p/test.hxml", "/p/other/test.hxml");
    store.unmarkTestsFile(MODULE, "/p/test.hxml");
    assertEquals(List.of("/p/other/test.hxml"), store.resolveTestsFiles(MODULE, ROOT, candidates),
                 "the exclusion holds against the conventional name; siblings stay");

    store.markTestsFile(MODULE, "/p/test.hxml");
    assertEquals(List.of("/p/test.hxml", "/p/other/test.hxml"), store.resolveTestsFiles(MODULE, ROOT, candidates),
                 "re-marking clears the exclusion");
  }

  @Test
  @DisplayName("every conventional candidate is suggested")
  public void everyConventionalCandidateIsSuggested() {
    // a container holding several sub-projects gets each of their tests
    // builds suggested - name matches and tests-directory residents alike
    List<String> candidates = List.of("/p/build.hxml", "/p/test.hxml",
                                      "/p/nme/tests/tests.nmml", "/p/openfl/tests/project.xml");
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    assertEquals(List.of("/p/test.hxml", "/p/nme/tests/tests.nmml", "/p/openfl/tests/project.xml"),
                 store.resolveTestsFiles(MODULE, ROOT, candidates));
  }

  @Test
  @DisplayName("no conventional candidate yields none")
  public void noConventionalCandidateYieldsNone() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    assertTrue(store.resolveTestsFiles(MODULE, ROOT, PLAIN_FILES).isEmpty());
  }

  /** (candidate path, container root, conventional tests build?). */
  static final List<Arguments> CONVENTIONAL_PATHS = List.of(
    // the directories above the container root never count
    arguments("/home/tests/app/build.hxml", "/home/tests/app", false),
    arguments("/home/tests/app/tests/build.hxml", "/home/tests/app", true),
    arguments("/home/tests/app/test.hxml", "/home/tests/app", true),
    arguments("C:\\tests\\app\\Tests\\build.hxml", "C:/tests/app", true),
    // the root's own name counts: a tests module
    arguments("/home/app/tests/build.hxml", "/home/app/tests", true),
    // no known root: the whole path is checked
    arguments("/home/tests/app/build.hxml", null, true));

  @ParameterizedTest(name = "{0} under {1}")
  @FieldSource("CONVENTIONAL_PATHS")
  @DisplayName("the tests directory rule is relative to the container root")
  public void theTestsDirectoryRuleIsRelativeToTheContainerRoot(String path, String containerRoot, boolean conventional) {
    assertEquals(conventional, HaxeTestsBuildFileStore.isConventionalTestsPath(path, containerRoot));
  }

  @Test
  @DisplayName("a checkout under a tests folder suggests only its own tests builds")
  public void aCheckoutUnderATestsFolderSuggestsOnlyItsOwnTestsBuilds() {
    List<String> candidates = List.of("/home/tests/app/build.hxml", "/home/tests/app/tests/build.hxml");
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    assertEquals(List.of("/home/tests/app/tests/build.hxml"), store.resolveTestsFiles(MODULE, "/home/tests/app", candidates));
  }

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    store.markTestsFile(MODULE, "/p/tests/compile-hl.hxml");
    store.markTestsFile(MODULE, "/p/test.hxml");
    store.unmarkTestsFile(MODULE, "/p/sub/test.hxml");

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeTestsBuildFileStore.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeTestsBuildFileStore.State.class);

    HaxeTestsBuildFileStore reloaded = new HaxeTestsBuildFileStore();
    reloaded.loadState(deserialized);
    assertEquals(List.of("/p/tests/compile-hl.hxml", "/p/test.hxml"), reloaded.getTestsFilePaths(MODULE));
    assertEquals(List.of("/p/sub/test.hxml"), reloaded.getAllExcludedFilePaths(), "exclusions survive the round trip");
  }
}
