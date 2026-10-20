package com.intellij.plugins.haxe.v2.buildtools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: build ownership")
public class HaxeBuildOwnershipTest {

  /** A candidate build: its name and the source directories it compiles. */
  private record Build(String name, List<String> sourceDirectories) {
  }

  private static final Build APP = new Build("app", List.of("/p/src"));
  private static final Build TOOLS = new Build("tools", List.of("/p/src/tools"));
  private static final Build WEB = new Build("web", List.of("/p/src"));
  private static final Build OTHER = new Build("other", List.of("/other/src"));
  private static final List<Build> CANDIDATES = List.of(APP, TOOLS, WEB, OTHER);

  /** (source directories, file path, specificity). */
  static final List<Arguments> SPECIFICITIES = List.of(
    arguments(List.of("/p/src"), "/p/src/Main.hx", 6),
    arguments(List.of("/p/src", "/p/src/tools"), "/p/src/tools/Tool.hx", 12),
    // a directory-name prefix is not a match
    arguments(List.of("/p/src"), "/p/srcx/Main.hx", -1),
    arguments(List.of(), "/p/src/Main.hx", -1));

  @ParameterizedTest(name = "{1} in {0}")
  @FieldSource("SPECIFICITIES")
  public void testSpecificity(List<String> sourceDirectories, String path, int expected) {
    assertEquals(expected, HaxeBuildOwnership.specificity(sourceDirectories, path + "/"));
  }

  @Test
  @DisplayName("the closest owners are the ties for the longest directory in candidate order")
  public void testTheClosestOwnersAreTheTiesForTheLongestDirectoryInCandidateOrder() {
    assertEquals(List.of(APP, WEB), HaxeBuildOwnership.closest("/p/src/Shared.hx", CANDIDATES, Build::sourceDirectories));
    assertEquals(List.of(TOOLS), HaxeBuildOwnership.closest("/p/src/tools/Tool.hx", CANDIDATES, Build::sourceDirectories));
    assertEquals(List.of(), HaxeBuildOwnership.closest("/q/Elsewhere.hx", CANDIDATES, Build::sourceDirectories));
  }
}
