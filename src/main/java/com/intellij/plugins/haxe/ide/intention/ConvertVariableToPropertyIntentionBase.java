package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class ConvertVariableToPropertyIntentionBase extends BaseIntentionAction {

  private HaxePsiField myField;


  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;
    attemptToFindField(editor, file);

    if (myField  == null) return false;

    if (myField.getTypeTag() == null) return false;
    if (myField.getVarInit() != null) return false;
    if (myField.getModel() instanceof HaxeFieldModel model) {
      if (model.hasModifier(HaxePsiModifier.INLINE)) return false;
      if (model.isProperty()) return false;
    }else {
      return false;
    }

    return true;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxePsiField element = getTempProperty(project);
    PsiElement copy = HaxeElementGenerator.createVarDeclaration(project, element.getText()).copy();
    myField.replace(copy);

  }

  @NotNull
  private HaxePsiField getTempProperty(@NotNull Project project) {
    HaxePsiField element = (HaxePsiField)myField.copy();
    HaxePropertyDeclaration declaration = generateTmpDeclaration(project);
    HaxeComponentName name = element.getComponentName();
    HaxeIdentifier identifier = name.getIdentifier();

    name.addAfter(declaration, identifier);
    return element;
  }

  private HaxePropertyDeclaration generateTmpDeclaration(@NotNull Project project) {
    return (HaxePropertyDeclaration)HaxeElementGenerator.createVarDeclaration(project, getPropertyElementString())
      .getPropertyDeclaration().copy();
  }

  @NotNull
  protected abstract String getPropertyElementString();


  private void attemptToFindField(Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeLocalVarDeclarationList varDeclarationList = PsiTreeUtil.getParentOfType(place, HaxeLocalVarDeclarationList.class);
    if (varDeclarationList != null) {
      List<HaxeLocalVarDeclaration> list = varDeclarationList.getLocalVarDeclarationList();
      if (!list.isEmpty()) myField = list.get(list.size() - 1);
    }
    else if (place instanceof HaxePsiField psiField) {
      myField = psiField;
    }
    else {
      myField = PsiTreeUtil.getParentOfType(place, HaxePsiField.class);
    }
  }
}