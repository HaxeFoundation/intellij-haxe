package com.intellij.plugins.haxe.runner.debugger.browser;

/**
 * One pinned, downloadable debug-adapter artifact. THE supply-chain contract
 * of the web debugger lives in these constants: the exact artifact URL and its
 * SHA-256 are hard-coded here, verified before unpacking, and changing a pin
 * is a reviewed code change gated by the live tests. Never npm, never a
 * package manager, never an unpinned "latest".
 *
 * @param id           store directory name (stable across versions)
 * @param version      artifact version (a new version unpacks beside the old)
 * @param url          direct artifact URL (GitHub releases / Open VSX only —
 *                     the VS Marketplace's terms forbid non-VS-product use)
 * @param sha256       lowercase hex SHA-256 the download must match
 * @param entryRelativePath the adapter's node entry point inside the archive
 */
public record AdapterPin(String id, String version, String url, String sha256, String entryRelativePath) {

  /**
   * vscode-firefox-debug from Open VSX (MIT, by Holger Benl). The .vsix is a
   * plain zip; the bundle is self-contained (plus mappings.wasm beside it).
   * Speaks DAP over TCP with {@code --server=<port>}; wire behaviour pinned
   * by FirefoxAdapterLiveProbe.
   */
  public static final AdapterPin FIREFOX = new AdapterPin(
    "firefox-debug",
    "2.15.0",
    "https://open-vsx.org/api/firefox-devtools/vscode-firefox-debug/2.15.0/file/"
    + "firefox-devtools.vscode-firefox-debug-2.15.0.vsix",
    "f72f7443331fa091bb2a7323828c4de5c70b54cf09de9348c37be164e4c4220f",
    "extension/dist/adapter.bundle.js");

  /**
   * vscode-js-debug's standalone DAP server (MIT, Microsoft), the official
   * {@code js-debug-dap} tarball from the GitHub release — the SAME engine
   * VS Code ships, entered through dapDebugServer.js (TCP DAP server; child
   * sessions via the startDebugging reverse request + __pendingTargetId).
   * Wire behaviour pinned by JsDebugAdapterLiveProbe.
   */
  public static final AdapterPin JS_DEBUG = new AdapterPin(
    "js-debug",
    "1.117.0",
    "https://github.com/microsoft/vscode-js-debug/releases/download/v1.117.0/js-debug-dap-v1.117.0.tar.gz",
    "ad8d04ede9d4b75cc290fd5438a65047a06f786d04f604b6112485b36f090772",
    "js-debug/src/dapDebugServer.js");

  /** Whether the artifact is a gzipped tar (else a plain zip/.vsix). */
  public boolean isTarGz() {
    return url.endsWith(".tar.gz") || url.endsWith(".tgz");
  }
}
