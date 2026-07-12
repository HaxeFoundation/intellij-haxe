package com.intellij.plugins.haxe.hashlink;

import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.SetBreakpointsResponse;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Bookkeeping for line breakpoints. DAP's {@code setBreakpoints} replaces the
 * whole set for a file, so this keeps breakpoints grouped per source file and
 * re-sends the file's complete list on every change. Before the session is
 * live (pre-configurationDone), register/unregister only mutate the maps; the
 * debug process flushes everything once during initialization.
 */
final class HashLinkBreakpointManager {
  private final HashLinkDebugProcess process;
  private final Map<String, LinkedHashSet<XLineBreakpoint<XBreakpointProperties>>> byFile = new LinkedHashMap<>();
  private boolean live = false;

  HashLinkBreakpointManager(HashLinkDebugProcess process) {
    this.process = process;
  }

  synchronized void register(XLineBreakpoint<XBreakpointProperties> breakpoint) {
    String path = filePath(breakpoint);
    if (path == null) {
      return;
    }
    byFile.computeIfAbsent(path, p -> new LinkedHashSet<>()).add(breakpoint);
    if (live) {
      process.onRequestThread(() -> flushFile(path));
    }
  }

  synchronized void unregister(XLineBreakpoint<XBreakpointProperties> breakpoint) {
    String path = filePath(breakpoint);
    if (path == null) {
      return;
    }
    LinkedHashSet<XLineBreakpoint<XBreakpointProperties>> set = byFile.get(path);
    if (set != null && set.remove(breakpoint) && live) {
      process.onRequestThread(() -> flushFile(path));
    }
  }

  /** Sends every file's breakpoints; called once from initialization (request thread). */
  void flushAll() {
    List<String> paths;
    synchronized (this) {
      live = true;
      paths = new ArrayList<>(byFile.keySet());
    }
    for (String path : paths) {
      flushFile(path);
    }
  }

  // Runs on the request thread only.
  private void flushFile(String path) {
    List<XLineBreakpoint<XBreakpointProperties>> ordered;
    synchronized (this) {
      LinkedHashSet<XLineBreakpoint<XBreakpointProperties>> set = byFile.get(path);
      ordered = set == null ? List.of() : new ArrayList<>(set);
    }

    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(path);
    source.setName(Path.of(path).getFileName().toString());
    arguments.setSource(source);
    List<SourceBreakpoint> requested = new ArrayList<>(ordered.size());
    for (XLineBreakpoint<XBreakpointProperties> breakpoint : ordered) {
      SourceBreakpoint sb = new SourceBreakpoint();
      sb.setLine(breakpoint.getLine() + 1); // DAP lines are 1-based
      sb.setCondition(conditionOf(breakpoint)); // the IDE's "Condition" field, evaluated at each hit
      requested.add(sb);
    }
    arguments.setBreakpoints(requested);
    request.setArguments(arguments);

    Response response = process.sendRequest(request);
    if (!(response instanceof SetBreakpointsResponse setResponse) || !response.isSuccess()) {
      return;
    }
    // responses come back in request order
    List<Breakpoint> results = setResponse.getBody().getBreakpoints();
    for (int i = 0; i < ordered.size() && i < results.size(); i++) {
      Breakpoint result = results.get(i);
      XLineBreakpoint<XBreakpointProperties> breakpoint = ordered.get(i);
      if (result.isVerified()) {
        process.getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_verified_breakpoint, null);
      } else {
        process.getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_invalid_breakpoint,
                                                          "No executable code at this line");
      }
    }
  }

  private static String filePath(XLineBreakpoint<XBreakpointProperties> breakpoint) {
    var position = breakpoint.getSourcePosition();
    return position != null ? position.getFile().getPath() : null;
  }

  // The IDE's per-breakpoint "Condition" expression, or null when unset/blank.
  // The adapter evaluates it at each hit and only stops when it is true.
  private static String conditionOf(XLineBreakpoint<XBreakpointProperties> breakpoint) {
    XExpression condition = breakpoint.getConditionExpression();
    if (condition == null) {
      return null;
    }
    String text = condition.getExpression();
    return text != null && !text.isBlank() ? text : null;
  }
}
