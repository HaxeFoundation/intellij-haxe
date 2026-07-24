package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import java.util.Map;

/**
 * A launch request whose arguments are an adapter-specific configuration map
 * (the DAP spec leaves launch arguments entirely to the adapter). Used for
 * external adapters with rich launch vocabularies (the vscode web debug
 * adapters: url/file/webRoot/firefoxExecutable/...), where a dedicated
 * arguments DTO per adapter would just mirror their docs field by field.
 */
public class ConfiguredLaunchRequest extends Request {
  private Map<String, Object> arguments;

  public ConfiguredLaunchRequest() {
    setCommand("launch");
  }

  public static ConfiguredLaunchRequest of(Map<String, Object> arguments) {
    ConfiguredLaunchRequest request = new ConfiguredLaunchRequest();
    request.arguments = arguments;
    return request;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  public void setArguments(Map<String, Object> arguments) {
    this.arguments = arguments;
  }
}
