package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SetVariableResponse extends Response {
  private SetVariableResponseBody body;
}
