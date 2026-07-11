package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the "scopes" request.
 */
@Data
public class ScopesArguments {
  private int frameId;
}
