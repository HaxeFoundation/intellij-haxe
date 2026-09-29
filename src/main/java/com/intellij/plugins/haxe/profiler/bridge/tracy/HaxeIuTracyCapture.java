package com.intellij.plugins.haxe.profiler.bridge.tracy;

import com.intellij.execution.Executor;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.HaxeProfilingNotifier;
import com.intellij.plugins.haxe.profiler.HaxeTracyCapture;
import com.intellij.plugins.haxe.profiler.bridge.HaxeCaptureFiles;
import com.intellij.plugins.haxe.profiler.bridge.HaxeIuProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.bridge.HaxeLiveCaptures;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurations;
import com.intellij.plugins.haxe.profiler.hxt.HxtZoneRecompressor;
import com.intellij.plugins.haxe.profiler.hxt.HxtZoneWriter;
import com.intellij.plugins.haxe.profiler.tracy.TracyEventReader;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import com.intellij.plugins.haxe.profiler.tracy.TracySourceLocation;
import com.intellij.plugins.haxe.profiler.tracy.connect.*;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The IU-side Tracy receiver: allocates the port the client will listen on
 * (handed to the process via TRACY_PORT), listens for the client's
 * broadcast to learn its protocol version, connects out with retries while
 * the process lives (offering the pinned version, else the announced one
 * and then the probe ladder), and spools the session into an HXTS zone
 * capture while it streams. The session opens in its profiler tab, or a
 * notification explains why nothing was captured.
 */
public class HaxeIuTracyCapture implements HaxeTracyCapture {

  private static final Logger LOG = Logger.getInstance(HaxeIuTracyCapture.class);

  @Override
  public @Nullable Handle start(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
                                @NotNull Executor executor) {
    int port;
    try (ServerSocket probe = new ServerSocket(0)) {
      port = probe.getLocalPort();
    }
    catch (IOException e) {
      LOG.warn("could not allocate a tracy port", e);
      return null;
    }
    return new Capture(project, displayName, sessionFile, port, settingsFor(executor));
  }

  /**
   * The settings of the LAUNCHING profiler configuration (each named
   * "hxcpp Tracy" entry is its own executor); the type's first
   * configuration, then a template, for launches carrying none.
   */
  @NotNull
  private static HaxeHxcppTracyProfilerConfigurationState settingsFor(@NotNull Executor executor) {
    if (HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeHxcppTracyProfilerConfigurationState launched) {
      return launched;
    }
    if (HaxeProfilerConfigurations.stateFor(HaxeHxcppTracyProfilerConfigurationType.ID)
          instanceof HaxeHxcppTracyProfilerConfigurationState first) {
      return first;
    }
    return new HaxeHxcppTracyProfilerConfigurationType().getTemplateState();
  }

  private static final class Capture implements Handle {
    private final Project project;
    private final String displayName;
    private final Path sessionFile;
    private final int port;
    private final int finalLevel;
    private final @Nullable TracyProtocolVersion pinnedProtocol;
    private final AtomicBoolean exited = new AtomicBoolean();
    private final AtomicReference<TracyLiveCapture> live = new AtomicReference<>();
    private volatile HaxeProfilerProcessUi.Session session;

    Capture(Project project, String displayName, Path sessionFile, int port,
            HaxeHxcppTracyProfilerConfigurationState settings) {
      this.project = project;
      this.displayName = displayName;
      this.sessionFile = HaxeCaptureFiles.perCaptureSessionPath(sessionFile);
      this.port = port;
      this.finalLevel = settings.getCompressionLevel();
      this.pinnedProtocol = settings.getPinnedProtocol();
      Thread receiver = new Thread(this::receive, "haxe-tracy-capture");
      receiver.setDaemon(true);
      // the profiled app runs concurrently - stay out of its scheduler slots
      receiver.setPriority(Thread.NORM_PRIORITY - 2);
      receiver.start();
    }

    @Override
    public int port() {
      return port;
    }

    @Override
    public void processExited() {
      exited.set(true);
      TracyLiveCapture capture = live.get();
      if (capture != null) {
        // the client waits for this acknowledgement before it lets go
        capture.requestDisconnect();
      }
    }

    private void receive() {
      TracyLiveCapture capture = connectToClient();
      if (capture == null) return;
      // the connection closes with the capture: a session file that fails
      // to open must not leave the client streaming into a socket nobody reads
      try (capture) {
        live.set(capture);
        if (exited.get()) {
          capture.requestDisconnect(); // exit raced the connect
        }
        else {
          session = HaxeIuProfilerProcessUi.open(project, displayName, sessionFile,
                                                 HaxeHxcppTracyProfilerConfigurationType.ID);
        }
        captureSession(capture);
      }
    }

    /**
     * The handshaken connection, or null after the failure was reported: the
     * process died before the client listened, the client refused every
     * protocol version, or the receiver itself broke (a missing item table).
     */
    @Nullable
    private TracyLiveCapture connectToClient() {
      // the broadcast listener runs only for the connect window: a null
      // listener (port not bindable) leaves the probe ladder to find the version
      try (TracyBroadcastListener broadcast = broadcastListener()) {
        TracyLiveCapture capture = TracyLiveCapture.connect(port, strategy(broadcast), () -> !exited.get());
        if (capture == null) {
          notifyNothingCaptured();
          return null;
        }
        LOG.info("tracy capture on protocol " + capture.welcome().protocolVersion());
        return capture;
      }
      catch (TracyProtocolUnsupportedException unsupported) {
        LOG.warn("tracy client refused every protocol version: " + unsupported.refused());
        notifyProtocolUnsupported(unsupported.refused());
        return null;
      }
      catch (IOException e) {
        LOG.warn("tracy connect failed", e);
        notifyNothingCaptured();
        return null;
      }
      catch (IllegalStateException | UncheckedIOException tableLoadFailed) {
        // a version's item table failed to load - a packaging defect, reported rather than left on the thread
        LOG.warn("tracy receiver broken", tableLoadFailed);
        notifyNothingCaptured();
        return null;
      }
    }

