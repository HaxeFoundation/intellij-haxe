package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.modcommand.ActionContext;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.modcommand.Presentation;
import com.intellij.modcommand.PsiUpdateModCommandAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.ide.HaxeCommenter;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.ide.documentation.HaxeDocCommentStyleConverter.isConvertible;
import static com.intellij.plugins.haxe.ide.documentation.HaxeDocCommentStyleConverter.isJavadocStyle;
import static com.intellij.plugins.haxe.ide.documentation.HaxeDocCommentStyleConverter.toHaxeStyle;
import static com.intellij.plugins.haxe.ide.documentation.HaxeDocCommentStyleConverter.toJavadocStyle;
import static com.intellij.plugins.haxe.util.HaxeDocumentationUtil.leadingWhitespace;

/** Rewrites the doc comment at the caret in the other style: Haxe style to Javadoc style, or back. */
public class ConvertDocCommentStyleIntention extends PsiUpdateModCommandAction<HaxePsiDocCommentImpl> {

  public ConvertDocCommentStyleIntention() {
    super(HaxePsiDocCommentImpl.class);
  }

  @Override
  public @Nls @NotNull String getFamilyName() {
    return HaxeBundle.message("haxe.intention.convert.doc.comment.style.family");
  }

  @Override
  protected boolean isFileAllowed(@NotNull PsiFile file) {
    return file instanceof HaxeFile;
  }

  @Override
  protected @Nullable Presentation getPresentation(@NotNull ActionContext context, @NotNull HaxePsiDocCommentImpl docComment) {
    if (!isConvertible(docComment)) return null;
    String key = isJavadocStyle(docComment)
                 ? "haxe.intention.convert.doc.comment.to.haxe.style"
                 : "haxe.intention.convert.doc.comment.to.javadoc.style";
    return Presentation.of(HaxeBundle.message(key));
  }

  @Override
  protected void invoke(@NotNull ActionContext context, @NotNull HaxePsiDocCommentImpl docComment, @NotNull ModPsiUpdater updater) {
    String text = docComment.getText();
    ElementManipulators.handleContentChange(docComment, TextRange.from(0, text.length()), converted(docComment, openerIndent(docComment)));
  }

  /** The whole converted comment, not the changed lines alone: every line but the opener's changes, some only in whitespace. */
  @Override
  protected @NotNull IntentionPreviewInfo generatePreview(ActionContext context, HaxePsiDocCommentImpl docComment) {
    String indent = openerIndent(docComment);
    // against an empty original the diff is one insertion, which the preview shows in full
    return new IntentionPreviewInfo.CustomDiff(HaxeFileType.INSTANCE, null, "", indent + converted(docComment, indent), false);
  }

  private static @NotNull String converted(@NotNull HaxePsiDocCommentImpl docComment, @NotNull String indent) {
    String text = docComment.getText();
    return isJavadocStyle(docComment)
           ? toHaxeStyle(text, indent, HaxeCommenter.haxeStyleBodyIndent(docComment.getContainingFile()))
           : toJavadocStyle(text, indent);
  }

  /** The indentation of the line the comment opens on. */
  private static @NotNull String openerIndent(@NotNull HaxePsiDocCommentImpl docComment) {
    String fileText = docComment.getContainingFile().getText();
    int commentStart = docComment.getTextRange().getStartOffset();
    int lineStart = fileText.lastIndexOf('\n', commentStart - 1) + 1;
    return leadingWhitespace(fileText.substring(lineStart, commentStart));
  }
}
