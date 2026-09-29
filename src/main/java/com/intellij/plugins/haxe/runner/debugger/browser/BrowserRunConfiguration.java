package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.HaxeProfilerExecutorSupport;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import java.nio.file.Files;
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
 * The browser comes from the IDE's browser registry (Settings | Tools | Web
 * Browsers) and picks the adapter by its family: Firefox flavors get
 * vscode-firefox-debug, Chromium flavors (incl. ungoogled-chromium) get
 * vscode-js-debug; other families cannot be debugged. Executable paths are
 * configured in the registry, not here.
 */
public class BrowserRunConfiguration extends DapRunConfigurationBase implements HaxeProfilableRunConfiguration {
  /** Which adapter family drives the session; derived from the selected browser. */
  public enum BrowserFamily {FIREFOX, CHROMIUM}

  private static final String URL = "url";
  private static final String SERVE_CONTENT = "serveContent";
  private static final String CONTENT_ROOT = "contentRoot";
  private static final String BROWSER_ID = "browserId";
  private static final String NODE_PATH = "nodePath";

  @Getter private String url = "";
  @Getter private boolean serveContent = true;
  @Getter private String contentRoot = "";

  @Getter private String browserId = "";
  @Getter private String nodePath = "";

  public BrowserRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
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

  public void setBrowserId(@Nullable String value) {
    browserId = value == null ? "" : value;
  }

  public void setNodePath(@Nullable String value) {
    nodePath = value == null ? "" : value;
  }

  /** The node the adapter runs on: this configuration's override, else the Haxe SDK's runtimes entry; empty leaves the PATH lookup to the locator. */
  @NotNull
  public String effectiveNodePath() {
    if (!nodePath.isBlank()) {
      return nodePath;
    }
    String fromSdk = HaxeToolPathResolver.resolveNodeExecutable(getProject(), null);
    return fromSdk != null ? fromSdk : "";
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
    } else {
      if (url.isBlank()) {
        throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("browser.runner.no.url"));
      }
      // the web root is optional in URL mode, but a set-but-invalid path would
      // become a bad webRoot - catch the typo here
      if (!contentRoot.isBlank()) {
        Path root = resolveContentRootOrNull();
        if (root == null || !Files.isDirectory(root)) {
          throw new RuntimeConfigurationError(
            HaxeDebuggerBundle.message("browser.runner.content.root.missing", contentRoot));
        }
      }
    }
    var browser = DebugBrowser.resolve(browserId);

    if (browser == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message(
        browserId.isBlank() ? "browser.runner.browser.none" : "browser.runner.browser.selection.gone"));
    }
    BrowserFamily family = DebugBrowser.familyOf(browser);

    if (family == null) {
      throw new RuntimeConfigurationError(
        HaxeDebuggerBundle.message("browser.runner.browser.unsupported", browser.getName()));
    }

    if (DebugBrowser.executableOf(browser) == null) {
      throw new RuntimeConfigurationError(
        HaxeDebuggerBundle.message("browser.runner.browser.no.exe", browser.getName()));
    }

    // downloading the adapter is the USER's explicit decision (the editor's
    // Download link) - a session never downloads, so a missing adapter is an
    // incorrect configuration, not a launch-time surprise
    AdapterPin pin = adapterPinFor(family);
    if (!new AdapterStore(BrowserDebugBackend.adapterStoreRoot()).isInstalled(pin)) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message(
        "browser.runner.adapter.missing", adapterDisplayName(family) + " " + pin.version()));
    }
  }

  /** The content directory: the setting, project-relative when not absolute. */
  public @Nullable Path resolveContentRootOrNull() {
    return contentRoot.isBlank() ? null : resolveAgainstProject(contentRoot);
  }

  @Override
  public @NotNull Lane profilingLane() {
    return Lane.JS;
  }

  /** Profiling drives V8 over CDP — a Chromium-family browser only; Firefox speaks no CDP. */
  @Override
  public boolean isProfilingReady() {
    if (DumbService.isDumb(getProject())) return false;
    if (DebugBrowser.familyOf(DebugBrowser.resolve(browserId)) != BrowserFamily.CHROMIUM) return false;
    return serveContent ? !contentRoot.isBlank() : !url.isBlank();
  }

  // The platform builds this state BEFORE any runner acts — for BOTH
  // executors. Run executes it (serve + open browser, no debugger); the debug
  // runner ignores it and builds the DAP session itself; the profiler
  // executor gets the CDP-driven state.
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    // non-null exactly when the IU "Run with Profiler" executor launched us
    // with the JavaScript Profiler entry selected
    Integer samplingIntervalUs = HaxeProfilerExecutorSupport.jsSamplingIntervalUs(executor);
    if (samplingIntervalUs != null) {
      return new BrowserProfilingState(this, samplingIntervalUs);
    }
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
    url = orEmpty(JDOMExternalizerUtil.readField(element, URL));
    // absent field keeps the field default (serve mode) - parsing "" would
    // silently turn it off
    String storedServeContent = JDOMExternalizerUtil.readField(element, SERVE_CONTENT);
    if (storedServeContent != null) {
      serveContent = Boolean.parseBoolean(storedServeContent);
    }
    contentRoot = orEmpty(JDOMExternalizerUtil.readField(element, CONTENT_ROOT));
    browserId = orEmpty(JDOMExternalizerUtil.readField(element, BROWSER_ID));
    nodePath = orEmpty(JDOMExternalizerUtil.readField(element, NODE_PATH));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element); // also serializes the module
    JDOMExternalizerUtil.writeField(element, URL, url);
    JDOMExternalizerUtil.writeField(element, SERVE_CONTENT, Boolean.toString(serveContent));
    JDOMExternalizerUtil.writeField(element, CONTENT_ROOT, contentRoot);
    JDOMExternalizerUtil.writeField(element, BROWSER_ID, browserId);
    JDOMExternalizerUtil.writeField(element, NODE_PATH, nodePath);
  }
}
