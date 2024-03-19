package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;


public class ConvertPropertyToVariableIntention extends BaseIntentionAction {


  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.property.to.var");
  }


  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;
    HaxeFieldDeclaration field = attemptToFindField(editor, file);

    if (field  == null) return false;
    if (field.getTypeTag() == null) return false;
    if (field.getPropertyDeclaration() == null) return false;

    return true;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxeFieldDeclaration field = attemptToFindField(editor, file);
    if (field != null && field.getPropertyDeclaration() != null) {
      field.getPropertyDeclaration().delete();
    }
  }





  private HaxeFieldDeclaration attemptToFindField(Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
    if (place instanceof HaxeFieldDeclaration psiField) {
      return psiField;
    } else {
      return PsiTreeUtil.getParentOfType(place, HaxeFieldDeclaration.class);
    }
  }
}