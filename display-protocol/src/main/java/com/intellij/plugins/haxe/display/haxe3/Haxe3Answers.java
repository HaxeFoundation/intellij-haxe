package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.client.DisplayJson;
import com.intellij.plugins.haxe.display.haxe3.Haxe3Wire.TypeEntry;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.FileDiagnostics;
import com.intellij.plugins.haxe.display.protocol.HoverInfo;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import java.util.ArrayList;
import java.util.List;

/**
 * The haxe 3 answers that are not completion lists, decoded into the client
 * API's types with their positions in characters:
 * <ul>
 *   <li>{@code @type}: {@code <type p="<position>" d="doc">Type</type>}, the
 *   position of the expression or declaration name asked about; one root per
 *   overload.</li>
 *   <li>{@code @position} and {@code @usage}: {@code <list><pos>position</pos>...</list>}.
 *   A declaration's position spans the whole declaration, often as
 *   {@code lines a-b}; a local's names just the local.</li>
 *   <li>{@code @diagnostics}: the JSON array haxe 4 sends too, with 0-based
 *   lines and byte columns.</li>
 * </ul>
 */
final class Haxe3Answers {

  private Haxe3Answers() {
  }

  /** The hover of a {@code @type} answer about the request's file; null when it names no type. */
  static HoverInfo hover(String payload, Haxe3SourceText text) throws MalformedPayloadException {
    List<TypeEntry> types = Haxe3Xml.read(payload).types();
    if (types.isEmpty()) return null;
    TypeEntry type = types.getFirst();
    Haxe3Position position = Haxe3Position.parse(type.position());
    Range range = position == null ? null : position.rangeIn(text);
    return new HoverInfo(range, null, Haxe3Types.parse(type.type()));
  }

  /** The locations a {@code @position} or {@code @usage} answer lists. */
  static List<Location> locations(String payload, Haxe3SourceTexts texts) throws MalformedPayloadException {
    List<Location> locations = new ArrayList<>();
    for (String positionText : Haxe3Xml.read(payload).positions()) {
      Haxe3Position position = Haxe3Position.parse(positionText);
      if (position == null) continue;
      Range range = position.rangeIn(texts.textOf(position.file()));
      locations.add(new Location(position.file(), range));
    }
    return List.copyOf(locations);
  }

  /** The diagnostics of a {@code @diagnostics} answer, their byte columns turned into characters. */
  static List<FileDiagnostics> diagnostics(String payload, Haxe3SourceTexts texts) throws DisplayRequestException {
    return DisplayJson.decodeDiagnostics(DisplayJson.parse(payload)).stream()
      .map(file -> inCharacters(file, texts.textOf(file.file())))
      .toList();
  }

  private static FileDiagnostics inCharacters(FileDiagnostics file, Haxe3SourceText text) {
    if (text == null) return file;
    List<Diagnostic> diagnostics = file.diagnostics().stream()
      .map(diagnostic -> inCharacters(diagnostic, text))
      .toList();
    return new FileDiagnostics(file.file(), diagnostics);
  }

  private static Diagnostic inCharacters(Diagnostic diagnostic, Haxe3SourceText text) {
    Range bytes = diagnostic.range();
    Position start = text.position(bytes.start().line(), bytes.start().character());
    Position end = text.position(bytes.end().line(), bytes.end().character());
    return new Diagnostic(diagnostic.kind(), new Range(start, end), diagnostic.severity(), diagnostic.args(), diagnostic.code());
  }
}
