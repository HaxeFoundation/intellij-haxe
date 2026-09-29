package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * The browser backend behind BROWSER-hosted test runs (lime/openfl html5):
 * the packaged web root is served over the plugin's loopback server and the
 * page runs in the IDE-registry browser, console-captured through the pinned
 * js-debug adapter. Chromium-family only — the run host and worker mux are
 * js-debug shapes; a firefox-first registry fails with a pointer to the Web
 * Browsers settings rather than degrading silently.
 */
public final class HaxeBrowserTestSupport {

  private HaxeBrowserTestSupport() {
  }

  /** The backend serving {@code webRoot}; shared by the run host and the debug session. */
  @NotNull
  public static BrowserDebugBackend createBackend(@NotNull Project project, @NotNull Path webRoot)
    throws ExecutionException {
    // the plan never checks existence (it also answers configuration
    // validation, which runs before any compile); by launch time the
    // before-run compile must have produced the output
    if (!Files.isDirectory(webRoot)) {
      throw new ExecutionException(HaxeBundle.message("haxe.test.browser.no.output", webRoot.toString()));
    }
    WebBrowser browser = DebugBrowser.resolve(null);
    if (DebugBrowser.familyOf(browser) != BrowserFamily.CHROMIUM) {
      throw new ExecutionException(HaxeBundle.message("haxe.test.browser.chromium.required"));
    }
    Path executable = DebugBrowser.executableOf(browser);
    if (executable == null) {
      throw new ExecutionException(HaxeBundle.message("haxe.test.browser.no.executable", browser.getName()));
    }
    String node = StringUtil.notNullize(HaxeToolPathResolver.resolveNodeExecutable(project, null));
    return new BrowserDebugBackend(BrowserFamily.CHROMIUM, node, executable.toString(), true, webRoot, null);
  }

  /** The RUN lane's process: orchestrates the backend and ends on the reporters' completion sentinel. */
  @NotNull
  public static BrowserTestRunHost createRunHost(@NotNull Project project, @NotNull Path webRoot)
    throws ExecutionException {
    return new BrowserTestRunHost(createBackend(project, webRoot));
  }
}
