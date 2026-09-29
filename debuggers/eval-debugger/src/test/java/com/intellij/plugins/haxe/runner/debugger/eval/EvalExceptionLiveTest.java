package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The uncaught-exception STOP itself, pinned for correctness: reason,
 * description text and the throwing line. The stall/promptness behaviour at
 * this stop — the IDE's full reportStopped hydration under timing — is
 * EvalStepExceptionLiveTest's subject.
 */
@DisplayName("Eval debugger: exception (live)")
public class EvalExceptionLiveTest extends EvalLiveTestBase {
  private static final int THROW_LINE = 9;

  @Timeout(60)
  @Test
  @DisplayName("uncaught exception stop carries the description and the throwing line")
  public void uncaughtExceptionStopCarriesTheDescriptionAndTheThrowingLine() throws Exception {
    // the IDE sends exception filters (the backend reports it can't honor
    // them, but the request must still not wedge the session)
    startSession(List.of("uncaught"));

    StoppedEvent stopped = awaitStopped();
    assertEquals("exception", stopped.getBody().getReason(), "stopped for an exception");
    String description = stopped.getBody().getDescription();
    assertTrue(description != null && description.contains("uncaught-boom"), "carries the thrown text");

    assertEquals(THROW_LINE, topFrame(stopped.getBody().getThreadId()).getLine(), "top frame is the throwing line");
  }

  @Override
  protected String fixtureMain() {
    return "EvalThrow";
  }
}
