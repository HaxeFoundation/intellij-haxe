package com.intellij.plugins.haxe.display.transport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The persistent channel speaks the compiler's frames: a request is the
 * argument list in one length-prefixed frame, the answer frame is classified
 * like a socket answer, and a missing answer breaks the channel for good.
 * The test plays the haxe side on the connection it opened to the listener.
 */
@DisplayName("Display protocol: framed channel")
public class FramedDisplayChannelTest {

  private final ExecutorService haxeSide = Executors.newSingleThreadExecutor();
  private ServerSocket listener;
  private Socket haxeSocket;
  private FramedDisplayChannel channel;

  @BeforeEach
  void connect() throws IOException {
    listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    haxeSocket = new Socket(InetAddress.getLoopbackAddress(), listener.getLocalPort());
    channel = new FramedDisplayChannel(listener.accept());
  }

  @AfterEach
  void disconnect() throws IOException {
    channel.close();
    haxeSocket.close();
    listener.close();
    haxeSide.shutdownNow();
  }

  @Test
  @DisplayName("a request is one frame of arguments and the answer frame is classified")
  public void testARequestIsOneFrameOfArgumentsAndTheAnswerFrameIsClassified() throws Exception {
    Future<String> received = answerWith("{\"jsonrpc\":\"2.0\"}\n\u0002\n");

    DisplayResponse response = channel.request(List.of("--cwd", "/p", "Main"), 2_000);

    assertEquals("--cwd\n/p\nMain", received.get());
    assertEquals("{\"jsonrpc\":\"2.0\"}", response.payload());
    assertTrue(response.hasError(), "the 0x02 marker line is read off the frame like off a socket");
    assertFalse(channel.isBroken());
  }

  @Test
  @DisplayName("an answer that never comes breaks the channel for good")
  public void testAnAnswerThatNeverComesBreaksTheChannelForGood() {
    Future<String> received = answerWith(null);

    DisplayRequestException timeout = assertThrows(DisplayRequestException.class, () -> channel.request(List.of("Main"), 200));
    DisplayRequestException afterwards = assertThrows(DisplayRequestException.class, () -> channel.request(List.of("Main"), 200));

    assertFalse(timeout.isCompilerAnswered(), "a missing answer is a transport failure, not a compiler verdict");
    assertTrue(channel.isBroken());
    assertTrue(afterwards.getMessage().contains("closed"));
    received.cancel(true);
  }

  /** Reads one request frame on the haxe side and answers with the body, or stays silent for a null body. */
  private Future<String> answerWith(String answerBody) {
    return haxeSide.submit(() -> {
      byte[] request = FramedDisplayChannel.readFrame(new DataInputStream(haxeSocket.getInputStream()));
      if (answerBody != null) {
        FramedDisplayChannel.writeFrame(haxeSocket.getOutputStream(), answerBody.getBytes(StandardCharsets.UTF_8));
      }
      return new String(request, StandardCharsets.UTF_8);
    });
  }
}
