package com.intellij.plugins.haxe.display.transport;

/**
 * The payload was not parseable JSON. A failed compile answers with the
 * compiler's error text instead of an envelope, so callers that also see
 * the transport's error flag reclassify this as a compiler failure.
 */
public final class MalformedPayloadException extends DisplayRequestException {

  public MalformedPayloadException(String message, Throwable cause) {
    super(message, cause);
  }
}
