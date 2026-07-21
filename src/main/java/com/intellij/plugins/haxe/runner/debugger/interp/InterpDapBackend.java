package com.intellij.plugins.haxe.runner.debugger.interp;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import com.intellij.plugins.haxe.runner.debugger.eval.EvalDebugAdapter;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Backend over the in-process {@link EvalDebugAdapter} (the haxe compiler's
 * eval-debugger wire protocol). The adapter binds its VM listener in the
 * constructor — before the runner spawns haxe — and the DAP conversation runs
 * over a loopback socket pair created at connect time, so the IDE side is
 * exactly a DAP client (the same {@code DapDebugProcess} machinery drives
 * this session).
 */
public class InterpDapBackend implements DapBackend {
  private final EvalDebugAdapter adapter;
  private volatile ServerSocket loopback;

  public InterpDapBackend(long vmConnectTimeoutMillis) throws IOException {
    this.adapter = new EvalDebugAdapter(vmConnectTimeoutMillis);
  }

  /** The port for the spawned haxe's {@code -D eval-debugger=127.0.0.1:<port>}. */
  public int getVmPort() {
    return adapter.getVmPort();
  }

  @Override
  public DapClient connect() throws IOException {
    ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    loopback = listener;
    Socket clientSide = new Socket("127.0.0.1", listener.getLocalPort());
    adapter.start(new DapConnection(listener.accept()));
    return new DapClient(new DapConnection(clientSide));
  }

  @Override
  public boolean requiresLaunchRequest() {
    return true; // launch = "the eval VM connected and is waiting before main"
  }

  @Override
  public boolean supportsExceptionFilters() {
    // the adapter maps the IDE's filter words onto the VM's setExceptionOptions
    // ("thrown" -> "all", "uncaught" -> "uncaught"). This must stay ON even
    // with every exception breakpoint disabled: the VM's DEFAULT is to stop on
    // uncaught exceptions, so the empty filter set the IDE then sends is what
    // lets an uncaught throw kill the program naturally instead of stopping.
    return true;
  }

  @Override
  public boolean supportsSmartStepInto() {
    // no such request in the eval WIRE protocol, but the adapter emulates
    // intellij/stepIntoFunction via sub-expression stepping (see
    // EvalDebugAdapter.handleStepIntoFunction)
    return true;
  }

  @Override
  public boolean supportsToStringRendering() {
    return false; // eval values are already rendered by the VM; not controllable
  }

  @Override
  public boolean supportsExpressionStepping() {
    return true; // the adapter's intellij/setExpressionStepping mode (eval only)
  }

  @Override
  public String startupHint() {
    return """
      The haxe process exited before its eval VM attached. Check the compiler arguments
      (they must form a valid compilation) and that haxe is version 4.0 or newer.""";
  }

  @Override
  public void close() throws IOException {
    try {
      adapter.close();
    } finally {
      ServerSocket listener = loopback;
      loopback = null;
      if (listener != null) {
        listener.close();
      }
    }
  }
}
