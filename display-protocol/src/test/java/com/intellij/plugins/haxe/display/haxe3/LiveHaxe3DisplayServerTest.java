package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.client.HaxeDisplayClient;
import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.transport.FramedDisplayChannel;
import com.intellij.plugins.haxe.display.transport.HaxeDisplayTransport;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the haxe 3 client against a real haxe 3 {@code --wait} server. The
 * suite is opt-in with {@code -PdisplayTests=true} and drives the haxe 3
 * executable {@code -PdisplayTestHaxe3} names; without one it skips itself.
 */
@DisplayName("Display protocol: live haxe3 display server (integration)")
public class LiveHaxe3DisplayServerTest {

  /** The haxe 3 executable to drive, from -PdisplayTestHaxe3; empty skips the suite. */
  private static final String HAXE3_EXE = System.getProperty("display.test.haxe3", "");

  private static final String MAIN_HX_NAME = "Main.hx";
  // "é" before the code on the label line makes every later byte column on it differ from its character column
  private static final String MAIN_HX_SOURCE = """
    import shapes.Circle;

    class Main {
      static var label = "é"; static var limit:Int = 3;

      static function main() {
        var circle = new Circle(2.0);
        var area = circle.area();
        trace(label + area + limit);
      }
    }
    """;
  private static final String CIRCLE_HX_NAME = "shapes/Circle.hx";
  private static final String CIRCLE_HX_SOURCE = """
    package shapes;

    class Circle {
      public var radius:Float;

      public function new(radius:Float) {
        this.radius = radius;
      }

      public function area():Float {
        return Math.PI * radius * radius;
      }
    }
    """;

  @TempDir
  static Path project;

  private static Process server;
  private static HaxeDisplayClient client;
  private static List<String> baseArgs;
  private static String mainFile;
  private static String circleFile;

  @BeforeAll
  static void startServer() throws Exception {
    boolean haxe3Named = !HAXE3_EXE.isEmpty() && new File(HAXE3_EXE).canExecute();
    assumeTrue(haxe3Named, "no haxe 3 (-PdisplayTestHaxe3) - skipping");
    mainFile = write(MAIN_HX_NAME, MAIN_HX_SOURCE);
    circleFile = write(CIRCLE_HX_NAME, CIRCLE_HX_SOURCE);
    int port;
    try (ServerSocket probe = new ServerSocket(0)) {
      port = probe.getLocalPort();
    }
    // stdin closed: a -D display-stdin request without its buffer would read it and hang the server
    server = new ProcessBuilder(HAXE3_EXE, "--wait", String.valueOf(port))
      .redirectInput(ProcessBuilder.Redirect.from(nullDevice()))
      .redirectErrorStream(true)
      .redirectOutput(ProcessBuilder.Redirect.DISCARD)
      .start();
    waitUntilAccepting(port);
    baseArgs = List.of("--cwd", project.toString(), "-cp", ".", "-main", "Main", "-js", "out.js", "--no-output");
    client = new Haxe3DisplayClient(HaxeDisplayTransport.socketPerRequest("127.0.0.1", port));
  }

