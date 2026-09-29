package com.intellij.plugins.haxe.profiler.bridge.js;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeJsProfilerCapture;
import com.intellij.plugins.haxe.profiler.HaxeProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.HaxeProfilingNotifier;
import com.intellij.plugins.haxe.profiler.bridge.HaxeCaptureFiles;
import com.intellij.plugins.haxe.profiler.bridge.HaxeIuProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.bridge.HaxeLiveCaptures;
import com.intellij.plugins.haxe.profiler.js.CpuProfileSessionBuilder;
import com.intellij.plugins.haxe.profiler.js.JsSourceMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The IU-side V8 capture: polls the Chromium child's DevTools endpoint
 * until its first page target appears, attaches over CDP (the JDK's own
 * WebSocket — no external client) and drives the sampling profiler in
 * SEGMENTS: {@code Profiler.stop} collects what has accumulated, the
 * segment appends to the HXTS session file, {@code Profiler.start} resumes
 * — every second. The session streams while the program runs (the live
 * view follows the growing file), and a browser closed by hand keeps every
 * segment already collected; only the in-flight one is lost. The run's
 * Stop collects the final segment before the browser is killed.
 *
 * Alongside the profiler, a Tracing session on the frame category cycles
 * with each segment: its DrawFrame instants (the compositor's real
 * presents, on the SAME microsecond clock as the profile) become the
 * session's display frames, and {@code Performance.getMetrics} supplies
 * the JS heap reading per segment. Either failing leaves the sampled
 * profile intact — frames and memory just stay absent.
 */
public class HaxeIuJsProfilerCapture implements HaxeJsProfilerCapture {

  private static final Logger LOG = Logger.getInstance(HaxeIuJsProfilerCapture.class);
  private static final int DISCOVERY_TIMEOUT_MS = 30_000;
  private static final int DISCOVERY_RETRY_MS = 200;
  /** The stop-append-start cadence: each cycle flushes one frame record, bounding what a dead browser can lose. */
  private static final int SEGMENT_MS = 1_000;
  /** Collecting a segment is quick, but a wedged browser must not hang Stop. */
  private static final int STOP_TIMEOUT_SECONDS = 10;
  /** A session smaller than its header carries no frame worth opening. */
  private static final int MINIMUM_SESSION_BYTES = 32;
  /** Enough written bytes to open the LIVE view: real frames are flowing by then. */
  private static final int LIVE_OPEN_BYTES = 4096;

  @Override
  public @Nullable Handle start(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
                                int debugPort, int samplingIntervalUs, @Nullable Path contentRoot, @Nullable String baseUrl) {
    return new Capture(project, displayName, sessionFile, debugPort, samplingIntervalUs, contentRoot, baseUrl);
  }

  private static final class Capture implements Handle {
    private final Project project;
    private final String displayName;
    private final Path sessionFile;
    private final int debugPort;
    private final int samplingIntervalUs;
    private final @Nullable Path contentRoot;
    private final @Nullable String baseUrl;
    /** Parsed source maps by script url; empty = looked and found none. Guarded by {@code segmentLock}. */
    private final Map<String, Optional<JsSourceMap>> scriptSourceMaps = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    /** DrawFrame instants streamed by the tracing session, drained per segment. */
    private final Queue<Long> drawFrameStamps = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean finished = new AtomicBoolean();
    /** Serializes segment collection against the final collect and the close. */
    private final Object segmentLock = new Object();
    private volatile CompletableFuture<Void> tracingComplete;
    private volatile boolean tracingActive;
    private volatile boolean cancelled;
    private volatile HaxeCdpConnection cdp;
    private volatile HaxeProfilerProcessUi.Session session;
    private OutputStream sessionStream;
    private CpuProfileSessionBuilder builder;
    private HaxeLiveCaptures.Entry liveEntry;
    private boolean liveOpened;

    Capture(Project project, String displayName, Path sessionFile, int debugPort, int samplingIntervalUs,
            @Nullable Path contentRoot, @Nullable String baseUrl) {
      this.project = project;
      this.displayName = displayName;
      this.sessionFile = HaxeCaptureFiles.perCaptureSessionPath(sessionFile);
      this.debugPort = debugPort;
      this.samplingIntervalUs = samplingIntervalUs;
      this.contentRoot = contentRoot;
      this.baseUrl = baseUrl;
      Thread attacher = new Thread(this::attachAndCollect, "haxe-js-profiler-capture");
      attacher.setDaemon(true);
      attacher.start();
    }

