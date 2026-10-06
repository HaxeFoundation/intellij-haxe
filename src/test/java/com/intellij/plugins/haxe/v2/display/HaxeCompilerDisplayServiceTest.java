package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.display.client.HaxeDisplayClient;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeContextFailures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Compiler services: compiler display service")
public class HaxeCompilerDisplayServiceTest extends HaxeLightFixtureTestCase {

  private static final String CONTAINER_ID = "display-service-test";
  private static final List<String> CONTEXT_ARGS = List.of("-main", "Main");
  private static final String COMPILE_ERROR = "Main.hx:1: characters 7-10 : Type not found : Foo";

  @Override
  protected String getBasePath() {
    // the server is faked on a socket; no test-data directory
    return "";
  }

  @AfterEach
  public void forgetRecordedOutcome() {
    HaxeCompilerDisplayService.getInstance(getProject()).clearCaches();
    HaxeContextFailures.getInstance(getProject()).record(CONTAINER_ID, null);
  }

  @Test
  @DisplayName("warm-up compile errors are recorded for the container")
  public void testWarmUpCompileErrorsAreRecordedForTheContainer() throws IOException {
    try (OneReplyServer server = new OneReplyServer(COMPILE_ERROR + "\n\u0002\n")) {
      boolean compiled = displayService().ensureContextCompiled(connected(server), "warm-up-errors", CONTAINER_ID);

      assertFalse(compiled, "a context whose compile reports errors is not marked compiled");
      String expected = HaxeBundle.message("haxe.display.context.compile.failed", COMPILE_ERROR);
      assertEquals(expected, failures().lastFailure(CONTAINER_ID));
    }
  }

  @Test
  @DisplayName("successful warm-up compile clears the container failure")
  public void testSuccessfulWarmUpCompileClearsTheContainerFailure() throws IOException {
    failures().record(CONTAINER_ID, "an earlier failure");

    try (OneReplyServer server = new OneReplyServer("")) {
      boolean compiled = displayService().ensureContextCompiled(connected(server), "warm-up-clean", CONTAINER_ID);

      assertTrue(compiled);
      assertNull(failures().lastFailure(CONTAINER_ID));
    }
  }

  @Test
  @DisplayName("unreachable server is recorded for the container")
  public void testUnreachableServerIsRecordedForTheContainer() throws IOException {
    int closedPort;
    try (ServerSocket probe = new ServerSocket(0)) {
      closedPort = probe.getLocalPort();
    }
    HaxeDisplayClient client = new HaxeDisplayClient(HaxeCompilationServerManager.SERVER_HOST, closedPort);
    var connected = new HaxeCompilerDisplayService.Connected(client, CONTEXT_ARGS, closedPort);

    boolean compiled = displayService().ensureContextCompiled(connected, "warm-up-unreachable", CONTAINER_ID);

    assertFalse(compiled);
    assertNotNull(failures().lastFailure(CONTAINER_ID), "a transport failure is a context failure too");
  }

  @Test
  @DisplayName("compiled context is remembered until the caches are cleared")
  public void testCompiledContextIsRememberedUntilTheCachesAreCleared() throws IOException {
    try (OneReplyServer clean = new OneReplyServer("")) {
      assertTrue(displayService().ensureContextCompiled(connected(clean), "remembered", CONTAINER_ID));
    }

    try (OneReplyServer broken = new OneReplyServer(COMPILE_ERROR + "\n\u0002\n")) {
      boolean compiled = displayService().ensureContextCompiled(connected(broken), "remembered", CONTAINER_ID);
      assertTrue(compiled, "a remembered context does not compile again, so a changed configuration stays hidden");
      assertNull(failures().lastFailure(CONTAINER_ID));
    }

    displayService().clearCaches();
    try (OneReplyServer broken = new OneReplyServer(COMPILE_ERROR + "\n\u0002\n")) {
      boolean compiled = displayService().ensureContextCompiled(connected(broken), "remembered", CONTAINER_ID);
      assertFalse(compiled, "after clearing, the next request compiles again and sees the failure");
      assertNotNull(failures().lastFailure(CONTAINER_ID));
    }
  }

  private HaxeCompilerDisplayService displayService() {
    return HaxeCompilerDisplayService.getInstance(getProject());
  }

  private HaxeContextFailures failures() {
    return HaxeContextFailures.getInstance(getProject());
  }

  private static HaxeCompilerDisplayService.Connected connected(OneReplyServer server) {
    HaxeDisplayClient client = new HaxeDisplayClient(HaxeCompilationServerManager.SERVER_HOST, server.port());
    return new HaxeCompilerDisplayService.Connected(client, CONTEXT_ARGS, server.port());
  }

  /// A stand-in for a `haxe --wait` server: it serves one connection, reads
  /// the request up to its null terminator, writes the reply and closes.
  private static final class OneReplyServer implements AutoCloseable {

    private final ServerSocket socket = new ServerSocket(0);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    OneReplyServer(String reply) throws IOException {
      byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
      // a Callable, so the serving thread may throw
      executor.submit(() -> {
        serve(bytes);
        return null;
      });
    }

    int port() {
      return socket.getLocalPort();
    }

    private void serve(byte[] reply) throws IOException {
      try (Socket connection = socket.accept()) {
        drainRequest(connection.getInputStream());
        OutputStream out = connection.getOutputStream();
        out.write(reply);
        out.flush();
      }
    }

    private static void drainRequest(InputStream in) throws IOException {
      while (in.read() > 0) {
        // the request ends at its null terminator
      }
    }

    @Override
    public void close() throws IOException {
      socket.close();
      executor.shutdownNow();
    }
  }
}
