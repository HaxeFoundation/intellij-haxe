package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ThreadsRequest extends Request {
  public static final String COMMAND = "threads";

  public ThreadsRequest() {
    super(COMMAND);
  }
}
