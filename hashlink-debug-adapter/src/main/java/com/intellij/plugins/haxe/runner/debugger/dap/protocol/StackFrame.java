package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * One frame in a "stackTrace" response.
 */
@Data
public class StackFrame {
  private int id;
  private String name;
  private int line;
  private int column;
  private Source source;
}
