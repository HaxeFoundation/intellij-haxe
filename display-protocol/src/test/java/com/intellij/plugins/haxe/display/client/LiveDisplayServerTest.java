package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.HaxeServerContext;
import com.intellij.plugins.haxe.display.protocol.server.ModuleInfo;
import com.intellij.plugins.haxe.display.protocol.server.TypeBlueprint;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import com.intellij.plugins.haxe.display.transport.HaxeDisplayTransport;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the client against a real {@code haxe --wait} server. The suite is
 * opt-in with {@code -PdisplayTests=true}. It drives the haxe on the PATH, or
 * the one {@code -PdisplayTestHaxe} names, and skips itself when that haxe
 * cannot run.
 */
@DisplayName("Display protocol: live server (integration)")
public class LiveDisplayServerTest {

  private static final String FIXTURE = """
    class Live {
    	public var label:String = "hi";
    	public function new() {}
    	public function shout():String return label.toUpperCase();
    	static function main() trace(new Live().shout());
    }
    """;

  /// The compiler to drive: the haxe on the PATH, or another binary named by
  /// -PdisplayTestHaxe (for example a haxe 5 preview).
  private static final String HAXE_EXE = System.getProperty("display.test.haxe", "haxe");

  @TempDir
  static Path workDir;

  private static Process server;
  private static int port;
  private static InitializeResult.SemVer serverVersion;
  private static HaxeDisplayClient client;
  private static List<String> baseArgs;
  private static String fixtureFile;

  @BeforeAll
  static void startServer() throws Exception {
    assumeTrue(haxeAvailable(), "haxe (" + HAXE_EXE + ") not runnable - skipping live display test");
    try (ServerSocket probe = new ServerSocket(0)) {
      port = probe.getLocalPort();
    }
    server = new ProcessBuilder(HAXE_EXE, "--wait", String.valueOf(port))
      .redirectErrorStream(true)
      .redirectOutput(ProcessBuilder.Redirect.DISCARD)
      .start();
    Path fixture = workDir.resolve("Live.hx");
    Files.writeString(fixture, FIXTURE);
    fixtureFile = fixture.toString();
    baseArgs = List.of("--cwd", workDir.toString(), "-cp", ".", "-main", "Live", "-js", "out.js", "--no-output");
    client = new HaxeDisplayClient("127.0.0.1", port);
    waitUntilAccepting();
    serverVersion = client.initialize(baseArgs).haxeVersion();
    System.out.println("[live] driving haxe " + serverVersion);
  }

