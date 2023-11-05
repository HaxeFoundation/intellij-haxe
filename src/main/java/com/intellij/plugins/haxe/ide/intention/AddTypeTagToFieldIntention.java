package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclarationList;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.util.HaxeElementGenerator.createTypeTag;

public class AddTypeTagToFieldIntention extends BaseIntentionAction {

  private HaxePsiField myField;
  private ResultHolder type;

  public AddTypeTagToFieldIntention() {
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return HaxeBundle.message("quick.fixes.family");
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.add.type.tag");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;

    attemptToFindField(editor, file);

    boolean isMissingTypeTag = myField != null && myField.getTypeTag() == null;
    if (isMissingTypeTag) {
      type = HaxeExpressionEvaluator.evaluate(myField, new HaxeExpressionEvaluatorContext(myField), null).result;
      return !(type == null || type.isUnknown());
    }
    return false;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {

      HaxeTypeTag tag = createTypeTag(project, type.getType().toString());
      myField.addAfter(tag, myField.getComponentName());

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