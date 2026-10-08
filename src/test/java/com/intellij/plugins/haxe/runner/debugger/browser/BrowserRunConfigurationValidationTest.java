package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/// The debug adapter gates Debug only: plain Run and Profile never touch it.
@DisplayName("Debugger: browser run configuration validation")
public class BrowserRunConfigurationValidationTest extends HaxeCodeInsightFixtureTestCase {
  @TempDir Path tempDir;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("check configuration passes without the debug adapter")
  public void testCheckConfigurationPassesWithoutTheDebugAdapter() throws Exception {
    assumeAdapterNotDownloaded();
    WebBrowserManager browsers = WebBrowserManager.getInstance();
    WebBrowser chrome = browsers.findBrowserById("chrome");
    String storedPath = chrome.getPath();
    boolean storedActive = browsers.isActive(chrome);
    Path executable = Files.createFile(tempDir.resolve("chrome.exe"));
    browsers.setBrowserPath(chrome, executable.toString(), true);

    try {
      BrowserRunConfiguration configuration = chromeConfiguration();
      configuration.setContentRoot(tempDir.toString());
      assertDoesNotThrow(configuration::checkConfiguration);
    } finally {
      browsers.setBrowserPath(chrome, storedPath, storedActive);
    }
  }

  @Test
  @DisplayName("debug runner requires the downloaded adapter")
  public void testDebugRunnerRequiresTheDownloadedAdapter() {
    assumeAdapterNotDownloaded();
    BrowserRunConfiguration configuration = chromeConfiguration();

    RuntimeConfigurationError error = assertThrows(RuntimeConfigurationError.class,
                                                   () -> configuration.checkRunnerSettings(new BrowserDebugRunner(), null, null));

    String adapterName = BrowserRunConfiguration.adapterDisplayName(BrowserFamily.CHROMIUM) + " " + AdapterPin.JS_DEBUG.version();
    assertEquals(HaxeDebuggerBundle.message("browser.runner.adapter.missing", adapterName), error.getMessage());
  }

  @Test
  @DisplayName("plain runner ignores the adapter")
  public void testPlainRunnerIgnoresTheAdapter() {
    assumeAdapterNotDownloaded();
    BrowserRunConfiguration configuration = chromeConfiguration();

    assertDoesNotThrow(() -> configuration.checkRunnerSettings(new BrowserRunner(), null, null));
  }

  private BrowserRunConfiguration chromeConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    BrowserRunConfiguration configuration = new BrowserRunConfiguration("browser", getProject(), new BrowserConfigurationFactory(type));
    configuration.setModule(getModule());
    configuration.setBrowserId("chrome");
    return configuration;
  }

  /** A test run's IDE system directory normally holds no adapter; one left there by a sandbox session would void every check. */
  private static void assumeAdapterNotDownloaded() {
    boolean downloaded = AdapterStores.open().isInstalled(AdapterPin.JS_DEBUG);
    assumeFalse(downloaded, "the Chromium adapter is downloaded in this IDE system directory");
  }
}
