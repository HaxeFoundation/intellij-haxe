package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEventBody;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ContinueRequest;

import java.io.IOException;
import java.util.Set;

/// Releases node's `--inspect-brk` hold once the debug session is set up.
///
/// js-debug's `continueOnAttach` resumes the hold right after
/// configurationDone, before the breakpoints of a source-mapped file are
/// bound - the map loads asynchronously - so a program whose code runs at
/// startup runs past them. Attached without it, js-debug reports the hold as
/// the session's first stop once the paused location is mapped, by which
/// time the breakpoints are bound; this endpoint resumes that stop itself and
/// keeps it out of the IDE session. Any other first stop (a breakpoint on
/// the held line) passes through.
public final class NodeEntryHoldRelease implements DapEndpoint {
  /// The stop reasons js-debug reports the hold under.
  private static final Set<String> HOLD_REASONS = Set.of("step", "entry", "pause");

  private final DapEndpoint session;
  private volatile boolean firstStopSeen;

  public NodeEntryHoldRelease(DapEndpoint session) {
    this.session = session;
  }

  @Override
  public Response sendRequest(Request request, long timeoutMillis) throws IOException, InterruptedException {
    return session.sendRequest(request, timeoutMillis);
  }

  @Override
  public void sendRequestNoWait(Request request) throws IOException {
    session.sendRequestNoWait(request);
  }

  @Override
  public Event pollEvent(long timeoutMillis) throws InterruptedException {
    Event event = session.pollEvent(timeoutMillis);
    if (firstStopSeen || !(event instanceof StoppedEvent stopped)) return event;
    firstStopSeen = true;
    Integer threadId = holdThreadOf(stopped);
    if (threadId == null) return event;
    try {
      session.sendRequestNoWait(ContinueRequest.of(threadId));
      return null;
    }
    catch (IOException e) {
      // the session is going away; show the stop rather than lose it
      return event;
    }
  }

  /// The held thread when `stopped` is the hold, null for any other stop - a
  /// stop that hit a breakpoint is the user's whatever reason it carries.
  private static Integer holdThreadOf(StoppedEvent stopped) {
    StoppedEventBody body = stopped.getBody();
    if (body == null || !HOLD_REASONS.contains(body.getReason())) return null;
    boolean hitBreakpoint = body.getHitBreakpointIds() != null && !body.getHitBreakpointIds().isEmpty();
    return hitBreakpoint ? null : body.getThreadId();
  }

  @Override
  public void close() throws IOException {
    session.close();
  }
}
