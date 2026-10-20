package com.intellij.plugins.haxe.display.transport;

import java.util.List;

/**
 * One server response, sorted by line type. {@code payload} joins the payload
 * lines: the JSON-RPC envelope for a display request, the compiler output
 * otherwise. {@code logs} holds the log lines, which start with 0x01 on the
 * wire. {@code hasError} tells whether the fatal-error marker (0x02) appeared.
 */
public record DisplayResponse(String payload, List<String> logs, boolean hasError) {
}
