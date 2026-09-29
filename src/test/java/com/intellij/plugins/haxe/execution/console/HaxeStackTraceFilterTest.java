package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Console: stack trace filter")
public class HaxeStackTraceFilterTest extends HaxeConsoleFilterTestBase {

  @BeforeEach
  public void addTraceSources() {
    myFixture.addFileToProject("openfl/filters/BlurFilter.hx", "class BlurFilter {}");
    myFixture.addFileToProject("BlurFilterTest.hx", "class BlurFilterTest {}");
    myFixture.addFileToProject("std/neko/_std/Array.hx", "class Array<T> {}");
    filter = new HaxeStackTraceFilter(getProject(), GlobalSearchScope.allScope(getProject()));
  }

  /** (trace line, file the link opens, 0-based line it opens at, highlighted span). */
  static final List<Arguments> FRAMES = List.of(
    arguments("Called from openfl/filters/BlurFilter.hx line 86", "BlurFilter.hx", 85, "openfl/filters/BlurFilter.hx line 86"),
    arguments("Called from BlurFilterTest.hx line 44", "BlurFilterTest.hx", 43, "BlurFilterTest.hx line 44"),
    // haxe.CallStack spelling, with and without the space HashLink drops
    arguments("Called from BlurFilterTest.main (BlurFilterTest.hx line 42)", "BlurFilterTest.hx", 41, "BlurFilterTest.hx line 42"),
    arguments("Called from BlurFilterTest.main(BlurFilterTest.hx:42)", "BlurFilterTest.hx", 41, "BlurFilterTest.hx:42"),
    arguments("Called from BlurFilterTest.~main.1 (BlurFilterTest.hx line 7)", "BlurFilterTest.hx", 6, "BlurFilterTest.hx line 7"),
    // a trace from another machine: the std path resolves by its trailing segments
    arguments("Called from /opt/hostedtoolcache/haxe/4.1.5/x64/std/neko/_std/Array.hx line 328", "Array.hx", 327,
              "/opt/hostedtoolcache/haxe/4.1.5/x64/std/neko/_std/Array.hx line 328"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("FRAMES")
  public void testLinksTheFrameToItsFile(String line, String fileName, int zeroBasedLine, String highlighted) {
    String console = "Uncaught exception - Invalid array access\n" + line + "\n";

    Filter.Result result = filter.applyFilter(line + "\n", console.length());

    assertNotNull(result, "frame must link");
    OpenFileDescriptor target = ((OpenFileHyperlinkInfo)result.getFirstHyperlinkInfo()).getDescriptor();
    assertEquals(fileName, target.getFile().getName());
    assertEquals(zeroBasedLine, target.getLine());
    assertEquals(highlighted, highlightedSpan(console, result));
  }

  @Test
  @DisplayName("frames without a source file do not link")
  public void testFramesWithoutASourceFileDoNotLink() {
    assertNull(applyToLine("Called from a C function"));
    assertNull(applyToLine("Called from unknown/Missing.hx line 3"));
  }
}
