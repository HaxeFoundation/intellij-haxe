package com.intellij.plugins.haxe.runner.debugger.dap.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapFraming;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link DapClient} against a fake adapter on a loopback socket. The
 * cases here pin the CONNECTION-DEATH behavior: a request whose response can
 * never arrive must fail promptly with an honest "connection" message — not
 * sleep out its full timeout and then claim the adapter was merely slow.
 */
@DisplayName("DAP protocol: client")
public class DapClientTest {
  private static final long TIMEOUT = 5_000;

  private ServerSocket listener;
  private DapClient client;
  private Socket serverSide;
  private InputStream serverIn;

  @BeforeEach
  public void setUp() throws IOException {
    listener = new ServerSocket(0);
    Socket clientSocket = new Socket("127.0.0.1", listener.getLocalPort());
    serverSide = listener.accept();
    serverIn = serverSide.getInputStream();
    client = new DapClient(new DapConnection(clientSocket));
  }

  @AfterEach
  public void tearDown() throws IOException {
    client.close();
    serverSide.close();
    listener.close();
  }

  @Test
  @DisplayName("in flight request fails fast when the connection dies")
  public void inFlightRequestFailsFastWhenTheConnectionDies() throws Exception {
    // the timeout is deliberately huge: the failure must come from the
    // connection death, not from waiting out the timer
    CompletableFuture<Exception> failure = new CompletableFuture<>();
    Thread caller = new Thread(() -> {
      try {
        client.sendRequest(new ThreadsRequest(), 60_000);
        failure.complete(null);
      } catch (Exception e) {
        failure.complete(e);
      }
    });
    caller.start();
    assertNotNull(DapFraming.readPayload(serverIn), "the request reached the wire");
    serverSide.close(); // the adapter/debuggee dies mid-request

    Exception e = failure.get(2, TimeUnit.SECONDS); // must beat the 60s timer by far
    assertNotNull(e, "the in-flight request must fail, not report success");
    assertTrue(e.getMessage().contains("connection"), "an honest message, not a timeout: " + e.getMessage());
    caller.join(TIMEOUT);
  }

  @Test
  @DisplayName("request after connection death fails immediately")
  public void requestAfterConnectionDeathFailsImmediately() throws Exception {
    serverSide.close();
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (!client.isConnectionFinished() && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertTrue(client.isConnectionFinished(), "reader noticed the death");

    long start = System.currentTimeMillis();
    try {
      client.sendRequest(new ThreadsRequest(), 60_000);
      fail("expected IOException");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("connection"), e.getMessage());
    }
    assertTrue(System.currentTimeMillis() - start < 5_000, "failed by the flag, not the timer");
  }
}
