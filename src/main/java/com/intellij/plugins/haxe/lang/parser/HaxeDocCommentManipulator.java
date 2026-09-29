package com.intellij.plugins.haxe.lang.parser;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Writes edits made inside a doc comment's injected code fragments back into
 * the comment: the changed range is spliced into the comment text and the
 * whole comment is reparsed from a dummy file.
 */
public class HaxeDocCommentManipulator extends AbstractElementManipulator<HaxePsiDocCommentImpl> {

  @Override
  public HaxePsiDocCommentImpl handleContentChange(@NotNull HaxePsiDocCommentImpl element,
                                                   @NotNull TextRange range,
                                                   String newContent) throws IncorrectOperationException {
    String oldText = element.getText();
    String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());

    PsiFile dummyFile = HaxeElementGenerator.createDummyFile(element.getProject(), newText + "\nclass DocOwner {}");
    HaxePsiDocCommentImpl newComment = PsiTreeUtil.findChildOfType(dummyFile, HaxePsiDocCommentImpl.class);
    if (newComment == null) {
      throw new IncorrectOperationException("edited text is no longer a doc comment");
    }
    return (HaxePsiDocCommentImpl)element.replace(newComment);
  }
}
