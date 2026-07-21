package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;

import java.util.List;
import lombok.Data;

@Data
public class ScopesResponseBody {
  private List<Scope> scopes;
}