    /** Waits out the browser's startup, connects to its first page target, starts the sampler and loops segments. */
    private void attachAndCollect() {
      try {
        String targetSocketUrl = discoverPageTarget();
        if (targetSocketUrl == null) return; // cancelled or timed out - the stop path explains
        cdp = HaxeCdpConnection.open(targetSocketUrl, this::onEvent);
        cdp.call("Profiler.enable", null);
        cdp.call("Profiler.setSamplingInterval", mapper.createObjectNode().put("interval", samplingIntervalUs));
        cdp.call("Profiler.start", null);
        cdp.call("Performance.enable", null);
        try {
          startTracing();
          tracingActive = true;
        }
        catch (Exception unavailable) {
          LOG.info("frame tracing unavailable - the session charts without display frames", unavailable);
        }
        session = HaxeIuProfilerProcessUi.open(project, displayName, sessionFile, HaxeJsProfilerConfigurationType.ID);
        openSessionFile();
      }
      catch (IOException | RuntimeException e) {
        LOG.warn("could not attach the js profiler", e);
        return;
      }
      collectSegmentsUntilDone();
    }

    private void openSessionFile() throws IOException {
      Files.createDirectories(sessionFile.getParent());
      synchronized (segmentLock) {
        if (cancelled) return; // the run was stopped while attaching - nothing to open
        sessionStream = Files.newOutputStream(sessionFile);
        builder = new CpuProfileSessionBuilder(sessionStream, this::sourceMapFor);
        liveEntry = HaxeLiveCaptures.register(sessionFile);
      }
    }

    /** The parsed source map beside a served script, cached per url; null when the script has none we can reach. */
    private @Nullable JsSourceMap sourceMapFor(@NotNull String scriptUrl) {
      return scriptSourceMaps.computeIfAbsent(scriptUrl, this::loadSourceMap).orElse(null);
    }

    private Optional<JsSourceMap> loadSourceMap(String scriptUrl) {
      Path script = servedScriptPath(scriptUrl);
      if (script == null) return Optional.empty();
      // the convention haxe/lime output follows: the map sits beside the
      // script as <name>.js.map
      Path map = script.resolveSibling(script.getFileName() + ".map");
      if (!Files.isRegularFile(map)) return Optional.empty();
      try {
        return Optional.of(JsSourceMap.parse(map));
      }
      catch (IOException e) {
        LOG.warn("unreadable source map " + map, e);
        return Optional.empty();
      }
    }

    /** The local file a sampled script url serves from, or null for scripts outside the served content. */
    private @Nullable Path servedScriptPath(String scriptUrl) {
      // everything before the query or fragment
      String noQuery = scriptUrl.split("[?#]")[0];
      String filePath = JsSourceMap.pathOfFileUrl(noQuery);
      if (filePath != null) {
        return Path.of(URLDecoder.decode(filePath, StandardCharsets.UTF_8));
      }
      if (contentRoot == null || baseUrl == null) return null;
      String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
      if (!noQuery.startsWith(base)) return null;
      String relative = URLDecoder.decode(noQuery.substring(base.length()), StandardCharsets.UTF_8);
      Path resolved = contentRoot.resolve(relative).normalize();
      return resolved.startsWith(contentRoot) ? resolved : null;
    }

    private void collectSegmentsUntilDone() {
      while (!cancelled) {
        try {
          Thread.sleep(SEGMENT_MS);
        }
        catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return;
        }
        synchronized (segmentLock) {
          if (cancelled || builder == null) return;
          try {
            collectSegment(true);
          }
          catch (Exception e) {
            // a dead browser ends the loop; whatever was flushed stands
            if (!cancelled) LOG.warn("js profile segment collection ended", e);
            return;
          }
        }
      }
    }

    /** One stop-append(-start) cycle; the caller holds {@code segmentLock}. */
    private void collectSegment(boolean restart) throws Exception {
      JsonNode result = cdp.call("Profiler.stop", null).get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      JsonNode profile = result.path("profile");
      if (profile.isMissingNode()) throw new IOException("Profiler.stop returned no profile");
      List<Long> frameStamps = tracingActive ? collectFrameStamps() : List.of();
      long heapUsed = 0;
      long heapTotal = 0;
      try {
        JsonNode metrics = cdp.call("Performance.getMetrics", null).get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        for (JsonNode metric : metrics.path("metrics")) {
          String name = metric.path("name").asString();
          if ("JSHeapUsedSize".equals(name)) heapUsed = (long)metric.path("value").asDouble();
          if ("JSHeapTotalSize".equals(name)) heapTotal = (long)metric.path("value").asDouble();
        }
      }
      catch (Exception noMetrics) {
        // heap readings are an extra - the sampled profile stands without them
      }
      builder.appendSegment(profile.toString(), frameStamps, heapUsed, heapTotal);
      if (!liveOpened && builder.bytesWritten() >= LIVE_OPEN_BYTES) {
        // the platform parses the partial file and shows the tabs NOW;
        // the live registry entry keeps the chart refreshing
        liveOpened = true;
        session.dataReady();
      }
      if (restart) {
        cdp.call("Profiler.start", null);
        if (tracingActive) {
          try {
            startTracing();
          }
          catch (Exception unavailable) {
            tracingActive = false;
            LOG.warn("frame tracing did not restart - later segments chart without display frames", unavailable);
          }
        }
      }
    }

