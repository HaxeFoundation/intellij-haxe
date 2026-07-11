package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;

import java.util.List;
import lombok.Data;

/**
 * Body of the "variables" response.
 */
@Data
public class VariablesResponseBody {
  private List<Variable> variables;
}
