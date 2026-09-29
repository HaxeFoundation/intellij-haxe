package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.BreakpointEvent;

/**
 * Bookkeeping for line breakpoints. DAP's {@code setBreakpoints} replaces the
 * whole set for a file, so this keeps breakpoints grouped per source file and
 * re-sends the file's complete list on every change. Before the session is
 * live (pre-configurationDone), register/unregister only mutate the maps; the
 * debug process flushes everything once during initialization.
 *
 * Conditions are passed through as written — the hxcpp-debug-server evaluates
 * them with its own expression interpreter at each hit.
 */
final class DapBreakpointManager {
  private final DapDebugProcess process;
  private final Map<String, LinkedHashSet<XLineBreakpoint<XBreakpointProperties>>> byFile = new LinkedHashMap<>();
  // adapter breakpoint id -> IDE breakpoint, rebuilt from each flush response;
  // lets async breakpoint events (source-map-lazy verification) find their
  // gutter icon
  private final Map<Integer, XLineBreakpoint<XBreakpointProperties>> byAdapterId = new LinkedHashMap<>();
  private boolean live = false;
  // A transient "run to cursor" line breakpoint (file path + 1-based line): appended
  // to its file's set while active, removed on the next stop. -1 line means none.
  private String runToPath;
  private int runToLine = -1;

