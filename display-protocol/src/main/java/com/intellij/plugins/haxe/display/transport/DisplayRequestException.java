package com.intellij.plugins.haxe.display.transport;

/**
 * A display request failed. The connection failed, the compiler rejected the
 * arguments (the response carried the 0x02 error marker), or the JSON-RPC
 * envelope carried an error.
 */
public class DisplayRequestException extends Exception {

  public DisplayRequestException(String message) {
    super(message);
  }

  public DisplayRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