    /** No listener when a version is pinned: nothing to detect. */
    @Nullable
    private TracyBroadcastListener broadcastListener() {
      return pinnedProtocol == null ? TracyBroadcastListener.listen(port) : null;
    }

    private void captureSession(TracyLiveCapture capture) {
      // zones spool to the session file AS THEY ARRIVE - a minutes-long
      // capture must never accumulate them on the heap. The live level
      // keeps the receiver's CPU out of the profiled app's way (a
      // configured level BELOW it wins - the user asked for even less);
      // the recompress pass runs after the app exited and the machine is
      // idle again.
      int liveLevel = Math.min(HxtZoneWriter.LIVE_LEVEL, finalLevel);
      long zoneCount;
      HaxeLiveCaptures.Entry liveEntry = null;
      try {
        Files.createDirectories(sessionFile.getParent());
        if (session != null) {
          liveEntry = HaxeLiveCaptures.register(sessionFile);
        }
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(sessionFile))) {
          HxtZoneWriter writer = new HxtZoneWriter(out, 0, liveLevel);
          TracySession session = capture.capture(liveOpening(writer));
          writer.finish(session);
          zoneCount = writer.zoneCount();
        }
      }
      catch (IOException | UncheckedIOException e) {
        LOG.warn("tracy capture failed", e);
        if (liveEntry != null) {
          liveEntry.finished();
        }
        if (session != null) {
          session.failed(HaxeProfilerBundle.message("haxe.profiler.tracy.capture.failed", e.getMessage()));
        }
        else {
          notifyNothingCaptured();
        }
        return;
      }
      // recompress BEFORE the completion rebuild: a store opened on the
      // live-level file keeps its chunk OFFSETS, and the recompressed
      // replacement lays chunks out differently, so a rebuild racing the
      // rewrite would scan the new file at stale offsets (the call chart and
      // frame breakdowns read it lazily). The live view keeps refreshing off
      // the untouched original meanwhile, and its per-tick reopen picks up
      // the swapped file cleanly.
      if (finalLevel > liveLevel) {
        try {
          HxtZoneRecompressor.recompress(sessionFile, finalLevel);
        }
        catch (IOException e) {
          // the live-level file is complete and valid - keep it
          LOG.warn("could not recompress the tracy session", e);
        }
      }
      if (liveEntry != null) {
        liveEntry.finished();
      }
      if (session != null) {
        session.dataReady();
      }
      else {
        notifyCaptured(zoneCount, capture.welcome().protocolVersion());
      }
    }

    @NotNull
    private TracyVersionStrategy strategy(@Nullable TracyBroadcastListener broadcast) {
      if (pinnedProtocol != null) return TracyVersionStrategy.pinned(pinnedProtocol);
      return TracyVersionStrategy.detect(broadcast == null ? () -> null : broadcast::heard);
    }

    /**
     * Wraps the writer's sink to open the LIVE view once real chunks are
     * on disk: the platform parses the partial file and shows the tabs
     * NOW; the live registry entry keeps the chart refreshing.
     */
    private TracyEventReader.ZoneSink liveOpening(HxtZoneWriter writer) {
      return new TracyEventReader.ZoneSink() {
        private boolean opened;

        @Override
        public void zone(int threadId, int depth, long startNs, long endNs, @NotNull TracySourceLocation location) {
          writer.zone(threadId, depth, startNs, endNs, location);
          if (!opened && writer.flushedZones() > 0) {
            opened = true;
            HaxeProfilerProcessUi.Session ui = session;
            if (ui != null) {
              ui.dataReady();
            }
          }
        }

        @Override
        public void series(TracyEventReader.@NotNull SeriesBatch batch) {
          writer.series(batch);
        }

        @Override
        public void finished(long base) {
          writer.finished(base);
        }
      };
    }

    private void notifyCaptured(long zoneCount, TracyProtocolVersion protocol) {
      String protocolLabel = HaxeHxcppTracyProfilerConfigurationType.protocolLabel(protocol);
      String content = HaxeProfilerBundle.message("haxe.profiler.tracy.captured", sessionFile.toString(), zoneCount, protocolLabel);
      HaxeProfilingNotifier.notifySnapshotReady(project, content, sessionFile);
    }

    private void notifyNothingCaptured() {
      HaxeProfilingNotifier.reportNothingCaptured(project, null, HaxeProfilerBundle.message("haxe.profiler.tracy.none"));
    }

    private void notifyProtocolUnsupported(List<TracyProtocolVersion> refused) {
      String offered = refused.stream().map(version -> String.valueOf(version.wire())).collect(Collectors.joining(", "));
      String content = HaxeProfilerBundle.message("haxe.profiler.tracy.protocol.unsupported", offered);
      HaxeProfilingNotifier.group().createNotification(content, NotificationType.ERROR).notify(project);
    }
  }
}
