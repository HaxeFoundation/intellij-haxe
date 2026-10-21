package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;

import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.BP_LINE;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.LOAD_BP_LINE;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.WEB_LOAD_HX_NAME;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.WEB_MAIN_HX_NAME;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.WEB_LOAD_HX_SOURCE;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.WEB_MAIN_HX_SOURCE;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.assertStoppedInHx;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.awaitInitialized;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.breakpointOf;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.breakpointsRequest;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.isStoppedInHx;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.continueRequest;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.firefoxExe;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.haxeOnPath;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.nodeExe;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.nodeRoot;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.probe;
import static com.intellij.plugins.haxe.runner.debugger.javascript.LiveProbeUtil.stackTraceRequest;
import static org.junit.jupiter.api.Assertions.*;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.intellij.plugins.haxe.runner.debugger.browser.ContentHttpServer;

/// Live probes of the firefox debug adapter (vscode-firefox-debug 2.15.0):
/// connecting, a served session in Firefox, breakpoint paths and load-time
/// breakpoints. Requires Firefox, the provisioned portable node with the
/// firefox adapter, and haxe on PATH.
///
/// Wire facts of this adapter, recorded here because the module has no README:
///
///   - The `initialized` event follows the launch response, so breakpoints
///     are registered after the launch; `setBreakpoints` answers them
///     unverified and a later breakpoint event upgrades each one once the
///     source map binds it.
///   - Two launch modes: `file` (a file:// url to the page) and `url` +
///     `webRoot` (a page served over http, webRoot mapping the served urls
///     back to the content directory for the source maps). The IDE backend
///     only ever serves.
///   - Breakpoint paths are matched LITERALLY against native paths: a
///     forward-slash spelling (the IDE's VFS form on Windows) never binds.
///   - Worker frames: scopes and variables answer, but a request the
///     in-worker devtools server never answers (its previewer crashes on
///     object inspection) wedges the adapter's per-actor FIFO queue for that
///     thread, and every later request to it hangs — an upstream Mozilla
///     limitation; the backend keeps a short request timeout and the Chromium
///     family is the recommendation for worker-heavy sessions.
///   - A worker PAUSED at a breakpoint when the served first page reloads
///     (the load-time strategy below) stays in the thread list after the
///     reload as a thread whose actor answers nothing.
@DisplayName("Browser debugger: firefox adapter (live)")
public class FirefoxAdapterLiveTest {
  private static final long TIMEOUT = 15_000;

  private Process adapter;
  private DapClient client;

  @BeforeEach
  public void spawnAdapter() throws IOException {
    Assumptions.assumeTrue(Files.isRegularFile(nodeExe()), "portable node not provisioned - skipping");
    Assumptions.assumeTrue(Files.isRegularFile(adapterBundle()), "firefox adapter not provisioned - skipping");

    int port = LiveProbeUtil.freePort();
    // cwd = dist so the bundle finds mappings.wasm however it resolves it
    adapter = LiveProbeUtil.spawnAdapterServer(
      List.of(nodeExe().toString(), adapterBundle().toString(), "--server=" + port),
      adapterBundle().getParent(), "waiting for debug protocol", "adapter");

    client = connectWithRetry(port);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (client != null) {
      try {
        client.close();
      } catch (IOException ignored) {
      }
    }
    if (adapter != null) {
      killTree(adapter);
    }
  }

  /// The SERVE mode the IDE backend uses: the fixture is hosted by the module's own
  /// ContentHttpServer and the browser navigates to the http url (webRoot maps
  /// served urls back to the content directory for the source maps). Mirrors
  /// BrowserDebugBackend's launch config; a stop must still land in the .hx.
  @Test
  @Timeout(60)
  @DisplayName("full session breakpoint in hx source via content server")
  public void fullSessionBreakpointInHxSourceViaContentServer() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path firefox = firefoxExe();
    Assumptions.assumeTrue(firefox != null, "firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping");
    Path fixture = buildFixture();

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      probe("serving " + fixture + " at " + content.getBaseUrl());

      assertTrue(client.sendRequest(initializeRequest(), TIMEOUT).isSuccess(), "initialize");

