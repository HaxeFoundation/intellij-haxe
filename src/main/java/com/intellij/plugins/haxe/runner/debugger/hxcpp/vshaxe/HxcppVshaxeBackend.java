package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;

import com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter.HxcppDebugAdapter;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Backend over the in-process {@link HxcppDebugAdapter} (vshaxe
 * hxcpp-debug-server wire protocol). The adapter binds its debuggee listener
 * in the constructor — before the runner spawns the debuggee — and the DAP
 * conversation runs over a loopback socket pair created at connect time, so
 * the IDE side is exactly a DAP client.
 */
public class HxcppVshaxeBackend implements DapBackend {
  private final HxcppDebugAdapter adapter;
  private volatile ServerSocket loopback;

  public HxcppVshaxeBackend(String debugHost, int debugPort, long debuggeeConnectTimeoutMillis) throws IOException {
    this.adapter = new HxcppDebugAdapter(debugHost, debugPort, debuggeeConnectTimeoutMillis);
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
    return true; // launch = "the debuggee's server connected to the adapter"
  }

  @Override
  public boolean supportsExceptionFilters() {
    return false; // the vshaxe wire protocol has no exception filter toggles
  }

  @Override
  public boolean supportsSmartStepInto() {
    return false; // no such request in the vshaxe wire protocol
  }

  @Override
  public boolean supportsToStringRendering() {
    return false; // the vshaxe server ALWAYS Std.string()s objects; not controllable
  }

  @Override
  public String startupHint() {
    return """
      Check that it was compiled with -debug and -lib hxcpp-debug-server, and that no previous
      instance of the program is still running (a leftover instance blocks the debug port and
      makes new ones crash on startup).""";
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
