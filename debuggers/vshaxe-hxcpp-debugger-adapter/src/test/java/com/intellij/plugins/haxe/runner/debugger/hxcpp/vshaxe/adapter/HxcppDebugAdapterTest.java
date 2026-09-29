package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Drives {@link HxcppDebugAdapter} end to end: a real {@link DapClient} on
 * the DAP side (loopback socket pair, exactly as the plugin will use it) and
 * a {@link FakeHxcppServer} playing the debuggee on the jsonrpc side.
 */
@DisplayName("HXCPP debugger (vshaxe): debug adapter")
public class HxcppDebugAdapterTest {
  private static final long TIMEOUT = 5_000;

  private HxcppDebugAdapter adapter;
  private DapClient dapClient;
  private FakeHxcppServer server;
  private ServerSocket dapListener;

  @BeforeEach
  public void setUp() throws IOException {
    adapter = new HxcppDebugAdapter("127.0.0.1", 0, TIMEOUT);
    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket("127.0.0.1", dapListener.getLocalPort());
    Socket adapterSide = dapListener.accept();
    adapter.start(new DapConnection(adapterSide));
    dapClient = new DapClient(new DapConnection(clientSide));
    server = new FakeHxcppServer(adapter.getDebuggeePort());
  }

  @AfterEach
  public void tearDown() throws IOException {
    dapClient.close();
    server.close();
    adapter.close();
    dapListener.close();
  }

  @Test
  @DisplayName("initialize reports capabilities and emits initialized")
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
  @DisplayName("launch succeeds once debuggee is connected")
  public void launchSucceedsOnceDebuggeeIsConnected() throws Exception {
    assertTrue(dapClient.sendRequest(new LaunchRequest(), TIMEOUT).isSuccess());
  }

  @Test
  @DisplayName("launch fails clearly without debuggee")
  public void launchFailsClearlyWithoutDebuggee() throws Exception {
    // separate adapter nobody connects to, with a short accept timeout
    try (HxcppDebugAdapter lonely = new HxcppDebugAdapter("127.0.0.1", 0, 300)) {
      ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      Socket clientSide = new Socket("127.0.0.1", listener.getLocalPort());
      lonely.start(new DapConnection(listener.accept()));

      try (DapClient client = new DapClient(new DapConnection(clientSide))) {
        Response response = client.sendRequest(new LaunchRequest(), TIMEOUT);
        assertFailedWith(response, "did not connect");
      } finally {
        listener.close();
      }
    }
  }

  @Test
  @DisplayName("set breakpoints translates and reports verified")
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
  @DisplayName("forward slash client paths become native separators")
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
  @DisplayName("configuration done releases the held debuggee")
  public void configurationDoneReleasesTheHeldDebuggee() throws Exception {
    assertTrue(dapClient.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());
    List<tools.jackson.databind.JsonNode> continues = server.requests("continue");
    assertEquals(1, continues.size());
    assertEquals(0, continues.get(0).path("params").path("threadId").asInt());
  }

  @Test
  @DisplayName("breakpoint stop becomes stopped event")
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
  @DisplayName("exception stop maps to thread zero with description")
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
  @DisplayName("thread lifecycle notifications become thread events")
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
  @DisplayName("threads translate")
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
  @DisplayName("stack trace maps source and drops artificial frames")
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
  @DisplayName("variables paths survive into set variable")
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
  @DisplayName("object values drop the doubled class name prefix")
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

