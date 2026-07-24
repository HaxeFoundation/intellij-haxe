package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/**
 * The debugger's view of the IDE browser registry (Settings | Tools | Web
 * Browsers): the run configuration stores only a browser id, executables are
 * configured in the registry, and the DAP adapter FAMILY is derived from the
 * selected browser — Firefox flavors get the firefox adapter, Chromium
 * flavors js-debug, anything else is unsupported. The adapters only launch a
 * browser they are handed (js-debug's own discovery misses a plain Chromium
 * such as ungoogled-chromium), so the session resolves the executable through
 * here and validation fails a browserless or unsupported configuration at
 * check time instead of the adapter timing out.
 */
final class DebugBrowser {
  private DebugBrowser() {
  }

  /**
   * The registry browser the configuration means: the selected id, or the
   * IDE's first active browser when no explicit selection was made ("Default"
   * in the chooser). Null when the id is stale or the registry is empty.
   */
  static @Nullable WebBrowser resolve(@Nullable String browserId) {
    WebBrowserManager manager = WebBrowserManager.getInstance();
    if (browserId != null && !browserId.isBlank()) {
      return manager.findBrowserById(browserId);
    }
    return manager.getFirstActiveBrowser();
  }

  /** The adapter family a registry browser maps to; null = debugging unsupported. */
  static @Nullable BrowserFamily familyOf(@Nullable WebBrowser browser) {
    if (browser == null) {
      return null;
    }
    return switch (browser.getFamily()) {
      case CHROME -> BrowserFamily.CHROMIUM;
      case FIREFOX -> BrowserFamily.FIREFOX;
      default -> null;
    };
  }

  /**
   * The browser's executable: its registry-configured path, or the platform's
   * default resolution for its family when none is set; null unless the
   * result is an existing file.
   */
  static @Nullable Path executableOf(WebBrowser browser) {
    String path = browser.getPath();
    if (path == null || path.isBlank()) {
      path = browser.getFamily().getExecutionPath();
    }
    if (path == null || path.isBlank()) {
      return null;
    }
    Path executable = Path.of(path);
    return Files.isRegularFile(executable) ? executable : null;
  }
}
