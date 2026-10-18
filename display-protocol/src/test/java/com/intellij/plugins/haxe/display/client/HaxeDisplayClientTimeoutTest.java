package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Whole-context requests carry the caller's read timeout; every other request keeps the client's default. */
@DisplayName("Display protocol: client read timeouts")
public class HaxeDisplayClientTimeoutTest {

  private static final int DEFAULT_TIMEOUT_MS = 1_000;
  private static final int CONTEXT_TIMEOUT_MS = 120_000;
  private static final List<String> ARGS = List.of("-main", "Main");

  private final List<Integer> timeouts = new ArrayList<>();
  private final HaxeDisplayClient client = new HaxeDisplayClient(timeoutRecordingChannel(), DEFAULT_TIMEOUT_MS);

  @Test
  @DisplayName("project diagnostics pass the caller's timeout")
  public void testProjectDiagnosticsPassTheCallersTimeout() {
    assertThrows(DisplayRequestException.class, () -> client.projectDiagnostics(ARGS, CONTEXT_TIMEOUT_MS));
    assertThrows(DisplayRequestException.class, () -> client.projectDiagnostics(ARGS));

    assertEquals(List.of(CONTEXT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS), timeouts);
  }

  @Test
  @DisplayName("compiled types pass the caller's timeout")
  public void testCompiledTypesPassTheCallersTimeout() {
    assertThrows(DisplayRequestException.class, () -> client.compiledTypes(ARGS, CONTEXT_TIMEOUT_MS));
    assertThrows(DisplayRequestException.class, () -> client.compiledTypes(ARGS));

    assertEquals(List.of(CONTEXT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS), timeouts);
  }

  /** Records the timeout of every request and answers none of them. */
  private DisplayChannel timeoutRecordingChannel() {
    return new DisplayChannel() {
      @Override
      public DisplayResponse request(List<String> args, int readTimeoutMs) throws DisplayRequestException {
        timeouts.add(readTimeoutMs);
        throw new DisplayRequestException("recording channel answers nothing");
      }

      @Override
      public String describe() {
        return "recording channel";
      }
    };
  }
}
