package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequest;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import java.io.Closeable;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The connection strategy behind a DAP debug session — the one interface a
 * new DAP-based debugger implements to get the whole IDE side ({@link
 * DapDebugProcess}, breakpoints, stacks, variables, evaluator) for free.
 * The debug process is a plain DAP client either way; what differs is who
 * the DAP peer is and how it comes up:
 *
 * <ul>
 *   <li>{@code HxcppVshaxeBackend} — the in-process {@code HxcppDebugAdapter}
 *       translating DAP to the vshaxe hxcpp-debug-server wire protocol; the
 *       debuggee's port is a compile-time define.</li>
 *   <li>{@code HxcppIntellijBackend} — the debuggee's embedded
 *       {@code intellij-hxcpp-debug-server} speaks DAP natively and connects
 *       OUT to a listener the runner bound on an ephemeral port before
 *       spawning it (handed over via HXCPP_DEBUG_HOST/PORT env vars).</li>
 *   <li>{@code InterpDapBackend} — the haxe eval VM's debug server, bridged
 *       by the in-process {@code EvalDebugAdapter}.</li>
 *   <li>{@code HashLinkBackend} — the bundled external {@code
 *       hl-debug-adapter.hl} process, spoken to over TCP; it attaches to the
 *       runner-spawned debuggee by pid.</li>
 * </ul>
 *
 * A backend is created by the debug runner BEFORE the debuggee is spawned (a
 * listening backend must be up when the debuggee starts connecting) and
 * closed by the debug process on teardown. Every hook has a default, so a
 * minimal backend only implements the connection and capability flags.
 */
public interface DapBackend extends Closeable {

  /**
   * Blocks until the DAP peer is ready and returns the client for the session.
   * Called once, on the debug process's request thread.
   */
  DapEndpoint connect() throws IOException;

  /**
   * Called right after {@link #connect()} succeeded, on the request thread —
   * the place to start backend-owned background work that reports through the
   * session (e.g. draining an external adapter's own output into the console).
   */
  default void onConnected(DapDebugProcess process) {
  }

  /**
   * The runner spawned the debuggee; called before the debug session starts.
   * Backends that attach by pid capture it here.
   */
  default void debuggeeSpawned(ColoredProcessHandler debuggeeHandler) {
  }

  /**
   * Whether the wire protocol includes a launch request (the vshaxe adapter's
   * does; the IntelliJ server considers the debuggee launched once connected).
   */
  boolean requiresLaunchRequest();

  /**
   * The launch request to send when {@link #requiresLaunchRequest()}; a
   * backend attaching to an already-spawned debuggee returns one carrying its
   * attach arguments.
   */
  default Request launchRequest() {
    return new LaunchRequest();
  }

  /**
   * The IDE's Terminate button is about to destroy the debuggee's process.
   * A backend whose debuggee is OS-debug-attached must break the attachment
   * here first (killing the debugger releases the debuggee), or the destroy
   * hangs on "waiting for process detach".
   */
  default void beforeDebuggeeDestroyed() {
  }

  /**
   * Whether the adapter emits its {@code initialized} event only AFTER the
   * launch response (the vscode web adapters do — launch actually starts the
   * browser); the haxe-side servers emit it right after initialize. Decides
   * where the debug process waits for it.
   */
  default boolean initializedEventAfterLaunch() {
    return false;
  }

  /**
   * Whether to send {@code configurationDone} after configuration. The
   * vscode-firefox-debug adapter reports {@code
   * supportsConfigurationDoneRequest=false} and needs none — its debuggee
   * simply runs, with breakpoints applied as they arrive.
   */
  default boolean sendsConfigurationDone() {
    return true;
  }

  /**
   * Whether the launch response arrives promptly enough to await. js-debug
   * answers launch only AFTER configurationDone, so awaiting it synchronously
   * would deadlock the setup sequence — such a backend returns false and the
   * launch is sent fire-and-forget.
   */
  default boolean awaitsLaunchResponse() {
    return true;
  }

  /**
   * Whether the server understands the {@code setExceptionBreakpoints}
   * filters (and thus whether the exception breakpoint types should drive
   * this session). The filter vocabulary comes from {@link #anyThrowFilterId}
   * / {@link #uncaughtFilterId} / {@link #criticalFilterId}.
   */
  boolean supportsExceptionFilters();

  /** The server's id for "stop on every throw, caught or not". */
  default String anyThrowFilterId() {
    return "thrown";
  }

  /** The server's id for "stop when no catch will handle the throw". */
  default String uncaughtFilterId() {
    return "uncaught";
  }

  /** The server's id for "stop on runtime critical errors" (null access, ...). */
  default String criticalFilterId() {
    return "critical";
  }

