package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticKind;
import com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity;
import com.intellij.plugins.haxe.display.protocol.InitializeResult.SemVer;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import tools.jackson.databind.node.StringNode;

import java.util.List;

import static com.intellij.plugins.haxe.display.protocol.DiagnosticKind.COMPILER_ERROR;
import static com.intellij.plugins.haxe.display.protocol.DiagnosticKind.DEPRECATION_WARNING;
import static com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity.ERROR;
import static com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity.WARNING;
import static com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel.HAXE_3_4;
import static com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel.HAXE_4_0;
import static com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel.HAXE_4_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Compiler diagnostics: message filter")
public class HaxeDiagnosticMessageFilterTest {

  private static final String ENUM_ABSTRACT_DEPRECATION = "`@:enum abstract` is deprecated in favor of `enum abstract`";
  private static final SemVer HAXE_4_3_7 = new SemVer(4, 3, 7, null, null);
  private static final SemVer HAXE_5_0_0 = new SemVer(5, 0, 0, "preview.1", null);

  /** (language level, compiler version, severity, kind, message, code, shown). */
  static final List<Arguments> VERDICTS = List.of(
    // deprecation hides below the replacement level and shows at it: enum
    // abstract does not exist at 3.4 - the nag is unactionable
    arguments(HAXE_3_4, HAXE_4_3_7, WARNING, COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, null, false),
    arguments(HAXE_4_0, HAXE_4_3_7, WARNING, COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, null, true),
    arguments(HAXE_4_3, HAXE_4_3_7, WARNING, COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, null, true),
    // specific code wins over the message: a haxe 5 compiler sends the SPECIFIC
    // warning id in "code"; the message is irrelevant when a code rule matches
    arguments(HAXE_3_4, HAXE_5_0_0, WARNING, COMPILER_ERROR, "any prose at all", "WDeprecatedEnumAbstract", false),
    arguments(HAXE_4_0, HAXE_5_0_0, WARNING, COMPILER_ERROR, "any prose at all", "WDeprecatedEnumAbstract", true),
    // unknown deprecation code falls back to the message table
    arguments(HAXE_3_4, HAXE_5_0_0, WARNING, COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, "WDeprecatedSomethingNew", false),
    arguments(HAXE_4_3, HAXE_5_0_0, WARNING, COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, "WDeprecatedSomethingNew", true),
    // errors always show
    arguments(HAXE_3_4, HAXE_4_3_7, ERROR, COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, null, true),
    // unknown deprecations show
    arguments(HAXE_3_4, HAXE_4_3_7, WARNING, DEPRECATION_WARNING, "SomeApi is deprecated, use OtherApi", null, true),
    // non deprecation warning codes show
    arguments(HAXE_3_4, HAXE_5_0_0, WARNING, COMPILER_ERROR, "This code has no effect", "WPointlessCode", true),
    // on haxe 5 an uncoded warning is never message sniffed: a 5+ compiler sends
    // codes; a warning WITHOUT one is not a warning-class diagnostic, so the
    // fragile message matching must not engage
    arguments(HAXE_3_4, HAXE_5_0_0, WARNING, COMPILER_ERROR, ENUM_ABSTRACT_DEPRECATION, null, true));

  @ParameterizedTest(name = "[{index}] {0} on {1}, {2} {3} code {5}: {4} -> shown {6}")
  @FieldSource("VERDICTS")
  @DisplayName("shows a diagnostic by level, code and message")
  public void showsADiagnosticByLevelCodeAndMessage(HaxeLanguageLevel level, SemVer version, DiagnosticSeverity severity,
                                                    DiagnosticKind kind, String message, String code, boolean shown) {
    Diagnostic diagnostic = new Diagnostic(kind, anyRange(), severity, StringNode.valueOf(message), code);

    boolean actual = HaxeDiagnosticMessageFilter.shouldShow(level, version, diagnostic);

    assertEquals(shown, actual);
  }

  private static Range anyRange() {
    return new Range(new Position(0, 0), new Position(0, 1));
  }
}
