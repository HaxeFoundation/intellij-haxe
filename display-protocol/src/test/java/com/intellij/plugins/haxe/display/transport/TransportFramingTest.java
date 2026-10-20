package com.intellij.plugins.haxe.display.transport;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static com.intellij.plugins.haxe.display.transport.FakeDisplayServer.ERROR_MARK;
import static com.intellij.plugins.haxe.display.transport.FakeDisplayServer.LOG_MARK;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Display protocol: transport framing")
public class TransportFramingTest {

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
  @DisplayName("request is newline separated args with a null terminator")
  public void requestIsNewlineSeparatedArgsWithANullTerminator() throws Exception {
    Future<byte[]> received = server.replyOnce("{}\n".getBytes(StandardCharsets.UTF_8));
    List<String> args = List.of("--cwd", "/work", "--display", "{\"a\":1}");

    HaxeDisplayTransport.request("127.0.0.1", server.port(), args, 5_000);

    String request = new String(received.get(), StandardCharsets.UTF_8);
    assertEquals("--cwd\n/work\n--display\n{\"a\":1}\n", request);
  }

  @Test
  @Timeout(10)
  @DisplayName("response lines classify into payload, logs and error marker")
  public void responseLinesClassifyIntoPayloadLogsAndErrorMarker() throws Exception {
    // one log line whose embedded newline arrives as a second 0x01 byte,
    // the JSON payload, and the fatal-error marker line
    String reply = """
      %sa log line%swith continuation
      {"jsonrpc":"2.0"}
      %s
      """.formatted(LOG_MARK, LOG_MARK, ERROR_MARK);
    server.replyOnce(reply.getBytes(StandardCharsets.UTF_8));

    DisplayResponse response = HaxeDisplayTransport.request("127.0.0.1", server.port(), List.of("Main"), 5_000);

    assertEquals("{\"jsonrpc\":\"2.0\"}", response.payload());
    assertEquals(List.of("a log line\nwith continuation"), response.logs());
    assertTrue(response.hasError());
  }

  @Test
  @Timeout(10)
  @DisplayName("plain compiler error output becomes payload with the error flag")
  public void plainCompilerErrorOutputBecomesPayloadWithTheErrorFlag() throws Exception {
    String reply = "Main.hx:7: characters 3-15 : Unknown identifier : unknownIdent\n" + ERROR_MARK + "\n";
    server.replyOnce(reply.getBytes(StandardCharsets.UTF_8));

    DisplayResponse response = HaxeDisplayTransport.request("127.0.0.1", server.port(), List.of("Main"), 5_000);

    assertTrue(response.hasError());
    assertEquals("Main.hx:7: characters 3-15 : Unknown identifier : unknownIdent", response.payload());
  }
}
