package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.plugins.haxe.runner.debugger.dap.transport.AdapterProcess;
import com.intellij.util.net.NetUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Spawns a vscode debug adapter bundle on the user's node in DAP-over-TCP
 * server mode and hands back the process + port. Wire behaviour pinned by
 * FirefoxAdapterLiveTest:
 *
 * <ul>
 *   <li>{@code --server=<port>} needs a 4-5 digit port (the adapter's own
 *       argv regex — it cannot bind port 0 itself), so the port is chosen
 *       HERE and passed explicitly;</li>
 *   <li>the adapter prints {@code waiting for debug protocol on port N}
 *       slightly BEFORE its listener accepts — callers must connect with a
 *       short retry, which {@code DapClient.connect} alone does not do;</li>
 *   <li>the returned stdout reader may hold buffered output beyond the
 *       announcement; keep reading THAT reader (gobble into the console) or
 *       the adapter can block on a full pipe.</li>
 * </ul>
 */
public final class JsAdapterLauncher {
  private static final String LISTENING_MARKER = "waiting for debug protocol";
  private static final String JS_DEBUG_LISTENING_MARKER = "Debug server listening";

  private JsAdapterLauncher() {
  }

  /**
   * Starts {@code node <bundle> --server=<port>} (cwd = the bundle's directory,
   * where its wasm/asset siblings live) and waits for the port announcement.
   * The caller owns the process. The vscode-firefox-debug flavour: the port is
   * chosen HERE (its argv regex wants 4-5 digits; no ephemeral-port support).
   */
  public static AdapterProcess launch(Path nodeExecutable, Path adapterBundle) throws IOException {
    // OS-assigned, never random: a random pick can land in a Windows
    // excluded port range (Hyper-V/WinNAT reserve blocks of the ephemeral
    // space) and the adapter then dies with EACCES before announcing.
    // Ephemeral ports are 5 digits, satisfying the adapter's argv regex.
    int port = NetUtils.findAvailableSocketPort();
    ProcessBuilder command = new ProcessBuilder(nodeExecutable.toString(), adapterBundle.toString(), "--server=" + port)
      .directory(adapterBundle.getParent().toFile());
    return AdapterProcess.launch(command, line -> line.contains(LISTENING_MARKER), line -> port);
  }

  /**
   * The vscode-js-debug flavour: {@code node dapDebugServer.js 0 127.0.0.1}
   * — port 0 makes the OS pick, the announcement carries the actual port
   * ("Debug server listening at 127.0.0.1:NNNN"), and the explicit host keeps
   * the listener loopback-only.
   */
  public static AdapterProcess launchJsDebug(Path nodeExecutable, Path dapServerJs) throws IOException {
    ProcessBuilder command = new ProcessBuilder(nodeExecutable.toString(), dapServerJs.toString(), "0", "127.0.0.1")
      .directory(dapServerJs.getParent().toFile());
    return AdapterProcess.launch(command, line -> line.contains(JS_DEBUG_LISTENING_MARKER), JsAdapterLauncher::announcedPort);
  }

  // "Debug server listening at 127.0.0.1:NNNN"
  private static int announcedPort(String announcement) {
    return Integer.parseInt(announcement.substring(announcement.lastIndexOf(':') + 1).trim());
  }
}
