package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * A source file descriptor used in breakpoint requests and responses.
 */
@Data
public class Source {
  private String name;
  private String path;
  private Integer sourceReference;
}
