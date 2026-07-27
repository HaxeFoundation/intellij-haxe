package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;

import java.util.List;
import lombok.Data;

@Data
public class ThreadsResponseBody {
  private List<DapThread> threads;
}
