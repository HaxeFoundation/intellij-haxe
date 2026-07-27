package com.intellij.plugins.haxe.matrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Injects the run's results into report-template.html as a JSON payload
 * (replacing the {@code /*__DATA__*}{@code /null} placeholder) and writes
 * the self-contained index.html.
 *
 * The tree is built field by field rather than serialized straight off the
 * records: this shape is the contract with the template's JS, so it is stated
 * here explicitly and does not drift when a record gains a field.
 */
final class Report {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private Report() {
  }

  static void write(Path template, Path out, List<Results.Cell> cells,
                    List<String> haxeNames, List<String> hlNames, Path resources, boolean full) throws IOException {
    ObjectNode payload = MAPPER.createObjectNode();

    ObjectNode meta = payload.putObject("meta");
    meta.put("generated", LocalDateTime.now().format(GENERATED));
    meta.put("testData", resources.toString());
    meta.put("full", full);
    putStrings(meta.putArray("haxeVersions"), haxeNames);
    putStrings(meta.putArray("hlRuntimes"), hlNames);

    ArrayNode cellNodes = payload.putArray("cells");
    for (Results.Cell cell : cells) {
      appendCell(cellNodes.addObject(), cell);
    }

    String html = Files.readString(template, StandardCharsets.UTF_8)
      // "</" would terminate the surrounding <script> mid-payload
      .replace("/*__DATA__*/null", MAPPER.writeValueAsString(payload).replace("</", "<\\/"));
    Files.createDirectories(out.getParent());
    Files.writeString(out, html, StandardCharsets.UTF_8);
  }

  private static void appendCell(ObjectNode node, Results.Cell cell) {
    node.put("lane", cell.lane());
    node.put("haxe", cell.haxe());
    node.put("runtime", cell.runtime());
    node.put("status", cell.status());
    node.put("seconds", cell.seconds());
    putStrings(node.putArray("flaky"), cell.flakyTests());

    ArrayNode classes = node.putArray("classes");
    for (Results.ClassResult cls : cell.classes()) {
      appendClass(classes.addObject(), cls);
    }
  }

  private static void appendClass(ObjectNode node, Results.ClassResult cls) {
    node.put("name", cls.name());
    node.put("tests", cls.tests());
    node.put("failures", cls.failures());
    node.put("errors", cls.errors());
    node.put("skipped", cls.skipped());

    ArrayNode failed = node.putArray("failed");
    for (Results.FailedTest test : cls.failed()) {
      ObjectNode entry = failed.addObject();
      entry.put("test", test.test());
      entry.put("message", test.message());
    }

    ArrayNode skipped = node.putArray("skippedTests");
    for (Results.SkippedTest test : cls.skippedTests()) {
      ObjectNode entry = skipped.addObject();
      entry.put("test", test.test());
      entry.put("message", test.message());
      entry.put("known", test.known());
    }
  }

  private static void putStrings(ArrayNode array, List<String> values) {
    for (String value : values) {
      array.add(value);
    }
  }
}
