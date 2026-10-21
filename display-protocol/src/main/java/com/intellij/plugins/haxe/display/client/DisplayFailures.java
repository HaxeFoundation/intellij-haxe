package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import java.util.List;

/** How a display answer without a result is classified and explained, the same for every display protocol. */
public final class DisplayFailures {

  private DisplayFailures() {
  }

  /**
   * The payload of a display answer. An answer without one fails: as a
   * compiler answer when it carried the error marker, as unexplained when it
   * carried no log line either.
   */
  public static String requirePayload(String method, DisplayResponse response) throws DisplayRequestException {
    if (!response.payload().isEmpty()) return response.payload();
    String message = "Display request '" + method + "' got no result: " + failureDetail(response);
    throw new DisplayRequestException(message, response.hasError(), isUnexplained(response));
  }

  /**
   * The compiler's explanation of a failure: the last few log lines when
   * there are any, otherwise the payload of a plain-text error report.
   */
  public static String failureDetail(DisplayResponse response) {
    List<String> lines = response.logs().stream()
      .filter(line -> !line.isBlank())
      .toList();
    if (lines.isEmpty()) {
      if (response.hasError() && !response.payload().isBlank()) {
        return "compiler error: " + response.payload();
      }
      return response.hasError() ? "compiler reported an error" : "empty response";
    }
    String tail = String.join(" | ", lines.subList(Math.max(0, lines.size() - 3), lines.size()));
    return (response.hasError() ? "compiler error: " : "") + tail;
  }

  /** Whether the response explains nothing: no error marker and no log line. */
  private static boolean isUnexplained(DisplayResponse response) {
    return !response.hasError() && response.logs().stream().allMatch(String::isBlank);
  }
}
