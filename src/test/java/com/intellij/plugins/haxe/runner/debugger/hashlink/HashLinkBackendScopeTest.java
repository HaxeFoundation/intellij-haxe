package com.intellij.plugins.haxe.runner.debugger.hashlink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Breakpoint file scoping: the HL debug tables carry classpath-relative file
 * names, so the backend must refuse breakpoint files outside the build's
 * source directories — a same-named sibling-project file would otherwise bind
 * into this debuggee.
 */
@DisplayName("Debugger: hashlink backend scope")
public class HashLinkBackendScopeTest {

  @Test
  @DisplayName("accepts files under a source directory")
  public void testAcceptsFilesUnderASourceDirectory() {
    HashLinkBackend backend = backend(List.of("C:/projects/buddy/hxml/src", "C:/projects/buddy/hxml/test"));
    assertTrue(backend.acceptsBreakpointFile("C:/projects/buddy/hxml/test/TestMain.hx"));
    assertTrue(backend.acceptsBreakpointFile("C:/projects/buddy/hxml/src/sub/Calculator.hx"));
  }

  @Test
  @DisplayName("rejects same named files from sibling projects")
  public void testRejectsSameNamedFilesFromSiblingProjects() {
    HashLinkBackend backend = backend(List.of("C:/projects/buddy/hxml/src"));
    assertFalse(backend.acceptsBreakpointFile("C:/projects/munit/openfl/src/Calculator.hx"));
  }

  @Test
  @DisplayName("a directory name prefix is not that directory")
  public void testADirectoryNamePrefixIsNotThatDirectory() {
    HashLinkBackend backend = backend(List.of("C:/projects/buddy/hxml/src"));
    assertFalse(backend.acceptsBreakpointFile("C:/projects/buddy/hxml/src2/Calculator.hx"));
  }

  @Test
  @DisplayName("without directories every file is accepted")
  public void testWithoutDirectoriesEveryFileIsAccepted() {
    HashLinkBackend backend = backend(List.of());
    assertTrue(backend.acceptsBreakpointFile("C:/anywhere/Calculator.hx"));
  }

  private static HashLinkBackend backend(List<String> sourceDirectories) {
    return new HashLinkBackend(Path.of("hl"), Path.of("tests.hl"), 6112, sourceDirectories);
  }
}
