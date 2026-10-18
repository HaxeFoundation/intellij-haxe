package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.AdapterProcess;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The vscode-js-debug session plumbing every lane on that adapter shares -
 * a browser page or a node process: connecting to the adapter it spawned,
 * the parent/child session hand-over, relaying the adapter's own output and
 * tearing the session down with the adapter's process tree.
 */
public final class JsDebugSessions {
  private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
  private static final long CONNECT_RETRY_WINDOW_MILLIS = 10_000;
  private static final long PARENT_HANDSHAKE_TIMEOUT_MILLIS = 60_000;

  private JsDebugSessions() {
  }

  /**
   * Drives a js-debug parent session to the child hand-over: initialize,
   * fire-and-forget launch (the response is deferred past configurationDone),
   * configurationDone on the initialized event, then the startDebugging
   * reverse request carries the child configuration (__pendingTargetId).
   * The node backend's parent config is an attach.
   */
  public static Map<String, Object> runParentHandshake(DapClient parent, String adapterId, Map<String, Object> parentConfig)
    throws IOException, InterruptedException {
    JsDebugHandshake.initialize(parent, adapterId, CONNECT_TIMEOUT_MILLIS);
    parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentConfig));

    long deadline = System.currentTimeMillis() + PARENT_HANDSHAKE_TIMEOUT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      Event event = parent.pollEvent(100);
      if (event instanceof InitializedEvent) {
        parent.sendRequest(new ConfigurationDoneRequest(), CONNECT_TIMEOUT_MILLIS);
      }
      Request incoming = parent.pollIncomingRequest(50);
      if (incoming != null) {
        parent.respond(incoming, true);
        if (incoming instanceof StartDebuggingRequest start
            && start.getArguments() != null && start.getArguments().getConfiguration() != null) {
          return start.getArguments().getConfiguration();
        }
      }
    }
    throw new IOException("js-debug never requested the child debug session"
                          + " (no startDebugging within " + PARENT_HANDSHAKE_TIMEOUT_MILLIS / 1000 + "s)");
  }

  /** A js-debug session's parts: the adapter, the parent connection, the child configuration the parent handed over and the mux over the first child. */
  public record Session(AdapterProcess adapter, DapClient parent, Map<String, Object> childConfiguration, JsDebugSessionMux mux) {
  }

  /**
   * Starts js-debug on the node executable, runs the parent handshake for
   * the lane's configuration and wires the first child (page or process) into
   * a mux - the sequence the browser and node backends share. The adapter is
   * torn down again when any step fails.
   */
  public static Session open(Path nodeExecutable, Path dapServerJs, String adapterId, Map<String, Object> parentConfig) throws IOException {
    AdapterProcess adapter = JsAdapterLauncher.launchJsDebug(nodeExecutable, dapServerJs);
    DapClient parent = null;
    try {
      parent = connectWithRetry(adapter.port());
      Map<String, Object> childConfig = parentHandshake(parent, adapterId, parentConfig);
      DapClient child = connectWithRetry(adapter.port());
      return new Session(adapter, parent, childConfig, new JsDebugSessionMux(parent, child, adapter.port(), adapterId));
    } catch (IOException e) {
      closeSession(null, parent, adapter.process());
      throw e;
    }
  }

  private static Map<String, Object> parentHandshake(DapClient parent, String adapterId, Map<String, Object> parentConfig) throws IOException {
    try {
      return runParentHandshake(parent, adapterId, parentConfig);
    } catch (IOException e) {
      throw new IOException("The js-debug parent session failed: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while starting the js-debug session", e);
    }
  }

  // The adapters announce their port slightly BEFORE the listener accepts;
  // retry inside a short window instead of failing the session.
  public static DapClient connectWithRetry(int port) throws IOException {
    return DapClient.connectWithRetry("127.0.0.1", port, CONNECT_TIMEOUT_MILLIS, CONNECT_RETRY_WINDOW_MILLIS);
  }

  /** Relays the adapter's own stdout and the mux's log into the console; a backend hands in its reader and mux. */
  public static void startBackgroundOutput(@Nullable BufferedReader adapterStdout, @Nullable JsDebugSessionMux mux, Consumer<String> sink) {
    if (adapterStdout != null) {
      AdapterProcess.drainOutput(adapterStdout, sink);
    }
    if (mux != null) {
      // the mux owns the parent pumping and worker attachment
      mux.setLogSink(line -> sink.accept("[js-debug] " + line));
    }
  }

  /**
   * Tears a js-debug session down: the mux (page or process, workers AND the
   * parent connection), else the parent alone (startup failed before the mux
   * existed), then the adapter's WHOLE process tree - killing node does not
   * kill the browser it spawned, and when the graceful DAP disconnect did not
   * happen (forced teardown) every session would otherwise leak a headless
   * browser.
   */
  public static void closeSession(@Nullable JsDebugSessionMux mux, @Nullable DapClient parent, @Nullable Process adapter) {
    if (mux != null) {
      try {
        mux.close();
      } catch (IOException ignored) {
      }
    }
    else if (parent != null) {
      try {
        parent.close();
      } catch (IOException ignored) {
      }
    }
    if (adapter != null) {
      adapter.descendants().forEach(ProcessHandle::destroyForcibly);
      adapter.destroy();
      try {
        if (!adapter.waitFor(AdapterProcess.KILL_WAIT_SECONDS, TimeUnit.SECONDS)) {
          adapter.destroyForcibly();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
