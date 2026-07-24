package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.util.net.NetUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Spawns a vscode debug adapter bundle on the user's node in DAP-over-TCP
 * server mode and hands back the process + port. Wire behaviour pinned by
 * FirefoxAdapterLiveProbe:
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
public final class BrowserAdapterLauncher {
  private static final String LISTENING_MARKER = "waiting for debug protocol";
  private static final String JS_DEBUG_LISTENING_MARKER = "Debug server listening";
  private static final long ANNOUNCE_TIMEOUT_MILLIS = 15_000;
  private static final long POLL_INTERVAL_MILLIS = 20;

  /** A started adapter: the node process, the TCP port, and its live stdout. */
  public record LaunchedAdapter(Process process, int port, BufferedReader stdout) {
  }

  private BrowserAdapterLauncher() {
  }

  /**
   * Starts {@code node <bundle> --server=<port>} (cwd = the bundle's directory,
   * where its wasm/asset siblings live) and waits for the port announcement.
   * The caller owns the process. The vscode-firefox-debug flavour: the port is
   * chosen HERE (its argv regex wants 4-5 digits; no ephemeral-port support).
   */
  public static LaunchedAdapter launch(Path nodeExecutable, Path adapterBundle) throws IOException {
    // OS-assigned, never random: a random pick can land in a Windows
    // excluded port range (Hyper-V/WinNAT reserve blocks of the ephemeral
    // space) and the adapter then dies with EACCES before announcing.
    // Ephemeral ports are 5 digits, satisfying the adapter's argv regex.
    int port = NetUtils.findAvailableSocketPort();
    Process process = new ProcessBuilder(
      nodeExecutable.toString(), adapterBundle.toString(), "--server=" + port)
      .directory(adapterBundle.getParent().toFile())
      .redirectErrorStream(true)
      .start();
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    try {
      awaitAnnouncement(process, stdout, LISTENING_MARKER);
      return new LaunchedAdapter(process, port, stdout);
    } catch (IOException e) {
      process.destroyForcibly();
      throw e;
    }
  }

  /**
   * The vscode-js-debug flavour: {@code node dapDebugServer.js 0 127.0.0.1}
   * — port 0 makes the OS pick, the announcement carries the actual port
   * ("Debug server listening at 127.0.0.1:NNNN"), and the explicit host keeps
   * the listener loopback-only.
   */
  public static LaunchedAdapter launchJsDebug(Path nodeExecutable, Path dapServerJs) throws IOException {
    Process process = new ProcessBuilder(
      nodeExecutable.toString(), dapServerJs.toString(), "0", "127.0.0.1")
      .directory(dapServerJs.getParent().toFile())
      .redirectErrorStream(true)
      .start();
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    try {
      String announcement = awaitAnnouncement(process, stdout, JS_DEBUG_LISTENING_MARKER);
      int colon = announcement.lastIndexOf(':');
      int port;
      try {
        port = Integer.parseInt(announcement.substring(colon + 1).trim());
      } catch (RuntimeException e) {
        throw new IOException("Malformed js-debug port announcement: " + announcement);
      }
      return new LaunchedAdapter(process, port, stdout);
    } catch (IOException e) {
      process.destroyForcibly();
      throw e;
    }
  }

  // Polls stdout for the announcement line with a deadline (a blocking
  // readLine could hang the launch on a wedged adapter forever). Everything
  // read before the marker is kept for the failure message. Returns the
  // matched line (js-debug's carries the actual port).
  private static String awaitAnnouncement(Process process, BufferedReader stdout, String marker) throws IOException {
    long deadline = System.currentTimeMillis() + ANNOUNCE_TIMEOUT_MILLIS;
    StringBuilder seen = new StringBuilder();
    StringBuilder line = new StringBuilder();
    while (System.currentTimeMillis() < deadline) {
      while (stdout.ready()) {
        int c = stdout.read();
        if (c < 0) {
          break;
        }
        if (c == '\n') {
          String text = line.toString().trim();
          line.setLength(0);
          if (text.contains(marker)) {
            return text;
          }
          seen.append(text).append('\n');
        } else if (c != '\r') {
          line.append((char)c);
        }
      }
      if (!process.isAlive() && !stdout.ready()) {
        throw new IOException("The debug adapter exited (code " + process.exitValue()
                              + ") before announcing its port." + outputSuffix(seen));
      }
      try {
        Thread.sleep(POLL_INTERVAL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while starting the debug adapter", e);
      }
    }
    throw new IOException("The debug adapter did not announce its port within "
                          + (ANNOUNCE_TIMEOUT_MILLIS / 1000) + "s." + outputSuffix(seen));
  }

  private static String outputSuffix(StringBuilder seen) {
    return seen.isEmpty() ? "" : " Adapter output:\n" + seen;
  }
}
