package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Display protocol: haxe3 display client")
public class Haxe3DisplayClientTest {

  // "é" takes two bytes, so every byte offset after it on line 2 is one past its character offset
  private static final String MAIN_HX_SOURCE = """
    class Main {
      static var label = "é"; static var count:Int = 1;
      static function main() {
        var circle = new Circle();
        circle.ar;
      }
    }
    """;
  private static final String CIRCLE_HX_SOURCE = """
    class Circle {
      public function new() {}
      public function area():Float {
        return 1.0;
      }
    }
    """;

  @TempDir
  Path project;

  private RecordingChannel channel;
  private Haxe3DisplayClient client;
  private List<String> baseArgs;
  private String mainFile;
  private String circleFile;

  @BeforeEach
  void setUp() throws IOException {
    Path main = Files.writeString(project.resolve("Main.hx"), MAIN_HX_SOURCE);
    Path circle = Files.writeString(project.resolve("Circle.hx"), CIRCLE_HX_SOURCE);
    mainFile = main.toString();
    circleFile = circle.toString();
    baseArgs = List.of("--cwd", project.toString(), "-cp", ".", "-main", "Main", "-js", "out.js");
    channel = new RecordingChannel();
    client = new Haxe3DisplayClient(channel);
  }

  @Test
  @DisplayName("initialize reports the version answer and the haxe 3 methods")
  public void initializeReportsTheVersionAnswerAndTheHaxe3Methods() throws Exception {
    channel.answering("3.4.7\n");

    InitializeResult result = client.initialize(baseArgs);

    assertEquals(List.of("-version"), channel.requests.getFirst());
    assertEquals("3.4.7", result.haxeVersion().toString());
    assertEquals(Haxe3DisplayClient.METHODS, result.methods());
  }

  @Test
  @DisplayName("a memory report asks display memory")
  public void aMemoryReportAsksDisplayMemory() throws Exception {
    client.requestMemoryReport();

    assertEquals(List.of("--display", "memory"), channel.requests.getFirst());
  }

  @Test
  @DisplayName("a refused memory report throws")
  public void aRefusedMemoryReportThrows() {
    channel.failing("Error: unknown display mode");

    assertThrows(DisplayRequestException.class, () -> client.requestMemoryReport());
  }

  @Test
  @DisplayName("hover asks inside the identifier at its byte offset")
  public void hoverAsksInsideTheIdentifierAtItsByteOffset() throws Exception {
    int countOffset = MAIN_HX_SOURCE.indexOf("count");
    channel.answering("<type p=\"" + mainFile + ":2: characters 38-43\">\nInt\n</type>\n");

    HoverInfo hover = client.hover(baseArgs, mainFile, countOffset, null);

    // one byte for "é" and one character into the name
    assertEquals(mainFile + "@" + (countOffset + 2) + "@type", channel.lastDisplayArgument());
    assertEquals(new Range(new Position(1, 37), new Position(1, 42)), hover.range());
    assertEquals("Int", hover.type().dotPath());
  }

  @Test
  @DisplayName("an unsaved buffer travels last after the display stdin define")
  public void anUnsavedBufferTravelsLastAfterTheDisplayStdinDefine() throws Exception {
    String buffer = MAIN_HX_SOURCE.replace("circle.ar;", "circle.area();");
    channel.answering("[]");

    client.diagnostics(baseArgs, mainFile, buffer);

    List<String> request = channel.requests.getFirst();
    assertEquals(List.of("-D", "display-stdin"), request.subList(baseArgs.size(), baseArgs.size() + 2));
    assertEquals(mainFile + "@0@diagnostics", channel.lastDisplayArgument());
    assertEquals("\u0001" + buffer, request.getLast());
  }

  @Test
  @DisplayName("diagnostics columns become characters")
  public void diagnosticsColumnsBecomeCharacters() throws Exception {
    String escapedFile = mainFile.replace("\\", "\\\\");
    channel.answering("""
      [{"file":"%s","diagnostics":[{"kind":2,"severity":1,
        "range":{"start":{"line":1,"character":38},"end":{"line":1,"character":43}},"args":"unused"}]}]
      """.formatted(escapedFile));

    List<FileDiagnostics> results = client.diagnostics(baseArgs, mainFile, null);

    Range range = results.getFirst().diagnostics().getFirst().range();
    assertEquals(new Range(new Position(1, 37), new Position(1, 42)), range);
  }

  @Test
  @DisplayName("definition reads a declaration span from another file")
  public void definitionReadsADeclarationSpanFromAnotherFile() throws Exception {
    int areaOffset = MAIN_HX_SOURCE.indexOf("circle.ar") + "circle.".length();
    channel.answering("<list>\n<pos>" + circleFile + ":3: lines 3-5</pos>\n</list>\n");

    List<Location> locations = client.definition(baseArgs, mainFile, areaOffset, null);

    assertEquals(List.of(new Location(circleFile, new Range(new Position(2, 0), new Position(4, 3)))), locations);
  }

  @Test
  @DisplayName("nothing at the position reads as an empty answer")
  public void nothingAtThePositionReadsAsAnEmptyAnswer() throws Exception {
    channel.failing("Error: No completion point was found").failing("Error: No completion point was found");

    HoverInfo hover = client.hover(baseArgs, mainFile, 0, null);
    List<Location> locations = client.references(baseArgs, mainFile, 0, null, FindReferencesKind.DIRECT);

    assertNull(hover);
    assertTrue(locations.isEmpty());
  }

  @Test
  @DisplayName("a compiler error fails as a compiler answer")
  public void aCompilerErrorFailsAsACompilerAnswer() {
    channel.failing("Main.hx:5: characters 4-13 : Circle has no field ar");

    DisplayRequestException failure = assertThrows(DisplayRequestException.class, () -> client.definition(baseArgs, mainFile, 0, null));

    assertTrue(failure.isCompilerAnswered());
  }

  @Test
  @DisplayName("completion after a dot asks in field mode right after it")
  public void completionAfterADotAsksInFieldModeRightAfterIt() throws Exception {
    int caret = MAIN_HX_SOURCE.indexOf("circle.ar") + "circle.ar".length();
    int afterDot = caret - "ar".length();
    channel.answering("<list>\n<i n=\"area\" k=\"method\"><t>Void -&gt; Float</t><d></d></i>\n</list>\n");

    CompletionList list = client.completion(baseArgs, mainFile, caret, null, false);

    // the only non-ASCII character sits before the dot: its byte offset is one more
    assertEquals(mainFile + "@" + (afterDot + 1), channel.lastDisplayArgument());
    assertEquals("area", list.items().getFirst().name());
  }

  @Test
  @DisplayName("server introspection is not a haxe 3 request")
  public void serverIntrospectionIsNotAHaxe3Request() {
    DisplayRequestException failure = assertThrows(DisplayRequestException.class, () -> client.contexts(baseArgs));

    assertFalse(failure.isCompilerAnswered());
    assertTrue(channel.requests.isEmpty());
  }

  @Test
  @DisplayName("invalidate sends nothing")
  public void invalidateSendsNothing() {
    client.invalidate(baseArgs, mainFile);

    assertTrue(channel.requests.isEmpty());
  }
}
