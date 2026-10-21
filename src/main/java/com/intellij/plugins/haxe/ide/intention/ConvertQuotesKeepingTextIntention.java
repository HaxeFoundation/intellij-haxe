package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.model.fixer.HaxeSurroundFixer;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * The second choice on a double-quoted string holding a {@code $}: single quotes
 * with every {@code $} escaped, so the text stays what it was. The plain
 * {@link ConvertQuotesIntention} on the same string starts interpolating instead.
 */
public class ConvertQuotesKeepingTextIntention extends BaseIntentionAction {

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return HaxeBundle.message("haxe.intention.convert.quotes.family");
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.convert.to.single.quotes.keep.text");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    HaxeStringLiteralExpression expression = ConvertQuotesIntention.stringLiteralAt(editor, file);
    return expression != null
           && ConvertQuotesIntention.isWrappedWithDoubleQuotes(expression)
           && ConvertQuotesIntention.hasDollar(expression);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxeStringLiteralExpression expression = ConvertQuotesIntention.stringLiteralAt(editor, file);
    if (expression == null) return;
    String commandName = HaxeBundle.message("haxe.intention.convert.quotes.command");
    WriteCommandAction.runWriteCommandAction(project, commandName, null, HaxeSurroundFixer.replaceQuotesWithSingleQuotesKeepingText(expression));
  }
}