      Map<String, Object> launchConfig = servedLaunchConfig(firefox, content.getBaseUrl(), fixture);
      Response launch = client.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000);
      assertTrue(launch.isSuccess(), "launch failed: " + launch.getMessage());

      boolean initialized = awaitInitialized(client, 15_000);
      assertTrue(initialized, "no initialized event after launch");

      assertTrue(client.sendRequest(breakpointsRequest(fixture, WEB_MAIN_HX_NAME, BP_LINE), TIMEOUT).isSuccess(), "setBreakpoints failed");

      StoppedEvent stopped = null;
      long deadline = System.currentTimeMillis() + 15_000;
      while (System.currentTimeMillis() < deadline && stopped == null) {
        if (client.pollEvent(250) instanceof StoppedEvent s) {
          stopped = s;
        }
      }
      assertNotNull(stopped, "breakpoint never hit over http");
      int threadId = threadIdOf(stopped);

      Response stResponse = client.sendRequest(stackTraceRequest(threadId), TIMEOUT);
      assertTrue(stResponse.isSuccess(), "stackTrace");
      List<StackFrame> frames = ((StackTraceResponse)stResponse).getBody().getStackFrames();
      assertFalse(frames.isEmpty(), "no frames");

      StackFrame top = frames.get(0);
      probe("http-mode top frame: " + top.getName()
            + " @ " + (top.getSource() != null ? top.getSource().getPath() : "?")
            + ":" + top.getLine());

      assertStoppedInHx(top, WEB_MAIN_HX_NAME, BP_LINE);

      Response disconnect = client.sendRequest(new DisconnectRequest(), TIMEOUT);
      probe("disconnect success=" + disconnect.isSuccess());
    }
  }

  /// Breakpoint paths are matched LITERALLY against native paths: the IDE's
  /// VFS spelling (forward slashes on Windows) never binds, the native one
  /// does — the reason BrowserDebugBackend.breakpointSourcePath converts to
  /// native separators. The two spellings only differ on Windows.
  @Test
  @Timeout(60)
  @DisplayName("breakpoint path separator sensitivity")
  public void breakpointPathSeparatorSensitivity() throws Exception {
    Assumptions.assumeTrue(File.separatorChar == '\\', "the path spellings only differ on windows - skipping");
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path firefox = firefoxExe();
    Assumptions.assumeTrue(firefox != null, "firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping");
    Path fixture = buildFixture(); // the ticking fixture: no load race involved

    boolean forwardBound;
    boolean nativeBound;
    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      forwardBound = runSeparatorVariant("forward", fixture, firefox, content,
                                         fixture.resolve(WEB_MAIN_HX_NAME).toString().replace('\\', '/'));
    }

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      nativeBound = runSeparatorVariant("native", fixture, firefox, content,
                                        fixture.resolve(WEB_MAIN_HX_NAME).toString());
    }

    probe("separator sensitivity: forward=" + forwardBound + " native=" + nativeBound);
    assertFalse(forwardBound, "a forward-slash breakpoint path must not bind (the backend converts to native for this)");
    assertTrue(nativeBound, "native breakpoint path must bind");
  }

  // ------------------------------------------- load-time breakpoints

  /// Code in main() runs while the page loads, before the standard flow
  /// (launch -> initialized -> setBreakpoints) completes, so the first load
  /// runs through a load-time breakpoint unpaused. The adapter applies
  /// breakpoints to a load only when they were registered BEFORE the load that
  /// taught it the sources: serving a synthetic bootstrap page first, or
  /// registering the breakpoints only when the reloaded page requests its
  /// script, arms nothing. Register -> load once -> reload once (the served
  /// first page refreshing itself, as BrowserDebugBackend arms it) is the
  /// working sequence: the second load stops on the .hx line.
  @Test
  @Timeout(60)
  @DisplayName("load time breakpoint hits on the refreshed first page")
  public void loadTimeBreakpointHitsOnTheRefreshedFirstPage() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path firefox = firefoxExe();
    Assumptions.assumeTrue(firefox != null, "firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping");
    Path fixture = buildLoadFixture();

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      content.setRequestListener(line -> System.out.println("[server] " + line));
      content.refreshFirstPage(2);
      assertTrue(client.sendRequest(initializeRequest(), TIMEOUT).isSuccess(), "initialize");

      Map<String, Object> launchConfig = servedLaunchConfig(firefox, content.getBaseUrl(), fixture);
      Response launch = client.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000);
      assertTrue(launch.isSuccess(), "launch failed: " + launch.getMessage());
      assertTrue(awaitInitialized(client, 15_000), "no initialized event after launch");
      assertTrue(client.sendRequest(breakpointsRequest(fixture, WEB_LOAD_HX_NAME, LOAD_BP_LINE), TIMEOUT).isSuccess(), "setBreakpoints failed");

      StackFrame top = awaitStopInHx(WEB_LOAD_HX_NAME, LOAD_BP_LINE, 15_000);
      assertNotNull(top, "the reloaded page never stopped on the load-time breakpoint");

      client.sendRequest(new DisconnectRequest(), TIMEOUT);
    }
  }

  // ---------------------------------------------------------------- helpers

  /// `<root>/node` — provided by the gradle test task; falls back for IDE runs.
  private static Path adapterBundle() {
    return nodeRoot().resolve("adapters/vscode-firefox-debug-2.15.0/extension/dist/adapter.bundle.js");
  }

  // The adapter prints its "waiting for debug protocol" line slightly
  // BEFORE the TCP listener accepts, so an immediate connect can be
  // refused - retry briefly (the production launcher must do the same).
  private static DapClient connectWithRetry(int port) throws IOException {
    return LiveProbeUtil.connectWithRetry(port, (int)TIMEOUT);
  }

  // Killing node does NOT kill the Firefox it spawned - reap the whole tree,
  // or every probe run leaks a headless browser.
  private static void killTree(Process process) throws InterruptedException {
    LiveProbeUtil.killTree(process);
  }

  /// The handshake every probe here opens with.
  private static InitializeRequest initializeRequest() {
    return InitializeRequest.standard("firefox", false);
  }

  /// The launch config every probe here starts from. The RDP port is always a
  /// fresh one: the adapter defaults to 6000, and a firefox surviving from an
  /// earlier session still holds it, so the adapter attaches to THAT instance
  /// instead of the one it just launched ("Not attaching to this thread" for
  /// every worker, and foreign processes' workers in the target list).
  private static Map<String, Object> baseLaunchConfig(Path firefox) throws IOException {
    Map<String, Object> config = new LinkedHashMap<>();

    config.put("request", "launch");
    config.put("firefoxExecutable", firefox.toString());
    config.put("firefoxArgs", List.of("-headless"));
    config.put("port", LiveProbeUtil.freePort());

    return config;
  }

  /// {@link #baseLaunchConfig} for a page served over http rather than a file:// url.
  private static Map<String, Object> servedLaunchConfig(Path firefox, String baseUrl, Path fixture)
    throws IOException {
    Map<String, Object> config = baseLaunchConfig(firefox);

    config.put("url", baseUrl);
    config.put("webRoot", fixture.toString());

    return config;
  }

  /// Writes + compiles the fixture, returns its directory (app.js/app.js.map/index.html/WebMain.hx).
  private static Path buildFixture() throws Exception {
    Path dir = Files.createTempDirectory("haxe-web-probe");
    Files.writeString(dir.resolve(WEB_MAIN_HX_NAME), WEB_MAIN_HX_SOURCE);
    LiveProbeUtil.writePageAndCompile(dir, "WebMain");
    return dir;
  }

  private static Path buildLoadFixture() throws Exception {
    Path dir = Files.createTempDirectory("haxe-web-load-probe");
    Files.writeString(dir.resolve(WEB_LOAD_HX_NAME), WEB_LOAD_HX_SOURCE);
    LiveProbeUtil.writePageAndCompile(dir, "WebLoad");
    return dir;
  }

  private boolean runSeparatorVariant(String label, Path fixture, Path firefox,
                                      ContentHttpServer content, String breakpointPath) throws Exception {
    int ownPort = LiveProbeUtil.freePort();
    Process ownAdapter = LiveProbeUtil.spawnAdapterServer(
      List.of(nodeExe().toString(), adapterBundle().toString(), "--server=" + ownPort),
      adapterBundle().getParent(), "waiting for debug protocol", "adapter-" + label);

    try {
      DapClient session = connectWithRetry(ownPort);
      try {
        if (!session.sendRequest(initializeRequest(), TIMEOUT).isSuccess()) {
          return false;
        }
        Map<String, Object> launchConfig = servedLaunchConfig(firefox, content.getBaseUrl(), fixture);
        if (!session.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000).isSuccess()) {
          return false;
        }
        boolean initialized = awaitInitialized(session, 15_000);
        if (!initialized) {
          return false;
        }
        session.sendRequest(breakpointsRequest(breakpointPath, WEB_MAIN_HX_NAME, BP_LINE), TIMEOUT);

        StoppedEvent stopped = null;
        // the fixture ticks every 250ms and lazy verification takes 1-2s, so
        // a path that binds stops well inside this window; the forward
        // variant is EXPECTED not to stop and pays the full wait
        long deadline = System.currentTimeMillis() + 8_000;

        while (System.currentTimeMillis() < deadline && stopped == null) {
          Event event = session.pollEvent(250);
          Breakpoint breakpoint = breakpointOf(event);
          if (event instanceof StoppedEvent s) {
            stopped = s;
          } else if (breakpoint != null) {
            probe("  " + label + " breakpointEvent verified=" + breakpoint.isVerified());
          }
        }
        probe("  " + label + " path stop=" + (stopped != null));
        session.sendRequest(new DisconnectRequest(), TIMEOUT);
        return stopped != null;
      } finally {
        session.close();
      }
    } finally {
      killTree(ownAdapter);
    }
  }

  /// The first stop ON the given .hx line within the timeout, or null. Any
  /// other stop (an entry pause, a foreign stop) is resumed after a beat so
  /// the adapter can bind the source map.
  private StackFrame awaitStopInHx(String hxFile, int line, long timeoutMillis) throws Exception {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (System.currentTimeMillis() < deadline) {
      Event event = client.pollEvent(250);
      if (event == null) {
        continue;
      }
      String detail = event instanceof StoppedEvent s ? " reason=" + s.getBody().getReason() : "";
      probe("event '" + event.getEvent() + "' (" + event.getClass().getSimpleName() + ")" + detail);
      if (!(event instanceof StoppedEvent stopped)) {
        continue;
      }
      int threadId = threadIdOf(stopped);

      StackFrame top = firstFrame(client.sendRequest(stackTraceRequest(threadId), TIMEOUT));
      probe("stop frame: " + frameLabel(top));
      if (isStoppedInHx(top, hxFile, line)) {
        return top;
      }
      Thread.sleep(1_500);
      client.sendRequest(continueRequest(threadId), TIMEOUT);
    }
    return null;
  }

  /// The top frame of a stackTrace response; null when the request failed or the stack is empty.
  private static StackFrame firstFrame(Response stackTraceResponse) {
    if (!(stackTraceResponse instanceof StackTraceResponse st) || !st.isSuccess()) return null;
    List<StackFrame> frames = st.getBody().getStackFrames();
    return frames.isEmpty() ? null : frames.get(0);
  }

  /// `path:line` of a frame for the probe log; `<none>` without a frame, `?` without a source.
  private static String frameLabel(StackFrame frame) {
    if (frame == null) return "<none>";
    String path = frame.getSource() != null ? frame.getSource().getPath() : "?";
    return path + ":" + frame.getLine();
  }

  /// The stopped event's thread id; DAP allows it to be absent, 1 is the conventional main thread.
  private static int threadIdOf(StoppedEvent stopped) {
    Integer threadId = stopped.getBody().getThreadId();
    return threadId != null ? threadId : 1;
  }

  /// A launch request whose arguments are the firefox adapter's own vocabulary.
  static final class FirefoxLaunchRequest extends Request {
    @SuppressWarnings("unused") // serialized by jackson
    private final Map<String, Object> arguments;

    FirefoxLaunchRequest(Map<String, Object> arguments) {
      setCommand("launch");
      this.arguments = arguments;
    }

    public Map<String, Object> getArguments() {
      return arguments;
    }
  }
}
