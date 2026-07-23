package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;

/**
 * DAP console tracing (developer diagnostics, off in production): flip
 * {@link #ENABLED} (in code, rebuild) to mirror every DAP request/response/
 * timeout and incoming event into the session console as grey [dap] lines —
 * command, thread/frame/reference ids, seq numbers, durations. This is how
 * the firefox worker-actor wedges were pinned; deliberately NOT exposed as
 * UI, it is a developer tool.
 */
final class DapConsoleTracer {
  static final boolean ENABLED = false;

  private DapConsoleTracer() {
  }

  static String describeEvent(Event event) {
    return switch (event) {
      case StoppedEvent e when e.getBody() != null ->
        "stopped thread=" + e.getBody().getThreadId() + " reason=" + e.getBody().getReason();
      case ContinuedEvent e when e.getBody() != null ->
        "continued thread=" + e.getBody().getThreadId()
        + " allThreads=" + e.getBody().getAllThreadsContinued();
      case BreakpointEvent e when e.getBody() != null && e.getBody().getBreakpoint() != null ->
        "breakpoint id=" + e.getBody().getBreakpoint().getId()
        + " verified=" + e.getBody().getBreakpoint().isVerified();
      default -> event.getEvent();
    };
  }

  /** The request's command plus whichever routing id it carries. */
  static String describeRequest(Request request) {
    StringBuilder text = new StringBuilder(request.getCommand());
    switch (request) {
      case StackTraceRequest r when r.getArguments() != null ->
        text.append(" thread=").append(r.getArguments().getThreadId());
      case ContinueRequest r when r.getArguments() != null ->
        text.append(" thread=").append(r.getArguments().getThreadId());
      case NextRequest r when r.getArguments() != null ->
        text.append(" thread=").append(r.getArguments().getThreadId());
      case StepInRequest r when r.getArguments() != null ->
        text.append(" thread=").append(r.getArguments().getThreadId());
      case StepOutRequest r when r.getArguments() != null ->
        text.append(" thread=").append(r.getArguments().getThreadId());
      case PauseRequest r when r.getArguments() != null ->
        text.append(" thread=").append(r.getArguments().getThreadId());
      case ScopesRequest r when r.getArguments() != null ->
        text.append(" frame=").append(r.getArguments().getFrameId());
      case EvaluateRequest r when r.getArguments() != null ->
        text.append(" frame=")
            .append(r.getArguments().getFrameId())
            .append(" expr=")
            .append(r.getArguments().getExpression());
      case VariablesRequest r when r.getArguments() != null ->
        text.append(" ref=").append(r.getArguments().getVariablesReference());
      case SetVariableRequest r when r.getArguments() != null ->
        text.append(" ref=").append(r.getArguments().getVariablesReference());
      case SetBreakpointsRequest r when r.getArguments() != null && r.getArguments().getSource() != null ->
        text.append(" source=").append(r.getArguments().getSource().getName());
      default -> { }
    }
    return text.toString();
  }
}