  /** Whether smart step into is available ({@link #createSmartStepIntoHandler}). */
  boolean supportsSmartStepInto();

  /**
   * How long one DAP request may wait for its response. The browser backends
   * use a SHORTER budget: firefox's per-actor FIFO queue can wedge on a
   * request the browser never answers (a devtools-internal crash while
   * previewing a worker object), and every request the IDE sends is
   * serialized on one thread — a long timeout turns one wedged request into a
   * long total freeze of the debugger views.
   */
  default long requestTimeoutMillis() {
    return 15_000;
  }

  /**
   * Whether the debuggee's threads pause and resume INDEPENDENTLY (the
   * browser targets: each worker is its own JS VM; the adapters' stopped
   * events are always allThreadsStopped=false and there is NO whole-program
   * pause/resume on the wire). For such a backend: a stop arriving while a
   * pause is already on screen leaves that thread paused but untouched
   * UI-wise (inspectable via the thread list), and the IDE's Resume sends a
   * continue to EVERY listed thread — background threads paused at their own
   * breakpoints and any zombie a page reload left behind are released too,
   * so the IDE and runtime states cannot drift apart (a running thread
   * answers the continue with an error, harmless). Emulating suspend-all by
   * pausing the other threads is deliberately NOT done: the firefox
   * adapter's per-thread FIFO request queue wedges forever on an interrupt
   * that races a breakpoint pause. The haxe-side servers are suspend-all
   * natively and keep the plain single continue.
   */
  default boolean threadsPauseIndependently() {
    return false;
  }

  /**
   * Whether the adapter evaluates expressions against a FOREIGN runtime that
   * legitimately knows more than the Haxe PSI — the browser JS runtime, reached
   * through externs that may not map every field. In such a session the
   * evaluate/watch views must not flag "unresolved" identifiers as errors: the
   * expression evaluates fine and the runtime is the source of truth (the haxe
   * native runtimes match the PSI, so their sessions keep the strict checks).
   */
  default boolean evaluatesAgainstForeignRuntime() {
    return false;
  }

  /**
   * The smart-step-into handler for this backend. The default resolves the
   * targets from the Haxe PSI and sends the custom {@code
   * intellij/stepIntoFunction} request; a backend whose adapter reports
   * targets itself (DAP {@code stepInTargets}) supplies its own handler.
   */
  default @Nullable XSmartStepIntoHandler<?> createSmartStepIntoHandler(DapDebugProcess process) {
    return supportsSmartStepInto() ? new PsiResolvedSmartStepHandler(process) : null;
  }

  /** An extra Debug tool window tab (e.g. a Registers view), or null for none. */
  default @Nullable XDebugTabLayouter createTabLayouter(DapDebugProcess process) {
    return null;
  }

  /** Called after a successful Set Value, for views the write may invalidate. */
  default void afterSetVariable(DapDebugProcess process) {
  }

  /**
   * Rewrites an expression or breakpoint condition before it is sent (e.g.
   * qualifying bare class names against the source file's imports). The
   * default sends expressions as written. Callers never pass null — an
   * absent condition is simply not qualified.
   */
  default String qualifyExpression(Project project, @Nullable XSourcePosition position, @NotNull String expression) {
    return expression;
  }

  /**
   * Maps a frame's server-reported source path to an IDE position. The
   * default resolves absolute paths directly and relative ones through the
   * filename index.
   */
  default @Nullable XSourcePosition resolveSource(Project project, @Nullable String path, StackFrame frame) {
    return DapSourceResolver.resolve(project, path, frame.getLine());
  }

  /**
   * The wire form of a breakpoint's source path. The IDE's VFS paths use
   * FORWARD slashes even on Windows; the haxe-side servers normalize
   * separators themselves, but the vscode web adapters match paths literally
   * and silently never bind a forward-slash path — such a backend converts to
   * native separators here.
   */
  default String breakpointSourcePath(String vfsPath) {
    return vfsPath;
  }

  /**
   * Whether the server understands the custom
   * {@code intellij/setToStringRendering} request (object labels via the
   * object's own toString, toggleable live). The vshaxe server does not —
   * it always renders through Std.string and offers no control.
   */
  boolean supportsToStringRendering();

  /**
   * Whether the server understands the custom
   * {@code intellij/setExpressionStepping} request (eval only: each step
   * becomes one raw interpreter sub-step and stack frames carry the exact
   * expression span for the editor highlight).
   */
  default boolean supportsExpressionStepping() {
    return false;
  }

  /** Appended to the "program exited before the debugger could attach" failure. */
  String startupHint();
}
