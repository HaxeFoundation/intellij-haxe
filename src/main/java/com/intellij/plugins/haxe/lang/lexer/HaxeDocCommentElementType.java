package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.NotNull;

/**
 * The doc comment chameleon: the flex lexer still emits one token, and the
 * line-oriented sub-tree ({@link HaxeDocTokenTypes}) is parsed on demand from
 * {@link HaxeDocLexer} - consumers that never look inside pay nothing.
 */
public class HaxeDocCommentElementType extends ILazyParseableElementType {

  public HaxeDocCommentElementType() {
    super("DOC_COMMENT", HaxeLanguage.INSTANCE);
  }

  @Override
  public ASTNode createNode(CharSequence text) {
    return new HaxePsiDocCommentImpl(this, text);
  }

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
    Project project = psi.getProject();
    PsiBuilder builder = PsiBuilderFactory.getInstance()
      .createBuilder(project, chameleon, new HaxeDocLexer(), getLanguage(), chameleon.getChars());
    PsiBuilder.Marker root = builder.mark();
    while (!builder.eof()) {
      builder.advanceLexer();
    }
    root.done(this);
    return builder.getTreeBuilt().getFirstChildNode();
  }
}
