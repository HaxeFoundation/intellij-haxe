package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * A thread descriptor, returned in the "threads" response.
 * (Named DapThread because the DAP type name "Thread" collides with java.lang.Thread.)
 */
@Data
public class DapThread {
  private int id;
  private String name;
}
