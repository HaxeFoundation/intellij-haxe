package com.intellij.plugins.haxe.profiler.hxcpp;

import com.intellij.plugins.haxe.profiler.hxcpp.HxcppProfileReport.Callee;
import com.intellij.plugins.haxe.profiler.hxcpp.HxcppProfileReport.Entry;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("hxcpp profiler: report translator")
public class HxcppProfileTranslatorTest {

  /** A real report, captured verbatim from a -debug -D HXCPP_PROFILER build. */
  private static final String CAPTURED_REPORT = """
    hxcpp.__hxcpp_main 100.00%/0.00%
       ProfMain.main 100.0%
       (internal) 0.0%
    ProfMain.main 85.61%/14.39%
       ProfMain.work 85.6%
       (internal) 14.4%
    ProfMain.work 0.00%/85.61%
    """;

  @Test
  @DisplayName("parses a captured report into entries with callee breakdowns")
  public void testParsesACapturedReportIntoEntriesWithCalleeBreakdowns() throws IOException {
    HxcppProfileReport report = HxcppProfileTranslator.translate(stream(CAPTURED_REPORT));

    assertEquals(3, report.entries().size());
    assertEquals(new Entry("hxcpp.__hxcpp_main", 100.0, 0.0, List.of(new Callee("ProfMain.main", 100.0))),
                 report.entries().get(0));
    assertEquals(new Entry("ProfMain.main", 85.61, 14.39, List.of(new Callee("ProfMain.work", 85.6))),
                 report.entries().get(1));
    assertEquals(new Entry("ProfMain.work", 0.0, 85.61, List.of()),
                 report.entries().get(2), "a leaf entry carries no breakdown - not even (internal)");
  }

  @Test
  @DisplayName("drops the internal pseudo callee but keeps real shares")
  public void testDropsTheInternalPseudoCalleeButKeepsRealShares() throws IOException {
    String text = """
      Game.update 40.00%/10.00%
         Game.physics 55.5%
         Game.animate 20.1%
         (internal) 24.4%
      """;

    HxcppProfileReport report = HxcppProfileTranslator.translate(stream(text));

    List<Callee> callees = report.entries().getFirst().callees();
    assertEquals(List.of(new Callee("Game.physics", 55.5), new Callee("Game.animate", 20.1)), callees);
  }

  @Test
  @DisplayName("a line matching neither shape fails with its line number")
  public void testALineMatchingNeitherShapeFailsWithItsLineNumber() {
    String text = """
      Game.update 40.00%/10.00%
      Warning - profiler has no effect without HXCPP_PROFILER
      """;

    ProfilerFormatException failure =
      assertThrows(ProfilerFormatException.class, () -> HxcppProfileTranslator.translate(stream(text)));
    assertTrue(failure.getMessage().contains("line 2"), failure.getMessage());
  }

  @Test
  @DisplayName("empty input is an empty report")
  public void testEmptyInputIsAnEmptyReport() throws IOException {
    assertEquals(List.of(), HxcppProfileTranslator.translate(stream("")).entries());
  }

  @Test
  @DisplayName("looks like report line sniffs entries and rejects everything else")
  public void testLooksLikeReportLineSniffsEntriesAndRejectsEverythingElse() {
    assertTrue(HxcppProfileTranslator.looksLikeReportLine("hxcpp.__hxcpp_main 100.00%/0.00%"));
    assertTrue(HxcppProfileTranslator.looksLikeReportLine("ProfMain.work 0.00%/85.61%"));

    assertTrue(!HxcppProfileTranslator.looksLikeReportLine("   ProfMain.main 100.0%"), "a callee line is no entry");
    assertTrue(!HxcppProfileTranslator.looksLikeReportLine("PROF binary header"), "the HL dump must not match");
  }

  private static InputStream stream(String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }
}
