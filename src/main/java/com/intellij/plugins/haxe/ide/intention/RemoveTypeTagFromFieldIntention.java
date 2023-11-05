package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclarationList;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RemoveTypeTagFromFieldIntention extends BaseIntentionAction {

  private HaxePsiField myField;


  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return HaxeBundle.message("quick.fixes.family");
  }
  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.remove.type.tag");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;

    attemptToFindField(editor, file);

    return  myField != null && myField.getTypeTag() != null;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    myField.getTypeTag().delete();
  }


  private void attemptToFindField(Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeLocalVarDeclarationList varDeclarationList = PsiTreeUtil.getParentOfType(place, HaxeLocalVarDeclarationList.class);
    if (varDeclarationList != null) {
      List<HaxeLocalVarDeclaration> list = varDeclarationList.getLocalVarDeclarationList();
      if (!list.isEmpty())myField = list.get(list.size() - 1);
    } else if (place instanceof HaxePsiField psiField) {
      myField = psiField;
    }else {
      myField = PsiTreeUtil.getParentOfType(place, HaxePsiField.class);
    }
  }


}