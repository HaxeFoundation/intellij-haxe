package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import java.util.List;
import lombok.Data;

/**
 * Body of the "threads" response.
 */
@Data
public class ThreadsResponseBody {
  private List<DapThread> threads;
}
