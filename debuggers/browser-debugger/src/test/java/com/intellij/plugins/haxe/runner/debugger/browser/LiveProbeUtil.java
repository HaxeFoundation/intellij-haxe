package com.intellij.plugins.haxe.runner.debugger.browser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ScopesResponse;
import com.intellij.util.net.NetUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/// The plumbing shared by the live probes: haxe availability, fixture
/// compilation, adapter/browser locations, request factories, adapter
/// connection with retry, and process-tree teardown.
final class LiveProbeUtil {
  /// The one-page host for the compiled fixture; every probe writes the same file.
  static final String INDEX_HTML = """
    <!DOCTYPE html><html><head><meta charset='utf-8'></head>\
    <body><script src='app.js'></script></body></html>""";

  // The ticking breakpoint fixture both browser families drive their
  // sessions against. Line numbers are load-bearing: BP_LINE is `counter++;`.
  static final int BP_LINE = 5;
  static final String WEB_MAIN_HX_NAME = "WebMain.hx";
  static final String WEB_MAIN_HX_SOURCE = """
    class WebMain {
    	static var counter = 0;

    	static function tick() {
    		counter++; // BP_LINE = 5
    		var label = "tick-" + counter;
    		js.Browser.console.log(label);
    	}

    	static function main() {
    		js.Browser.window.setInterval(tick, 250);
    	}
    }
    """;

  // WebLoad.hx line numbers are load-bearing: LOAD_BP_LINE is `var marker...`,
  // which executes DURING PAGE LOAD - the race the ticking fixture cannot see.
  static final String WEB_LOAD_HX_NAME = "WebLoad.hx";
  static final int LOAD_BP_LINE = 3;
  static final String WEB_LOAD_HX_SOURCE = """
    class WebLoad {
    	static function main() {
    		var marker = "before"; // LOAD_BP_LINE = 3
    		js.Browser.console.log(marker + "-loaded");
    	}
    }
    """;

  /// The pinned js-debug-dap release the probes drive (GitHub release, sha256-verified by the provisioner).
  static final String JS_DEBUG_VERSION = "1.117.0";

  /// Machine-wide chromium-family install locations, tried after the per-user LOCALAPPDATA ones.
  private static final List<String> CHROMIUM_PATHS = List.of(
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    "C:/Program Files/Microsoft/Edge/Application/msedge.exe",
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
    "/usr/bin/google-chrome",
    "/snap/bin/chromium");

  /// Standard firefox install locations, tried when WEB_DEBUG_FIREFOX_EXE is unset.
  private static final List<String> FIREFOX_PATHS = List.of(
    "C:/Program Files/Mozilla Firefox/firefox.exe",
    "C:/Program Files (x86)/Mozilla Firefox/firefox.exe",
    "/usr/bin/firefox",
    "/usr/bin/firefox-esr",
    "/snap/bin/firefox");

  private LiveProbeUtil() {
  }

  /// The provisioned js-debug standalone DAP server's entry script.
  static Path dapServerJs() {
    return nodeRoot().resolve("adapters/js-debug-" + JS_DEBUG_VERSION + "/js-debug/src/dapDebugServer.js");
  }

  /// The browser under test: the `WEB_DEBUG_CHROMIUM_EXE` environment
  /// variable when set (any chromium-family build — e.g. a provisioned
  /// ungoogled-chromium), else an installed Chrome/Edge — mirroring the IDE
  /// behaviour, where a blank executable lets js-debug find the default
  /// installation. A set-but-invalid path SKIPS rather than silently testing
  /// a different browser than the one asked for.
  static Path chromiumExe() {
    String env = System.getenv("WEB_DEBUG_CHROMIUM_EXE");
    if (env != null && !env.isBlank()) {
      Path fromEnv = Path.of(env);
      return Files.isRegularFile(fromEnv) ? fromEnv : null;
    }
    List<Path> candidates = new ArrayList<>();
    String localAppData = System.getenv("LOCALAPPDATA");
    if (localAppData != null && !localAppData.isBlank()) {
      // per-user installs; plain Chromium (e.g. ungoogled-chromium, the
      // reference browser of this module) ahead of the branded ones
      candidates.add(Path.of(localAppData, "Chromium/Application/chrome.exe"));
      candidates.add(Path.of(localAppData, "Google/Chrome/Application/chrome.exe"));
    }
    for (String candidate : CHROMIUM_PATHS) {
      candidates.add(Path.of(candidate));
    }
    for (Path path : candidates) {
      if (Files.isRegularFile(path)) {
        return path;
      }
    }
    return null;
  }

