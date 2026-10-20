package com.intellij.plugins.haxe.runner.debugger.hashlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

/**
 * Breakpoint file scoping: the HL debug tables carry classpath-relative file
 * names, so the backend must refuse breakpoint files outside the build's
 * source directories — a same-named sibling-project file would otherwise bind
 * into this debuggee.
 */
@DisplayName("Debugger: hashlink backend scope")
public class HashLinkBackendScopeTest {

  /** (source directories, breakpoint file, accepted). */
  static final List<Arguments> BREAKPOINT_FILES = List.of(
    // accepts files under a source directory
    arguments(List.of("C:/projects/buddy/hxml/src", "C:/projects/buddy/hxml/test"), "C:/projects/buddy/hxml/test/TestMain.hx", true),
    arguments(List.of("C:/projects/buddy/hxml/src", "C:/projects/buddy/hxml/test"), "C:/projects/buddy/hxml/src/sub/Calculator.hx", true),
    // rejects same named files from sibling projects
    arguments(List.of("C:/projects/buddy/hxml/src"), "C:/projects/munit/openfl/src/Calculator.hx", false),
    // a directory name prefix is not that directory
    arguments(List.of("C:/projects/buddy/hxml/src"), "C:/projects/buddy/hxml/src2/Calculator.hx", false),
    // without directories every file is accepted
    arguments(List.of(), "C:/anywhere/Calculator.hx", true));

  @ParameterizedTest(name = "{1} against {0}")
  @FieldSource("BREAKPOINT_FILES")
  @DisplayName("breakpoint file is accepted inside the source directories only")
  public void testBreakpointFileIsAcceptedInsideTheSourceDirectoriesOnly(List<String> sourceDirectories, String file, boolean accepted) {
    HashLinkBackend backend = new HashLinkBackend(Path.of("hl"), Path.of("tests.hl"), 6112, sourceDirectories);

    assertEquals(accepted, backend.acceptsBreakpointFile(file));
  }
}
