package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Presents js-debug's MULTI-SESSION model (one DAP child session per target:
 * the page, each web/service worker) as a SINGLE {@link DapEndpoint}, so the
 * IDE debug process stays a plain single-session client and workers appear as
 * additional THREADS of the one debug session.
 *
 * How: every child session gets an index; ids the wire hands out (threadId,
 * frameId, variablesReference, breakpoint id) are COMPOSITED with the session
 * index in the high bits (the page session, index 0, passes through
 * unchanged). Requests carrying an id are routed to the owning session and
 * the id un-composited; responses and events are rewritten on the way out.
 * Breakpoints and exception filters are BROADCAST to every session, cached,
 * and replayed to workers as they attach. A worker's termination removes its
 * session quietly (a worker exiting is not the end of the debug session);
 * only the PAGE session's termination ends the run.
 *
 * New targets announce themselves through the {@code startDebugging} REVERSE
 * request on ANY session's connection (the parent announces the page; the
 * page session announces its workers); the mux answers, opens a new
 * connection with the handed-over {@code __pendingTargetId} configuration,
 * and runs the child handshake itself — initialize, fire-and-forget launch,
 * configurationDone on initialized, cached breakpoints in between.
 *
 * Pure protocol code (no IDE types) so the live probes exercise it headless
 * against the real adapter.
 */
public final class JsDebugSessionMux implements DapEndpoint {
  /** Session index lives above this bit; raw ids must stay below it. */
  private static final int SESSION_SHIFT = 24;
  private static final int RAW_MASK = (1 << SESSION_SHIFT) - 1;
  /**
   * Highest index the remaining bits can hold. One past it, the shift
   * overflows to zero and the session's ids become indistinguishable from the
   * PAGE's - silent misrouting, so attaching stops here instead.
   */
  private static final int MAX_SESSION_INDEX = (1 << (Integer.SIZE - SESSION_SHIFT)) - 1;
  private static final long CHILD_TIMEOUT_MILLIS = 10_000;
  private static final long PUMP_POLL_MILLIS = 200;

  /** One child session: its connection and liveness. */
  private static final class ChildSession {
    final int index;
    final DapClient client;
    final String label;
    volatile boolean gone;

    ChildSession(int index, DapClient client, String label) {
      this.index = index;
      this.client = client;
      this.label = label;
    }
  }

  /**
   * One requested breakpoint's verification across all sessions. The IDE sees
   * ONE breakpoint (addressed by the page session's adapter id); each session
   * votes verified/unverified, and the breakpoint presents as verified when
   * ANY live session verified it — a worker-source breakpoint is unverifiable
   * in the page session but real once the worker loads the script.
   */
  private static final class TrackedBreakpoint {
    volatile Integer pageId;
    final Map<Integer, Boolean> verifiedBySession = new ConcurrentHashMap<>();

    boolean mergedVerified() {
      return verifiedBySession.containsValue(Boolean.TRUE);
    }
  }

  private final DapClient parent;
  private final int adapterPort;
  private final Map<Integer, ChildSession> sessions = new ConcurrentHashMap<>();
  private final AtomicInteger nextSessionIndex = new AtomicInteger(0);
  private final BlockingQueue<Event> mergedEvents = new LinkedBlockingQueue<>();
  /** Broadcast state replayed to every newly attached session. */
  private final Map<String, SetBreakpointsRequest> breakpointsBySource = new LinkedHashMap<>();
  /** Source path -> the tracked verification state, parallel to the request's list. */
  private final Map<String, List<TrackedBreakpoint>> trackedBySource = new LinkedHashMap<>();
  /** (session index, that session's adapter breakpoint id) -> tracked breakpoint. */
  private final Map<Long, TrackedBreakpoint> trackedBySessionId = new ConcurrentHashMap<>();
  private volatile SetExceptionBreakpointsRequest exceptionFilters;
  private volatile boolean closed;
  private volatile Consumer<String> logSink = line -> { };

  /**
   * Wraps the already-connected parent and PAGE child session. The page's
   * handshake stays the caller's job (the IDE debug process performs it, as
   * for any single session); the mux only takes over event pumping and
   * later-target attachment.
   */
  public JsDebugSessionMux(DapClient parent, DapClient pageSession, int adapterPort) {
    this.parent = parent;
    this.adapterPort = adapterPort;
    ChildSession page = new ChildSession(nextSessionIndex.getAndIncrement(), pageSession, "page");
    sessions.put(page.index, page);
    startPump(page);
    startParentPump();
  }