  /// The browser under test: the `WEB_DEBUG_FIREFOX_EXE` environment
  /// variable when set (e.g. an ESR install), else the standard installation
  /// paths. A set-but-invalid path SKIPS rather than silently testing a
  /// different browser than the one asked for.
  static Path firefoxExe() {
    String env = System.getenv("WEB_DEBUG_FIREFOX_EXE");
    if (env != null && !env.isBlank()) {
      Path fromEnv = Path.of(env);
      return Files.isRegularFile(fromEnv) ? fromEnv : null;
    }
    for (String candidate : FIREFOX_PATHS) {
      Path path = Path.of(candidate);
      if (Files.isRegularFile(path)) {
        return path;
      }
    }
    return null;
  }

  /// The probes' initialize: the given adapterID, IntelliJ Haxe as the client, startDebugging supported.
  static InitializeRequest initializeRequest(String adapterId) {
    InitializeRequest initialize = InitializeRequest.standard(adapterId, true);
    initialize.getArguments().setClientName("IntelliJ Haxe");
    return initialize;
  }

  /// The parent-session chrome launch config every browser probe sends.
  static Map<String, Object> baseLaunchConfig(String baseUrl, Path fixture) {
    Map<String, Object> config = new LinkedHashMap<>();

    config.put("type", "pwa-chrome");
    config.put("request", "launch");
    config.put("name", "probe");
    config.put("url", baseUrl);
    config.put("webRoot", fixture.toString());
    config.put("runtimeExecutable", chromiumExe().toString());
    config.put("runtimeArgs", List.of("--headless=new"));

    return config;
  }

  /// One source breakpoint on the fixture's `hxFileName` at `line`.
  static SetBreakpointsRequest breakpointsRequest(Path fixture, String hxFileName, int line) {
    return breakpointsRequest(fixture.resolve(hxFileName).toString(), hxFileName, line);
  }

  /// Raw-path form for probes that vary the path SPELLING (separator variants).
  static SetBreakpointsRequest breakpointsRequest(String sourcePath, String sourceName, int line) {
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();

    Source source = new Source();
    source.setPath(sourcePath);
    source.setName(sourceName);
    bpArgs.setSource(source);

    SourceBreakpoint bp = new SourceBreakpoint();
    bp.setLine(line);
    bpArgs.setBreakpoints(List.of(bp));

    setBreakpoints.setArguments(bpArgs);
    return setBreakpoints;
  }

  /// Drives the parent session until js-debug asks for the child session:
  /// configurationDone on the initialized event, every reverse request answered
  /// as the IDE answers it. Null when no startDebugging arrives in time.
  static StartDebuggingRequest awaitStartDebugging(DapClient parent, long millis, long requestTimeout)
    throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      Event event = parent.pollEvent(100);
      if (event instanceof InitializedEvent) {
        parent.sendRequest(new ConfigurationDoneRequest(), requestTimeout);
      }

