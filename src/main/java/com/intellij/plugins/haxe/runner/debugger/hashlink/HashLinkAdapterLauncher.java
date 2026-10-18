package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.AdapterProcess;
import com.intellij.plugins.haxe.util.HaxePluginPaths;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Spawns the bundled DAP debug adapter ({@code <plugin>/adapter/hl-debug-adapter.hl})
 * on the HashLink VM and waits for it to announce its listening port on stdout.
 * The caller owns the returned process (wrap it in a ProcessHandler so its
 * remaining output is drained and Stop can kill it).
 */
public final class HashLinkAdapterLauncher {
  private static final String ADAPTER_RELATIVE_PATH = "adapter/hl-debug-adapter.hl";
  private static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";

  private HashLinkAdapterLauncher() {
  }

  /// Starts `hl <adapter.hl> --port 0` and parses the announced port.
  public static AdapterProcess launch(Path hlExecutable) throws IOException {
    ProcessBuilder command = new ProcessBuilder(hlExecutable.toString(), bundledAdapterPath().toString(), "--port", "0");
    return AdapterProcess.launch(command, line -> line.startsWith(LISTENING_PREFIX), HashLinkAdapterLauncher::announcedPort);
  }

  private static int announcedPort(String listeningLine) {
    return Integer.parseInt(listeningLine.substring(LISTENING_PREFIX.length()).trim());
  }

  /** The adapter bytecode shipped inside the plugin directory. */
  private static Path bundledAdapterPath() throws IOException {
    Path adapter = HaxePluginPaths.bundledFile(ADAPTER_RELATIVE_PATH);
    if (adapter == null) {
      Path home = HaxePluginPaths.pluginHome();
      String expected = home != null ? home.resolve(ADAPTER_RELATIVE_PATH).toString() : ADAPTER_RELATIVE_PATH;
      throw new IOException(HaxeDebuggerBundle.message("haxe.hl.adapter.missing", expected));
    }
    return adapter;
  }
}
