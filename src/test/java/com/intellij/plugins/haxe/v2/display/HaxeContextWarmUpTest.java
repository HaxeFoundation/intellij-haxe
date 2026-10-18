package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticKind;
import com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity;
import com.intellij.plugins.haxe.display.protocol.FileDiagnostics;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Compiler services: context warm-up")
public class HaxeContextWarmUpTest {

  @Test
  @DisplayName("context errors list only error severity diagnostics")
  public void testContextErrorsListOnlyErrorSeverityDiagnostics() {
    Diagnostic unresolved = diagnostic(DiagnosticKind.UNRESOLVED_IDENTIFIER, DiagnosticSeverity.ERROR, 2);
    Diagnostic unusedImport = diagnostic(DiagnosticKind.UNUSED_IMPORT, DiagnosticSeverity.WARNING, 24);
    List<FileDiagnostics> results = List.of(new FileDiagnostics("/p/src/Main.hx", List.of(unresolved)),
                                            new FileDiagnostics("/std/Iterator.hx", List.of(unusedImport)));

    String errors = HaxeCompilerDisplayService.contextErrors(results);

    assertTrue(errors.startsWith("/p/src/Main.hx:3: "), "the error with its 1-based line, got: " + errors);
    assertEquals(1, errors.lines().count(), "the warning is left out, got: " + errors);
  }

  @Test
  @DisplayName("a context without errors has no context errors")
  public void testAContextWithoutErrorsHasNoContextErrors() {
    Diagnostic unusedImport = diagnostic(DiagnosticKind.UNUSED_IMPORT, DiagnosticSeverity.WARNING, 24);

    String errors = HaxeCompilerDisplayService.contextErrors(List.of(new FileDiagnostics("/std/Iterator.hx", List.of(unusedImport))));

    assertEquals("", errors);
  }

  private static Diagnostic diagnostic(DiagnosticKind kind, DiagnosticSeverity severity, int line) {
    Range range = new Range(new Position(line, 4), new Position(line, 15));
    return new Diagnostic(kind, range, severity, null, null);
  }
}
