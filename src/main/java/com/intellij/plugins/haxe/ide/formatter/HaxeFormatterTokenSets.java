package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.psi.tree.TokenSet;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.FUNCTION_DEFINITION;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.PPBODY;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * Token sets used only by the formatter's rules. Groupings for lexing and
 * parsing live in HaxeTokenTypeSets.
 */
public final class HaxeFormatterTokenSets {

  // The tokens a function header can end with: PRPAREN closes the parameter
  // list, TYPE_TAG is a return type and KUNTYPED the untyped marker.
  static final TokenSet FUNCTION_HEADER_END = TokenSet.create(PRPAREN, TYPE_TAG, KUNTYPED);

  // The header's own trailing parts, which can also follow a header end.
  // Only what comes after the last of them is the body.
  static final TokenSet FUNCTION_HEADER_TRAILERS = TokenSet.orSet(FUNCTION_HEADER_END, TokenSet.create(OSEMI, BLOCK_STATEMENT));

  // Every node that owns a parameter list and a body: the parser's function
  // kinds plus the module-level function, which FUNCTION_DEFINITION lacks.
  public static final TokenSet FUNCTION_LIKE_OWNERS = TokenSet.orSet(FUNCTION_DEFINITION, TokenSet.create(MODULE_METHOD_DECLARATION));

  // the list nodes whose wrapped items indent from the line that opened them
  public static final TokenSet PARAMETER_AND_ARGUMENT_LISTS = TokenSet.create(PARAMETER_LIST, EXPRESSION_LIST, CALL_EXPRESSION_LIST);

  // a list's own punctuation, which never indents like an item
  static final TokenSet LIST_PUNCTUATION = TokenSet.create(PLPAREN, PRPAREN, OCOMMA);

  // the literals whose items sit one step inside their brackets
  static final TokenSet BRACKET_LITERALS = TokenSet.create(ARRAY_LITERAL, MAP_LITERAL);

  // The nodes whose children indent one step from them. The map entry types
  // are deliberately missing: indenting an entry's children would indent the
  // entry's first token a second time when the literal wraps one item per line.
  static final TokenSet INDENTED_CONTAINERS = TokenSet.create(
    BLOCK_STATEMENT, CLASS_BODY, ABSTRACT_BODY, ANONYMOUS_TYPE_BODY, OBJECT_LITERAL, XML_LITERAL_EXPRESSION, XML_MARKUP_ELEMENT,
    MAP_LOOP_INITIALIZER_EXPRESSION, EXTERN_CLASS_DECLARATION_BODY, ENUM_BODY, INTERFACE_BODY, SWITCH_BLOCK, SWITCH_CASE_BLOCK);

  // An inactive branch's node (PPBODY) and the lists wrapping its parsed
  // content. These layers add no indent, so the content aligns with the directives.
  static final TokenSet INACTIVE_BRANCH_LAYERS = TokenSet.create(PPBODY, INACTIVE_MEMBER_LIST, INACTIVE_STATEMENT_LIST, INACTIVE_MODULE_LIST);

  // no space between these and an inline #if or #end that follows them
  static final TokenSet OPENING_BRACKETS = TokenSet.create(PLPAREN, PLBRACK, PLCURLY);

  // no space between an inline #end and one of these after it, as haxe-formatter prints it
  static final TokenSet UNSPACED_AFTER_INLINE_END = TokenSet.create(OCOMMA, OSEMI, PRPAREN, PRBRACK, PRCURLY, ODOT);

  private HaxeFormatterTokenSets() {
  }
}
