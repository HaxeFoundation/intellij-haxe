package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LaunchRequest extends Request {
  public static final String COMMAND = "launch";

  private LaunchRequestArguments arguments;

  public LaunchRequest() {
    super(COMMAND);
  }
}