  /** Optional sink for the mux's own noteworthy moments (worker attach/exit). */
  public void setLogSink(Consumer<String> sink) {
    logSink = sink != null ? sink : line -> { };
  }

  // ------------------------------------------------------------- id algebra

  private static int composite(int sessionIndex, int rawId) {
    if (sessionIndex == 0) {
      return rawId; // the page's ids pass through untouched
    }
    if ((rawId & ~RAW_MASK) != 0) {
      // a raw id too large to composite: pass through (worst case the request
      // routes to the page session); js-debug's per-pause handles stay small
      return rawId;
    }
    return (sessionIndex << SESSION_SHIFT) | rawId;
  }

  private static int sessionOf(int compositeId) {
    return compositeId >>> SESSION_SHIFT;
  }

  private static int rawOf(int compositeId) {
    return sessionOf(compositeId) == 0 ? compositeId : compositeId & RAW_MASK;
  }

  private ChildSession route(int compositeId) {
    ChildSession session = sessions.get(sessionOf(compositeId));
    return session != null && !session.gone ? session : sessions.get(0);
  }

  // --------------------------------------------------------------- requests

  @Override
  public Response sendRequest(Request request, long timeoutMillis) throws IOException, InterruptedException {
    switch (request) {
      // broadcast configuration; cache it for future workers
      case SetBreakpointsRequest breakpoints -> {
        synchronized (breakpointsBySource) {
          breakpointsBySource.put(sourcePathOf(breakpoints), breakpoints);
        }
        return broadcastBreakpoints(breakpoints, timeoutMillis);
      }
      case SetExceptionBreakpointsRequest filters -> {
        exceptionFilters = filters;
        return broadcast(request, timeoutMillis);
      }
      case DisconnectRequest ignored -> {
        return broadcast(request, timeoutMillis);
      }
      case ThreadsRequest ignored -> {
        return mergedThreads(request, timeoutMillis);
      }

      // requests routed by the id they carry (composited -> session + raw id)
      case StackTraceRequest stackTrace -> {
        return routeRewriting(request, stackTrace.getArguments().getThreadId(),
                              raw -> stackTrace.getArguments().setThreadId(raw), timeoutMillis);
      }
      case ContinueRequest resume -> {
        // routed to the owning session ONLY: another session paused at its own
        // breakpoint stays paused - the IDE holds its stop back and presents
        // it after this resume (DapDebugProcess.pendingStops)
        return routeByThread(resume, resume.getArguments() != null ? resume.getArguments().getThreadId() : null,
                             raw -> resume.getArguments().setThreadId(raw), timeoutMillis);
      }
      case NextRequest next -> {
        return routeByThread(next, next.getArguments() != null ? next.getArguments().getThreadId() : null,
                             raw -> next.getArguments().setThreadId(raw), timeoutMillis);
      }
      case StepInRequest stepIn -> {
        return routeByThread(stepIn, stepIn.getArguments() != null ? stepIn.getArguments().getThreadId() : null,
                             raw -> stepIn.getArguments().setThreadId(raw), timeoutMillis);
      }
      case StepOutRequest stepOut -> {
        return routeByThread(stepOut, stepOut.getArguments() != null ? stepOut.getArguments().getThreadId() : null,
                             raw -> stepOut.getArguments().setThreadId(raw), timeoutMillis);
      }
      case PauseRequest pause -> {
        return routeByThread(pause, pause.getArguments() != null ? pause.getArguments().getThreadId() : null,
                             raw -> pause.getArguments().setThreadId(raw), timeoutMillis);
      }
      case ScopesRequest scopes -> {
        return routeRewriting(request, scopes.getArguments().getFrameId(),
                              raw -> scopes.getArguments().setFrameId(raw), timeoutMillis);
      }
      case VariablesRequest variables -> {
        return routeRewriting(request, variables.getArguments().getVariablesReference(),
                              raw -> variables.getArguments().setVariablesReference(raw), timeoutMillis);
      }
      case SetVariableRequest setVariable -> {
        return routeRewriting(request, setVariable.getArguments().getVariablesReference(),
                              raw -> setVariable.getArguments().setVariablesReference(raw), timeoutMillis);
      }
      case EvaluateRequest evaluate -> {
        Integer frameId = evaluate.getArguments() != null ? evaluate.getArguments().getFrameId() : null;
        ChildSession session = sessionForOptionalId(frameId, raw -> evaluate.getArguments().setFrameId(raw));
        return rewriteResponse(session, session.client.sendRequest(request, timeoutMillis));
      }
      case StepInTargetsRequest targets -> {
        Integer frameId = targets.getArguments() != null ? targets.getArguments().getFrameId() : null;
        ChildSession session = sessionForOptionalId(frameId, raw -> targets.getArguments().setFrameId(raw));
        // target ids are only meaningful within the frame's own session and
        // travel back through a stepIn routed by threadId - no rewrite needed
        return session.client.sendRequest(request, timeoutMillis);
      }
      case CompletionsRequest completions -> {
        Integer frameId = completions.getArguments() != null ? completions.getArguments().getFrameId() : null;
        ChildSession session = sessionForOptionalId(frameId, raw -> completions.getArguments().setFrameId(raw));
        return session.client.sendRequest(request, timeoutMillis);
      }

      // everything else (initialize, launch, configurationDone, custom
      // requests) belongs to the page session's own lifecycle
      default -> {
        return sessions.get(0).client.sendRequest(request, timeoutMillis);
      }
    }
  }

