package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives {@link HxcppDebugAdapter} end to end: a real {@link DapClient} on
 * the DAP side (loopback socket pair, exactly as the plugin will use it) and
 * a {@link FakeHxcppServer} playing the debuggee on the jsonrpc side.
 */
public class HxcppDebugAdapterTest {
  private static final long TIMEOUT = 5_000;

  private HxcppDebugAdapter adapter;
  private DapClient dapClient;
  private FakeHxcppServer server;
  private ServerSocket dapListener;

  @Before
  public void setUp() throws IOException {
    adapter = new HxcppDebugAdapter("127.0.0.1", 0, TIMEOUT);

    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket("127.0.0.1", dapListener.getLocalPort());
    Socket adapterSide = dapListener.accept();
    adapter.start(new DapConnection(adapterSide));
    dapClient = new DapClient(new DapConnection(clientSide));

    server = new FakeHxcppServer(adapter.getDebuggeePort());
  }

  @After
  public void tearDown() throws IOException {
    dapClient.close();
    server.close();
    adapter.close();
    dapListener.close();
  }

  private Event awaitEvent(Class<? extends Event> type) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event != null && type.isInstance(event)) {
        return event;
      }
    }
    throw new AssertionError("No " + type.getSimpleName() + " within " + TIMEOUT + " ms");
  }

  private void stopAtBreakpoint(int threadId) throws Exception {
    server.notify("breakpointStop", """
        {"threadId": %d}
        """.formatted(threadId));
    awaitEvent(StoppedEvent.class);
  }

  @Test
  public void initializeReportsCapabilitiesAndEmitsInitialized() throws Exception {
    Response response = dapClient.sendRequest(new InitializeRequest(), TIMEOUT);
    assertTrue(response.isSuccess());
    InitializeResponse initialize = (InitializeResponse)response;
    assertEquals(Boolean.TRUE, initialize.getBody().getSupportsConfigurationDoneRequest());
    assertEquals(Boolean.TRUE, initialize.getBody().getSupportsSetVariable());
    assertEquals(Boolean.TRUE, initialize.getBody().getSupportsConditionalBreakpoints());
    awaitEvent(InitializedEvent.class);
  }

  @Test
  public void launchSucceedsOnceDebuggeeIsConnected() throws Exception {
    assertTrue(dapClient.sendRequest(new LaunchRequest(), TIMEOUT).isSuccess());
  }

  @Test
  public void launchFailsClearlyWithoutDebuggee() throws Exception {
    // separate adapter nobody connects to, with a short accept timeout
    try (HxcppDebugAdapter lonely = new HxcppDebugAdapter("127.0.0.1", 0, 300)) {
      ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      Socket clientSide = new Socket("127.0.0.1", listener.getLocalPort());
      lonely.start(new DapConnection(listener.accept()));
      try (DapClient client = new DapClient(new DapConnection(clientSide))) {
        Response response = client.sendRequest(new LaunchRequest(), TIMEOUT);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage(), response.getMessage().contains("did not connect"));
      } finally {
        listener.close();
      }
    }
  }

  @Test
  public void setBreakpointsTranslatesAndReportsVerified() throws Exception {
    server.handle("setBreakpoints", params -> {
      assertEquals("C:\\project\\src\\Main.hx", params.path("file").asString());
      assertEquals(14, params.path("breakpoints").get(0).path("line").asInt());
      assertEquals("n > 2", params.path("breakpoints").get(1).path("condition").asString());
      return """
          [
            {"id": 11},
            {"id": 12}
          ]
          """;
    });

    Source source = new Source();
    source.setPath("C:\\project\\src\\Main.hx");
    SourceBreakpoint plain = new SourceBreakpoint();
    plain.setLine(14);
    SourceBreakpoint conditional = new SourceBreakpoint();
    conditional.setLine(20);
    conditional.setCondition("n > 2");
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    arguments.setSource(source);
    arguments.setBreakpoints(List.of(plain, conditional));
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    request.setArguments(arguments);

    SetBreakpointsResponse response = (SetBreakpointsResponse)dapClient.sendRequest(request, TIMEOUT);
    assertTrue(response.isSuccess());
    assertEquals(2, response.getBody().getBreakpoints().size());
    assertTrue(response.getBody().getBreakpoints().get(0).isVerified());
    assertEquals(Integer.valueOf(11), response.getBody().getBreakpoints().get(0).getId());
    assertEquals(Integer.valueOf(12), response.getBody().getBreakpoints().get(1).getId());
    assertEquals(Integer.valueOf(14), response.getBody().getBreakpoints().get(0).getLine());
  }

  @Test
  public void forwardSlashClientPathsBecomeNativeSeparators() throws Exception {
    // the server matches breakpoint files by EXACT string against the
    // compiler-recorded paths; IDE paths use forward slashes on Windows
    String expected = "C:/project/src/Main.hx".replace('/', File.separatorChar);
    server.handle("setBreakpoints", params -> {
      assertEquals(expected, params.path("file").asString());
      return """
          [{"id": 1}]
          """;
    });

    Source source = new Source();
    source.setPath("C:/project/src/Main.hx");
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(3);
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    arguments.setSource(source);
    arguments.setBreakpoints(List.of(breakpoint));
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    request.setArguments(arguments);

    assertTrue(dapClient.sendRequest(request, TIMEOUT).isSuccess());
    assertEquals(1, server.requests("setBreakpoints").size());
  }

  @Test
  public void configurationDoneReleasesTheHeldDebuggee() throws Exception {
    assertTrue(dapClient.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());
    List<tools.jackson.databind.JsonNode> continues = server.requests("continue");
    assertEquals(1, continues.size());
    assertEquals(0, continues.get(0).path("params").path("threadId").asInt());
  }

  @Test
  public void breakpointStopBecomesStoppedEvent() throws Exception {
    server.notify("breakpointStop", """
        {"threadId": 2}
        """);
    StoppedEvent stopped = (StoppedEvent)awaitEvent(StoppedEvent.class);
    assertEquals("breakpoint", stopped.getBody().getReason());
    assertEquals(Integer.valueOf(2), stopped.getBody().getThreadId());
    assertEquals(Boolean.TRUE, stopped.getBody().getAllThreadsStopped());
  }

  @Test
  public void exceptionStopMapsToThreadZeroWithDescription() throws Exception {
    server.notify("exceptionStop", """
        {"text": "Null Object Reference"}
        """);
    StoppedEvent stopped = (StoppedEvent)awaitEvent(StoppedEvent.class);
    assertEquals("exception", stopped.getBody().getReason());
    assertEquals(Integer.valueOf(0), stopped.getBody().getThreadId());
    assertEquals("Null Object Reference", stopped.getBody().getDescription());
  }

  @Test
  public void threadLifecycleNotificationsBecomeThreadEvents() throws Exception {
    server.notify("threadStart", """
        {"threadId": 3}
        """);
    ThreadEvent started = (ThreadEvent)awaitEvent(ThreadEvent.class);
    assertEquals(ThreadEvent.REASON_STARTED, started.getBody().getReason());
    assertEquals(3, started.getBody().getThreadId());

    server.notify("ThreadExit", """
        {"threadId": 3}
        """);
    ThreadEvent exited = (ThreadEvent)awaitEvent(ThreadEvent.class);
    assertEquals(ThreadEvent.REASON_EXITED, exited.getBody().getReason());
  }

  @Test
  public void threadsTranslate() throws Exception {
    server.handle("threads", params -> """
        [
          {"id": 0, "name": "main"},
          {"id": 5, "name": "worker"}
        ]
        """);
    ThreadsResponse response = (ThreadsResponse)dapClient.sendRequest(new ThreadsRequest(), TIMEOUT);
    assertEquals(2, response.getBody().getThreads().size());
    assertEquals("worker", response.getBody().getThreads().get(1).getName());
    assertEquals(5, response.getBody().getThreads().get(1).getId());
  }

  @Test
  public void stackTraceMapsSourceAndDropsArtificialFrames() throws Exception {
    server.handle("stackTrace", params -> {
      assertEquals(2, params.path("threadId").asInt());
      return """
          [
            {"id": 0, "name": "debugger::internal", "source": null, "line": 0, "column": 0, "artificial": true},
            {"id": 1, "name": "Main.loop", "source": "C:\\\\project\\\\src\\\\Main.hx", "line": 14, "column": 3}
          ]
          """;
    });
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(2);
    StackTraceRequest request = new StackTraceRequest();
    request.setArguments(arguments);

    StackTraceResponse response = (StackTraceResponse)dapClient.sendRequest(request, TIMEOUT);
    assertEquals(1, response.getBody().getStackFrames().size());
    assertEquals("Main.loop", response.getBody().getStackFrames().get(0).getName());
    assertEquals(14, response.getBody().getStackFrames().get(0).getLine());
    assertEquals("Main.hx", response.getBody().getStackFrames().get(0).getSource().getName());
    assertEquals("C:\\project\\src\\Main.hx", response.getBody().getStackFrames().get(0).getSource().getPath());
  }

  @Test
  public void variablesPathsSurviveIntoSetVariable() throws Exception {
    server.handle("getScopes", params -> """
        [{"id": 100, "name": "Locals"}]
        """);
    server.handle("getVariables", params -> switch (params.path("variablesReference").asInt()) {
      case 100 -> """
          [{"name": "obj", "type": "MyObj", "value": "MyObj", "variablesReference": 101}]
          """;
      case 101 -> """
          [
            {"name": "items", "type": "Array<Int>", "value": "Array(2)", "variablesReference": 102},
            {"name": "x", "type": "Int", "value": "7", "variablesReference": 0}
          ]
          """;
      default -> "[]";
    });
    server.handle("setVariable", params -> {
      assertEquals("obj.x", params.path("expr").asString());
      assertEquals("9", params.path("value").asString());
      return """
          {"name": "x", "type": "Int", "value": "9", "variablesReference": 0}
          """;
    });
    server.handle("evaluate", params -> // write verification read-back
      """
      {"name": "obj.x", "type": "Int", "value": "9", "variablesReference": 0}
      """);

    ScopesArguments scopesArguments = new ScopesArguments();
    scopesArguments.setFrameId(1);
    ScopesRequest scopesRequest = new ScopesRequest();
    scopesRequest.setArguments(scopesArguments);
    ScopesResponse scopes = (ScopesResponse)dapClient.sendRequest(scopesRequest, TIMEOUT);
    assertEquals(100, scopes.getBody().getScopes().get(0).getVariablesReference());

    VariablesArguments variablesArguments = new VariablesArguments();
    variablesArguments.setVariablesReference(100);
    VariablesRequest variablesRequest = new VariablesRequest();
    variablesRequest.setArguments(variablesArguments);
    VariablesResponse locals = (VariablesResponse)dapClient.sendRequest(variablesRequest, TIMEOUT);
    assertEquals("obj", locals.getBody().getVariables().get(0).getName());

    VariablesArguments childArguments = new VariablesArguments();
    childArguments.setVariablesReference(101);
    VariablesRequest childRequest = new VariablesRequest();
    childRequest.setArguments(childArguments);
    dapClient.sendRequest(childRequest, TIMEOUT);

    SetVariableArguments setArguments = new SetVariableArguments();
    setArguments.setVariablesReference(101);
    setArguments.setName("x");
    setArguments.setValue("9");
    SetVariableRequest setRequest = new SetVariableRequest();
    setRequest.setArguments(setArguments);
    SetVariableResponse setResponse = (SetVariableResponse)dapClient.sendRequest(setRequest, TIMEOUT);
    assertTrue(setResponse.isSuccess());
    assertEquals("9", setResponse.getBody().getValue());
  }

  @Test
  public void objectValuesDropTheDoubledClassNamePrefix() throws Exception {
    // The server prints a class instance as "ShortName, Std.string(obj)" and
    // Std.string without a custom toString is the class name AGAIN — raw
    // values read "ClassB, ClassB". The IDE shows the type separately, so
    // the adapter keeps only the informative part.
    server.handle("getVariables", params -> """
        [
          {"name": "b", "type": "ClassB", "value": "ClassB, ClassB", "variablesReference": 7},
          {"name": "w", "type": "pkg.Widget", "value": "Widget, Widget#3", "variablesReference": 8},
          {"name": "n", "type": "Int", "value": "7", "variablesReference": 0},
          {"name": "m", "type": "haxe.ds.StringMap", "value": "{a => 1}", "variablesReference": 9}
        ]
        """);

    VariablesArguments arguments = new VariablesArguments();
    arguments.setVariablesReference(100);
    VariablesRequest request = new VariablesRequest();
    request.setArguments(arguments);
    VariablesResponse response = (VariablesResponse)dapClient.sendRequest(request, TIMEOUT);

    assertEquals("no toString: the class name once, not doubled",
                 "ClassB", response.getBody().getVariables().get(0).getValue());
    assertEquals("custom toString: its text (the type is already shown separately)",
                 "Widget#3", response.getBody().getVariables().get(1).getValue());
    assertEquals("primitives pass through untouched",
                 "7", response.getBody().getVariables().get(2).getValue());
    assertEquals("maps pass through untouched (no short-name prefix)",
                 "{a => 1}", response.getBody().getVariables().get(3).getValue());
  }

  @Test
  public void evaluateResultDropsTheDoubledClassNamePrefix() throws Exception {
    server.handle("evaluate", params -> """
        {"name": "b", "type": "ClassB", "value": "ClassB, ClassB", "variablesReference": 7}
        """);

    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression("b");
    arguments.setFrameId(0);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    EvaluateResponse response = (EvaluateResponse)dapClient.sendRequest(request, TIMEOUT);

    assertEquals("ClassB", response.getBody().getResult());
  }

  @Test
  public void numericChildNamesBecomeIndexExpressions() throws Exception {
    server.handle("getScopes", params -> """
        [{"id": 100, "name": "Locals"}]
        """);
    server.handle("getVariables", params -> switch (params.path("variablesReference").asInt()) {
      case 100 -> """
          [{"name": "items", "type": "Array<Int>", "value": "Array(4)", "variablesReference": 110}]
          """;
      default -> "[]";
    });
    server.handle("setVariable", params -> {
      assertEquals("items[3]", params.path("expr").asString());
      return """
          {"name": "3", "type": "Int", "value": "42", "variablesReference": 0}
          """;
    });
    server.handle("evaluate", params -> // write verification read-back
      """
      {"name": "items[3]", "type": "Int", "value": "42", "variablesReference": 0}
      """);

    ScopesArguments scopesArguments = new ScopesArguments();
    scopesArguments.setFrameId(1);
    ScopesRequest scopesRequest = new ScopesRequest();
    scopesRequest.setArguments(scopesArguments);
    dapClient.sendRequest(scopesRequest, TIMEOUT);
    VariablesArguments variablesArguments = new VariablesArguments();
    variablesArguments.setVariablesReference(100);
    VariablesRequest variablesRequest = new VariablesRequest();
    variablesRequest.setArguments(variablesArguments);
    dapClient.sendRequest(variablesRequest, TIMEOUT);

    SetVariableArguments setArguments = new SetVariableArguments();
    setArguments.setVariablesReference(110);
    setArguments.setName("3");
    setArguments.setValue("42");
    SetVariableRequest setRequest = new SetVariableRequest();
    setRequest.setArguments(setArguments);
    assertTrue(dapClient.sendRequest(setRequest, TIMEOUT).isSuccess());
  }

  @Test
  public void staleReferenceIsRefusedAfterResume() throws Exception {
    server.handle("getScopes", params -> """
        [{"id": 100, "name": "Locals"}]
        """);
    ScopesArguments scopesArguments = new ScopesArguments();
    scopesArguments.setFrameId(1);
    ScopesRequest scopesRequest = new ScopesRequest();
    scopesRequest.setArguments(scopesArguments);
    dapClient.sendRequest(scopesRequest, TIMEOUT);

    ContinueArguments continueArguments = new ContinueArguments();
    continueArguments.setThreadId(0);
    ContinueRequest continueRequest = new ContinueRequest();
    continueRequest.setArguments(continueArguments);
    assertTrue(dapClient.sendRequest(continueRequest, TIMEOUT).isSuccess());

    SetVariableArguments setArguments = new SetVariableArguments();
    setArguments.setVariablesReference(100);
    setArguments.setName("x");
    setArguments.setValue("1");
    SetVariableRequest setRequest = new SetVariableRequest();
    setRequest.setArguments(setArguments);
    Response response = dapClient.sendRequest(setRequest, TIMEOUT);
    assertFalse(response.isSuccess());
    assertTrue(response.getMessage(), response.getMessage().contains("Stale"));
  }

  @Test
  public void evaluateTranslatesAndItsResultIsSettable() throws Exception {
    server.handle("evaluate", params -> switch (params.path("expr").asString()) {
      case "cfg" -> {
        assertEquals(1, params.path("frameId").asInt());
        yield """
            {"name": "cfg", "type": "Config", "value": "Config", "variablesReference": 200}
            """;
      }
      // write verification read-back
      case "cfg.count" -> """
          {"name": "cfg.count", "type": "Int", "value": "9", "variablesReference": 0}
          """;
      default -> throw new RuntimeException("unexpected evaluate " + params.path("expr").asString());
    });
    server.handle("setVariable", params -> {
      assertEquals("cfg.count", params.path("expr").asString());
      return """
          {"name": "count", "type": "Int", "value": "9", "variablesReference": 0}
          """;
    });

    EvaluateArguments evaluateArguments = new EvaluateArguments();
    evaluateArguments.setExpression("cfg");
    evaluateArguments.setFrameId(1);
    EvaluateRequest evaluateRequest = new EvaluateRequest();
    evaluateRequest.setArguments(evaluateArguments);
    EvaluateResponse evaluate = (EvaluateResponse)dapClient.sendRequest(evaluateRequest, TIMEOUT);
    assertEquals("Config", evaluate.getBody().getResult());
    assertEquals(200, evaluate.getBody().getVariablesReference());

    SetVariableArguments setArguments = new SetVariableArguments();
    setArguments.setVariablesReference(200);
    setArguments.setName("count");
    setArguments.setValue("9");
    SetVariableRequest setRequest = new SetVariableRequest();
    setRequest.setArguments(setArguments);
    assertTrue(dapClient.sendRequest(setRequest, TIMEOUT).isSuccess());
  }

  @Test
  public void evaluateAssignmentRoutesToSetVariableAndVerifies() throws Exception {
    server.handle("setVariable", params -> {
      assertEquals("n", params.path("expr").asString());
      assertEquals("100", params.path("value").asString());
      return """
          {"name": "n", "type": "Int", "value": "100", "variablesReference": 0}
          """;
    });
    // the write is verified by re-reading the target
    server.handle("evaluate", params -> {
      assertEquals("n", params.path("expr").asString());
      return """
          {"name": "n", "type": "Int", "value": "100", "variablesReference": 0}
          """;
    });

    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression("n = 100");
    arguments.setFrameId(1);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    EvaluateResponse response = (EvaluateResponse)dapClient.sendRequest(request, TIMEOUT);
    assertTrue(response.isSuccess());
    assertEquals("100", response.getBody().getResult());

    assertEquals(1, server.requests("setVariable").size());
    assertEquals(1, server.requests("evaluate").size());
  }

  @Test
  public void evaluateAssignmentWithExpressionValueEvaluatesTheRightSideFirst() throws Exception {
    server.handle("evaluate", params -> switch (params.path("expr").asString()) {
      case "m * 2" -> """
          {"name": "m * 2", "type": "Int", "value": "84", "variablesReference": 0}
          """;
      // read-back
      case "n" -> """
          {"name": "n", "type": "Int", "value": "84", "variablesReference": 0}
          """;
      default -> throw new RuntimeException("unexpected evaluate " + params.path("expr").asString());
    });
    server.handle("setVariable", params -> {
      assertEquals("n", params.path("expr").asString());
      assertEquals("84", params.path("value").asString());
      return """
          {"name": "n", "type": "Int", "value": "84", "variablesReference": 0}
          """;
    });

    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression("n = m * 2");
    arguments.setFrameId(1);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    EvaluateResponse response = (EvaluateResponse)dapClient.sendRequest(request, TIMEOUT);
    assertTrue(response.isSuccess());
    assertEquals("84", response.getBody().getResult());
  }

  @Test
  public void silentlyIgnoredWriteBecomesAnHonestError() throws Exception {
    // the real server reports success even when the variable was not found
    // in the top frame; the read-back must expose the unchanged value
    server.handle("setVariable", params -> """
        {"name": "n", "type": "Int", "value": "100", "variablesReference": 0}
        """);
    // unchanged!
    server.handle("evaluate", params -> """
        {"name": "n", "type": "Int", "value": "3", "variablesReference": 0}
        """);

    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression("n = 100");
    arguments.setFrameId(1);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    Response response = dapClient.sendRequest(request, TIMEOUT);
    assertFalse(response.isSuccess());
    assertTrue(response.getMessage(), response.getMessage().contains("TOP stack frame"));
  }

  @Test
  public void comparisonsAreNotAssignments() throws Exception {
    server.handle("evaluate", params -> {
      assertEquals("n == 100", params.path("expr").asString());
      return """
          {"name": "n == 100", "type": "Bool", "value": "false", "variablesReference": 0}
          """;
    });
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression("n == 100");
    arguments.setFrameId(1);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    assertTrue(dapClient.sendRequest(request, TIMEOUT).isSuccess());
    assertEquals(0, server.requests("setVariable").size());
  }

  @Test
  public void topLevelAssignmentDetection() {
    assertEquals(2, HxcppDebugAdapter.topLevelAssignment("n = 100"));
    assertEquals(9, HxcppDebugAdapter.topLevelAssignment("arr[i+1] = x"));
    assertEquals(-1, HxcppDebugAdapter.topLevelAssignment("n == 100"));
    assertEquals(-1, HxcppDebugAdapter.topLevelAssignment("n != 100"));
    assertEquals(-1, HxcppDebugAdapter.topLevelAssignment("n <= 100"));
    assertEquals(-1, HxcppDebugAdapter.topLevelAssignment("n >= 100"));
    assertEquals(-1, HxcppDebugAdapter.topLevelAssignment("f(a = 1)"));
    assertEquals(-1, HxcppDebugAdapter.topLevelAssignment("\"a = b\""));
    assertEquals(2, HxcppDebugAdapter.topLevelAssignment("s = \"x == y\""));
  }

  @Test
  public void setVariableVerifiesAgainstTheReferencesFrame() throws Exception {
    server.handle("getScopes", params -> """
        [{"id": 100, "name": "Locals"}]
        """);
    server.handle("setVariable", params -> """
        {"name": "x", "type": "Int", "value": "5", "variablesReference": 0}
        """);
    server.handle("evaluate", params -> {
      // the read-back must target the frame the reference was handed out for
      assertEquals("x", params.path("expr").asString());
      assertEquals(7, params.path("frameId").asInt());
      return """
          {"name": "x", "type": "Int", "value": "5", "variablesReference": 0}
          """;
    });

    ScopesArguments scopesArguments = new ScopesArguments();
    scopesArguments.setFrameId(7);
    ScopesRequest scopesRequest = new ScopesRequest();
    scopesRequest.setArguments(scopesArguments);
    dapClient.sendRequest(scopesRequest, TIMEOUT);

    SetVariableArguments setArguments = new SetVariableArguments();
    setArguments.setVariablesReference(100);
    setArguments.setName("x");
    setArguments.setValue("5");
    SetVariableRequest setRequest = new SetVariableRequest();
    setRequest.setArguments(setArguments);
    assertTrue(dapClient.sendRequest(setRequest, TIMEOUT).isSuccess());
    assertEquals(1, server.requests("evaluate").size());
  }

  @Test
  public void steppingRequiresTheStoppedThread() throws Exception {
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(2);
    NextRequest request = new NextRequest();
    request.setArguments(arguments);

    // not stopped at all
    Response notStopped = dapClient.sendRequest(request, TIMEOUT);
    assertFalse(notStopped.isSuccess());
    assertTrue(notStopped.getMessage(), notStopped.getMessage().contains("not stopped"));

    stopAtBreakpoint(2);

    // wrong thread
    NextArguments wrongThread = new NextArguments();
    wrongThread.setThreadId(1);
    NextRequest wrongRequest = new NextRequest();
    wrongRequest.setArguments(wrongThread);
    Response wrong = dapClient.sendRequest(wrongRequest, TIMEOUT);
    assertFalse(wrong.isSuccess());
    assertTrue(wrong.getMessage(), wrong.getMessage().contains("stopped thread"));

    // right thread reaches the server
    NextRequest okRequest = new NextRequest();
    NextArguments okArguments = new NextArguments();
    okArguments.setThreadId(2);
    okRequest.setArguments(okArguments);
    assertTrue(dapClient.sendRequest(okRequest, TIMEOUT).isSuccess());
    assertEquals(1, server.requests("next").size());
  }

  @Test
  public void pauseTranslates() throws Exception {
    assertTrue(dapClient.sendRequest(new PauseRequest(), TIMEOUT).isSuccess());
    assertEquals(1, server.requests("pause").size());
  }

  @Test
  public void serverErrorBecomesDapErrorResponse() throws Exception {
    server.handle("threads", params -> {
      throw new RuntimeException("boom");
    });
    Response response = dapClient.sendRequest(new ThreadsRequest(), TIMEOUT);
    assertFalse(response.isSuccess());
    assertTrue(response.getMessage(), response.getMessage().contains("boom"));
  }

  @Test
  public void requestLoopSurvivesAFailedRequest() throws Exception {
    server.handle("threads", params -> {
      throw new RuntimeException("boom");
    });
    assertFalse(dapClient.sendRequest(new ThreadsRequest(), TIMEOUT).isSuccess());

    server.handle("threads", params -> """
        [{"id": 0, "name": "main"}]
        """);
    assertTrue(dapClient.sendRequest(new ThreadsRequest(), TIMEOUT).isSuccess());
  }

  @Test
  public void debuggeeDisconnectEmitsTerminated() throws Exception {
    // make sure the pump is attached before killing the connection
    assertTrue(dapClient.sendRequest(new LaunchRequest(), TIMEOUT).isSuccess());
    server.close();
    awaitEvent(TerminatedEvent.class);
  }

  @Test
  public void disconnectResponds() throws Exception {
    Response response = dapClient.sendRequest(new DisconnectRequest(), TIMEOUT);
    assertTrue(response.isSuccess());
  }

  @Test
  public void unknownEventNamesAreIgnoredWithoutKillingThePump() throws Exception {
    server.notify("someFutureThing", "{}");
    server.notify("breakpointStop", """
        {"threadId": 1}
        """);
    StoppedEvent stopped = (StoppedEvent)awaitEvent(StoppedEvent.class);
    assertNotNull(stopped);
    assertNull(stopped.getBody().getDescription());
  }
}
