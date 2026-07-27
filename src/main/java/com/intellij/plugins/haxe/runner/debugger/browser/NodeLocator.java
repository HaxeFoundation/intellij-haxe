package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import java.io.File;
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
final class NodeLocator {
  static final int MIN_NODE_MAJOR = 18;

  private NodeLocator() {
  }

  static Path locate(String configuredNodePath) throws IOException {
    if (!configuredNodePath.isBlank()) {
      Path node = Path.of(configuredNodePath);
      if (!Files.isRegularFile(node)) {
        throw new IOException(HaxeDebuggerBundle.message("browser.runner.node.configured.missing", configuredNodePath));
      }
      return node;
    }
    File onPath = PathEnvironmentVariableUtil.findInPath(nodeBinaryName());
    if (onPath == null) {
      throw new IOException(HaxeDebuggerBundle.message("browser.runner.node.not.found"));
    }
    return onPath.toPath();
  }

  private static String nodeBinaryName() {
    return System.getProperty("os.name", "").toLowerCase().contains("win") ? "node.exe" : "node";
  }

  // `node --version` prints e.g. v24.18.0
  static void requireModern(Path node) throws IOException {
    String version;
    try {
      Process probe = new ProcessBuilder(node.toString(), "--version").redirectErrorStream(true).start();
      version = new String(probe.getInputStream().readAllBytes()).trim();
      if (!probe.waitFor(10, TimeUnit.SECONDS)) {
        probe.destroyForcibly();
        throw new IOException(HaxeDebuggerBundle.message("browser.runner.node.broken", node, "--version timed out"));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while checking node", e);
    }
    int major = parseMajor(version);
    if (major < 0) {
      throw new IOException(HaxeDebuggerBundle.message("browser.runner.node.broken", node, version));
    }
    if (major < MIN_NODE_MAJOR) {
      throw new IOException(HaxeDebuggerBundle.message("browser.runner.node.too.old", version, MIN_NODE_MAJOR));
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
