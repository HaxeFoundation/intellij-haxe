package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import java.util.List;
import lombok.Data;

/**
 * Body of the "variables" response.
 */
@Data
public class VariablesResponseBody {
  private List<Variable> variables;
}
