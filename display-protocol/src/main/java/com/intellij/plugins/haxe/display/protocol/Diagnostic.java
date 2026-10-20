package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;

/**
 * One entry of a {@code display/diagnostics} result. Its {@code args} take
 * the shape of its {@link DiagnosticKind} ({@link DiagnosticArgs}); the
 * {@code ...Arg} accessors read the shapes the plugin uses. {@code code} is
 * the diagnostic's LSP-style identifier. It is null when the compiler sends
 * none, and haxe 4 never sends one.
 */
public record Diagnostic(DiagnosticKind kind,
                         Range range,
                         DiagnosticSeverity severity,
                         @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                                       include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                                       property = "kind",
                                       defaultImpl = DiagnosticArgs.Other.class)
                         DiagnosticArgs args,
                         @JsonSetter(nulls = Nulls.SET) String code) {

  /** One suggestion of an UNRESOLVED_IDENTIFIER: {@code kind} 0 is an import candidate, 1 a typo correction. */
  public record IdentifierSuggestion(int kind, String name) {
    public boolean isImportCandidate() {
      return kind == 0;
    }
  }

  public Diagnostic {
    kind = kind == null ? DiagnosticKind.UNKNOWN : kind;
    severity = severity == null ? DiagnosticSeverity.UNKNOWN : severity;
  }

  /** The message of a COMPILER_ERROR, PARSER_ERROR or DEPRECATION_WARNING. */
  public String messageArg() {
    return args instanceof DiagnosticArgs.Message(String text) ? text : "";
  }

  /** The suggestions of an UNRESOLVED_IDENTIFIER. */
  public List<IdentifierSuggestion> suggestionArgs() {
    return args instanceof DiagnosticArgs.Suggestions(List<IdentifierSuggestion> suggestions1) ? suggestions1 : List.of();
  }

  /** The missing fields of a MISSING_FIELDS entry; null for any other kind. */
  public MissingFields missingFieldsArg() {
    return args instanceof MissingFields.Args missing ? missing.toMissingFields() : null;
  }

  /** The description of a REMOVABLE_CODE entry; empty for any other kind. */
  public String descriptionArg() {
    return args instanceof DiagnosticArgs.RemovableCode removable ? removable.description() : "";
  }

  /**
   * The span a REMOVABLE_CODE fix should delete. It can be wider than the
   * highlighted {@link #range()}. Null when the compiler supplies none.
   */
  public Range removableRangeArg() {
    return args instanceof DiagnosticArgs.RemovableCode removable ? removable.range() : null;
  }

  /**
   * The replacement text haxe 5 offers for the removable span
   * ({@code newCode}). Null when absent or empty; both mean the span is
   * simply removed.
   */
  public String newCodeArg() {
    String newCode = args instanceof DiagnosticArgs.RemovableCode removable ? removable.newCode() : "";
    return newCode.isEmpty() ? null : newCode;
  }
}
