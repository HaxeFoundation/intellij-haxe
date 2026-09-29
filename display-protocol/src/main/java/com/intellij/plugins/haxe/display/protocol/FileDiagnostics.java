package com.intellij.plugins.haxe.display.protocol;

import java.util.List;

/** Diagnostics of one file, as returned by {@code display/diagnostics}. */
public record FileDiagnostics(String file, List<Diagnostic> diagnostics) {
}
