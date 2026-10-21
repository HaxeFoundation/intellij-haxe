package com.intellij.plugins.haxe.display.transport;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The channel over a {@code haxe --wait stdio} process: requests go to its
 * stdin as frames, answer frames come from its stderr, and an overdue answer
 * ends the process, since a pipe read has no timeout of its own. The test
 * plays the process on in-memory pipes; ending it closes its stderr.
 */
@DisplayName("Display protocol: framed stdio channel")
public class FramedStdioChannelTest {

  private final ExecutorService haxeSide = Executors.newSingleThreadExecutor();
  private final AtomicBoolean processEnded = new AtomicBoolean();
  private PipedInputStream haxeStdin;
  private PipedOutputStream haxeStderr;
  private FramedDisplayChannel channel;

  @BeforeEach
  void startProcess() throws IOException {
    PipedOutputStream requests = new PipedOutputStream();
    haxeStdin = new PipedInputStream(requests);
    haxeStderr = new PipedOutputStream();
    PipedInputStream answers = new PipedInputStream(haxeStderr);
    channel = FramedDisplayChannel.overStdio(answers, requests, this::endProcess, "haxe 3 stdio");
  }

  @AfterEach
  void stopProcess() {
    channel.close();
    haxeSide.shutdownNow();
  }

  @Test
  @DisplayName("a request goes to stdin and the stderr frame is the answer")
  public void testARequestGoesToStdinAndTheStderrFrameIsTheAnswer() throws Exception {
    Future<String> received = answerWith("<list>\n</list>\n");

    DisplayResponse response = channel.request(List.of("--display", "Main.hx@0"), 2_000);

    assertEquals("--display\nMain.hx@0", received.get());
    assertEquals("<list>\n</list>", response.payload().strip());
    assertFalse(response.hasError());
    assertFalse(channel.isBroken());
    assertFalse(processEnded.get());
  }

  @Test
  @DisplayName("an overdue answer ends the process and breaks the channel")
  public void testAnOverdueAnswerEndsTheProcessAndBreaksTheChannel() {
    Future<String> received = answerWith(null);

    DisplayRequestException timeout = assertThrows(DisplayRequestException.class, () -> channel.request(List.of("Main"), 200));

    assertFalse(timeout.isCompilerAnswered(), "a missing answer is a transport failure, not a compiler verdict");
    assertTrue(processEnded.get(), "only the end of the process ends the blocked pipe read");
    assertTrue(channel.isBroken());
    received.cancel(true);
  }

  /** Reads one request frame from the process's stdin and answers on its stderr, or stays silent for a null body. */
  private Future<String> answerWith(String answerBody) {
    return haxeSide.submit(() -> {
      byte[] request = FramedDisplayChannel.readFrame(new DataInputStream(haxeStdin));
      if (answerBody != null) {
        FramedDisplayChannel.writeFrame(haxeStderr, answerBody.getBytes(StandardCharsets.UTF_8));
      }
      return new String(request, StandardCharsets.UTF_8);
    });
  }

  private void endProcess() throws IOException {
    processEnded.set(true);
    haxeStderr.close();
  }
}
