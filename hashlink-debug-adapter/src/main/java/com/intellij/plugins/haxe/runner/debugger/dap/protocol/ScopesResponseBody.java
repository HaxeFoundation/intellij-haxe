package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import java.util.List;
import lombok.Data;

/**
 * Body of the "scopes" response.
 */
@Data
public class ScopesResponseBody {
  private List<Scope> scopes;
}