      Request incoming = parent.pollIncomingRequest(50);
      if (incoming != null) {
        parent.respond(incoming, true);
        if (incoming instanceof StartDebuggingRequest start) {
          return start;
        }
      }
    }
    return null;
  }

  /// Probe-side diagnostics; the tag separates them from the adapter and server streams.
  static void probe(String message) {
    System.out.println("[probe] " + message);
  }

  /// Whether the frame landed on the haxe original at `line` (source-mapped, not the generated js).
  static boolean isStoppedInHx(StackFrame frame, String hxFile, int line) {
    if (frame == null || frame.getSource() == null || frame.getSource().getPath() == null) return false;
    return frame.getSource().getPath().endsWith(hxFile) && frame.getLine() == line;
  }

  /// The breakpoint carried by a breakpoint event; null for other events and empty bodies.
  static Breakpoint breakpointOf(Event event) {
    if (event instanceof BreakpointEvent be && be.getBody() != null) return be.getBody().getBreakpoint();
    return null;
  }

  /// The output event's text; null for other events and output-less bodies.
  static String outputTextOf(Event event) {
    if (event instanceof OutputEvent output && output.getBody() != null) return output.getBody().getOutput();
    return null;
  }

  /// The first scope's variables reference; -1 when the request failed or no scopes came back.
  static int firstScopeReference(Response response) {
    if (response instanceof ScopesResponse ok && ok.getBody() != null && !ok.getBody().getScopes().isEmpty()) {
      return ok.getBody().getScopes().get(0).getVariablesReference();
    }
    return -1;
  }

  /// Asserts the stop landed on the haxe original at `line` — a frame
  /// pointing at the generated app.js means the source map was not applied.
  static void assertStoppedInHx(StackFrame top, String hxFile, int line) {
    assertNotNull(top.getSource(), "top frame has no source");
    assertTrue(top.getSource().getPath() != null && top.getSource().getPath().endsWith(hxFile), "top frame is not the .hx original: " + top.getSource().getPath());
    assertTrue(top.getLine() == line, "wrong line: " + top.getLine());
  }

  static StackTraceRequest stackTraceRequest(int threadId) {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static ScopesRequest scopesRequest(int frameId) {
    ScopesRequest request = new ScopesRequest();
    ScopesArguments arguments = new ScopesArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request;
  }

  static VariablesRequest variablesRequest(int variablesReference) {
    VariablesRequest request = new VariablesRequest();
    VariablesArguments arguments = new VariablesArguments();
    arguments.setVariablesReference(variablesReference);
    request.setArguments(arguments);
    return request;
  }

  static ContinueRequest continueRequest(int threadId) {
    ContinueRequest request = new ContinueRequest();
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static PauseRequest pauseRequest(int threadId) {
    PauseRequest request = new PauseRequest();
    PauseArguments arguments = new PauseArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static NextRequest nextRequest(int threadId) {
    NextRequest request = new NextRequest();
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static StepInTargetsRequest stepInTargetsRequest(int frameId) {
    StepInTargetsRequest request = new StepInTargetsRequest();
    StepInTargetsArguments arguments = new StepInTargetsArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request;
  }

  /// Smart step into: enter the chosen call on the line rather than the first one.
  static StepInRequest stepInRequest(int threadId, int targetId) {
    StepInRequest request = new StepInRequest();
    StepInArguments arguments = new StepInArguments();
    arguments.setThreadId(threadId);
    arguments.setTargetId(targetId);
    request.setArguments(arguments);
    return request;
  }

  static SetExceptionBreakpointsRequest exceptionBreakpointsRequest(List<String> filters) {
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(filters);
    request.setArguments(arguments);
    return request;
  }

  /// True when the adapter's initialized event arrives before the timeout.
  static boolean awaitInitialized(DapEndpoint endpoint, long millis) throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      if (endpoint.pollEvent(250) instanceof InitializedEvent) {
        return true;
      }
    }
    return false;
  }

  /// The next stopped event, or null when none arrives before the timeout.
  static StoppedEvent awaitStopped(DapEndpoint endpoint, long millis) throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      if (endpoint.pollEvent(250) instanceof StoppedEvent stopped) {
        return stopped;
      }
    }
    return null;
  }

  /// Where the provisioned node + adapters live; the matrix lanes point elsewhere.
  static Path nodeRoot() {
    String override = System.getProperty("web.debug.node.root");
    return override != null ? Path.of(override) : Path.of("../../node").toAbsolutePath().normalize();
  }

  static Path nodeExe() {
    // the compat-matrix web lanes point each cell at a provisioned node
    String override = System.getProperty("web.debug.node.exe");
    if (override != null) return Path.of(override);
    return nodeRoot().resolve("node-v24.18.0-win-x64/node.exe");
  }

  static boolean haxeOnPath() {
    try {
      Process probe = new ProcessBuilder("haxe", "--version").redirectErrorStream(true).start();
      return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  /// The host page plus its compiled app.js — the two files every probe fixture needs.
  static void writePageAndCompile(Path dir, String mainClass) throws Exception {
    Files.writeString(dir.resolve("index.html"), INDEX_HTML);
    compileHaxeJs(dir, mainClass, "app.js");
  }

  /// Compiles one `haxe -js` unit with `-debug` (source maps);
  /// fails the test with the compiler's output when the compile fails.
  static void compileHaxeJs(Path classPath, String mainClass, String outJsName) throws Exception {
    compileHaxeJs(classPath, mainClass, outJsName, 30, List.of());
  }

  /// As above with extra compiler arguments (libs, defines, macros) and a timeout for builds that pull libraries.
  static void compileHaxeJs(Path classPath, String mainClass, String outJsName,
                            long timeoutSeconds, List<String> extraArgs) throws Exception {
    List<String> command = new ArrayList<>(List.of("haxe", "-cp", classPath.toString()));
    command.addAll(extraArgs);
    command.addAll(List.of("-main", mainClass, "-js", classPath.resolve(outJsName).toString(), "-debug"));
    Process haxe = new ProcessBuilder(command).redirectErrorStream(true).start();

    String output = new String(haxe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!haxe.waitFor(timeoutSeconds, TimeUnit.SECONDS) || haxe.exitValue() != 0) {
      throw new AssertionError("fixture compile of " + mainClass + " failed:\n" + output);
    }
  }

  /// A free TCP port from the OS, for the adapter's DAP listener and the RDP
  /// port in launch configs. A random pick from a fixed range flakes on
  /// Windows: Hyper-V/WinNAT reserve blocks of the port space (excluded port
  /// ranges) and a bind inside one dies with EACCES.
  static int freePort() throws IOException {
    return NetUtils.findAvailableSocketPort();
  }

  /// Connects to an adapter's DAP port, retrying briefly (see DapClient.connectWithRetry).
  static DapClient connectWithRetry(int port, int connectTimeoutMillis) throws IOException {
    return DapClient.connectWithRetry("127.0.0.1", port, connectTimeoutMillis, 10_000);
  }

  /// Spawns a node-hosted adapter server, verifies its announcement line, and
  /// keeps DRAINING its merged output on a daemon thread. The drain is
  /// load-bearing: an undrained pipe blocks the adapter once the OS buffer
  /// fills, which reads as a wedged session minutes later - every spawn goes
  /// through here so it cannot be forgotten.
  static Process spawnAdapterServer(List<String> command, Path workingDir,
                                    String expectedAnnouncement, String tag) throws IOException {
    Process adapter = new ProcessBuilder(command)
      .directory(workingDir.toFile())
      .redirectErrorStream(true)
      .start();
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(adapter.getInputStream(), StandardCharsets.UTF_8));
    String line = stdout.readLine();
    System.out.println("[" + tag + "] " + line);
    assertNotNull(line, tag + " announced nothing (died?)");
    assertTrue(line.contains(expectedAnnouncement), "unexpected announcement: " + line);

    Thread gobbler = new Thread(() -> {
      try {
        String out;
        while ((out = stdout.readLine()) != null) {
          System.out.println("[" + tag + "] " + out);
        }
      } catch (IOException ignored) {
      }
    }, tag + "-gobbler");
    gobbler.setDaemon(true);
    gobbler.start();
    return adapter;
  }

  /// Kills the WHOLE process tree: killing node does not kill the browser it
  /// spawned, and every leaked headless browser poisons later
  /// launches.
  static void killTree(Process process) throws InterruptedException {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroy();
    if (!process.waitFor(3, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      process.waitFor(3, TimeUnit.SECONDS);
    }
  }
}
