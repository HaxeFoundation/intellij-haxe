package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

/**
 * Regression test for the IDE's startup ordering, which differs from the
 * other integration test: the runner constructs the adapter (binding the
 * listener) and spawns the debuggee immediately, but {@code adapter.start}
 * and the DAP conversation only happen later, from sessionInitialized. The
 * debuggee's connect must be parked in the listener's backlog until then.
 */
public class HxcppStartOrderIntegrationTest {
  private static final long TIMEOUT = 15_000;

  @Test
  public void debuggeeConnectingBeforeAdapterStartIsAccepted() throws Exception {
    String exeProperty = System.getProperty("hxcpp.fixture.exe");
    assumeTrue("hxcpp fixture exe not built (haxe/hxcpp toolchain missing)",
               exeProperty != null && Files.isRegularFile(Path.of(exeProperty)));
    Path exe = Path.of(exeProperty);
    int port = Integer.getInteger("hxcpp.fixture.port", 6973);

    // runner order: adapter constructed (listener bound) ...
    HxcppDebugAdapter adapter = new HxcppDebugAdapter("127.0.0.1", port, TIMEOUT);
    // ... debuggee spawned right away ...
    Process debuggee = new ProcessBuilder(exe.toString())
      .directory(exe.getParent().toFile()).redirectErrorStream(true).start();
    Thread gobbler = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(debuggee.getInputStream()))) {
        while (reader.readLine() != null) {
          // drained; content is irrelevant here
        }
      } catch (Exception ignored) {
      }
    });
    gobbler.setDaemon(true);
    gobbler.start();

    // ... and the DAP side only comes up later (sessionInitialized)
    Thread.sleep(2_000);

    try {
      ServerSocket dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      try (Socket clientSide = new Socket("127.0.0.1", dapListener.getLocalPort())) {
        adapter.start(new DapConnection(dapListener.accept()));
        try (DapClient client = new DapClient(new DapConnection(clientSide))) {
          client.sendRequest(new InitializeRequest(), TIMEOUT);
          client.pollEvent(TIMEOUT); // initialized event
          Response launch = client.sendRequest(new LaunchRequest(), TIMEOUT);
          assertTrue("launch failed: " + launch.getMessage(), launch.isSuccess());
        }
      } finally {
        dapListener.close();
      }
    } finally {
      debuggee.destroyForcibly();
      adapter.close();
    }
  }
}
