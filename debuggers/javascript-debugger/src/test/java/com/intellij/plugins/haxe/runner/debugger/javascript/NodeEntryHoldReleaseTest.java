package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEventBody;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ContinueRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Browser debugger: node entry hold release")
public class NodeEntryHoldReleaseTest {

  @Test
  @DisplayName("the hold stop is resumed and never reaches the session")
  public void theHoldStopIsResumedAndNeverReachesTheSession() throws Exception {
    StoppedEvent breakpoint = stopped("breakpoint", 7);
    ScriptedSession session = new ScriptedSession(stopped("step", 7), breakpoint);
    NodeEntryHoldRelease release = new NodeEntryHoldRelease(session);

    Event first = release.pollEvent(10);
    assertNull(first);
    assertEquals(List.of(7), session.continuedThreads());

    Event second = release.pollEvent(10);
    assertSame(breakpoint, second);
    assertEquals(List.of(7), session.continuedThreads());
  }

  @Test
  @DisplayName("a breakpoint as the first stop passes through")
  public void aBreakpointAsTheFirstStopPassesThrough() throws Exception {
    StoppedEvent breakpoint = stopped("breakpoint", 3);
    ScriptedSession session = new ScriptedSession(breakpoint);
    NodeEntryHoldRelease release = new NodeEntryHoldRelease(session);

    Event first = release.pollEvent(10);

    assertSame(breakpoint, first);
    assertTrue(session.continuedThreads().isEmpty());
  }

  @Test
  @DisplayName("only the first stop is the hold")
  public void onlyTheFirstStopIsTheHold() throws Exception {
    StoppedEvent laterStep = stopped("step", 4);
    ScriptedSession session = new ScriptedSession(stopped("entry", 4), laterStep);
    NodeEntryHoldRelease release = new NodeEntryHoldRelease(session);

    release.pollEvent(10);
    Event second = release.pollEvent(10);

    assertSame(laterStep, second, "a user's step after the hold is a real stop");
    assertEquals(List.of(4), session.continuedThreads());
  }

  @Test
  @DisplayName("a first stop that hit a breakpoint passes through whatever its reason")
  public void aFirstStopThatHitABreakpointPassesThroughWhateverItsReason() throws Exception {
    StoppedEvent heldOnBreakpoint = stopped("step", 2);
    heldOnBreakpoint.getBody().setHitBreakpointIds(List.of(1));
    ScriptedSession session = new ScriptedSession(heldOnBreakpoint);
    NodeEntryHoldRelease release = new NodeEntryHoldRelease(session);

    Event first = release.pollEvent(10);

    assertSame(heldOnBreakpoint, first);
    assertTrue(session.continuedThreads().isEmpty());
  }

  private static StoppedEvent stopped(String reason, int threadId) {
    StoppedEventBody body = new StoppedEventBody();
    body.setReason(reason);
    body.setThreadId(threadId);
    StoppedEvent event = new StoppedEvent();
    event.setBody(body);
    return event;
  }

  /** Hands out the scripted events in order and records the continue requests sent. */
  private static final class ScriptedSession implements DapEndpoint {
    private final Deque<Event> events;
    private final List<Request> sent = new ArrayList<>();

    ScriptedSession(Event... events) {
      this.events = new ArrayDeque<>(List.of(events));
    }

    List<Integer> continuedThreads() {
      return sent.stream()
        .filter(ContinueRequest.class::isInstance)
        .map(request -> ((ContinueRequest)request).getArguments().getThreadId())
        .toList();
    }

    @Override
    public Response sendRequest(Request request, long timeoutMillis) {
      sent.add(request);
      return null;
    }

    @Override
    public void sendRequestNoWait(Request request) {
      sent.add(request);
    }

    @Override
    public Event pollEvent(long timeoutMillis) {
      return events.poll();
    }

    @Override
    public void close() {
    }
  }
}