  DapBreakpointManager(DapDebugProcess process) {
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

  /**
   * One-line description of the line breakpoints this session armed (file
   * names with 1-based lines), for the session console. The first thing to
   * check when a session "does not stop": an empty set means the IDE side
   * never handed the session any breakpoints, while armed-but-unverified ones
   * show a rejected icon on their gutter line instead. Files the backend
   * scoped out (a same-named file from a sibling project) are listed as
   * skipped so their silence is explained too.
   */
  synchronized String armedDescription() {
    StringBuilder armed = new StringBuilder();
    StringBuilder skipped = new StringBuilder();
    for (Map.Entry<String, LinkedHashSet<XLineBreakpoint<XBreakpointProperties>>> entry : byFile.entrySet()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      StringBuilder target = process.backend().acceptsBreakpointFile(entry.getKey()) ? armed : skipped;
      if (!target.isEmpty()) {
        target.append("; ");
      }
      target.append(Path.of(entry.getKey()).getFileName()).append(':');
      String lines = entry.getValue().stream()
        .map(breakpoint -> String.valueOf(breakpoint.getLine() + 1))
        .collect(Collectors.joining(","));
      target.append(lines);
    }
    StringBuilder description = new StringBuilder();
    description.append(armed.isEmpty() ? "no line breakpoints registered" : "line breakpoints armed: " + armed);
    if (!skipped.isEmpty()) {
      description.append(" — outside this build's classpaths, not armed: ").append(skipped);
    }
    return description.toString();
  }

  /**
   * Adds a transient run-to-cursor breakpoint at path:line (1-based) and flushes that
   * file. Returns whether the line resolved to executable code (so the caller knows
   * whether resuming will actually stop there). Runs on the request thread.
   */
  boolean setRunToBreakpoint(String path, int line) {
    synchronized (this) {
      runToPath = path;
      runToLine = line;
    }
    return flushFile(path);
  }

  /** Removes the transient run-to-cursor breakpoint (if any) and reflushes its file. */
  void clearRunToBreakpoint() {
    String path;
    synchronized (this) {
      if (runToLine < 0) {
        return;
      }
      path = runToPath;
      runToPath = null;
      runToLine = -1;
    }
    if (path != null) {
      flushFile(path);
    }
  }

  // Runs on the request thread (or event pump). Returns whether the transient
  // run-to line for this file (if any) resolved to code; true when there is none.
  private boolean flushFile(String path) {
    // a file the backend scopes out (outside the build's source directories)
    // is never offered - the debuggee-side name/suffix matching would bind it
    // onto a same-named file that IS part of this build
    if (!process.backend().acceptsBreakpointFile(path)) {
      return false;
    }
    List<XLineBreakpoint<XBreakpointProperties>> ordered;
    boolean appendRunTo;
    int runToLineLocal;
    synchronized (this) {
      LinkedHashSet<XLineBreakpoint<XBreakpointProperties>> set = byFile.get(path);
      ordered = set == null ? List.of() : new ArrayList<>(set);
      appendRunTo = path.equals(runToPath) && runToLine > 0;
      runToLineLocal = runToLine;
    }

    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    // the backend decides the wire form (VFS forward slashes vs native)
    source.setPath(process.backend().breakpointSourcePath(path));
    source.setName(Path.of(path).getFileName().toString());
    arguments.setSource(source);

    List<SourceBreakpoint> requested = new ArrayList<>(ordered.size() + 1);
    for (XLineBreakpoint<XBreakpointProperties> breakpoint : ordered) {
      SourceBreakpoint sb = new SourceBreakpoint();
      sb.setLine(breakpoint.getLine() + 1); // DAP lines are 1-based
      // the IDE's "Condition" field, evaluated by the server at each hit -
      // qualified like watch expressions (against the breakpoint's file)
      String condition = conditionOf(breakpoint);
      if (condition != null) {
        condition = process.backend().qualifyExpression(
          process.getSession().getProject(), breakpoint.getSourcePosition(), condition);
      }
      sb.setCondition(condition);
      requested.add(sb);
    }
    if (appendRunTo) {
      SourceBreakpoint runTo = new SourceBreakpoint();
      runTo.setLine(runToLineLocal); // already 1-based
      requested.add(runTo);
    }
    arguments.setBreakpoints(requested);
    request.setArguments(arguments);

    Response response = process.sendRequest(request);
    if (!(response instanceof SetBreakpointsResponse setResponse) || !response.isSuccess()) {
      return false;
    }
    // responses come back in request order: the breakpoints, then the run-to line
    List<Breakpoint> results = setResponse.getBody().getBreakpoints();
    for (int i = 0; i < ordered.size() && i < results.size(); i++) {
      Breakpoint result = results.get(i);
      XLineBreakpoint<XBreakpointProperties> breakpoint = ordered.get(i);
      if (result.getId() != null) {
        synchronized (this) {
          byAdapterId.put(result.getId(), breakpoint);
        }
      }
      presentBreakpointState(breakpoint, result);
    }
    if (appendRunTo && ordered.size() < results.size()) {
      return results.get(ordered.size()).isVerified();
    }
    return true;
  }

  /**
   * An async breakpoint state change pushed by the adapter — the web adapters
   * verify lazily: setBreakpoints answers verified=false, and this event
   * upgrades the breakpoint once the source map resolves it (or downgrades it
   * when a reload invalidates it). Runs on the event pump.
   */
  void onBreakpointEvent(BreakpointEvent event) {
    if (event.getBody() == null || event.getBody().getBreakpoint() == null) {
      return;
    }
    Breakpoint state = event.getBody().getBreakpoint();
    if (state.getId() == null) {
      return;
    }
    XLineBreakpoint<XBreakpointProperties> breakpoint;
    synchronized (this) {
      breakpoint = byAdapterId.get(state.getId());
    }
    if (breakpoint != null) {
      presentBreakpointState(breakpoint, state);
    }
  }

  private void presentBreakpointState(XLineBreakpoint<XBreakpointProperties> breakpoint, Breakpoint state) {
    if (state.isVerified()) {
      process.getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_verified_breakpoint, null);
    } else {
      // prefer the server's reason (line-table reject vs unknown file) over the generic text
      String message = state.getMessage() != null ? state.getMessage() : "No executable code at this line";
      process.getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_invalid_breakpoint, message);
    }
  }

  private static String filePath(XLineBreakpoint<XBreakpointProperties> breakpoint) {
    var position = breakpoint.getSourcePosition();
    return position != null ? position.getFile().getPath() : null;
  }

  // The IDE's per-breakpoint "Condition" expression, or null when unset/blank.
  // The server evaluates it at each hit and only stops when it is true.
  private static String conditionOf(XLineBreakpoint<XBreakpointProperties> breakpoint) {
    XExpression condition = breakpoint.getConditionExpression();
    if (condition == null) {
      return null;
    }
    String text = condition.getExpression();
    return text != null && !text.isBlank() ? text : null;
  }
}
