package com.intellij.plugins.haxe.profiler.bridge.hxcpp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.HaxeProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.HaxeProfilingNotifier;
import com.intellij.plugins.haxe.profiler.HaxeTelemetryCapture;
import com.intellij.plugins.haxe.profiler.bridge.flash.FlashTelemetryConfig;
import com.intellij.plugins.haxe.profiler.bridge.flash.HaxeFlashProfilerConfigurationType;
import com.intellij.plugins.haxe.profiler.bridge.HaxeCaptureFiles;
import com.intellij.plugins.haxe.profiler.flash.FlashTelemetryTranscoder;
import com.intellij.plugins.haxe.profiler.bridge.HaxeIuProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.bridge.HaxeLiveCaptures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The IU-side telemetry receiver: one ephemeral loopback listener per
 * profiled run, spooling the collector's stream to the session file. The
 * hxcpp collector already speaks HXTS, so its bytes land verbatim; the
 * flash lane receives the runtime's own Scout telemetry (pointed here via
 * {@code ~/.telemetry.cfg}) and transcodes it to HXTS on the way to disk.
 * The exit notification offers to open the session in the profiler, or
 * explains that nothing arrived.
 */
public class HaxeIuTelemetryCapture implements HaxeTelemetryCapture {

  private static final Logger LOG = Logger.getInstance(HaxeIuTelemetryCapture.class);
  /** A session smaller than its header carries no frame worth opening. */
  private static final int MINIMUM_SESSION_BYTES = 32;
  /** Enough spooled bytes to open the LIVE view: real frames are flowing by then. */
  private static final int LIVE_OPEN_BYTES = 4096;
  /** Waiting for a debuggee that never connects must not pin the listener forever. */
  private static final int ACCEPT_TIMEOUT_MS = 10 * 60 * 1000;
  private static final long DRAIN_TIMEOUT_SECONDS = 3;

  @Override
  public @Nullable Handle start(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
                                HaxeProfilableRunConfiguration.@NotNull Lane lane) {
    try {
      return new Capture(project, displayName, sessionFile, lane);
    }
    catch (IOException e) {
      LOG.warn("could not open a telemetry listener", e);
      return null;
    }
  }

  /** The profiler entry a lane's sessions display under. */
  private static String configurationTypeIdFor(HaxeProfilableRunConfiguration.Lane lane) {
    return lane == HaxeProfilableRunConfiguration.Lane.FLASH
           ? HaxeFlashProfilerConfigurationType.ID
           : HaxeHxcppProfilerConfigurationType.ID;
  }

  /** Why nothing may have arrived, per lane: the flash sampler needs the debugger runtime, hxcpp the telemetry build. */
  private static String noneMessageFor(HaxeProfilableRunConfiguration.Lane lane) {
    return HaxeProfilerBundle.message(lane == HaxeProfilableRunConfiguration.Lane.FLASH
                                      ? "haxe.profiler.flash.none"
                                      : "haxe.profiler.telemetry.none");
  }

  private static final class Capture implements Handle {
    private final Project project;
    private final String displayName;
    private final Path sessionFile;
    private final HaxeProfilableRunConfiguration.Lane lane;
    private final ServerSocket listener;
    private final FlashTelemetryConfig telemetryConfig;
    private final CountDownLatch drained = new CountDownLatch(1);
    private volatile long receivedBytes;
    private volatile HaxeProfilerProcessUi.Session session;
    private volatile HaxeLiveCaptures.Entry liveEntry;
    private boolean liveOpened;

    Capture(Project project, String displayName, Path sessionFile,
            HaxeProfilableRunConfiguration.Lane lane) throws IOException {
      this.project = project;
      this.displayName = displayName;
      this.sessionFile = HaxeCaptureFiles.perCaptureSessionPath(sessionFile);
      this.lane = lane;
      listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      listener.setSoTimeout(ACCEPT_TIMEOUT_MS);
      // the flash runtime finds the receiver through the global telemetry
      // config, not a launch argument - installed before adl starts
      telemetryConfig = lane == HaxeProfilableRunConfiguration.Lane.FLASH
                        ? new FlashTelemetryConfig(listener.getLocalPort())
                        : null;
      if (telemetryConfig != null) {
        telemetryConfig.install();
      }
      Thread spooler = new Thread(this::spool, "haxe-telemetry-capture");
      spooler.setDaemon(true);
      spooler.start();
    }

