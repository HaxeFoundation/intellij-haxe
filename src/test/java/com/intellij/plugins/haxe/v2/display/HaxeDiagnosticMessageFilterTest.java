package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticKind;
import com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity;
import com.intellij.plugins.haxe.display.protocol.InitializeResult;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Compiler diagnostics: message filter")
public class HaxeDiagnosticMessageFilterTest {

  private static final String ENUM_ABSTRACT_DEPRECATION = "`@:enum abstract` is deprecated in favor of `enum abstract`";
  private static final InitializeResult.SemVer HAXE_4_3_7 = new InitializeResult.SemVer(4, 3, 7, null, null);
  private static final InitializeResult.SemVer HAXE_5_0_0 = new InitializeResult.SemVer(5, 0, 0, "preview.1", null);

  @Test
  @DisplayName("deprecation hides below the replacement level and shows at it")
  public void deprecationHidesBelowTheReplacementLevelAndShowsAtIt() {
    Diagnostic deprecation = warning(DiagnosticKind.COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, null);

    assertFalse(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_3_4, HAXE_4_3_7, deprecation),
                "enum abstract does not exist at 3.4 - the nag is unactionable");
    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_4_0, HAXE_4_3_7, deprecation));
    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_4_3, HAXE_4_3_7, deprecation));
  }

  @Test
  @DisplayName("specific code wins over the message")
  public void specificCodeWinsOverTheMessage() {
    // a haxe 5 compiler sends the SPECIFIC warning id in "code"; the
    // message is irrelevant when a code rule matches
    Diagnostic coded = warning(DiagnosticKind.COMPILER_ERROR, "any prose at all", "WDeprecatedEnumAbstract");

    assertFalse(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_3_4, HAXE_5_0_0, coded));
    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_4_0, HAXE_5_0_0, coded));
  }

  @Test
  @DisplayName("unknown deprecation code falls back to the message table")
  public void unknownDeprecationCodeFallsBackToTheMessageTable() {
    Diagnostic coded = warning(DiagnosticKind.COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, "WDeprecatedSomethingNew");

    assertFalse(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_3_4, HAXE_5_0_0, coded));
    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_4_3, HAXE_5_0_0, coded));
  }

  @Test
  @DisplayName("errors always show")
  public void errorsAlwaysShow() {
    StringNode message = StringNode.valueOf(ENUM_ABSTRACT_DEPRECATION);
    Diagnostic error = new Diagnostic(DiagnosticKind.COMPILER_ERROR, anyRange(), DiagnosticSeverity.ERROR, message, null);

    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_3_4, HAXE_4_3_7, error));
  }

  @Test
  @DisplayName("unknown deprecations show")
  public void unknownDeprecationsShow() {
    Diagnostic unknown = warning(DiagnosticKind.DEPRECATION_WARNING, "SomeApi is deprecated, use OtherApi", null);

    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_3_4, HAXE_4_3_7, unknown));
  }

  @Test
  @DisplayName("non deprecation warning codes show")
  public void nonDeprecationWarningCodesShow() {
    Diagnostic pointless = warning(DiagnosticKind.COMPILER_ERROR, "This code has no effect", "WPointlessCode");

    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_3_4, HAXE_5_0_0, pointless));
  }

  @Test
  @DisplayName("on haxe 5 an uncoded warning is never message sniffed")
  public void onHaxe5AnUncodedWarningIsNeverMessageSniffed() {
    // a 5+ compiler sends codes; a warning WITHOUT one is not a warning-class
    // diagnostic, so the fragile message matching must not engage
    Diagnostic uncoded = warning(DiagnosticKind.COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, null);

    assertTrue(HaxeDiagnosticMessageFilter.shouldShow(HaxeLanguageLevel.HAXE_3_4, HAXE_5_0_0, uncoded));
  }

  private static Diagnostic warning(DiagnosticKind kind, String message, String code) {
    return new Diagnostic(kind, anyRange(), DiagnosticSeverity.WARNING, StringNode.valueOf(message), code);
  }

  private static Range anyRange() {
    return new Range(new Position(0, 0), new Position(0, 1));
  }
}
