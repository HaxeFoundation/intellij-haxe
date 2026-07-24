package com.intellij.plugins.haxe.matrix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** JUnit-XML parsing and the per-cell result model the report is built from. */
final class Results {

  record FailedTest(String test, String message) {
  }

  /**
   * {@code known} = the test flagged itself as a deliberate version/OS
   * constraint (its Assume message starts with "known limitation:", stripped
   * here). Everything else is a missing prerequisite of THIS pass — a runtime
   * not provisioned, a fixture not built — and must not be presented as a
   * limitation.
   */
  record SkippedTest(String test, String message, boolean known) {
  }

  record ClassResult(String name, String fqName, int tests, int failures, int errors, int skipped,
                     List<FailedTest> failed, List<SkippedTest> skippedTests) {
  }

  record Cell(String lane, String haxe, String runtime, String status, List<ClassResult> classes,
              List<String> flakyTests, long seconds) {
    int totalFailures() {
      return classes.stream().mapToInt(c -> c.failures() + c.errors()).sum();
    }

    int totalSkipped() {
      return classes.stream().mapToInt(ClassResult::skipped).sum();
    }
  }

  /** Assume-message marker for deliberate version/OS constraint skips. */
  static final String KNOWN_LIMITATION_PREFIX = "known limitation:";

  private Results() {
  }

  /** Parses every junit XML in {@code dir} into per-class summaries. */
  static List<ClassResult> parse(Path dir) {
    List<ClassResult> classes = new ArrayList<>();
    if (!Files.isDirectory(dir)) {
      return classes;
    }
    try (var files = Files.list(dir)) {
      for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".xml")).sorted().toList()) {
        ClassResult suite = parseOne(file);
        if (suite != null) {
          classes.add(suite);
        }
      }
    } catch (IOException ignored) {
    }
    return classes;
  }

  /** One junit XML, or null when unparsable (mid-write, or not a result file). */
  private static ClassResult parseOne(Path file) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      Document doc = factory.newDocumentBuilder().parse(file.toFile());
      Element suite = doc.getDocumentElement();
      List<FailedTest> failed = new ArrayList<>();
      List<SkippedTest> skippedTests = new ArrayList<>();
      NodeList cases = suite.getElementsByTagName("testcase");
      for (int i = 0; i < cases.getLength(); i++) {
        Element testcase = (Element)cases.item(i);
        NodeList failures = testcase.getElementsByTagName("failure");
        if (failures.getLength() > 0) {
          failed.add(new FailedTest(testcase.getAttribute("name"),
                                    ((Element)failures.item(0)).getAttribute("message")));
        }
        NodeList skips = testcase.getElementsByTagName("skipped");
        if (skips.getLength() > 0) {
          String reason = skipReason(((Element)skips.item(0)).getAttribute("message"));
          boolean known = reason.startsWith(KNOWN_LIMITATION_PREFIX);
          if (known) {
            reason = reason.substring(KNOWN_LIMITATION_PREFIX.length()).trim();
          }
          skippedTests.add(new SkippedTest(testcase.getAttribute("name"), reason, known));
        }
      }
      String name = suite.getAttribute("name");
      return new ClassResult(
        name.substring(name.lastIndexOf('.') + 1), name,
        intAttr(suite, "tests"), intAttr(suite, "failures"),
        intAttr(suite, "errors"), intAttr(suite, "skipped"), failed, skippedTests);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Copies a module's junit XMLs into the report's evidence dir and parses
   * them. A non-null {@code classNameFilter} keeps only XMLs whose filename
   * contains it — for lanes that share a module's results dir and must not
   * pick up another lane's suites.
   */
  static List<ClassResult> collect(Path moduleResults, Path evidenceDir, String classNameFilter) throws IOException {
    if (Files.exists(evidenceDir)) {
      try (var walk = Files.walk(evidenceDir)) {
        for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
          Files.deleteIfExists(p);
        }
      }
    }
    Files.createDirectories(evidenceDir);
    if (Files.isDirectory(moduleResults)) {
      try (var files = Files.list(moduleResults)) {
        for (Path file : files.filter(f -> isResultXml(f, classNameFilter)).toList()) {
          Files.copy(file, evidenceDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
    return parse(evidenceDir);
  }

  static boolean isResultXml(Path file, String classNameFilter) {
    String name = file.getFileName().toString();
    return name.endsWith(".xml") && (classNameFilter == null || name.contains(classNameFilter));
  }

  // An Assume skip's message is "org.junit.AssumptionViolatedException: <reason>";
  // only the reason is worth showing.
  private static String skipReason(String message) {
    return message == null ? "" : message.replaceFirst("^[A-Za-z0-9_.$]+(?:Exception|Error): ", "");
  }

  private static int intAttr(Element element, String name) {
    try {
      return Integer.parseInt(element.getAttribute(name));
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
