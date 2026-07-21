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

  record ClassResult(String name, String fqName, int tests, int failures, int errors, int skipped,
                     List<FailedTest> failed) {
  }

  record Cell(String lane, String haxe, String runtime, String status, List<ClassResult> classes,
              List<String> flakyTests, long seconds) {
    int totalFailures() {
      return classes.stream().mapToInt(c -> c.failures() + c.errors()).sum();
    }
  }

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
      NodeList cases = suite.getElementsByTagName("testcase");
      for (int i = 0; i < cases.getLength(); i++) {
        Element testcase = (Element)cases.item(i);
        NodeList failures = testcase.getElementsByTagName("failure");
        if (failures.getLength() > 0) {
          failed.add(new FailedTest(testcase.getAttribute("name"),
                                    ((Element)failures.item(0)).getAttribute("message")));
        }
      }
      String name = suite.getAttribute("name");
      return new ClassResult(
        name.substring(name.lastIndexOf('.') + 1), name,
        intAttr(suite, "tests"), intAttr(suite, "failures"),
        intAttr(suite, "errors"), intAttr(suite, "skipped"), failed);
    } catch (Exception e) {
      return null;
    }
  }

  /** Copies a module's junit XMLs into the report's evidence dir and parses them. */
  static List<ClassResult> collect(Path moduleResults, Path evidenceDir) throws IOException {
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
        for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".xml")).toList()) {
          Files.copy(file, evidenceDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
    return parse(evidenceDir);
  }

  private static int intAttr(Element element, String name) {
    try {
      return Integer.parseInt(element.getAttribute(name));
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
