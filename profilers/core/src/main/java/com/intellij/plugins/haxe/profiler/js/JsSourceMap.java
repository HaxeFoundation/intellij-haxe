package com.intellij.plugins.haxe.profiler.js;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A version-3 source map, queried by generated position — how the profiler
 * maps sampled positions in haxe's generated .js back to the .hx sources.
 * Only the position mappings are decoded (sources + line/column segments);
 * names and content embeds are ignored.
 */
public final class JsSourceMap {

  /** A generated-line's mappings: sorted generated columns with their source index and 0-based source line. */
  private record Entry(int generatedColumn, int sourceIndex, int sourceLine) {}

  /** A mapped source position; {@code line} is 1-based, matching the model's convention. */
  public record Position(@NotNull String file, int line) {}

  private final List<String> sourcePaths;
  private final List<List<Entry>> lines;

  private JsSourceMap(List<String> sourcePaths, List<List<Entry>> lines) {
    this.sourcePaths = sourcePaths;
    this.lines = lines;
  }

  /** Parses {@code mapFile}, resolving relative source paths against its directory. */
  @NotNull
  public static JsSourceMap parse(@NotNull Path mapFile) throws IOException {
    JsonNode map;
    try {
      map = new ObjectMapper().readTree(Files.readString(mapFile));
    }
    catch (JacksonException malformed) {
      throw new ProfilerFormatException("not a source map: " + malformed.getMessage());
    }
    if (map.path("version").asInt(0) != 3 || !map.path("mappings").isString()) {
      throw new ProfilerFormatException("not a version-3 source map: " + mapFile);
    }
    String sourceRoot = map.path("sourceRoot").asString("");
    List<String> sourcePaths = new ArrayList<>();
    for (JsonNode source : map.path("sources")) {
      sourcePaths.add(resolveSource(mapFile, sourceRoot, source.asString("")));
    }
    return new JsSourceMap(sourcePaths, decodeMappings(map.path("mappings").asString("")));
  }

  /**
   * The source position at generated {@code line}/{@code column} (both
   * 0-based): the closest mapping at or before the column on that line, or
   * null where the line carries no mappings.
   */
  @Nullable
  public Position resolve(int line, int column) {
    if (line < 0 || line >= lines.size()) return null;
    List<Entry> entries = lines.get(line);
    Entry best = null;
    for (Entry entry : entries) {
      if (entry.generatedColumn() > column) break;
      best = entry;
    }
    if (best == null && !entries.isEmpty()) {
      // a sample's column may point before the line's first mapping (the
      // indentation); the line's first mapped position is still the answer
      best = entries.get(0);
    }
    if (best == null || best.sourceIndex() < 0 || best.sourceIndex() >= sourcePaths.size()) return null;
    return new Position(sourcePaths.get(best.sourceIndex()), best.sourceLine() + 1);
  }

  /**
   * Haxe writes sources as {@code file:///} URLs or paths relative to the
   * map; both normalize to a plain forward-slash path.
   */
  private static String resolveSource(Path mapFile, String sourceRoot, String source) {
    String combined = sourceRoot.isEmpty() ? source : sourceRoot + (sourceRoot.endsWith("/") ? "" : "/") + source;
    String fromUrl = pathOfFileUrl(combined);
    if (fromUrl != null) return fromUrl;
    Path parent = mapFile.getParent();
    if (parent == null) return combined;
    return parent.resolve(combined)
      .normalize()
      .toString()
      .replace('\\', '/');
  }

  /**
   * The plain path of a {@code file://} URL; null for anything else.
   * {@code file:///C:/x} arrives with a slash before the drive letter, which
   * is dropped.
   */
  @Nullable
  public static String pathOfFileUrl(@NotNull String url) {
    if (!url.startsWith("file://")) return null;
    String path = url.substring("file://".length());
    boolean slashBeforeDrive = path.length() > 2 && path.charAt(0) == '/' && path.charAt(2) == ':';
    return slashBeforeDrive ? path.substring(1) : path;
  }

  /**
   * The {@code mappings} field: generated lines separated by {@code ;},
   * segments by {@code ,}; each segment is 1, 4 or 5 base64-VLQ deltas
   * [generatedColumn, sourceIndex, sourceLine, sourceColumn, nameIndex].
   * The column delta resets per line, the source deltas carry across lines.
   */
  private static List<List<Entry>> decodeMappings(String mappings) throws IOException {
    List<List<Entry>> lines = new ArrayList<>();
    Vlq vlq = new Vlq(mappings);
    int sourceIndex = 0;
    int sourceLine = 0;

    List<Entry> line = new ArrayList<>();
    int generatedColumn = 0;
    while (true) {
      int separator = vlq.separator();
      if (separator == ';' || separator == -1) {
        lines.add(List.copyOf(line));
        if (separator == -1) break;
        line = new ArrayList<>();
        generatedColumn = 0;
        continue;
      }
      generatedColumn += vlq.next();
      if (vlq.inSegment()) {
        sourceIndex += vlq.next();
        sourceLine += vlq.next();
        vlq.next(); // source column, not kept
        if (vlq.inSegment()) {
          vlq.next(); // name index, not kept
        }
        line.add(new Entry(generatedColumn, sourceIndex, sourceLine));
      }
    }
    return lines;
  }

  /** Base64-VLQ cursor over the mappings text: 6-bit digits, bit 5 continues, bit 0 of the first digit is the sign. */
  private static final class Vlq {
    private final String text;
    private int position;

    Vlq(String text) {
      this.text = text;
    }

    /** Consumes {@code ,}; returns {@code ;} (consumed) at a line break, -1 at the end, 0 inside a segment. */
    int separator() {
      if (position >= text.length()) return -1;
      char c = text.charAt(position);
      if (c == ';' || c == ',') {
        position++;
        return c == ';' ? ';' : separator();
      }
      return 0;
    }

    /** More fields before the segment ends? */
    boolean inSegment() {
      if (position >= text.length()) return false;
      char c = text.charAt(position);
      return c != ',' && c != ';';
    }

    int next() throws IOException {
      int shift = 0;
      int value = 0;
      while (true) {
        if (position >= text.length()) throw new ProfilerFormatException("truncated VLQ in source map mappings");
        int digit = base64(text.charAt(position++));
        value |= (digit & 0x1F) << shift;
        if ((digit & 0x20) == 0) break;
        shift += 5;
      }
      int magnitude = value >>> 1;
      return (value & 1) != 0 ? -magnitude : magnitude;
    }

    private static int base64(char c) throws IOException {
      if (c >= 'A' && c <= 'Z') return c - 'A';
      if (c >= 'a' && c <= 'z') return c - 'a' + 26;
      if (c >= '0' && c <= '9') return c - '0' + 52;
      if (c == '+') return 62;
      if (c == '/') return 63;
      throw new ProfilerFormatException("invalid base64 digit '" + c + "' in source map mappings");
    }
  }
}
