package com.intellij.plugins.haxe.display.transport;

import java.util.List;

/**
 * The way requests reach a haxe server: one socket per request to a
 * {@code --wait} server, or one framed connection a {@code --server-connect}
 * process opened. A request is the compiler's argument list; the answer is
 * what the server wrote back, sorted into payload, log lines and the error
 * marker.
 */
public interface DisplayChannel {

  DisplayResponse request(List<String> args, int readTimeoutMs) throws DisplayRequestException;

  /** Names the server for log lines and error messages. */
  String describe();

  /** Whether a failed exchange closed the channel for good; a request on a broken channel fails without reaching a server. */
  default boolean isBroken() {
    return false;
  }
}
