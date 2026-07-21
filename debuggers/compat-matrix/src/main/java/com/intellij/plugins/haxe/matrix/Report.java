package com.intellij.plugins.haxe.matrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Injects the run's results into report-template.html as a JSON payload
 * (replacing the {@code /*__DATA__*}{@code /null} placeholder) and writes
 * the self-contained index.html. The tiny JSON writer below keeps the tool
 * dependency-free; the payload shape is the contract with the template's JS.
 */
final class Report {
  private Report() {
  }

  static void write(Path template, Path out, List<Results.Cell> cells,
                    List<String> haxeNames, List<String> hlNames, Path resources, boolean full) throws IOException {
    StringBuilder json = new StringBuilder(1 << 16);
    json.append("{\"meta\":{");
    json.append("\"generated\":").append(quote(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
    json.append(",\"testData\":").append(quote(resources.toString()));
    json.append(",\"full\":").append(full);
    json.append(",\"haxeVersions\":").append(array(haxeNames));
    json.append(",\"hlRuntimes\":").append(array(hlNames));
    json.append("},\"cells\":[");
    for (int i = 0; i < cells.size(); i++) {
      Results.Cell cell = cells.get(i);
      if (i > 0) {
        json.append(',');
      }
      json.append("{\"lane\":").append(quote(cell.lane()));
      json.append(",\"haxe\":").append(quote(cell.haxe()));
      json.append(",\"runtime\":").append(cell.runtime() == null ? "null" : quote(cell.runtime()));
      json.append(",\"status\":").append(quote(cell.status()));
      json.append(",\"seconds\":").append(cell.seconds());
      json.append(",\"flaky\":").append(array(cell.flakyTests()));
      json.append(",\"classes\":[");
      List<Results.ClassResult> classes = cell.classes();
      for (int c = 0; c < classes.size(); c++) {
        Results.ClassResult cls = classes.get(c);
        if (c > 0) {
          json.append(',');
        }
        json.append("{\"name\":").append(quote(cls.name()));
        json.append(",\"tests\":").append(cls.tests());
        json.append(",\"failures\":").append(cls.failures());
        json.append(",\"errors\":").append(cls.errors());
        json.append(",\"skipped\":").append(cls.skipped());
        json.append(",\"failed\":[");
        List<Results.FailedTest> failed = cls.failed();
        for (int f = 0; f < failed.size(); f++) {
          if (f > 0) {
            json.append(',');
          }
          json.append("{\"test\":").append(quote(failed.get(f).test()));
          json.append(",\"message\":").append(quote(failed.get(f).message())).append('}');
        }
        json.append("]}");
      }
      json.append("]}");
    }
    json.append("]}");

    String html = Files.readString(template, StandardCharsets.UTF_8)
      // "</" would terminate the surrounding <script> mid-payload
      .replace("/*__DATA__*/null", json.toString().replace("</", "<\\/"));
    Files.createDirectories(out.getParent());
    Files.writeString(out, html, StandardCharsets.UTF_8);
  }

  private static String array(List<String> values) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(quote(values.get(i)));
    }
    return sb.append(']').toString();
  }

  private static String quote(String value) {
    StringBuilder sb = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int)c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.append('"').toString();
  }
}
