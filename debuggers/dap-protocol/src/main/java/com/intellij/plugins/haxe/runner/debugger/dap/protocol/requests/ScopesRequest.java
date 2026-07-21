package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScopesRequest extends Request {
  public static final String COMMAND = "scopes";

  private ScopesArguments arguments;

  public ScopesRequest() {
    super(COMMAND);
  }
}
