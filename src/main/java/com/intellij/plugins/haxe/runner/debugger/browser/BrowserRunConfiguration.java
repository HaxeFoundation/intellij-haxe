package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Debug a haxe -js build in a browser (experimental): the vscode DAP debug
 * adapters drive the browser, breakpoints map through the compiler's source
 * maps back to .hx. Two content modes:
 *
 * <ul>
 *   <li>SERVE: the plugin's own loopback http server serves the configured
 *       content directory (compiled js + source map + index.html) and the
 *       browser is pointed at it;</li>
 *   <li>URL: the user's own server hosts the app; the browser is pointed at
 *       the configured URL.</li>
 * </ul>
 *
 * Browser family selects the adapter (Firefox: vscode-firefox-debug;
 * Chromium: vscode-js-debug). The optional browser executable supports any
 * family fork (tested against ungoogled-chromium); blank lets the adapter
 * find the default installation.
 */
public class BrowserRunConfiguration extends DapRunConfigurationBase {
  /** Which adapter family drives the session. */
  public enum BrowserFamily {FIREFOX, CHROMIUM}

  private static final String FAMILY = "browserFamily";
  private static final String URL = "url";
  private static final String SERVE_CONTENT = "serveContent";
  private static final String CONTENT_ROOT = "contentRoot";
  private static final String BROWSER_EXECUTABLE = "browserExecutable";
  private static final String NODE_PATH = "nodePath";

  @Getter private BrowserFamily browserFamily = BrowserFamily.FIREFOX;
  @Getter private String url = "";
  @Getter private boolean serveContent = true;
  @Getter private String contentRoot = "";
  @Getter private String browserExecutablePath = "";
  @Getter private String nodePath = "";

  public BrowserRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  public void setBrowserFamily(@Nullable BrowserFamily family) {
    browserFamily = family == null ? BrowserFamily.FIREFOX : family;
  }

  public void setUrl(@Nullable String value) {
    url = value == null ? "" : value;
  }

  public void setServeContent(boolean value) {
    serveContent = value;
  }

  public void setContentRoot(@Nullable String value) {
    contentRoot = value == null ? "" : value;
  }

  public void setBrowserExecutablePath(@Nullable String value) {
    browserExecutablePath = value == null ? "" : value;
  }

  public void setNodePath(@Nullable String value) {
    nodePath = value == null ? "" : value;
  }

  /** The pinned DAP adapter driving the given family. */
  public static AdapterPin adapterPinFor(BrowserFamily family) {
    return family == BrowserFamily.CHROMIUM ? AdapterPin.JS_DEBUG : AdapterPin.FIREFOX;
  }

  /** The user-facing adapter name ("Firefox DAP debugger" / "Chromium DAP debugger"). */
  public static String adapterDisplayName(BrowserFamily family) {
    return HaxeDebuggerBundle.message(family == BrowserFamily.CHROMIUM
                                      ? "browser.runner.adapter.name.chromium"
                                      : "browser.runner.adapter.name.firefox");
  }

  // --- validation ---

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (getConfigurationModule().getModule() == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("browser.runner.no.module"));
    }
    if (serveContent) {
      Path root = resolveContentRootOrNull();
      if (root == null) {
        throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("browser.runner.no.content.root"));
      }
      if (!Files.isDirectory(root)) {
        throw new RuntimeConfigurationError(
          HaxeDebuggerBundle.message("browser.runner.content.root.missing", contentRoot));
      }
    } else if (url.isBlank()) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("browser.runner.no.url"));
    }
    // downloading the adapter is the USER's explicit decision (the editor's
    // Download link) - a session never downloads, so a missing adapter is an
    // incorrect configuration, not a launch-time surprise
    AdapterPin pin = adapterPinFor(browserFamily);
    if (!new AdapterStore(BrowserDebugBackend.adapterStoreRoot()).isInstalled(pin)) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message(
        "browser.runner.adapter.missing", adapterDisplayName(browserFamily) + " " + pin.version()));
    }
  }

  /** The content directory: the setting, project-relative when not absolute. */
  public @Nullable Path resolveContentRootOrNull() {
    if (contentRoot.isBlank()) {
      return null;
    }
    try {
      Path path = Path.of(contentRoot);
      if (path.isAbsolute()) {
        return path;
      }
      String basePath = getProject().getBasePath();
      return basePath != null ? Path.of(basePath).resolve(path) : path;
    } catch (InvalidPathException e) {
      return null;
    }
  }

  // The platform builds this state BEFORE any runner acts — for BOTH
  // executors. Run executes it (serve + open browser, no debugger); the debug
  // runner ignores it and builds the DAP session itself.
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    return new BrowserRunningState(this);
  }

  @Override
  public @NotNull SettingsEditor<? extends BrowserRunConfiguration> getConfigurationEditor() {
    return new BrowserRunConfigurationEditor(getProject());
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
    try {
      browserFamily = BrowserFamily.valueOf(orEmpty(JDOMExternalizerUtil.readField(element, FAMILY)));
    } catch (IllegalArgumentException notStored) {
      browserFamily = BrowserFamily.FIREFOX;
    }
    url = orEmpty(JDOMExternalizerUtil.readField(element, URL));
    // absent field keeps the field default (serve mode) - parsing "" would
    // silently turn it off
    String storedServeContent = JDOMExternalizerUtil.readField(element, SERVE_CONTENT);
    if (storedServeContent != null) {
      serveContent = Boolean.parseBoolean(storedServeContent);
    }
    contentRoot = orEmpty(JDOMExternalizerUtil.readField(element, CONTENT_ROOT));
    browserExecutablePath = orEmpty(JDOMExternalizerUtil.readField(element, BROWSER_EXECUTABLE));
    nodePath = orEmpty(JDOMExternalizerUtil.readField(element, NODE_PATH));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element); // also serializes the module
    JDOMExternalizerUtil.writeField(element, FAMILY, browserFamily.name());
    JDOMExternalizerUtil.writeField(element, URL, url);
    JDOMExternalizerUtil.writeField(element, SERVE_CONTENT, Boolean.toString(serveContent));
    JDOMExternalizerUtil.writeField(element, CONTENT_ROOT, contentRoot);
    JDOMExternalizerUtil.writeField(element, BROWSER_EXECUTABLE, browserExecutablePath);
    JDOMExternalizerUtil.writeField(element, NODE_PATH, nodePath);
  }
}