    @Override
    public int port() {
      return listener.getLocalPort();
    }

    /** Accepts the single collector connection and spools its stream to the session file. */
    private void spool() {
      try (ServerSocket server = listener;
           Socket client = server.accept();
           InputStream in = client.getInputStream()) {
        session = HaxeIuProfilerProcessUi.open(project, displayName, sessionFile, configurationTypeIdFor(lane));
        liveEntry = HaxeLiveCaptures.register(sessionFile);
        Files.createDirectories(sessionFile.getParent());
        try (OutputStream out = Files.newOutputStream(sessionFile)) {
          pump(in, liveOpening(out));
        }
        // an EOF long before process exit means the collector died - its
        // stderr in the run console names the reason
        LOG.info("telemetry stream ended after " + receivedBytes + " bytes");
      }
      catch (SocketTimeoutException neverConnected) {
        // the exit notification explains it
      }
      catch (IOException e) {
        // a closed listener after process exit is the normal no-connection path
        if (receivedBytes > 0) LOG.warn("telemetry stream ended abnormally", e);
      }
      finally {
        if (telemetryConfig != null) {
          telemetryConfig.restore();
        }
        drained.countDown();
      }
    }

    /** Flash arrives as the runtime's telemetry and transcodes to HXTS; the hxcpp collector's bytes already are it. */
    private void pump(InputStream in, OutputStream out) throws IOException {
      if (lane == HaxeProfilableRunConfiguration.Lane.FLASH) {
        FlashTelemetryTranscoder.transcode(in, out);
        return;
      }
      byte[] buffer = new byte[64 * 1024];
      int read;
      while ((read = in.read(buffer)) >= 0) {
        out.write(buffer, 0, read);
        out.flush(); // the live view re-reads the file while it grows
      }
    }

    /** Counts what lands in the FILE (transcoded for flash) and opens the live view once real frames flow. */
    private OutputStream liveOpening(OutputStream out) {
      return new FilterOutputStream(out) {
        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
          out.write(bytes, offset, length);
          received(length);
        }

        @Override
        public void write(int value) throws IOException {
          out.write(value);
          received(1);
        }
      };
    }

    private void received(int count) {
      receivedBytes += count;
      if (!liveOpened && receivedBytes >= LIVE_OPEN_BYTES) {
        // the platform parses the partial file and shows the tabs NOW;
        // the live registry entry keeps the chart refreshing
        liveOpened = true;
        session.dataReady();
      }
    }

    @Override
    public void processExited() {
      try {
        // the collector's stop() flushes and closes right before Sys.exit -
        // give the last records a moment to arrive
        if (!drained.await(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          listener.close();
          drained.await(1, TimeUnit.SECONDS);
        }
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      catch (IOException ignored) {
      }
      if (telemetryConfig != null) {
        telemetryConfig.restore();
      }
      notifyOutcome();
    }

    private void notifyOutcome() {
      // the file is final: the live view does its last refresh and the
      // component rebuilds with the complete data
      HaxeLiveCaptures.Entry live = liveEntry;
      if (live != null) {
        live.finished();
      }
      if (receivedBytes < MINIMUM_SESSION_BYTES) {
        HaxeProfilingNotifier.reportNothingCaptured(project, session, noneMessageFor(lane));
        return;
      }
      if (session != null) {
        session.dataReady();
        return;
      }
      long receivedKb = receivedBytes / 1024;
      String content = HaxeProfilerBundle.message("haxe.profiler.telemetry.captured", sessionFile.toString(), receivedKb);
      HaxeProfilingNotifier.notifySnapshotReady(project, content, sessionFile);
    }
  }
}
