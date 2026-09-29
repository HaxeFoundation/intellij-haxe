package com.intellij.plugins.haxe.profiler.hxcpp;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads the text report {@code cpp.vm.Profiler.start/stop} writes (requires
 * a {@code -debug -D HXCPP_PROFILER} build). The file has no header — the
 * whole format is two printf shapes: an entry line, then optionally its
 * callee breakdown. A line matching neither fails with its line number.
 */
public final class HxcppProfileTranslator {

  // a report entry: `ProfMain.main 85.61%/14.39%` - symbol, then total%/self% with exactly two decimals
  private static final Pattern ENTRY = Pattern.compile("^(.*) (\\d+\\.\\d{2})%/(\\d+\\.\\d{2})%$");
  // a callee line: three-space indent, symbol, share of the caller with exactly one decimal - `   ProfMain.work 85.6%`
  private static final Pattern CALLEE = Pattern.compile("^ {3}(.*) (\\d+\\.\\d)%$");
  /** The caller's own code in the breakdown — already carried by the entry's selfPercent, so it is dropped. */
  private static final String INTERNAL_SYMBOL = "(internal)";

  private HxcppProfileTranslator() {
  }

  /** True when the line is a report entry — the sniff for a headerless format. */
  public static boolean looksLikeReportLine(@NotNull String line) {
    return ENTRY.matcher(line).matches();
  }

  @NotNull
  public static HxcppProfileReport translate(@NotNull InputStream in) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    List<HxcppProfileReport.Entry> entries = new ArrayList<>();

    String openSymbol = null;
    double openTotal = 0;
    double openSelf = 0;
    List<HxcppProfileReport.Callee> openCallees = new ArrayList<>();

    String line;
    int lineNumber = 0;
    while ((line = reader.readLine()) != null) {
      lineNumber++;
      if (line.isBlank()) continue;

      var entry = ENTRY.matcher(line);
      if (entry.matches()) {
        if (openSymbol != null) {
          entries.add(new HxcppProfileReport.Entry(openSymbol, openTotal, openSelf, List.copyOf(openCallees)));
        }
        openSymbol = entry.group(1);
        openTotal = Double.parseDouble(entry.group(2));
        openSelf = Double.parseDouble(entry.group(3));
        openCallees.clear();
        continue;
      }

      var callee = CALLEE.matcher(line);
      if (callee.matches() && openSymbol != null) {
        if (!INTERNAL_SYMBOL.equals(callee.group(1))) {
          openCallees.add(new HxcppProfileReport.Callee(callee.group(1), Double.parseDouble(callee.group(2))));
        }
        continue;
      }

      throw new ProfilerFormatException("not an hxcpp profiler report line (line " + lineNumber + "): " + line);
    }
    if (openSymbol != null) {
      entries.add(new HxcppProfileReport.Entry(openSymbol, openTotal, openSelf, List.copyOf(openCallees)));
    }
    return new HxcppProfileReport(List.copyOf(entries));
  }
}
