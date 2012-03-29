package com.intellij.plugins.haxe.ide;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCommenter implements CodeDocumentationAwareCommenter {
  @Override
  public String getLineCommentPrefix() {
    return "//";
  }

  @Override
  public String getBlockCommentPrefix() {
    return "/*";
  }

  @Override
  public String getBlockCommentSuffix() {
    return "*/";
  }

  @Override
  public String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public String getCommentedBlockCommentSuffix() {
    return null;
  }

  @Override
  public IElementType getLineCommentTokenType() {
    return HaxeTokenTypeSets.MSL_COMMENT;
  }

  @Override
  public IElementType getBlockCommentTokenType() {
    return HaxeTokenTypeSets.MML_COMMENT;
  }

  @Override
  public IElementType getDocumentationCommentTokenType() {
    return HaxeTokenTypeSets.DOC_COMMENT;
  }

  @Override
  public String getDocumentationCommentPrefix() {
    return "/**";
  }

  @Override
  public String getDocumentationCommentLinePrefix() {
    return "*";
  }

  @Override
  public String getDocumentationCommentSuffix() {
    return "**/";
  }

  @Override
  public boolean isDocumentationComment(PsiComment element) {
    return element.getTokenType() == HaxeTokenTypeSets.DOC_COMMENT;
  }
}
