package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * The {@code args} of a {@link Diagnostic}, whose shape its
 * {@link DiagnosticKind} decides: the kind's wire code names the record.
 * Every kind the plugin does not read, and any kind a newer compiler adds,
 * reads as {@link Other}.
 */
@JsonSubTypes({
  @JsonSubTypes.Type(value = DiagnosticArgs.Message.class, names = {"2", "4", "5"}),
  @JsonSubTypes.Type(value = DiagnosticArgs.Suggestions.class, name = "1"),
  @JsonSubTypes.Type(value = DiagnosticArgs.RemovableCode.class, name = "3"),
  @JsonSubTypes.Type(value = MissingFields.Args.class, name = "7")})
public sealed interface DiagnosticArgs
  permits DiagnosticArgs.Message, DiagnosticArgs.Suggestions, DiagnosticArgs.RemovableCode, MissingFields.Args, DiagnosticArgs.Other {

  /** COMPILER_ERROR, PARSER_ERROR, DEPRECATION_WARNING: the message, a plain string on the wire. */
  record Message(String text) implements DiagnosticArgs {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static Message of(String text) {
      return new Message(text);
    }
  }

  /** UNRESOLVED_IDENTIFIER: the import and typo suggestions, an array on the wire. */
  record Suggestions(List<Diagnostic.IdentifierSuggestion> suggestions) implements DiagnosticArgs {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static Suggestions of(List<Diagnostic.IdentifierSuggestion> suggestions) {
      return new Suggestions(suggestions);
    }
  }

  /**
   * REMOVABLE_CODE: what the code is and the span to delete, which can be
   * wider than the highlighted range; haxe 5 adds the {@code newCode} that
   * replaces it.
   */
  record RemovableCode(String description, Range range, String newCode) implements DiagnosticArgs {
  }

  /** The args of a kind the plugin does not read, whatever their shape. */
  record Other() implements DiagnosticArgs {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static Other of(JsonNode ignored) {
      return new Other();
    }
  }
}
