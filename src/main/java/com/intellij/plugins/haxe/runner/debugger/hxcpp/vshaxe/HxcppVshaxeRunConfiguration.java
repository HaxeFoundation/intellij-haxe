package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationBase;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An HXCPP (vshaxe debug server) run/debug configuration: the shared
 * executable configuration plus the debug host/port. For debugging, the
 * executable must have been compiled with {@code -debug} and {@code -lib
 * hxcpp-debug-server}; its embedded debug server connects out to host:port,
 * which are compile-time defines in the executable
 * (HXCPP_DEBUG_HOST/HXCPP_DEBUG_PORT) — the fields here are prefilled with
 * the protocol defaults and only need changing when the build overrides them.
 */
public class HxcppVshaxeRunConfiguration extends DapExecutableRunConfigurationBase {
  /** The haxelib id of vshaxe's in-debuggee debug server the executable must be compiled with. */
  public static final String VSHAXE_DEBUG_SERVER_LIB = "hxcpp-debug-server";
  public static final String DEFAULT_DEBUG_HOST = "127.0.0.1";
  public static final int DEFAULT_DEBUG_PORT = 6972;

  private static final String DEBUG_HOST = "debugHost";
  private static final String DEBUG_PORT = "debugPort";

  @Getter private String debugHost = DEFAULT_DEBUG_HOST;
  @Getter private String debugPort = Integer.toString(DEFAULT_DEBUG_PORT);

  public HxcppVshaxeRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  public void setDebugHost(@Nullable String host) {
    debugHost = host == null || host.isBlank() ? DEFAULT_DEBUG_HOST : host.trim();
  }

  public void setDebugPort(@Nullable String port) {
    debugPort = port == null || port.isBlank() ? Integer.toString(DEFAULT_DEBUG_PORT) : port.trim();
  }

  int resolveDebugPort() throws ExecutionException {
    int port = parsedDebugPort();
    if (port < 0) {
      throw new ExecutionException(HaxeDebuggerBundle.message("hxcpp.runner.bad.port"));
    }
    return port;
  }

  private int parsedDebugPort() {
    try {
      int port = Integer.parseInt(debugPort);
      return port >= 1 && port <= 65535 ? port : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HxcppVshaxeRunConfigurationEditor(getProject());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();
    if (parsedDebugPort() < 0) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hxcpp.runner.bad.port"));
    }
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    setDebugHost(JDOMExternalizerUtil.readField(element, DEBUG_HOST));
    setDebugPort(JDOMExternalizerUtil.readField(element, DEBUG_PORT));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    JDOMExternalizerUtil.writeField(element, DEBUG_HOST, debugHost);
    JDOMExternalizerUtil.writeField(element, DEBUG_PORT, debugPort);
  }
}