    /** Ends the tracing cycle and drains the DrawFrame instants it delivered. */
    private List<Long> collectFrameStamps() {
      try {
        // the completion future must be armed before end() - events stream in between
        tracingComplete = new CompletableFuture<>();
        cdp.call("Tracing.end", null).get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        tracingComplete.get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
      catch (Exception e) {
        tracingActive = false;
        LOG.warn("frame tracing ended - later segments chart without display frames", e);
      }
      List<Long> frames = new ArrayList<>();
      Long stamp;
      while ((stamp = drawFrameStamps.poll()) != null) {
        frames.add(stamp);
      }
      return frames;
    }

    private void startTracing() throws Exception {
      var config = mapper.createObjectNode().put("transferMode", "ReportEvents");
      var traceConfig = mapper.createObjectNode();
      traceConfig.putArray("includedCategories").add("disabled-by-default-devtools.timeline.frame");
      // default-enabled categories trace unless excluded - the include list alone is not a filter
      traceConfig.putArray("excludedCategories").add("*");
      config.set("traceConfig", traceConfig);
      cdp.call("Tracing.start", config).get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /** The DevTools websocket url of the first page target, polling while the browser starts; null when cancelled/overdue. */
    @Nullable
    private String discoverPageTarget() {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debugPort + "/json/list"))
        .timeout(Duration.ofSeconds(2))
        .build();
      long deadline = System.currentTimeMillis() + DISCOVERY_TIMEOUT_MS;
      while (!cancelled && System.currentTimeMillis() < deadline) {
        try {
          HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
          for (JsonNode target : mapper.readTree(response.body())) {
            if ("page".equals(target.path("type").asString()) && target.hasNonNull("webSocketDebuggerUrl")) {
              return target.get("webSocketDebuggerUrl").asString();
            }
          }
        }
        catch (IOException | RuntimeException notUpYet) {
          // the browser is still starting - keep polling
        }
        catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return null;
        }
        try {
          Thread.sleep(DISCOVERY_RETRY_MS);
        }
        catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return null;
        }
      }
      return null;
    }

    @Override
    public void finishCapture() {
      if (!finished.compareAndSet(false, true)) return;
      cancelled = true;
      synchronized (segmentLock) {
        if (cdp != null && builder != null) {
          try {
            collectSegment(false);
          }
          catch (Exception e) {
            LOG.warn("final js segment collection failed - keeping what was flushed", e);
          }
        }
        finalizeSession();
      }
      // the browser is killed next; its dropped socket is no failure
      if (cdp != null) cdp.abandon();
    }

    @Override
    public void connectionLost() {
      if (!finished.compareAndSet(false, true)) return;
      cancelled = true;
      // unblock an in-flight Profiler.stop before taking the lock - the
      // browser is gone, its response will never arrive
      if (cdp != null) cdp.abandon();
      synchronized (segmentLock) {
        finalizeSession();
      }
    }

    /** The session file is final: end the live view and open the result, or explain the empty run. */
    private void finalizeSession() {
      if (sessionStream != null) {
        try {
          sessionStream.close();
        }
        catch (IOException e) {
          LOG.warn("could not close the js session file", e);
        }
      }
      if (liveEntry != null) {
        liveEntry.finished();
      }
      if (builder != null && builder.bytesWritten() >= MINIMUM_SESSION_BYTES) {
        if (session != null) {
          session.dataReady();
        }
        return;
      }
      HaxeProfilingNotifier.reportNothingCaptured(project, session, HaxeProfilerBundle.message("haxe.profiler.js.none"));
    }

    /** The CDP events: the tracing session's frame instants and its completion. */
    private void onEvent(JsonNode notification) {
      String method = notification.path("method").asString("");
      if ("Tracing.dataCollected".equals(method)) {
        for (JsonNode event : notification.path("params").path("value")) {
          // one DrawFrame per compositor present - the rest of the frame
          // category (pipeline stages) is not needed
          if ("DrawFrame".equals(event.path("name").asString())) {
            drawFrameStamps.add(event.path("ts").asLong());
          }
        }
      }
      else if ("Tracing.tracingComplete".equals(method)) {
        CompletableFuture<Void> complete = tracingComplete;
        if (complete != null) {
          complete.complete(null);
        }
      }
    }
  }
}
