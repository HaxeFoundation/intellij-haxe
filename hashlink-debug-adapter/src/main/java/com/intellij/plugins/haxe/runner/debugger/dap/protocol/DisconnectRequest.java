package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DisconnectRequest extends Request {
  public static final String COMMAND = "disconnect";

  public DisconnectRequest() {
    super(COMMAND);
  }
}
