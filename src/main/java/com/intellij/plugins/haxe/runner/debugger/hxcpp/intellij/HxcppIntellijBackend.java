package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapSourceResolver;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapSourceScopes;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XSourcePosition;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Backend for the debuggee's embedded {@code intellij-hxcpp-debug-server}: the
 * server compiled INTO the executable speaks DAP natively, so there is no
 * adapter — the IDE listens on an ephemeral loopback port (bound in the
 * constructor, BEFORE the runner spawns the debuggee) and the debuggee
 * connects out to it during startup, guided by the HXCPP_DEBUG_HOST/PORT env
 * vars the runner sets. An ephemeral port per session means concurrent debug
 * sessions never collide and nothing is baked into the executable.
 */
public class HxcppIntellijBackend implements DapBackend {
  /** Env vars read by the haxelib's Config.resolve (env beats defines beats defaults). */
  public static final String ENV_DEBUG_HOST = "HXCPP_DEBUG_HOST";
  public static final String ENV_DEBUG_PORT = "HXCPP_DEBUG_PORT";

  private final ServerSocket listener;
  private final int acceptTimeoutMillis;
  private final List<String> sourceDirectories;

  /**
   * With the build's source directories (absolute, VFS separators): the
   * server matches breakpoint files by their compile-time relative names, so
   * without a scope every same-named file in the IDE project is offered too —
   * and since setBreakpoints REPLACES a file's whole set, the sibling
   * project's same-named file then clobbers the real file's breakpoints. An
   * empty list disables the scoping.
   */
  public HxcppIntellijBackend(int acceptTimeoutMillis, List<String> sourceDirectories) throws IOException {
    this.listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    this.acceptTimeoutMillis = acceptTimeoutMillis;
    this.sourceDirectories = List.copyOf(sourceDirectories);
  }

  @Override
  public boolean acceptsBreakpointFile(String vfsPath) {
    return DapSourceScopes.acceptsWhenScoped(vfsPath, sourceDirectories);
  }

  @Override
  public @Nullable XSourcePosition resolveSource(Project project, @Nullable String path, StackFrame frame) {
    return DapSourceResolver.resolve(project, path, frame.getLine(), sourceDirectories);
  }

  /** The host the spawned debuggee should connect to (loopback). */
  public String getHost() {
    return listener.getInetAddress().getHostAddress();
  }

  /** The ephemeral port this session listens on. */
  public int getPort() {
    return listener.getLocalPort();
  }

  @Override
  public DapClient connect() throws IOException {
    listener.setSoTimeout(acceptTimeoutMillis);
    try {
      Socket debuggee = listener.accept();
      return new DapClient(new DapConnection(debuggee));
    } catch (SocketTimeoutException e) {
      throw new IOException("the program did not connect to the debugger within "
                            + (acceptTimeoutMillis / 1000) + "s — was it compiled with -debug and "
                            + "-lib " + HaxeDebugAdditions.HXCPP_DEBUG_SERVER_LIB + "?");
    }
  }

  @Override
  public boolean requiresLaunchRequest() {
    return false; // the accepted connection IS the launch
  }

  @Override
  public boolean supportsExceptionFilters() {
    return true; // uncaught/critical (see the server's initialize capabilities)
  }

  @Override
  public boolean supportsSmartStepInto() {
    return true; // the server implements custom/stepIntoFunction
  }

  @Override
  public boolean supportsToStringRendering() {
    return true; // the server implements custom/setToStringRendering
  }

  @Override
  public String startupHint() {
    return "Check that it was compiled with -debug and -lib " + HaxeDebugAdditions.HXCPP_DEBUG_SERVER_LIB + ".";
  }

  @Override
  public void close() throws IOException {
    listener.close();
  }
}
