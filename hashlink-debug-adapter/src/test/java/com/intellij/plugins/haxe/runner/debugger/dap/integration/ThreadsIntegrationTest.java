package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ThreadsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ThreadsResponse;
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

  private static final int WORKER_LINE = 38; // Threads.worker(): Sys.println("worker:" + workerLocal)

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