  @AfterAll
  static void stopServer() {
    if (server != null) {
      server.destroy();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("initialize reports haxe 3")
  public void initializeReportsHaxe3() throws Exception {
    InitializeResult result = client.initialize(baseArgs);

    assertEquals(3, result.haxeVersion().major());
    assertTrue(result.supports(DisplayMethods.COMPLETION));
  }

  @Test
  @Timeout(60)
  @DisplayName("hover at a declaration name reports its range in characters")
  public void hoverAtADeclarationNameReportsItsRangeInCharacters() throws Exception {
    int limitOffset = MAIN_HX_SOURCE.indexOf("limit:");
    Position start = positionOf(MAIN_HX_SOURCE, limitOffset);

    HoverInfo hover = client.hover(baseArgs, mainFile, limitOffset, null);

    Position end = new Position(start.line(), start.character() + "limit".length());
    assertEquals(new Range(start, end), hover.range());
    assertEquals("Int", hover.type().dotPath());
  }

  @Test
  @Timeout(60)
  @DisplayName("completion after a dot lists the fields")
  public void completionAfterADotListsTheFields() throws Exception {
    String typing = MAIN_HX_SOURCE.replace("circle.area();", "circle.ra;");
    int caret = typing.indexOf("circle.ra") + "circle.ra".length();

    CompletionList list = client.completion(baseArgs, mainFile, caret, typing, false);

    List<String> names = list.items().stream().map(CompletionItem::name).toList();
    assertTrue(names.contains("radius"), "fields of Circle: " + names);
    assertTrue(names.contains("area"), "fields of Circle: " + names);
  }

  @Test
  @Timeout(60)
  @DisplayName("definition of a method spans its declaration in its file")
  public void definitionOfAMethodSpansItsDeclarationInItsFile() throws Exception {
    int areaCall = MAIN_HX_SOURCE.indexOf("circle.area") + "circle.".length();

    List<Location> locations = client.definition(baseArgs, mainFile, areaCall, null);

    Location location = locations.getFirst();
    int declarationLine = positionOf(CIRCLE_HX_SOURCE, CIRCLE_HX_SOURCE.indexOf("public function area")).line();
    assertTrue(Path.of(location.file()).endsWith(Path.of("shapes", "Circle.hx")), location.file());
    assertEquals(declarationLine, location.range().start().line());
  }

  @Test
  @Timeout(60)
  @DisplayName("references at a declaration list its usages")
  public void referencesAtADeclarationListItsUsages() throws Exception {
    int radiusDeclaration = CIRCLE_HX_SOURCE.indexOf("radius:Float;");

    List<Location> locations = client.references(baseArgs, circleFile, radiusDeclaration, null, FindReferencesKind.DIRECT);

    assertFalse(locations.isEmpty(), "radius is used in the constructor and in area()");
  }

  @Test
  @Timeout(60)
  @DisplayName("diagnostics follow the unsaved buffer in characters")
  public void diagnosticsFollowTheUnsavedBufferInCharacters() throws Exception {
    String broken = MAIN_HX_SOURCE.replace("limit:Int = 3;", "limit:Int = missing;");
    int missing = broken.indexOf("missing");
    Position start = positionOf(broken, missing);

    List<FileDiagnostics> results = client.diagnostics(baseArgs, mainFile, broken);

    Diagnostic unknown = results.stream()
      .flatMap(file -> file.diagnostics().stream())
      .filter(diagnostic -> diagnostic.kind() == DiagnosticKind.UNRESOLVED_IDENTIFIER)
      .findFirst()
      .orElseThrow();
    assertEquals(start, unknown.range().start());
  }

  @Test
  @Timeout(60)
  @DisplayName("a stdio process answers like the socket server")
  public void aStdioProcessAnswersLikeTheSocketServer() throws Exception {
    Process stdioServer = new ProcessBuilder(HAXE3_EXE, "--wait", "stdio")
      .redirectOutput(ProcessBuilder.Redirect.DISCARD)
      .start();
    FramedDisplayChannel channel =
      FramedDisplayChannel.overStdio(stdioServer.getErrorStream(), stdioServer.getOutputStream(), stdioServer::destroy, "haxe 3 stdio");
    HaxeDisplayClient stdioClient = new Haxe3DisplayClient(channel);
    String typing = MAIN_HX_SOURCE.replace("circle.area();", "circle.ra;");
    int caret = typing.indexOf("circle.ra") + "circle.ra".length();

    try {
      InitializeResult result = stdioClient.initialize(baseArgs);
      CompletionList list = stdioClient.completion(baseArgs, mainFile, caret, typing, false);

      List<String> names = list.items().stream().map(CompletionItem::name).toList();
      assertEquals(3, result.haxeVersion().major());
      assertTrue(names.contains("radius"), "fields of Circle: " + names);
    }
    finally {
      channel.close();
    }
  }

  private static String write(String relativePath, String source) throws IOException {
    Path file = project.resolve(relativePath);
    Files.createDirectories(file.getParent());
    return Files.writeString(file, source).toString();
  }

  /** The 0-based line and character of an offset in a text. */
  private static Position positionOf(String text, int offset) {
    String before = text.substring(0, offset);
    int line = (int) before.chars().filter(c -> c == '\n').count();
    return new Position(line, offset - (before.lastIndexOf('\n') + 1));
  }

  private static File nullDevice() {
    return new File(File.separatorChar == '\\' ? "NUL" : "/dev/null");
  }

  private static void waitUntilAccepting(int port) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      try (Socket ignored = new Socket("127.0.0.1", port)) {
        return;
      } catch (IOException e) {
        Thread.sleep(100);
      }
    }
  }
}
