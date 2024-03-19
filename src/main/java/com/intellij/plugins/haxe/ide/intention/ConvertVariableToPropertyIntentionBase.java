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

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;
    HaxePsiField field = attemptToFindFieldDeclaration(editor, file);

    if (field  == null) return false;

    if (field.getTypeTag() == null) return false;
    if (field.getVarInit() != null) return false;
    if (field.getModel() instanceof HaxeFieldModel model) {
      if (model.hasModifier(HaxePsiModifier.INLINE)) return false;
      if (model.isProperty()) return false;
    }else {
      return false;
    }

    return true;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxePsiField field = attemptToFindFieldDeclaration(editor, file);
    if (field!= null) {
      HaxePsiField element = getTempProperty(project, field);
      PsiElement copy = HaxeElementGenerator.createVarDeclaration(project, element.getText()).copy();
      field.replace(copy);
    }

  }

  @NotNull
  private HaxePsiField getTempProperty(@NotNull Project project,  HaxePsiField field) {
    HaxePsiField element = (HaxePsiField)field.copy();
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


  private HaxeFieldDeclaration attemptToFindFieldDeclaration(Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeLocalVarDeclarationList varDeclarationList = PsiTreeUtil.getParentOfType(place, HaxeLocalVarDeclarationList.class);

    HaxeFieldDeclaration declaration = null;
    if (varDeclarationList != null) {
      List<HaxeLocalVarDeclaration> list = varDeclarationList.getLocalVarDeclarationList();
      if (!list.isEmpty()){
        if(list.get(list.size() - 1) instanceof HaxeFieldDeclaration fieldDeclaration){
          declaration = fieldDeclaration;
        }
      }
    }
    else if (place instanceof HaxeFieldDeclaration fieldDeclaration) {
      declaration = fieldDeclaration;
    }
    else {
      declaration = PsiTreeUtil.getParentOfType(place, HaxeFieldDeclaration.class);
    }
    return declaration;
  }
}