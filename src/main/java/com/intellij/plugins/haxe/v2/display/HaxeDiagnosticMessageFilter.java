package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticKind;
import com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity;
import com.intellij.plugins.haxe.display.protocol.InitializeResult;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decides, on a best-effort basis, whether a compiler WARNING is shown. A
 * module pinned to an older language level is not warned about deprecations
 * whose replacement does not exist at that level. Errors always show.
 *
 * A deprecation is recognized by the diagnostic's code when the compiler
 * sends one: haxe 5 fills the LSP-style {@code code} field with the specific
 * {@code -w} warning id, such as {@code WDeprecatedEnumAbstract}. Older
 * compilers are recognized by the diagnostic's kind and message. The level
 * of the replacement is looked up by code first, then in a table of message
 * fragments. A deprecation neither lookup knows always shows.
 */
public final class HaxeDiagnosticMessageFilter {

  /** A recognizable deprecation: a fragment of its message and the level that introduced its REPLACEMENT. */
  private record DeprecationRule(String messageFragment, HaxeLanguageLevel replacementLevel) {
  }

  /** haxe 5 deprecation codes share this prefix; specific ids extend it (WDeprecatedEnumAbstract). */
  private static final String DEPRECATION_CODE_PREFIX = "WDeprecated";

  /// Specific haxe 5 deprecation codes, mapped to the level that introduced their replacement.
  private static final Map<String, HaxeLanguageLevel> CODE_RULES = Map.of(
    "WDeprecatedEnumAbstract", HaxeLanguageLevel.HAXE_4_0);

  // The level that introduced each replacement, per the language level
  // reference (3.4 to 5.0). A warning is hidden only while the advised
  // replacement cannot be used yet.
  private static final List<DeprecationRule> DEPRECATION_RULES = List.of(
    new DeprecationRule("`@:enum abstract`", HaxeLanguageLevel.HAXE_4_0),  // -> enum abstract keyword form
    new DeprecationRule("@:final", HaxeLanguageLevel.HAXE_4_0),            // -> final keyword
    new DeprecationRule("@:extern", HaxeLanguageLevel.HAXE_4_0),           // -> extern field modifier
    new DeprecationRule("Std.is", HaxeLanguageLevel.HAXE_4_1),             // -> Std.isOfType
    new DeprecationRule("Std.instance", HaxeLanguageLevel.HAXE_4_0),       // -> Std.downcast
    new DeprecationRule("haxe.Utf8", HaxeLanguageLevel.HAXE_4_0),          // -> UnicodeString
    new DeprecationRule("haxe.xml.Fast", HaxeLanguageLevel.HAXE_4_0),      // -> haxe.xml.Access
    new DeprecationRule("__js__", HaxeLanguageLevel.HAXE_4_0),             // -> js.Syntax.code
    new DeprecationRule("__php__", HaxeLanguageLevel.HAXE_4_0));           // -> php.Syntax.code

  private HaxeDiagnosticMessageFilter() {
  }

  /** Whether to render the diagnostic for a module at {@code level}; anything unrecognized shows. */
  public static boolean shouldShow(@NotNull HaxeLanguageLevel level,
                                   @Nullable InitializeResult.SemVer haxeVersion,
                                   @NotNull Diagnostic diagnostic) {
    if (diagnostic.severity() != DiagnosticSeverity.WARNING) return true;
    if (!isDeprecationWarning(diagnostic, haxeVersion)) return true;

    HaxeLanguageLevel replacementLevel = replacementLevelOf(diagnostic);
    return replacementLevel == null || level.isAtLeast(replacementLevel);
  }

  /** The level that introduced the deprecation's replacement, by specific code (haxe 5) first and message fragment second; null when unknown. */
  @Nullable
  private static HaxeLanguageLevel replacementLevelOf(@NotNull Diagnostic diagnostic) {
    if (diagnostic.code() != null) {
      HaxeLanguageLevel byCode = CODE_RULES.get(diagnostic.code());
      if (byCode != null) return byCode;
    }
    String message = diagnostic.messageArg();
    for (DeprecationRule rule : DEPRECATION_RULES) {
      if (message.contains(rule.messageFragment())) {
        return rule.replacementLevel();
      }
    }
    return null;
  }

  /**
   * Whether the diagnostic is a deprecation warning. The level filter and the
   * modernize fix both rely on this single definition. It uses the code when
   * the compiler sends one. On haxe 5 or newer, a diagnostic without a code
   * is not a warning-class diagnostic, so its message is not examined. Haxe
   * 4 reports syntax deprecations as generic compiler warnings, so there the
   * message decides.
   */
  public static boolean isDeprecationWarning(@NotNull Diagnostic diagnostic,
                                             @Nullable InitializeResult.SemVer haxeVersion) {
    if (diagnostic.code() != null) return diagnostic.code().startsWith(DEPRECATION_CODE_PREFIX);
    if (haxeVersion != null && haxeVersion.major() >= 5) return false;
    if (diagnostic.kind() == DiagnosticKind.DEPRECATION_WARNING) return true;
    return diagnostic.kind() == DiagnosticKind.COMPILER_ERROR && diagnostic.messageArg().contains("deprecated");
  }
}