    assertEquals("ClassB", response.getBody().getVariables().get(0).getValue(), "no toString: the class name once, not doubled");
    assertEquals("Widget#3", response.getBody().getVariables().get(1).getValue(), "custom toString: its text (the type is already shown separately)");
    assertEquals("7", response.getBody().getVariables().get(2).getValue(), "primitives pass through untouched");
    assertEquals("{a => 1}", response.getBody().getVariables().get(3).getValue(), "maps pass through untouched (no short-name prefix)");
  }

  @Test
  @DisplayName("evaluate result drops the doubled class name prefix")
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
  @DisplayName("numeric child names become index expressions")
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
  @DisplayName("stale reference is refused after resume")
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
    assertFailedWith(response, "Stale");
  }

  @Test
  @DisplayName("evaluate translates and its result is settable")
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
  @DisplayName("evaluate assignment routes to set variable and verifies")
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
  @DisplayName("evaluate assignment with expression value evaluates the right side first")
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
  @DisplayName("silently ignored write becomes an honest error")
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
    assertFailedWith(response, "TOP stack frame");
  }

  /** The routing decision itself (== is not an assignment) is topLevelAssignment's pin; the wire pin here is only that setVariable never fired. */
  @Test
  @DisplayName("comparisons are not assignments")
  public void comparisonsAreNotAssignments() throws Exception {
    server.handle("evaluate", params -> """
        {"name": "n == 100", "type": "Bool", "value": "false", "variablesReference": 0}
        """);
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression("n == 100");
    arguments.setFrameId(1);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    assertTrue(dapClient.sendRequest(request, TIMEOUT).isSuccess());
    assertEquals(0, server.requests("setVariable").size());
  }

  /** (expression, index of the top-level `=` — -1 when the expression is not an assignment). */
  static final List<Arguments> TOP_LEVEL_ASSIGNMENTS = List.of(
    arguments("n = 100", 2),
    arguments("arr[i+1] = x", 9),
    arguments("n == 100", -1),
    arguments("n != 100", -1),
    arguments("n <= 100", -1),
    arguments("n >= 100", -1),
    arguments("f(a = 1)", -1),
    arguments("\"a = b\"", -1),
    arguments("s = \"x == y\"", 2));

  @ParameterizedTest(name = "{0} -> {1}")
  @FieldSource("TOP_LEVEL_ASSIGNMENTS")
  @DisplayName("top level assignment detection")
  public void topLevelAssignmentDetection(String expression, int index) {
    assertEquals(index, HxcppDebugAdapter.topLevelAssignment(expression));
  }

  @Test
  @DisplayName("set variable verifies against the references frame")
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
  @DisplayName("stepping requires the stopped thread")
  public void steppingRequiresTheStoppedThread() throws Exception {
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(2);
    NextRequest request = new NextRequest();
    request.setArguments(arguments);

    // not stopped at all
    Response notStopped = dapClient.sendRequest(request, TIMEOUT);
    assertFalse(notStopped.isSuccess());
    assertTrue(notStopped.getMessage().contains("not stopped"), notStopped.getMessage());

    stopAtBreakpoint(2);

    // wrong thread
    NextArguments wrongThread = new NextArguments();
    wrongThread.setThreadId(1);
    NextRequest wrongRequest = new NextRequest();
    wrongRequest.setArguments(wrongThread);
    Response wrong = dapClient.sendRequest(wrongRequest, TIMEOUT);
    assertFalse(wrong.isSuccess());
    assertTrue(wrong.getMessage().contains("stopped thread"), wrong.getMessage());

    // right thread reaches the server
    NextRequest okRequest = new NextRequest();
    NextArguments okArguments = new NextArguments();
    okArguments.setThreadId(2);
    okRequest.setArguments(okArguments);
    assertTrue(dapClient.sendRequest(okRequest, TIMEOUT).isSuccess());
    assertEquals(1, server.requests("next").size());
  }

  @Test
  @DisplayName("pause translates")
  public void pauseTranslates() throws Exception {
    assertTrue(dapClient.sendRequest(new PauseRequest(), TIMEOUT).isSuccess());
    assertEquals(1, server.requests("pause").size());
  }

  @Test
  @DisplayName("server error becomes dap error response")
  public void serverErrorBecomesDapErrorResponse() throws Exception {
    server.handle("threads", params -> {
      throw new RuntimeException("boom");
    });
    Response response = dapClient.sendRequest(new ThreadsRequest(), TIMEOUT);
    assertFailedWith(response, "boom");
  }

  @Test
  @DisplayName("request loop survives a failed request")
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
  @DisplayName("debuggee disconnect emits terminated")
  public void debuggeeDisconnectEmitsTerminated() throws Exception {
    // make sure the pump is attached before killing the connection
    assertTrue(dapClient.sendRequest(new LaunchRequest(), TIMEOUT).isSuccess());
    server.close();
    awaitEvent(TerminatedEvent.class);
  }

  @Test
  @DisplayName("disconnect responds")
  public void disconnectResponds() throws Exception {
    Response response = dapClient.sendRequest(new DisconnectRequest(), TIMEOUT);
    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("unknown event names are ignored without killing the pump")
  public void unknownEventNamesAreIgnoredWithoutKillingThePump() throws Exception {
    server.notify("someFutureThing", "{}");
    server.notify("breakpointStop", """
        {"threadId": 1}
        """);
    StoppedEvent stopped = (StoppedEvent)awaitEvent(StoppedEvent.class);
    assertNotNull(stopped);
    assertNull(stopped.getBody().getDescription());
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

  /** The request failed, and its message names the cause. */
  private static void assertFailedWith(Response response, String expected) {
    assertFalse(response.isSuccess(), "expected failure, got success");
    assertTrue(response.getMessage().contains(expected), response.getMessage());
  }
}
