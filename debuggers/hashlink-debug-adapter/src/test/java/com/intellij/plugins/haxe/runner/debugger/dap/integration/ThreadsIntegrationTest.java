package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Multi-threaded debugging against real HashLink (suspend-all). A worker thread
 * hits a breakpoint while main is parked in block(); that stop is checked against
 * the full thread list and — the decisive check — against each thread's OWN stack
 * and locals (worker: workerLocal=222; main: v=111).
 */
@DisplayName("HashLink debugger: threads (integration)")
public class ThreadsIntegrationTest extends DapIntegrationTestBase {

  private static final int WORKER_LINE = 39; // Threads.worker(): Sys.println("worker:" + workerLocal)
  private static final int BLOCK_LINE = 40; // Threads.worker(): gate.wait() — never released

  @BeforeEach
  public void requireThreadsFixture() {
    Assumptions.assumeTrue(threadsFixtureHl != null, "threads fixture not built - skipping");
  }

  @Test
  @DisplayName("inspects every thread stack and locals when suspended")
  public void inspectsEveryThreadStackAndLocalsWhenSuspended() throws Exception {
    // launch the dedicated multi-threaded program and break in the worker
    initialize();
    assertTrue(launch(threadsFixtureHl.toString()).isSuccess(), "launch succeeds");
    assertTrue(setBreakpoint("Threads.hx", WORKER_LINE).isSuccess(), "breakpoint set");
    configurationDone();
    StoppedEvent stopped = awaitStopped();
    int stoppedThreadId = stopped.getBody().getThreadId();

    // the thread list has main + the worker; main is the lowest id
    List<DapThread> threads = threadList();
    assertTrue(threads.size() >= 2, "at least two threads (" + names(threads) + ")");
    DapThread main = lowestId(threads);
    assertEquals("main", main.getName(), "lowest id is named main");

    // the worker (the stopped thread) shows its own frame + local
    Map<String, String> workerLocals = localsOf(stoppedThreadId);
    assertEquals("222", workerLocals.get("workerLocal"), "worker local (workerLocal=222)");

    // the OTHER thread (main) shows a DIFFERENT stack + its own local — the
    // decisive per-thread check: main is parked in block(v=111)
    int mainThreadId = otherThread(threads, stoppedThreadId);
    Map<String, String> mainLocals = localsOf(mainThreadId);
    assertEquals("111", mainLocals.get("v"), "main's block() argument (v=111)");
    assertTrue(topFrameName(mainThreadId).endsWith("block"), "main's top frame is block(), not the worker");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("step over a blocking call keeps the session running and responsive")
  public void stepOverABlockingCallKeepsTheSessionRunningAndResponsive() throws Exception {
    // The worker parks on gate.wait() (never released). Stepping over it plants
    // a landing that is never reached — CORRECT debugger semantics is to keep
    // the session running with the step pending (exactly like stepping over an
    // infinite loop): no hang, no fake stop, and — the regression part — no
    // wedged adapter. Before the Handled-event fixes, stray thread-lifecycle
    // events during the resume dance could freeze the whole debuggee.
    initialize();
    assertTrue(launch(threadsFixtureHl.toString()).isSuccess(), "launch succeeds");
    assertTrue(setBreakpoint("Threads.hx", BLOCK_LINE).isSuccess(), "breakpoint set");
    configurationDone();

    int threadId = awaitStopped().getBody().getThreadId(); // stopped ON gate.wait()

    assertTrue(request(nextRequest(threadId)).isSuccess(), "stepOver accepted");

    // no stop and no downgrade within a generous window: the program runs
    // freely (main keeps spinning) with the step pending
    Event event = client.pollEvent(3000);
    boolean runStateChanged = event instanceof StoppedEvent || event instanceof ContinuedEvent;
    assertTrue(!runStateChanged, "no stop/continued should arrive while the step is pending (got "
               + (event == null ? "nothing" : event.getEvent()) + ")");

    // the adapter must still be fully responsive (not wedged on a pending event)
    assertTrue(request(new ThreadsRequest()).isSuccess(), "threads request still answered");

    request(new DisconnectRequest());
  }

  private List<DapThread> threadList() throws Exception {
    return ((ThreadsResponse)request(new ThreadsRequest())).getBody().getThreads();
  }

  private Map<String, String> localsOf(int threadId) throws Exception {
    return variablesByName(localsScopeReference(topFrameId(threadId)));
  }

  private static DapThread lowestId(List<DapThread> threads) {
    DapThread lowest = threads.get(0);
    for (DapThread t : threads) {
      if (t.getId() < lowest.getId()) lowest = t;
    }
    return lowest;
  }

  private static int otherThread(List<DapThread> threads, int notThisId) {
    for (DapThread t : threads) {
      if (t.getId() != notThisId) return t.getId();
    }
    throw new IllegalStateException("no thread other than " + notThisId);
  }

  private static String names(List<DapThread> threads) {
    StringBuilder sb = new StringBuilder();
    for (DapThread t : threads) {
      sb.append(t.getName())
        .append("[")
        .append(t.getId())
        .append("] ");
    }
    return sb.toString();
  }
}
