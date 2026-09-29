package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: tests build file store")
public class HaxeTestsBuildFileStoreTest {

  private static final String MODULE = "app";
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
    assertEquals(List.of("/p/project.xml"), store.resolveTestsFiles(MODULE, PLAIN_FILES));
    assertEquals(List.of("/p/test.hxml", "/p/project.xml"),
                 store.resolveTestsFiles(MODULE, List.of("/p/test.hxml", "/p/project.xml")),
                 "a mark does not hide the sibling conventional candidate");
  }

  @Test
  @DisplayName("stale marked paths drop out")
  public void staleMarkedPathsDropOut() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    store.markTestsFile(MODULE, "/p/deleted.hxml");
    assertTrue(store.resolveTestsFiles(MODULE, PLAIN_FILES).isEmpty());
  }

  @Test
  @DisplayName("unmarking a conventional file excludes it")
  public void unmarkingAConventionalFileExcludesIt() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    List<String> candidates = List.of("/p/build.hxml", "/p/test.hxml", "/p/other/test.hxml");
    store.unmarkTestsFile(MODULE, "/p/test.hxml");
    assertEquals(List.of("/p/other/test.hxml"), store.resolveTestsFiles(MODULE, candidates),
                 "the exclusion holds against the conventional name; siblings stay");

    store.markTestsFile(MODULE, "/p/test.hxml");
    assertEquals(List.of("/p/test.hxml", "/p/other/test.hxml"), store.resolveTestsFiles(MODULE, candidates),
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
                 store.resolveTestsFiles(MODULE, candidates));
  }

  @Test
  @DisplayName("no conventional candidate yields none")
  public void noConventionalCandidateYieldsNone() {
    HaxeTestsBuildFileStore store = new HaxeTestsBuildFileStore();
    assertTrue(store.resolveTestsFiles(MODULE, PLAIN_FILES).isEmpty());
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
