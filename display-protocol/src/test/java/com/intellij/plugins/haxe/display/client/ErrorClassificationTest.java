package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.FakeDisplayServer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static com.intellij.plugins.haxe.display.transport.FakeDisplayServer.ERROR_MARK;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins how the client classifies a non-JSON payload: with the fatal-error
 * marker it is the compiler's own error report (a failed compile), without
 * it a genuinely malformed response.
 */
@DisplayName("Display protocol: error classification")
public class ErrorClassificationTest {

  private FakeDisplayServer server;

  @BeforeEach
  void setUp() throws Exception {
    server = new FakeDisplayServer();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.close();
  }

  @Test
  @Timeout(10)
  @DisplayName("compiler error payload reports the request as failed")
  public void compilerErrorPayloadReportsTheRequestAsFailed() {
    String reply = "Main.hx:7: characters 3-15 : Unknown identifier : unknownIdent\n" + ERROR_MARK + "\n";
    server.replyOnce(reply.getBytes(StandardCharsets.UTF_8));
    HaxeDisplayClient client = new HaxeDisplayClient("127.0.0.1", server.port());

    DisplayRequestException e =
      assertThrows(DisplayRequestException.class, () -> client.projectDiagnostics(List.of("Main")));

    String message = e.getMessage();
    assertTrue(message.startsWith("Display request 'display/diagnostics' failed:"), message);
    assertTrue(message.contains("Unknown identifier : unknownIdent"), message);
    assertFalse(message.contains("Malformed"), message);
  }

  @Test
  @Timeout(10)
  @DisplayName("unparseable payload without the error flag stays malformed")
  public void unparseablePayloadWithoutTheErrorFlagStaysMalformed() {
    server.replyOnce("not json at all\n".getBytes(StandardCharsets.UTF_8));
    HaxeDisplayClient client = new HaxeDisplayClient("127.0.0.1", server.port());

    DisplayRequestException e =
      assertThrows(DisplayRequestException.class, () -> client.projectDiagnostics(List.of("Main")));

    assertTrue(e.getMessage().startsWith("Malformed display response:"), e.getMessage());
  }
}
