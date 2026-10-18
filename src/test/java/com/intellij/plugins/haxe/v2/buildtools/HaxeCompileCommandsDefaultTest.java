package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands.Resolved;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** A module without a build command builds its default build file; one set to skip building builds nothing. */
@DisplayName("Build tools: compile commands default")
public class HaxeCompileCommandsDefaultTest extends HaxeCodeInsightFixtureTestCase {
  private static final String BUILD_HXML_SOURCE = """
    -cp src
    -main Main
    --interp
    """;
  private static final String TEST_HXML_SOURCE = """
    -cp test
    -lib utest
    -main TestMain
    --interp
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** (build file paths, tests build paths, active build file, expected default). */
  static final List<Arguments> DEFAULT_BUILD_FILES = List.of(
    arguments(List.of("/a/build.hxml", "/a/web.hxml"), List.of(), "/a/web.hxml", "/a/web.hxml"),
    // the active build file of another module does not count
    arguments(List.of("/a/build.hxml"), List.of(), "/b/build.hxml", "/a/build.hxml"),
    arguments(List.of("/a/build.hxml", "/a/test.hxml"), List.of("/a/test.hxml"), null, "/a/build.hxml"),
    arguments(List.of("/a/build.hxml", "/a/web.hxml"), List.of(), null, null),
    arguments(List.of("/a/test.hxml"), List.of("/a/test.hxml"), null, null),
    arguments(List.of(), List.of(), null, null));

  @ParameterizedTest(name = "{0}, tests {1}, active {2}")
  @FieldSource("DEFAULT_BUILD_FILES")
  @DisplayName("default build file")
  public void testDefaultBuildFile(List<String> buildFilePaths, List<String> testsPaths, String activePath,
                                   String expectedPath) {
    assertEquals(expectedPath, HaxeCompileCommands.defaultBuildFilePath(buildFilePaths, testsPaths, activePath));
  }

  @Test
  @DisplayName("a module without a build command builds its only build file")
  public void testAModuleWithoutABuildCommandBuildsItsOnlyBuildFile() {
    myFixture.addFileToProject("build.hxml", BUILD_HXML_SOURCE);
    myFixture.addFileToProject("test.hxml", TEST_HXML_SOURCE);

    Resolved resolved = resolve(myFixture.getModule().getName());

    assertNotNull(resolved, "the module builds its default build file");
    boolean buildsBuildHxml = resolved.command().stream().anyMatch(argument -> argument.endsWith("build.hxml"));
    assertTrue(buildsBuildHxml, "the tests build is no default, got: " + resolved.command());
  }

  @Test
  @DisplayName("a module set to skip building builds nothing")
  public void testAModuleSetToSkipBuildingBuildsNothing() {
    myFixture.addFileToProject("build.hxml", BUILD_HXML_SOURCE);
    String containerId = myFixture.getModule().getName();
    HaxeEnvironmentStore.getInstance(getProject()).skipBuild(containerId);

    Resolved resolved = resolve(containerId);

    assertNull(resolved);
  }

  private Resolved resolve(String containerId) {
    return HaxeReadActions.compute(() -> HaxeCompileCommands.resolve(getProject(), containerId));
  }
}
