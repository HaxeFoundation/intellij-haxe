package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the IDE's startup ordering, which differs from the
 * other integration tests (so it wires the session itself instead of using
 * {@code launchFixture}): the runner constructs the adapter (binding the
 * listener) and spawns the debuggee immediately, but {@code adapter.start}
 * and the DAP conversation only happen later, from sessionInitialized. The
 * debuggee's connect must be parked in the listener's backlog until then.
 */
@DisplayName("HXCPP debugger (vshaxe): start order (integration)")
public class HxcppStartOrderIntegrationTest extends HxcppIntegrationTestBase {

  @Test
  @DisplayName("debuggee connecting before adapter start is accepted")
  public void debuggeeConnectingBeforeAdapterStartIsAccepted() throws Exception {
    String exeProperty = System.getProperty("hxcpp.fixture.exe");
    assumeTrue(exeProperty != null && Files.isRegularFile(Path.of(exeProperty)), "hxcpp fixture exe not built (haxe/hxcpp toolchain missing)");
    Path exe = Path.of(exeProperty);
    int port = Integer.getInteger("hxcpp.fixture.port", 6973);

    // runner order: adapter constructed (listener bound) ...
    adapter = new HxcppDebugAdapter("127.0.0.1", port, TIMEOUT);
    // ... debuggee spawned right away, its connect parked in the backlog ...
    spawnDebuggee(exe);
    // ... and the DAP side only comes up later (sessionInitialized)
    Thread.sleep(2_000);

    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket("127.0.0.1", dapListener.getLocalPort());
    adapter.start(new DapConnection(dapListener.accept()));
    dapClient = new DapClient(new DapConnection(clientSide));

    dapClient.sendRequest(new InitializeRequest(), TIMEOUT);
    dapClient.pollEvent(TIMEOUT); // initialized event
    Response launch = dapClient.sendRequest(new LaunchRequest(), TIMEOUT);
    assertTrue(launch.isSuccess(), "launch failed: " + launch.getMessage());
  }
}
