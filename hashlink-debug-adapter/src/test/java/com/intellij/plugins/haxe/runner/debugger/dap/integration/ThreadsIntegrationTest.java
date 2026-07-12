package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ThreadsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ThreadsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ContinuedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import java.util.List;
import java.util.Map;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Multi-threaded debugging against real HashLink (suspend-all). A worker thread
 * hits a breakpoint while main is parked in block(); at that stop we assert the
 * full thread list, and — the decisive check — read each thread's OWN stack and
 * locals (worker: workerLocal=222; main: v=111).
 */
public class ThreadsIntegrationTest extends DapIntegrationTestBase {

  private static final int WORKER_LINE = 39; // Threads.worker(): Sys.println("worker:" + workerLocal)
  private static final int BLOCK_LINE = 40; // Threads.worker(): gate.wait() — never released

  @Before
  public void requireThreadsFixture() {
    Assume.assumeTrue("threads fixture not built - skipping", threadsFixtureHl != null);
  }

  @Test
  public void inspectsEveryThreadStackAndLocalsWhenSuspended() throws Exception {
    // launch the dedicated multi-threaded program and break in the worker
    initialize();
    assertTrue("launch succeeds", launch(threadsFixtureHl.toString()).isSuccess());
    assertTrue("breakpoint set", setBreakpoint("Threads.hx", WORKER_LINE).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());
    StoppedEvent stopped = awaitStopped();
    int stoppedThreadId = stopped.getBody().getThreadId();

    // the thread list has main + the worker; main is the lowest id
    List<DapThread> threads = threadList();
    assertTrue("at least two threads (" + names(threads) + ")", threads.size() >= 2);
    DapThread main = lowestId(threads);
    assertEquals("lowest id is named main", "main", main.getName());

    // the worker (the stopped thread) shows its own frame + local
    Map<String, String> workerLocals = localsOf(stoppedThreadId);
    assertEquals("worker local (workerLocal=222)", "222", workerLocals.get("workerLocal"));

    // the OTHER thread (main) shows a DIFFERENT stack + its own local — the
    // decisive per-thread check: main is parked in block(v=111)
    int mainThreadId = otherThread(threads, stoppedThreadId);
    Map<String, String> mainLocals = localsOf(mainThreadId);
    assertEquals("main's block() argument (v=111)", "111", mainLocals.get("v"));
    assertTrue("main's top frame is block(), not the worker",
               topFrameName(mainThreadId).endsWith("block"));

    request(new DisconnectRequest());
  }

  @Test
  public void stepOverABlockingCallKeepsTheSessionRunningAndResponsive() throws Exception {
    // The worker parks on gate.wait() (never released). Stepping over it plants
    // a landing that is never reached — CORRECT debugger semantics is to keep
    // the session running with the step pending (exactly like stepping over an
    // infinite loop): no hang, no fake stop, and — the regression part — no
    // wedged adapter. Before the Handled-event fixes, stray thread-lifecycle
    // events during the resume dance could freeze the whole debuggee.
    initialize();
    assertTrue("launch succeeds", launch(threadsFixtureHl.toString()).isSuccess());
    assertTrue("breakpoint set", setBreakpoint("Threads.hx", BLOCK_LINE).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());
    int threadId = awaitStopped().getBody().getThreadId(); // stopped ON gate.wait()

    assertTrue("stepOver accepted", request(nextRequest(threadId)).isSuccess());

    // no stop and no downgrade within a generous window: the program runs
    // freely (main keeps spinning) with the step pending
    Event event = client.pollEvent(3000);
    assertTrue("no stop/continued should arrive while the step is pending (got "
               + (event == null ? "nothing" : event.getEvent()) + ")",
               event == null || !(event instanceof StoppedEvent) && !(event instanceof ContinuedEvent));

    // the adapter must still be fully responsive (not wedged on a pending event)
    assertTrue("threads request still answered", request(new ThreadsRequest()).isSuccess());

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
    for (DapThread t : threads) sb.append(t.getName()).append("[").append(t.getId()).append("] ");
    return sb.toString();
  }
}