  private interface RawIdSetter {
    void set(int rawId);
  }

  private Response routeByThread(Request request, Integer compositeThreadId, RawIdSetter setter, long timeoutMillis)
    throws IOException, InterruptedException {
    ChildSession session = sessionForOptionalId(compositeThreadId, setter);
    return session.client.sendRequest(request, timeoutMillis);
  }

  /** Routes by a composited id and composites the response's ids on the way back. */
  private Response routeRewriting(Request request, int compositeId, RawIdSetter setter, long timeoutMillis)
    throws IOException, InterruptedException {
    ChildSession session = route(compositeId);
    setter.set(rawOf(compositeId));
    return rewriteResponse(session, session.client.sendRequest(request, timeoutMillis));
  }

  /** The owning session of a nullable composited id (null -> the page), un-compositing it in place. */
  private ChildSession sessionForOptionalId(Integer compositeId, RawIdSetter setter) {
    if (compositeId == null) {
      return sessions.get(0);
    }
    ChildSession session = route(compositeId);
    setter.set(rawOf(compositeId));
    return session;
  }

  @Override
  public void sendRequestNoWait(Request request) throws IOException {
    // fire-and-forget continues (Resume releases every thread) route by
    // their composite thread id like their awaited counterpart
    if (request instanceof ContinueRequest resume
        && resume.getArguments() != null) {
      int id = resume.getArguments().getThreadId();
      ChildSession session = route(id);
      resume.getArguments().setThreadId(rawOf(id));
      session.client.sendRequestNoWait(request);
      return;
    }
    sessions.get(0).client.sendRequestNoWait(request); // the page launch
  }

  private static String sourcePathOf(SetBreakpointsRequest request) {
    return request.getArguments() != null && request.getArguments().getSource() != null
           ? String.valueOf(request.getArguments().getSource().getPath()) : "?";
  }

  private static long sessionIdKey(int sessionIndex, int adapterBreakpointId) {
    return ((long)sessionIndex << 32) | (adapterBreakpointId & 0xFFFFFFFFL);
  }

