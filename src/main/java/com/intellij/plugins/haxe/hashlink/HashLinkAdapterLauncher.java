package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.plugins.haxe.HaxeBundle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Spawns the bundled DAP debug adapter ({@code <plugin>/adapter/hl-debug-adapter.hl})
 * on the HashLink VM and waits for it to announce its listening port on stdout.
 * The caller owns the returned process (wrap it in a ProcessHandler so its
 * remaining output is drained and Stop can kill it).
 */
public final class HashLinkAdapterLauncher {
  private static final String PLUGIN_ID = "com.intellij.plugins.haxe";
  private static final String ADAPTER_RELATIVE_PATH = "adapter/hl-debug-adapter.hl";
  private static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";
  private static final long STARTUP_TIMEOUT_MILLIS = 15_000;
  private static final long POLL_INTERVAL_MILLIS = 20;

  /**
   * A started adapter process, the TCP port it listens on, and the stdout
   * reader used during startup — keep reading from THIS reader (it may hold
   * buffered bytes beyond the port line).
   */
  public record LaunchedAdapter(Process process, int port, BufferedReader stdout) {
  }

  private HashLinkAdapterLauncher() {
  }

  /** Starts {@code hl <adapter.hl> --port 0} and parses the announced port. */
  public static LaunchedAdapter launch(Path hlExecutable) throws ExecutionException {
    Path adapter = bundledAdapterPath();
    Process process;
    try {
      process = new ProcessBuilder(hlExecutable.toString(), adapter.toString(), "--port", "0")
        .redirectErrorStream(true)
        .start();
    } catch (IOException e) {
      throw new ExecutionException("Cannot start the HashLink debug adapter: " + e.getMessage(), e);
    }
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    try {
      int port = awaitListeningPort(process, stdout);
      return new LaunchedAdapter(process, port, stdout);
    } catch (ExecutionException e) {
      process.destroyForcibly();
      throw e;
    }
  }

  /** The adapter bytecode shipped inside the plugin directory. */
  public static Path bundledAdapterPath() throws ExecutionException {
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
    Path adapter = plugin != null ? plugin.getPluginPath().resolve(ADAPTER_RELATIVE_PATH) : null;
    if (adapter == null || !Files.isRegularFile(adapter)) {
      throw new ExecutionException(HaxeBundle.message("haxe.hl.adapter.missing", String.valueOf(adapter)));
    }
    return adapter;
  }

  // Reads stdout until the DAP-ADAPTER-LISTENING:<port> line. Polls with a
  // deadline instead of a blocking readLine so a wedged adapter cannot hang
  // the launch forever; any output before the port line is kept for the error.
  private static int awaitListeningPort(Process process, BufferedReader stdout) throws ExecutionException {
    long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MILLIS;
    StringBuilder seen = new StringBuilder();
    StringBuilder line = new StringBuilder();
    try {
      while (System.currentTimeMillis() < deadline) {
        while (stdout.ready()) {
          int c = stdout.read();
          if (c < 0) {
            break;
          }
          if (c == '\n') {
            String text = line.toString().trim();
            line.setLength(0);
            if (text.startsWith(LISTENING_PREFIX)) {
              return parsePort(text);
            }
            seen.append(text).append('\n');
          } else if (c != '\r') {
            line.append((char)c);
          }
        }
        if (!process.isAlive() && !stdout.ready()) {
          throw new ExecutionException(
            "The HashLink debug adapter exited before announcing its port (exit code "
            + process.exitValue() + ")." + outputSuffix(seen));
        }
        Thread.sleep(POLL_INTERVAL_MILLIS);
      }
    } catch (IOException e) {
      throw new ExecutionException("Cannot read the HashLink debug adapter output: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ExecutionException("Interrupted while starting the HashLink debug adapter", e);
    }
    throw new ExecutionException(
      "The HashLink debug adapter did not announce its port within "
      + (STARTUP_TIMEOUT_MILLIS / 1000) + "s." + outputSuffix(seen));
  }

  private static int parsePort(String listeningLine) throws ExecutionException {
    try {
      return Integer.parseInt(listeningLine.substring(LISTENING_PREFIX.length()).trim());
    } catch (NumberFormatException e) {
      throw new ExecutionException("Malformed adapter port announcement: " + listeningLine);
    }
  }

  private static String outputSuffix(StringBuilder seen) {
    return seen.isEmpty() ? "" : " Adapter output:\n" + seen;
  }
}
