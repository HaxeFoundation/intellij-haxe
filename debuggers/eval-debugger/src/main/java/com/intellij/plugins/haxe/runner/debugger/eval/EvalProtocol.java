package com.intellij.plugins.haxe.runner.debugger.eval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Typed wrappers over the eval debug methods (wire names and shapes from
 * vshaxe/eval-debugger's Protocol.hx, round-trip-verified against haxe
 * 4.3.7). Threads are optional on run-control requests — omitted means
 * "the current/only thread".
 */
public final class EvalProtocol {
  /** Notification method names the VM pushes (id-less messages). */
  public static final String EVENT_BREAKPOINT_STOP = "breakpointStop";
  public static final String EVENT_EXCEPTION_STOP = "exceptionStop";
  public static final String EVENT_THREAD_EVENT = "threadEvent";

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final long DEFAULT_TIMEOUT_MS = 10_000;

  private final EvalConnection connection;

  public EvalProtocol(EvalConnection connection) {
    this.connection = connection;
  }

  public record EvalThread(int id, String name) {
  }

  public record EvalStackFrame(int id, String name, String source, int line, int column,
                               int endLine, int endColumn, boolean artificial) {
  }

  public record EvalScope(int id, String name) {
  }

  public record EvalVar(int id, String name, String type, String value, int numChildren) {
  }

  public record EvalBreakpoint(int id) {
  }

  // --- run control ---

  public void resume() throws IOException {
    connection.request("continue", null, DEFAULT_TIMEOUT_MS);
  }

  public void pause() throws IOException {
    connection.request("pause", null, DEFAULT_TIMEOUT_MS);
  }

  public void stepIn() throws IOException {
    connection.request("stepIn", null, DEFAULT_TIMEOUT_MS);
  }

  public void next() throws IOException {
    connection.request("next", null, DEFAULT_TIMEOUT_MS);
  }

  public void stepOut() throws IOException {
    connection.request("stepOut", null, DEFAULT_TIMEOUT_MS);
  }

  // --- introspection ---

  public List<EvalThread> getThreads() throws IOException {
    JsonNode result = connection.request("getThreads", null, DEFAULT_TIMEOUT_MS);
    List<EvalThread> threads = new ArrayList<>();
    for (JsonNode thread : result) {
      threads.add(new EvalThread(thread.path("id").asInt(0), thread.path("name").asString("")));
    }
    return threads;
  }

  public List<EvalStackFrame> stackTrace(Integer threadId) throws IOException {
    ObjectNode params = MAPPER.createObjectNode();
    if (threadId != null) {
      params.put("threadId", threadId.intValue());
    }
    JsonNode result = connection.request("stackTrace", params, DEFAULT_TIMEOUT_MS);
    List<EvalStackFrame> frames = new ArrayList<>();
    for (JsonNode frame : result) {
      frames.add(new EvalStackFrame(
        frame.path("id").asInt(0),
        frame.path("name").asString(""),
        frame.path("source").isNull() ? null : frame.path("source").asString(null),
        frame.path("line").asInt(0),
        frame.path("column").asInt(0),
        frame.path("endLine").asInt(0),
        frame.path("endColumn").asInt(0),
        frame.path("artificial").asBoolean(false)));
    }
    return frames;
  }

  public List<EvalScope> getScopes(int frameId) throws IOException {
    ObjectNode params = MAPPER.createObjectNode();
    params.put("frameId", frameId);
    JsonNode result = connection.request("getScopes", params, DEFAULT_TIMEOUT_MS);
    List<EvalScope> scopes = new ArrayList<>();
    for (JsonNode scope : result) {
      scopes.add(new EvalScope(scope.path("id").asInt(0), scope.path("name").asString("")));
    }
    return scopes;
  }

  public List<EvalVar> getVariables(int id) throws IOException {
    ObjectNode params = MAPPER.createObjectNode();
    params.put("id", id);
    return parseVars(connection.request("getVariables", params, DEFAULT_TIMEOUT_MS));
  }

  public EvalVar evaluate(String expression, int frameId) throws IOException {
    ObjectNode params = MAPPER.createObjectNode();
    params.put("expr", expression);
    params.put("frameId", frameId);
    return parseVar(connection.request("evaluate", params, DEFAULT_TIMEOUT_MS));
  }

  /**
   * Sets the named member of container {@code id} (a scope or a value id — the
   * VM's single id space) to the parsed {@code value} expression; returns the
   * variable's new state (evalDebugSocket.ml: id/name/value, var_to_json back).
   */
  public EvalVar setVariable(int id, String name, String value) throws IOException {
    ObjectNode params = MAPPER.createObjectNode();
    params.put("id", id);
    params.put("name", name);
    params.put("value", value);
    return parseVar(connection.request("setVariable", params, DEFAULT_TIMEOUT_MS));
  }

  // --- breakpoints ---

  /** Replaces the file's breakpoints; returns the VM-assigned ids in order. */
  public List<EvalBreakpoint> setBreakpoints(String file, int... lines) throws IOException {
    ObjectNode params = MAPPER.createObjectNode();
    params.put("file", file);
    ArrayNode breakpoints = params.putArray("breakpoints");
    for (int line : lines) {
      breakpoints.addObject().put("line", line);
    }
    JsonNode result = connection.request("setBreakpoints", params, DEFAULT_TIMEOUT_MS);
    List<EvalBreakpoint> ids = new ArrayList<>();
    for (JsonNode breakpoint : result) {
      ids.add(new EvalBreakpoint(breakpoint.path("id").asInt(-1)));
    }
    return ids;
  }

  public void setExceptionOptions(List<String> filters) throws IOException {
    // params IS a bare array for this method (Protocol.hx: Array<String>)
    ArrayNode params = MAPPER.createArrayNode();
    for (String filter : filters) {
      params.add(filter);
    }
    connection.request("setExceptionOptions", params, DEFAULT_TIMEOUT_MS);
  }

  private static List<EvalVar> parseVars(JsonNode result) {
    List<EvalVar> vars = new ArrayList<>();
    for (JsonNode var : result) {
      vars.add(parseVar(var));
    }
    return vars;
  }

  private static EvalVar parseVar(JsonNode var) {
    return new EvalVar(
      var.path("id").asInt(0),
      var.path("name").asString(""),
      var.path("type").asString(""),
      var.path("value").asString(""),
      var.path("numChildren").asInt(0));
  }
}
