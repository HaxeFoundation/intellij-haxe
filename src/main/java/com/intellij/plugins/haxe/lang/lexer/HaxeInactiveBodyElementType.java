package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.parser.HaxeParser;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * An INACTIVE conditional-compilation branch as a chameleon: the region
 * between directives arrives as one merged token (see HaxeLexer) and is
 * parsed lazily. The parser entry points are tried in an order chosen by
 * where the branch sits: members between class members, statements inside a
 * body, module content at top level, an expression for inline branches; the
 * first entry that parses the branch without errors wins.
 * <p>
 * When no entry parses cleanly, the context's first entry is kept WITH the
 * parser's error recovery if that recovery produced real structure (a broken
 * statement stays a local error, the rest keeps real PSI); fragments that
 * recover into nothing but error shells fall back to a flat run of raw
 * tokens. The formatter preserves both verbatim, since only clean parses
 * reformat, matching the reference formatter's refusal to touch what it
 * cannot parse.
 */
public class HaxeInactiveBodyElementType extends ILazyParseableElementType {

  /**
   * The entry that parsed the branch without errors, or null for a recovered
   * (error-carrying) parse, which the formatter preserves verbatim. Recorded
   * on the chameleon node because the parsed tree's root type is not
   * reliable: the expression root collapses into the concrete expression.
   */
  public static final Key<IElementType> CLEAN_PARSE_ENTRY = Key.create("haxe.inactive.clean.parse.entry");

  private static final List<IElementType> MEMBER_CONTEXT = List.of(INACTIVE_MEMBER_LIST, INACTIVE_STATEMENT_LIST, EXPRESSION);
  private static final List<IElementType> STATEMENT_CONTEXT = List.of(INACTIVE_STATEMENT_LIST, EXPRESSION, INACTIVE_MEMBER_LIST);
  private static final List<IElementType> MODULE_CONTEXT = List.of(INACTIVE_MODULE_LIST, INACTIVE_MEMBER_LIST, INACTIVE_STATEMENT_LIST);
  private static final List<IElementType> EXPRESSION_CONTEXT = List.of(EXPRESSION, INACTIVE_STATEMENT_LIST);
  private static final TokenSet STRUCTURE_WRAPPERS = TokenSet.create(
    INACTIVE_MEMBER_LIST,
    INACTIVE_STATEMENT_LIST,
    INACTIVE_MODULE_LIST,
    EXPRESSION);

  public HaxeInactiveBodyElementType() {
    super("PPBODY", HaxeLanguage.INSTANCE);
  }

  @Override
  public ASTNode createNode(CharSequence text) {
    return new HaxeInactiveBody(this, text);
  }

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
    Project project = psi.getProject();
    List<IElementType> entries = entriesFor(chameleon);
    for (IElementType entry : entries) {
      ASTNode parsed = parse(project, chameleon, entry);
      if (!containsErrors(parsed)) {
        chameleon.putUserData(CLEAN_PARSE_ENTRY, entry);
        return parsed;
      }
    }
    // Trivial fragments (a spliced keyword, half an operator) recover into
    // nothing but error shells and read better as flat tokens. Either way the
    // missing clean entry keeps the formatter preserving the text.
    chameleon.putUserData(CLEAN_PARSE_ENTRY, null);
    ASTNode recovered = parse(project, chameleon, entries.getFirst());
    if (hasMeaningfulStructure(recovered)) {
      return recovered;
    }
    return flatTokens(project, chameleon);
  }

  /**
   * Whether recovery produced NESTED real structure: a non-shell composite
   * containing another non-shell composite (a declaration with its name, a
   * statement with its expression). Error elements, dummy blocks and the
   * bare entry wrappers are shells; so is a lone concrete node whose only
   * content is an error husk around raw tokens.
   */
  private static boolean hasMeaningfulStructure(@NotNull ASTNode node) {
    IElementType type = node.getElementType();
    // collapsed nested chameleons must not be touched in a detached tree
    if (type instanceof ILazyParseableElementType) {
      return false;
    }
    if (!isShell(type) && hasRealCompositeChild(node)) {
      return true;
    }
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (hasMeaningfulStructure(child)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isShell(@NotNull IElementType type) {
    return type == TokenType.ERROR_ELEMENT
           || type == GeneratedParserUtilBase.DUMMY_BLOCK
           || STRUCTURE_WRAPPERS.contains(type);
  }

  private static boolean hasRealCompositeChild(@NotNull ASTNode node) {
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      IElementType type = child.getElementType();
      if (type instanceof ILazyParseableElementType) continue;
      if (isShell(type)) continue;
      if (child.getFirstChildNode() != null) {
        return true;
      }
    }
    return false;
  }

  /** The opaque fallback: the branch's raw tokens as flat leaves, structure-free. */
  @NotNull
  private ASTNode flatTokens(@NotNull Project project, @NotNull ASTNode chameleon) {
    PsiBuilder builder = newBuilder(project, chameleon);
    PsiBuilder.Marker root = builder.mark();
    while (!builder.eof()) {
      builder.advanceLexer();
    }
    root.done(this);
    return builder.getTreeBuilt().getFirstChildNode();
  }

  /** Entries in context order: what the branch's surroundings say its content most likely is. */
  @NotNull
  private static List<IElementType> entriesFor(@NotNull ASTNode chameleon) {
    ASTNode parent = chameleon.getTreeParent();
    IElementType parentType = parent == null ? null : parent.getElementType();
    if (parentType == CLASS_BODY || parentType == ABSTRACT_BODY
        || parentType == INTERFACE_BODY || parentType == EXTERN_CLASS_DECLARATION_BODY) {
      return MEMBER_CONTEXT;
    }
    if (parentType == BLOCK_STATEMENT || parentType == SWITCH_CASE_BLOCK) {
      return STATEMENT_CONTEXT;
    }
    if (parentType == null || parentType == HaxeTokenTypeSets.HAXE_FILE || parentType == MODULE) {
      return MODULE_CONTEXT;
    }
    // inline positions (expression lists, initializers, conditions...)
    return EXPRESSION_CONTEXT;
  }

  @NotNull
  private static ASTNode parse(@NotNull Project project, @NotNull ASTNode chameleon, @NotNull IElementType entry) {
    PsiBuilder builder = newBuilder(project, chameleon);
    return new HaxeParser().parse(entry, builder);
  }

  @NotNull
  private static PsiBuilder newBuilder(@NotNull Project project, @NotNull ASTNode chameleon) {
    return PsiBuilderFactory.getInstance()
      .createBuilder(project, chameleon, new HaxeLexer(project), HaxeLanguage.INSTANCE, chameleon.getChars());
  }

  private static boolean containsErrors(@NotNull ASTNode node) {
    IElementType type = node.getElementType();
    if (type == TokenType.ERROR_ELEMENT || type == GeneratedParserUtilBase.DUMMY_BLOCK) {
      return true;
    }
    // nested chameleons (doc comments, metadata) stay collapsed: the candidate
    // tree is still DETACHED, and the platform forbids parsing a chameleon
    // outside a file tree - their contents cannot make the branch unparseable anyway
    if (type instanceof ILazyParseableElementType) {
      return false;
    }
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (containsErrors(child)) {
        return true;
      }
    }
    return false;
  }
}
