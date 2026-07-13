package com.intellij.plugins.haxe.runner.debugger.dap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.BreakpointEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ConfigurationDoneResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ContinueResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.NextResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.PauseResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.StepInResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.StepOutResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.DisconnectResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ErrorResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ContinuedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ExitedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.InitializeResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.LaunchResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ProtocolMessage;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ScopesResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.SetBreakpointsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.SetExceptionBreakpointsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.EvaluateResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.SetVariableResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.VariablesResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ThreadsResponse;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encodes/decodes DAP messages as JSON.
 *
 * Decoding discriminates hand-rolled on the DAP envelope: first on {@code type},
 * then on {@code command} (responses) or {@code event} (events). Any response
 * with {@code success == false} decodes as {@link ErrorResponse} regardless of
 * command, since error replies share one body shape.
 */
public final class DapJson {
  private static final ObjectMapper MAPPER = JsonMapper.builder()
    .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  private DapJson() {
  }

  public static String encode(ProtocolMessage message) {
    return MAPPER.writeValueAsString(message);
  }

  public static ProtocolMessage decode(String json) {
    JsonNode root = MAPPER.readTree(json);
    String type = root.path("type").asString("");
    return switch (type) {
      case Response.TYPE -> decodeResponse(root);
      case Event.TYPE -> decodeEvent(root);
      case Request.TYPE -> MAPPER.treeToValue(root, Request.class);
      default -> throw new IllegalArgumentException("Unknown DAP message type '" + type + "' in: " + json);
    };
  }

  private static Response decodeResponse(JsonNode root) {
    if (!root.path("success").asBoolean(false)) {
      return MAPPER.treeToValue(root, ErrorResponse.class);
    }
    String command = root.path("command").asString("");
    Class<? extends Response> target = switch (command) {
      case "initialize" -> InitializeResponse.class;
      case "setBreakpoints" -> SetBreakpointsResponse.class;
      case "setExceptionBreakpoints" -> SetExceptionBreakpointsResponse.class;
      case "configurationDone" -> ConfigurationDoneResponse.class;
      case "launch" -> LaunchResponse.class;
      case "threads" -> ThreadsResponse.class;
      case "continue" -> ContinueResponse.class;
      case "next" -> NextResponse.class;
      case "stepIn" -> StepInResponse.class;
      case "stepOut" -> StepOutResponse.class;
      case "pause" -> PauseResponse.class;
      case "stackTrace" -> StackTraceResponse.class;
      case "scopes" -> ScopesResponse.class;
      case "variables" -> VariablesResponse.class;
      case "setVariable" -> SetVariableResponse.class;
      case "evaluate" -> EvaluateResponse.class;
      case "disconnect" -> DisconnectResponse.class;
      default -> Response.class;
    };
    return MAPPER.treeToValue(root, target);
  }

  private static Event decodeEvent(JsonNode root) {
    String event = root.path("event").asString("");
    Class<? extends Event> target = switch (event) {
      case InitializedEvent.EVENT -> InitializedEvent.class;
      case StoppedEvent.EVENT -> StoppedEvent.class;
      case TerminatedEvent.EVENT -> TerminatedEvent.class;
      case ExitedEvent.EVENT -> ExitedEvent.class;
      case OutputEvent.EVENT -> OutputEvent.class;
      case BreakpointEvent.EVENT -> BreakpointEvent.class;
      case ContinuedEvent.EVENT -> ContinuedEvent.class;
      default -> Event.class;
    };
    return MAPPER.treeToValue(root, target);
  }
}
