package com.intellij.plugins.haxe.runner.debugger.dap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ProtocolMessage;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
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
    // @formatter:off
    return switch (type) {
      case Response.TYPE -> decodeResponse(root);
      case Event.TYPE    -> decodeEvent(root);
      case Request.TYPE  -> decodeRequest(root);
      default            -> throw new IllegalArgumentException("Unknown DAP message type '" + type + "' in: " + json);
    };
    // @formatter:on
  }

  /**
   * Requests decode to their typed subclass (so a DAP server implementation
   * gets typed arguments); unknown commands fall back to the bare envelope.
   */
  private static Request decodeRequest(JsonNode root) {
    String command = root.path("command").asString("");
    // @formatter:off
    Class<? extends Request> target = switch (command) {
      case InitializeRequest.COMMAND              -> InitializeRequest.class;
      case LaunchRequest.COMMAND                  -> LaunchRequest.class;
      case SetBreakpointsRequest.COMMAND          -> SetBreakpointsRequest.class;
      case SetExceptionBreakpointsRequest.COMMAND -> SetExceptionBreakpointsRequest.class;
      case ConfigurationDoneRequest.COMMAND       -> ConfigurationDoneRequest.class;
      case ThreadsRequest.COMMAND                 -> ThreadsRequest.class;
      case StackTraceRequest.COMMAND              -> StackTraceRequest.class;
      case ScopesRequest.COMMAND                  -> ScopesRequest.class;
      case VariablesRequest.COMMAND               -> VariablesRequest.class;
      case ContinueRequest.COMMAND                -> ContinueRequest.class;
      case NextRequest.COMMAND                    -> NextRequest.class;
      case StepInRequest.COMMAND                  -> StepInRequest.class;
      case StepInTargetsRequest.COMMAND           -> StepInTargetsRequest.class;
      case StepOutRequest.COMMAND                 -> StepOutRequest.class;
      case PauseRequest.COMMAND                   -> PauseRequest.class;
      case EvaluateRequest.COMMAND                -> EvaluateRequest.class;
      case ExceptionInfoRequest.COMMAND           -> ExceptionInfoRequest.class;
      case SetVariableRequest.COMMAND             -> SetVariableRequest.class;
      case DisconnectRequest.COMMAND              -> DisconnectRequest.class;
      // custom: needed by JAVA-side DAP servers (the eval adapter emulates
      // smart step into); the haxe-side servers decode it themselves
      case StepIntoFunctionRequest.COMMAND        -> StepIntoFunctionRequest.class;
      case SetExpressionSteppingRequest.COMMAND   -> SetExpressionSteppingRequest.class;
      default                                     -> Request.class;
    };
    // @formatter:on
    return MAPPER.treeToValue(root, target);
  }

  private static Response decodeResponse(JsonNode root) {
    if (!root.path("success").asBoolean(false)) {
      return MAPPER.treeToValue(root, ErrorResponse.class);
    }
    String command = root.path("command").asString("");
    // a response is keyed by the command it answers, so it switches on the
    // request classes' COMMAND constants
    // @formatter:off
    Class<? extends Response> target = switch (command) {
      case InitializeRequest.COMMAND              -> InitializeResponse.class;
      case SetBreakpointsRequest.COMMAND          -> SetBreakpointsResponse.class;
      case SetExceptionBreakpointsRequest.COMMAND -> SetExceptionBreakpointsResponse.class;
      case ConfigurationDoneRequest.COMMAND       -> ConfigurationDoneResponse.class;
      case LaunchRequest.COMMAND                  -> LaunchResponse.class;
      case ThreadsRequest.COMMAND                 -> ThreadsResponse.class;
      case ContinueRequest.COMMAND                -> ContinueResponse.class;
      case NextRequest.COMMAND                    -> NextResponse.class;
      case StepInRequest.COMMAND                  -> StepInResponse.class;
      case StepInTargetsRequest.COMMAND           -> StepInTargetsResponse.class;
      case StepOutRequest.COMMAND                 -> StepOutResponse.class;
      case PauseRequest.COMMAND                   -> PauseResponse.class;
      case ExceptionInfoRequest.COMMAND           -> ExceptionInfoResponse.class;
      case StackTraceRequest.COMMAND              -> StackTraceResponse.class;
      case ScopesRequest.COMMAND                  -> ScopesResponse.class;
      case VariablesRequest.COMMAND               -> VariablesResponse.class;
      case SetVariableRequest.COMMAND             -> SetVariableResponse.class;
      case EvaluateRequest.COMMAND                -> EvaluateResponse.class;
      case DisconnectRequest.COMMAND              -> DisconnectResponse.class;
      default                                     -> Response.class;
    };
    // @formatter:on
    return MAPPER.treeToValue(root, target);
  }

  private static Event decodeEvent(JsonNode root) {
    String event = root.path("event").asString("");
    // @formatter:off
    Class<? extends Event> target = switch (event) {
      case InitializedEvent.EVENT -> InitializedEvent.class;
      case StoppedEvent.EVENT     -> StoppedEvent.class;
      case TerminatedEvent.EVENT  -> TerminatedEvent.class;
      case ExitedEvent.EVENT      -> ExitedEvent.class;
      case OutputEvent.EVENT      -> OutputEvent.class;
      case BreakpointEvent.EVENT  -> BreakpointEvent.class;
      case ContinuedEvent.EVENT   -> ContinuedEvent.class;
      case ThreadEvent.EVENT      -> ThreadEvent.class;
      default                     -> Event.class;
    };
    // @formatter:on
    return MAPPER.treeToValue(root, target);
  }
}
