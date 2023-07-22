package com.intellij.plugins.haxe.ide.editor;

import com.intellij.java.JavaBundle;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ElementColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxePsiToken;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.LITHEX;


public class HaxeColorProvider implements ElementColorProvider {

  private static final Pattern colorPattern = Pattern.compile("^0x[0-9a-f]{6}$", Pattern.CASE_INSENSITIVE);

  @Override
  public @Nullable Color getColorFrom(@NotNull PsiElement element) {

    if (element instanceof HaxePsiToken psiToken) {
      if (psiToken.getTokenType() == LITHEX) {

        String text = psiToken.getText();
        if (colorPattern.matcher(text).matches()) {
          int i = Integer.parseInt(text.substring(2), 16);
          return new Color(i);
        }
      }
    }

    return null;
  }

  @Override
  public void setColorTo(@NotNull PsiElement element, @NotNull Color color) {
    if (element.isValid()) {
      Project project = element.getProject();
      String hexColor = String.format("0x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
      final Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());

      Runnable command = () -> {
        PsiElementFactory instance = PsiElementFactory.getInstance(project);
        PsiExpression newToken = instance.createExpressionFromText(hexColor, element);
        element.replace(newToken);
      };
      CommandProcessor.getInstance()
        .executeCommand(project, command, JavaBundle.message("change.color.command.text"), null, document);
    }
  }
}
