package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.plugins.haxe.HaxeDebuggerBundle;

import java.io.IOException;
import java.nio.file.Path;

/** The pinned vscode debug adapters as the IDE names and locates them. */
public final class JsDebugAdapters {

  private JsDebugAdapters() {
  }

  /** The user-facing adapter name: js-debug serves Chromium and Node.js, the Firefox adapter its own family. */
  public static String displayName(AdapterPin pin) {
    String key = pin.equals(AdapterPin.JS_DEBUG) ? "javascript.adapter.name.js.debug" : "javascript.adapter.name.firefox";
    return HaxeDebuggerBundle.message(key);
  }

  /** The adapter's status in an editor: name and version, marked downloaded or not. */
  public static String statusText(AdapterPin pin, boolean installed) {
    String name = displayName(pin) + " " + pin.version();
    return installed ? HaxeDebuggerBundle.message("javascript.adapter.downloaded", name) : name + " \u2014";
  }

  public static String downloadingText(AdapterPin pin) {
    return HaxeDebuggerBundle.message("javascript.adapter.downloading", displayName(pin) + " " + pin.version());
  }

  /**
   * The adapter's entry point, WITHOUT downloading: acquisition is the user's
   * explicit decision (the run configuration's Download link). The browser
   * configuration already refuses a Debug launch without it; this guard
   * covers the node and test lanes, whose configurations validate nothing
   * about it.
   */
  public static Path installedEntry(AdapterStore store, AdapterPin pin) throws IOException {
    if (!store.isInstalled(pin)) {
      throw new IOException(HaxeDebuggerBundle.message("javascript.adapter.missing", displayName(pin) + " " + pin.version()));
    }
    return store.resolveEntry(pin); // already installed: no network
  }
}
