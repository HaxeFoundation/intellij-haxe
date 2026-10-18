package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeRuntimeTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Finds and validates the node runtime the debug adapters run on: an explicit
 * configured path wins, else {@code node} from PATH, and anything below the
 * supported major (EOL) is refused with the same install hint as a missing
 * node.
 */
public final class NodeLocator {
  static final int MIN_NODE_MAJOR = 18;

  private NodeLocator() {
  }

  public static Path locate(String configuredNodePath) throws IOException {
    if (!configuredNodePath.isBlank()) {
      Path node = Path.of(configuredNodePath);
      if (!Files.isRegularFile(node)) {
        throw new IOException(HaxeDebuggerBundle.message("javascript.node.configured.missing", configuredNodePath));
      }
      return node;
    }
    String onPath = HaxeRuntimeTool.NODE.fromPath(HaxeRuntimeTool.PROCESS_ENVIRONMENT);
    if (onPath == null) {
      throw new IOException(HaxeDebuggerBundle.message("javascript.node.not.found"));
    }
    return Path.of(onPath);
  }

  // `node --version` prints e.g. v24.18.0
  public static void requireModern(Path node) throws IOException {
    String version;
    try {
      Process probe = new ProcessBuilder(node.toString(), "--version").redirectErrorStream(true).start();
      version = new String(probe.getInputStream().readAllBytes()).trim();
      if (!probe.waitFor(10, TimeUnit.SECONDS)) {
        probe.destroyForcibly();
        throw new IOException(HaxeDebuggerBundle.message("javascript.node.broken", node, "--version timed out"));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while checking node", e);
    }
    int major = parseMajor(version);
    if (major < 0) {
      throw new IOException(HaxeDebuggerBundle.message("javascript.node.broken", node, version));
    }
    if (major < MIN_NODE_MAJOR) {
      throw new IOException(HaxeDebuggerBundle.message("javascript.node.too.old", version, MIN_NODE_MAJOR));
    }
  }

  private static int parseMajor(String version) {
    if (!version.startsWith("v")) {
      return -1;
    }
    int dot = version.indexOf('.');
    try {
      return Integer.parseInt(version.substring(1, dot > 0 ? dot : version.length()));
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