  /**
   * setBreakpoints to every live session, with MERGED verification: the page
   * session's response is returned (its adapter ids are the ones the IDE will
   * track), upgraded to verified where any other session verified the line.
   */
  private Response broadcastBreakpoints(SetBreakpointsRequest request, long timeoutMillis)
    throws IOException, InterruptedException {
    int count = request.getArguments() != null && request.getArguments().getBreakpoints() != null
                ? request.getArguments().getBreakpoints().size() : 0;
    List<TrackedBreakpoint> tracked = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      tracked.add(new TrackedBreakpoint());
    }
    List<TrackedBreakpoint> replaced;
    synchronized (trackedBySource) {
      replaced = trackedBySource.put(sourcePathOf(request), tracked);
    }
    if (replaced != null) {
      // ids of the file's previous set must not resolve to dead entries
      trackedBySessionId.values().removeIf(replaced::contains);
    }
    Response pageResponse = null;
    for (ChildSession session : List.copyOf(sessions.values())) {
      if (session.gone) {
        continue;
      }
      try {
        Response response = session.client.sendRequest(request, Math.min(timeoutMillis, CHILD_TIMEOUT_MILLIS));
        recordBreakpointResults(session.index, tracked, response, false);
        if (session.index == 0) {
          pageResponse = response;
        }
      } catch (IOException e) {
        if (session.index == 0) {
          throw e;
        }
      }
    }
    upgradeToMergedVerification(pageResponse, tracked);
    return pageResponse;
  }

  /** Upgrades the page's answer to verified where ANY session verified the line. */
  private static void upgradeToMergedVerification(Response pageResponse, List<TrackedBreakpoint> tracked) {
    if (!(pageResponse instanceof SetBreakpointsResponse ok) || !ok.isSuccess() || ok.getBody() == null
        || ok.getBody().getBreakpoints() == null) {
      return;
    }
    List<Breakpoint> results = ok.getBody().getBreakpoints();
    for (int i = 0; i < results.size() && i < tracked.size(); i++) {
      if (!results.get(i).isVerified() && tracked.get(i).mergedVerified()) {
        results.get(i).setVerified(true);
        results.get(i).setMessage(null);
      }
    }
  }

  /**
   * Records one session's setBreakpoints results into the tracked state.
   * With {@code announce}, a breakpoint whose MERGED state flips to verified
   * is pushed to the IDE as a synthetic breakpoint event (a worker replaying
   * cached breakpoints verifies them long after the IDE saw the page's
   * unverified answer).
   */
  private void recordBreakpointResults(int sessionIndex, List<TrackedBreakpoint> tracked,
                                       Response response, boolean announce) {
    if (!(response instanceof SetBreakpointsResponse ok) || !ok.isSuccess() || ok.getBody() == null
        || ok.getBody().getBreakpoints() == null) {
      return;
    }
    List<Breakpoint> results = ok.getBody().getBreakpoints();
    for (int i = 0; i < results.size() && i < tracked.size(); i++) {
      Breakpoint result = results.get(i);
      TrackedBreakpoint bp = tracked.get(i);
      boolean before = bp.mergedVerified();
      bp.verifiedBySession.put(sessionIndex, result.isVerified());
      if (result.getId() != null) {
        trackedBySessionId.put(sessionIdKey(sessionIndex, result.getId()), bp);
        if (sessionIndex == 0) {
          bp.pageId = result.getId();
        }
      }
      if (announce && before != bp.mergedVerified()) {
        announceBreakpointState(bp);
      }
    }
  }

  /** Tells the IDE a breakpoint's MERGED verification changed (by its page id). */
  private void announceBreakpointState(TrackedBreakpoint bp) {
    Integer pageId = bp.pageId;
    if (pageId == null) {
      return; // the IDE has no id to find this breakpoint by
    }
    BreakpointEvent event = new BreakpointEvent();
    BreakpointEventBody body =
      new BreakpointEventBody();
    body.setReason("changed");
    Breakpoint state = new Breakpoint();
    state.setId(pageId);
    state.setVerified(bp.mergedVerified());
    body.setBreakpoint(state);
    event.setBody(body);
    mergedEvents.offer(event);
  }

  /** A session ended: retract its verification votes; downgrade what it alone verified. */
  private void dropBreakpointVotes(int sessionIndex) {
    trackedBySessionId.keySet().removeIf(key -> (int)(key >>> 32) == sessionIndex);
    List<TrackedBreakpoint> all;
    synchronized (trackedBySource) {
      all = trackedBySource.values().stream().flatMap(List::stream).toList();
    }
    for (TrackedBreakpoint bp : all) {
      boolean before = bp.mergedVerified();
      bp.verifiedBySession.remove(sessionIndex);
      if (before != bp.mergedVerified()) {
        announceBreakpointState(bp);
      }
    }
  }

  /** Sends to every live session; the PAGE session's response is the caller's. */
  private Response broadcast(Request request, long timeoutMillis) throws IOException, InterruptedException {
    Response pageResponse = null;
    for (ChildSession session : List.copyOf(sessions.values())) {
      if (session.gone) {
        continue;
      }
      try {
        Response response = session.client.sendRequest(request, Math.min(timeoutMillis, CHILD_TIMEOUT_MILLIS));
        if (session.index == 0) {
          pageResponse = rewriteResponse(session, response);
        }
      } catch (IOException e) {
        if (session.index == 0) {
          throw e; // the page session's failures are real failures
        }
        // a wedged/dying worker must not break configuration of the rest
      }
    }
    return pageResponse;
  }

  private Response mergedThreads(Request request, long timeoutMillis) throws InterruptedException, IOException {
    List<DapThread> merged = new ArrayList<>();
    for (ChildSession session : List.copyOf(sessions.values())) {
      if (session.gone) {
        continue;
      }
      try {
        Response response = session.client.sendRequest(new ThreadsRequest(), Math.min(timeoutMillis, CHILD_TIMEOUT_MILLIS));
        if (response instanceof ThreadsResponse ok && ok.isSuccess() && ok.getBody() != null
            && ok.getBody().getThreads() != null) {
          for (DapThread thread : ok.getBody().getThreads()) {
            DapThread rewritten = new DapThread();
            rewritten.setId(composite(session.index, thread.getId()));
            rewritten.setName(mergedThreadName(session, thread.getName()));
            merged.add(rewritten);
          }
        }
      } catch (IOException e) {
        if (session.index == 0) {
          throw e;
        }
      }
    }
    ThreadsResponse response = new ThreadsResponse();
    ThreadsResponseBody body = new ThreadsResponseBody();
    body.setThreads(merged);
    response.setBody(body);
    response.setSuccess(true);
    response.setCommand(request.getCommand());
    response.setRequest_seq(request.getSeq());
    return response;
  }

  /**
   * A worker thread's presented name. js-debug names a worker's thread by its
   * script URL - the same thing the session label was derived from; don't say
   * it twice.
   */
  private static String mergedThreadName(ChildSession session, String rawName) {
    String name = rawName == null || rawName.isBlank() ? session.label : rawName;
    if (session.index == 0) {
      return name;
    }
    String shortName = shortLabel(name);
    return shortName.equals(session.label) ? session.label : "[" + session.label + "] " + shortName;
  }

  // ------------------------------------------------------- response rewrite

  /** Composites every session-scoped id (frames, variablesReferences) on the way out. */
  private Response rewriteResponse(ChildSession session, Response response) {
    if (session.index == 0 || response == null || !response.isSuccess()) {
      return response; // page ids pass through; errors carry no ids
    }
    int k = session.index;
    if (response instanceof StackTraceResponse stackTrace && stackTrace.getBody() != null
        && stackTrace.getBody().getStackFrames() != null) {
      for (StackFrame frame : stackTrace.getBody().getStackFrames()) {
        frame.setId(composite(k, frame.getId()));
      }
    } else if (response instanceof ScopesResponse scopes && scopes.getBody() != null
               && scopes.getBody().getScopes() != null) {
      for (Scope scope : scopes.getBody().getScopes()) {
        scope.setVariablesReference(composite(k, scope.getVariablesReference()));
      }
    } else if (response instanceof VariablesResponse variables && variables.getBody() != null
               && variables.getBody().getVariables() != null) {
      for (Variable variable : variables.getBody().getVariables()) {
        if (variable.getVariablesReference() > 0) {
          variable.setVariablesReference(composite(k, variable.getVariablesReference()));
        }
      }
    } else if (response instanceof EvaluateResponse evaluate && evaluate.getBody() != null) {
      if (evaluate.getBody().getVariablesReference() > 0) {
        evaluate.getBody().setVariablesReference(composite(k, evaluate.getBody().getVariablesReference()));
      }
    } else if (response instanceof SetVariableResponse setVariable && setVariable.getBody() != null) {
      if (setVariable.getBody().getVariablesReference() > 0) {
        setVariable.getBody().setVariablesReference(composite(k, setVariable.getBody().getVariablesReference()));
      }
    }
    return response;
  }

  // ----------------------------------------------------------- event pumps

  @Override
  public Event pollEvent(long timeoutMillis) throws InterruptedException {
    return mergedEvents.poll(timeoutMillis, TimeUnit.MILLISECONDS);
  }

  private void startPump(ChildSession session) {
    Thread pump = new Thread(() -> pumpSession(session), "js-debug-mux-" + session.label);
    pump.setDaemon(true);
    pump.start();
  }

  private void pumpSession(ChildSession session) {
    try {
      while (!closed && !session.client.isConnectionFinished()) {
        Event event = session.client.pollEvent(PUMP_POLL_MILLIS);
        if (event != null) {
          handleSessionEvent(session, event);
        }
        Request incoming = session.client.pollIncomingRequest(20);
        if (incoming != null) {
          handleIncoming(session.client, incoming);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      logSink.accept("session '" + session.label + "' pump failed: " + e.getMessage());
    } finally {
      if (session.index != 0 && !closed) {
        session.gone = true;
        sessions.remove(session.index);
        // what only this target verified is no longer verified by anything
        dropBreakpointVotes(session.index);
        logSink.accept("worker session '" + session.label + "' ended");
      }
    }
  }

  private void handleSessionEvent(ChildSession session, Event event) {
    int k = session.index;
    switch (event) {
      case StoppedEvent stopped -> {
        if (stopped.getBody() != null && stopped.getBody().getThreadId() != null) {
          stopped.getBody().setThreadId(composite(k, stopped.getBody().getThreadId()));
        }
        mergedEvents.offer(stopped);
      }
      case ContinuedEvent continued -> {
        if (continued.getBody() != null) {
          continued.getBody().setThreadId(composite(k, continued.getBody().getThreadId()));
          if (k != 0 || sessions.size() > 1) {
            // "all threads" was true only WITHIN that one session - the
            // merged session's other targets are untouched by it
            continued.getBody().setAllThreadsContinued(false);
          }
        }
        mergedEvents.offer(continued);
      }
      case ThreadEvent thread -> {
        if (thread.getBody() != null) {
          thread.getBody().setThreadId(composite(k, thread.getBody().getThreadId()));
        }
        mergedEvents.offer(thread);
      }
      case BreakpointEvent breakpoint -> {
        Breakpoint state = breakpoint.getBody() != null ? breakpoint.getBody().getBreakpoint() : null;
        TrackedBreakpoint bp = state != null && state.getId() != null
                               ? trackedBySessionId.get(sessionIdKey(k, state.getId())) : null;
        if (bp == null) {
          if (k == 0) {
            mergedEvents.offer(breakpoint); // untracked page state passes through
          }
          return; // a worker's untracked breakpoint means nothing to the IDE
        }
        boolean before = bp.mergedVerified();
        bp.verifiedBySession.put(k, state.isVerified());
        if (k == 0 || before != bp.mergedVerified()) {
          // present the MERGED state under the id the IDE knows (the page's)
          state.setId(bp.pageId != null ? bp.pageId : state.getId());
          state.setVerified(bp.mergedVerified());
          if (state.isVerified()) {
            state.setMessage(null);
          }
          mergedEvents.offer(breakpoint);
        }
      }
      case TerminatedEvent terminated -> {
        if (k == 0) {
          mergedEvents.offer(terminated); // the page ending ends the session
        } else {
          session.gone = true; // a worker exiting is routine
        }
      }
      case InitializedEvent initialized -> {
        if (k == 0) {
          mergedEvents.offer(initialized); // workers' handshakes are mux-internal
        }
      }
      default -> {
        if (k == 0) {
          mergedEvents.offer(event); // output etc. from the page as-is
        } else {
          mergedEvents.offer(event); // worker output flows too
        }
      }
    }
  }

  private void startParentPump() {
    Thread pump = new Thread(() -> {
      try {
        while (!closed && !parent.isConnectionFinished()) {
          parent.pollEvent(PUMP_POLL_MILLIS); // parent chatter is not the session's
          Request incoming = parent.pollIncomingRequest(20);
          if (incoming != null) {
            handleIncoming(parent, incoming);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (IOException e) {
        logSink.accept("parent pump failed: " + e.getMessage());
      }
    }, "js-debug-mux-parent");
    pump.setDaemon(true);
    pump.start();
  }

  private void handleIncoming(DapClient on, Request incoming) throws IOException {
    on.respond(incoming, true);
    if (incoming instanceof StartDebuggingRequest start
        && start.getArguments() != null && start.getArguments().getConfiguration() != null) {
      attachChild(start.getArguments().getConfiguration());
    }
  }

  /**
   * Opens and handshakes a NEW child session for a target the adapter
   * announced (a worker, an iframe): initialize, fire-and-forget launch with
   * the handed-over configuration, then on the initialized event the cached
   * breakpoints/filters and configurationDone. The new session joins the
   * thread space once its pump starts.
   */
  private void attachChild(Map<String, Object> configuration) {
    Object name = configuration.get("name");
    String label = name != null && !String.valueOf(name).isBlank() ? shortLabel(String.valueOf(name)) : "worker";
    Thread attach = new Thread(() -> {
      try {
        handshakeChild(configuration, label);
      } catch (IOException e) {
        logSink.accept("failed to attach debug target '" + label + "': " + e.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "js-debug-mux-attach");
    attach.setDaemon(true);
    attach.start();
  }

  /** The child handshake: initialize, fire-and-forget launch, replayed configuration, join. */
  private void handshakeChild(Map<String, Object> configuration, String label)
    throws IOException, InterruptedException {
    int index = nextSessionIndex.getAndIncrement();
    if (index > MAX_SESSION_INDEX) {
      logSink.accept("cannot debug target '" + label + "': " + MAX_SESSION_INDEX
                     + " targets already attached in this session");
      return;
    }
    DapClient client = DapClient.connect("127.0.0.1", adapterPort, (int)CHILD_TIMEOUT_MILLIS);
    if (!client.sendRequest(InitializeRequest.standard("chrome", true), CHILD_TIMEOUT_MILLIS).isSuccess()) {
      client.close();
      return;
    }
    client.sendRequestNoWait(ConfiguredLaunchRequest.of(configuration));
    long deadline = System.currentTimeMillis() + CHILD_TIMEOUT_MILLIS;
    boolean initialized = false;
    while (System.currentTimeMillis() < deadline && !initialized) {
      initialized = client.pollEvent(100) instanceof InitializedEvent;
    }
    replayCachedConfiguration(client, index);
    client.sendRequest(new ConfigurationDoneRequest(), CHILD_TIMEOUT_MILLIS);

    ChildSession session = new ChildSession(index, client, label);
    sessions.put(session.index, session);
    startPump(session);
    logSink.accept("attached debug target '" + label + "' as an additional thread");
  }

  /**
   * Replays the session-wide configuration a newly attached session missed;
   * where the new target verifies a breakpoint the page could not
   * (worker-only sources), the merged upgrade is announced to the IDE.
   */
  private void replayCachedConfiguration(DapClient client, int index)
    throws IOException, InterruptedException {
    List<SetBreakpointsRequest> cached;
    synchronized (breakpointsBySource) {
      cached = List.copyOf(breakpointsBySource.values());
    }
    for (SetBreakpointsRequest breakpoints : cached) {
      Response response = client.sendRequest(breakpoints, CHILD_TIMEOUT_MILLIS);
      List<TrackedBreakpoint> tracked;
      synchronized (trackedBySource) {
        tracked = trackedBySource.get(sourcePathOf(breakpoints));
      }
      if (tracked != null) {
        recordBreakpointResults(index, tracked, response, true);
      }
    }
    SetExceptionBreakpointsRequest filters = exceptionFilters;
    if (filters != null) {
      client.sendRequest(filters, CHILD_TIMEOUT_MILLIS);
    }
  }

  /**
   * js-debug names worker targets by their full script URL; the last path
   * segment (query stripped) is the recognizable part for a thread prefix.
   */
  private static String shortLabel(String name) {
    String base = name;
    int query = base.indexOf('?');
    if (query > 0) {
      base = base.substring(0, query);
    }
    int slash = base.lastIndexOf('/');
    return slash >= 0 && slash < base.length() - 1 ? base.substring(slash + 1) : name;
  }

  @Override
  public void close() throws IOException {
    closed = true;
    for (ChildSession session : List.copyOf(sessions.values())) {
      try {
        session.client.close();
      } catch (IOException ignored) {
      }
    }
    try {
      parent.close();
    } catch (IOException ignored) {
    }
  }
}