  @AfterAll
  static void stopServer() {
    if (server != null) {
      server.destroy();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("initialize reports version and methods")
  public void initializeReportsVersionAndMethods() throws Exception {
    InitializeResult result = client.initialize(baseArgs);
    assertTrue(result.haxeVersion().major() >= 4);
    assertTrue(result.supports(DisplayMethods.HOVER));
  }

  @Test
  @Timeout(60)
  @DisplayName("diagnostics follow unsaved contents after an invalidate")
  public void diagnosticsFollowUnsavedContentsAfterAnInvalidate() throws Exception {
    List<FileDiagnostics> onDisk = client.diagnostics(baseArgs, fixtureFile, null);
    boolean cleanOnDisk = onDisk.isEmpty() || onDisk.get(0).diagnostics().isEmpty();
    assertTrue(cleanOnDisk, "fixture should have no diagnostics on disk");

    // Once the server has cached a module, it IGNORES `contents` and serves
    // the cached result, because the file's mtime is unchanged. The file
    // must be invalidated whenever the buffer diverges from disk.
    client.invalidate(baseArgs, fixtureFile);

    String broken = FIXTURE.replace("label.toUpperCase()", "labell.toUpperCase()");
    List<FileDiagnostics> withContents = client.diagnostics(baseArgs, fixtureFile, broken);
    boolean flaggedUnresolved = withContents.stream()
      .flatMap(file -> file.diagnostics().stream())
      .anyMatch(diagnostic -> diagnostic.kind() == DiagnosticKind.UNRESOLVED_IDENTIFIER);
    assertTrue(flaggedUnresolved, "unsaved contents must drive the diagnostics");
  }

  @Test
  @Timeout(60)
  @DisplayName("deprecation warning identification per compiler generation")
  public void deprecationWarningIdentificationPerCompilerGeneration() throws Exception {
    String withOldEnumAbstract = FIXTURE
      .replace("static function main() trace(new Live().shout());",
               "static function main() { trace(new Live().shout()); trace(Old.A); }")
      + """

      @:enum abstract Old(Int) {
      	var A = 1;
      }
      """;
    client.invalidate(baseArgs, fixtureFile);
    List<FileDiagnostics> results = client.diagnostics(baseArgs, fixtureFile, withOldEnumAbstract);

    Diagnostic deprecation = results.stream()
      .flatMap(file -> file.diagnostics().stream())
      .filter(diagnostic -> diagnostic.messageArg().contains("deprecated"))
      .findFirst()
      .orElse(null);
    assertNotNull(deprecation, "@:enum abstract must surface a deprecation warning");

    // Haxe 4.x identifies a warning only by its message. Haxe 5+ fills `code`
    // with the SPECIFIC -w warning identifier: WDeprecatedEnumAbstract here,
    // not just the WDeprecated class.
    if (sendsDiagnosticCodes()) {
      assertNotNull(deprecation.code(), "haxe 5+ identifies warnings by code");
      assertTrue(deprecation.code().startsWith("WDeprecated"),
                 "deprecation codes share the WDeprecated prefix, got " + deprecation.code());
    } else {
      assertNull(deprecation.code(), "haxe 4.x sends no code ids");
    }

    // Without codes, a whole warning CLASS can still be filtered: -w
    // -WDeprecated in the request arguments suppresses it.
    List<String> suppressed = new ArrayList<>(baseArgs);
    suppressed.add("-w");
    suppressed.add("-WDeprecated");
    client.invalidate(suppressed, fixtureFile);
    List<FileDiagnostics> filtered = client.diagnostics(suppressed, fixtureFile, withOldEnumAbstract);
    boolean stillWarned = filtered.stream()
      .flatMap(file -> file.diagnostics().stream())
      .anyMatch(diagnostic -> diagnostic.messageArg().contains("deprecated"));
    assertFalse(stillWarned, "-w -WDeprecated must suppress the deprecation warning class");
  }

  @Test
  @Timeout(60)
  @DisplayName("removable code range for an unused local keeps the initializer")
  public void removableCodeRangeForAnUnusedLocalKeepsTheInitializer() throws Exception {
    String withUnusedVar = FIXTURE.replace(
      "static function main() trace(new Live().shout());",
      """
      static function main() {
      		var dummy:Int = 0;
      		trace(new Live().shout());
      	}""");
    client.invalidate(baseArgs, fixtureFile);
    List<FileDiagnostics> results = client.diagnostics(baseArgs, fixtureFile, withUnusedVar);

    Diagnostic removable = results.stream()
      .flatMap(file -> file.diagnostics().stream())
      .filter(diagnostic -> diagnostic.kind() == DiagnosticKind.REMOVABLE_CODE)
      .findFirst()
      .orElse(null);
    assertNotNull(removable, "the unused local must surface as REMOVABLE_CODE");

    // The remove quick fix relies on this: the removal span in the args
    // covers the BINDING ("var dummy:Int = ") and KEEPS the initializer, so
    // `var x = sideEffect();` does not lose the call.
    Range removal = removable.removableRangeArg();
    assertNotNull(removal, "removable-code args must carry the removal range");
    if (sendsReplaceableCode()) {
      System.out.println("[live] haxe5 replaceable newCode = " + removable.args().path("newCode"));
      return;
    }
    List<String> lines = withUnusedVar.lines().toList();
    int varLineIndex = IntStream.range(0, lines.size())
      .filter(index -> lines.get(index).contains("var dummy:Int = 0;"))
      .findFirst()
      .orElseThrow();
    int initializerColumn = lines.get(varLineIndex).indexOf("0;");
    boolean initializerKept = removal.end().line() == varLineIndex && removal.end().character() <= initializerColumn;
    assertTrue(initializerKept, "expected the removal range to end before the initializer, got " + removal);
  }

  @Test
  @Timeout(60)
  @DisplayName("hover definition and references resolve the fixture")
  public void hoverDefinitionAndReferencesResolveTheFixture() throws Exception {
    int labelUsage = FIXTURE.indexOf("label.toUpperCase") + 2;

    HoverInfo hover = client.hover(baseArgs, fixtureFile, labelUsage, null);
    assertNotNull(hover);
    assertEquals("String", hover.type().dotPath());

    List<Location> definitions = client.definition(baseArgs, fixtureFile, labelUsage, null);
    assertEquals(1, definitions.size());
    // declaration is on fixture line 2; wire ranges are 0-based
    assertEquals(1, definitions.get(0).range().start().line());

    int shoutDecl = FIXTURE.indexOf("function shout") + "function s".length();
    List<Location> references =
      client.references(baseArgs, fixtureFile, shoutDecl, null, FindReferencesKind.DIRECT);
    assertFalse(references.isEmpty(), "the call in main() must be found");
  }

  /**
   * The compiler completes after a dot and at the end of a PARTIAL
   * identifier, which is what an editor sends along with its buffer as
   * contents. It refuses a request placed on an identifier that already
   * resolves as "Unsupported method".
   */
  @Test
  @DisplayName("completion answers fields after a dot and keywords at a partial identifier")
  public void completionAnswersFieldsAfterADotAndKeywordsAtAPartialIdentifier() throws Exception {
    int afterDot = FIXTURE.indexOf("label.toUpperCase") + "label.".length();
    CompletionList fields = client.completion(baseArgs, fixtureFile, afterDot, null, false);
    List<String> fieldNames = fields.items().stream().map(CompletionItem::name).toList();
    assertEquals(0, fields.modeKind(), "field mode after the dot");
    assertTrue(fieldNames.contains("toUpperCase"), "String members must be offered: " + fieldNames);

    String partial = FIXTURE.replace("static function main() trace(new Live().shout());", "static function main() {\n\t\ttr\n\t}");
    int partialEnd = partial.indexOf("\t\ttr") + "\t\ttr".length();
    client.invalidate(baseArgs, fixtureFile);
    CompletionList toplevel = client.completion(baseArgs, fixtureFile, partialEnd, partial, true);
    List<String> keywords = toplevel.items().stream().filter(CompletionItem::isKeywordOrLiteral).map(CompletionItem::name).toList();
    List<String> names = toplevel.items().stream().map(CompletionItem::name).toList();
    assertEquals(2, toplevel.modeKind(), "toplevel mode at a statement");
    assertTrue(keywords.contains("var"), "statement keywords must be offered: " + keywords);
    assertTrue(names.contains("trace"), "the toplevel list must include trace: " + names.size() + " items");
    // the range covers the typed prefix `tr` on the (0-based) fixture line 5
    assertEquals(new Range(new Position(5, 2), new Position(5, 4)), toplevel.replaceRange());
  }

  @Test
  @Timeout(60)
  @DisplayName("type blueprint hydrates after a compile through the server")
  public void typeBlueprintHydratesAfterACompileThroughTheServer() throws Exception {
    // populate the module cache: an actual compile with the same args
    HaxeDisplayTransport.request("127.0.0.1", port, baseArgs, 30_000);

    List<HaxeServerContext> contexts = client.contexts(baseArgs);
    HaxeServerContext modulesContext = contexts.stream()
      .filter(context -> contextHasModule(baseArgs, context, "Live"))
      .findFirst()
      .orElse(null);
    assertNotNull(modulesContext, "a context holding the compiled module must exist");

    TypeBlueprint blueprint = client.typeBlueprint(baseArgs, modulesContext.signature(), "Live", "Live");
    assertEquals("class", blueprint.kind());
    assertNotNull(blueprint.findMember("label"), "members must be listed by name");
    assertNotNull(blueprint.findMember("shout"), "members must be listed by name");
    if (blueprintTypesResolved()) {
      assertEquals("String", blueprint.findMember("label").type().dotPath());
      assertEquals("() -> String", blueprint.findMember("shout").type().presentable());
    }
    else {
      System.out.println("[live] unresolved blueprint member type kinds: label="
                         + blueprint.findMember("label").type().kind()
                         + " shout=" + blueprint.findMember("shout").type().presentable());
    }
  }

  // A type defined by a macro. It exists in NO source file, only in the
  // compiler's typed program after macros ran; the IDE's type catalog serves
  // this case. defineType runs inside onAfterInitMacros because haxe 5 forbids
  // it directly in an initialization macro; the deferred form works on 4.2+ too.
  private static final String GEN_MACRO = """
    import haxe.macro.Context;
    class GenMacro {
    	public static function define() {
    		Context.onAfterInitMacros(() -> Context.defineType({
    			pack: ["gen"],
    			name: "GeneratedThing",
    			pos: Context.currentPos(),
    			kind: TDClass(),
    			fields: [{
    				name: "tag",
    				access: [APublic],
    				kind: FVar(macro :String, macro "gen"),
    				pos: Context.currentPos()
    			}, {
    				name: "make",
    				access: [APublic, AStatic],
    				kind: FFun({args: [], ret: macro :String, expr: macro return "made"}),
    				pos: Context.currentPos()
    			}]
    		}));
    	}
    }
    """;
  private static final String GEN_MAIN = """
    class LiveGen {
    	static function main() trace(gen.GeneratedThing.make());
    }
    """;

  @Test
  @Timeout(90)
  @DisplayName("macro defined type appears in modules and blueprints after a compile")
  public void macroDefinedTypeAppearsInModulesAndBlueprintsAfterACompile() throws Exception {
    Files.writeString(workDir.resolve("GenMacro.hx"), GEN_MACRO);
    Files.writeString(workDir.resolve("LiveGen.hx"), GEN_MAIN);
    List<String> genArgs = List.of("--cwd", workDir.toString(), "-cp", ".", "-main", "LiveGen",
                                   "--macro", "GenMacro.define()", "-js", "gen.js", "--no-output");

    // the module cache stays EMPTY until a real compile, which is why the IDE
    // runs a warm-up compile per context
    assertNull(typedContextHolding(genArgs, "LiveGen"), "no module cache before a compile");

    DisplayResponse compiled = HaxeDisplayTransport.request("127.0.0.1", port, genArgs, 30_000);
    assertFalse(compiled.hasError(), "fixture compile must succeed: " + compiled.payload());

    HaxeServerContext context = typedContextHolding(genArgs, "LiveGen");
    assertNotNull(context, "the typed context must list the compiled module");
    assertTrue(context.holdsTypedModules(), "the IDE filters typed contexts by their desc");

    // server/modules does NOT list a module that defineType created, and haxe 4
    // serves no ModuleInfo for it. It appears only in the dependency lists of
    // the modules using it, which is where the IDE's type catalog finds it.
    List<String> listed = client.modules(genArgs, context.signature());
    assertFalse(listed.contains("gen.GeneratedThing"), "server/modules must not list the defined module");
    ModuleInfo userInfo = client.module(genArgs, context.signature(), "LiveGen");
    assertTrue(userInfo.dependencies().contains("gen.GeneratedThing"),
               "the using module's dependencies expose the defined module");
    assertFalse(userInfo.sign().isEmpty(), "sign drives the catalog's incremental refresh");
    if (servesDefinedModuleInfo()) {
      ModuleInfo definedInfo = client.module(genArgs, context.signature(), "gen.GeneratedThing");
      assertFalse(definedInfo.sign().isEmpty(), "haxe 5 serves ModuleInfo for a defined module");
    }
    else {
      assertThrows(Exception.class,
                   () -> client.module(genArgs, context.signature(), "gen.GeneratedThing"),
                   "haxe 4 server/module rejects a defined module");
    }

    // server/type answers on both generations, so blueprints make the defined
    // type's members visible
    TypeBlueprint blueprint = client.typeBlueprint(genArgs, context.signature(), "gen.GeneratedThing", "GeneratedThing");
    assertNotNull(blueprint.findMember("tag"), "generated members must be listed by name");
    assertNotNull(blueprint.findMember("make"), "generated members must be listed by name");
    if (blueprintTypesResolved()) {
      assertEquals("String", blueprint.findMember("tag").type().dotPath());
      assertEquals("() -> String", blueprint.findMember("make").type().presentable());
    }
  }

  /// Each capability helper below names one haxe 5 behavior change. A test
  /// checks the capability it exercises, never a bare version number or the
  /// helper of an unrelated capability.
  private static boolean isHaxe5OrNewer() {
    return serverVersion.major() >= 5;
  }

  /// Haxe 5+ populates the LSP-style diagnostic code with -w warning identifiers.
  private static boolean sendsDiagnosticCodes() {
    return isHaxe5OrNewer();
  }

  /// Haxe 5 renames the removable-code kind to ReplaceableCode and may supply newCode.
  private static boolean sendsReplaceableCode() {
    return isHaxe5OrNewer();
  }

  /// Haxe 5 answers server/module for defineType-created modules too.
  private static boolean servesDefinedModuleInfo() {
    return isHaxe5OrNewer();
  }

  /// Haxe 5 (preview) serializes server/type member types BEFORE it forces
  /// lazy typing, so the fields arrive as unresolved TMono. Haxe 4.x sends
  /// concrete types. Names and shapes are reliable on both.
  private static boolean blueprintTypesResolved() {
    return !isHaxe5OrNewer();
  }

  /** The server context whose module cache holds {@code module}. Null when none does, including before any compile. */
  private static HaxeServerContext typedContextHolding(List<String> args, String module) {
    try {
      for (HaxeServerContext context : client.contexts(args)) {
        if (contextHasModule(args, context, module)) return context;
      }
    } catch (Exception ignored) {
      // a fresh server rejects context listing until something compiled
    }
    return null;
  }

  private static boolean contextHasModule(List<String> args, HaxeServerContext context, String module) {
    try {
      return client.modules(args, context.signature()).contains(module);
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean haxeAvailable() {
    try {
      Process process = new ProcessBuilder(HAXE_EXE, "--version")
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .start();
      return process.waitFor() == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  /** The server needs a moment to bind its port, so this polls with a cheap request. */
  private static void waitUntilAccepting() throws Exception {
    long deadline = System.currentTimeMillis() + 15_000;
    while (true) {
      try {
        // Any completed exchange proves the server accepts connections. The
        // response may be empty: haxe 5 answers the legacy --version request
        // by just closing the connection.
        HaxeDisplayTransport.request("127.0.0.1", port, List.of("--version"), 5_000);
        return;
      } catch (Exception e) {
        if (System.currentTimeMillis() > deadline) {
          throw e;
        }
        Thread.sleep(200);
      }
    }
  }
}
