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
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeObjectLiteralClassModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.util.HaxeElementGenerator.createTypeTag;

public class AddTypeTagToFieldIntention extends BaseIntentionAction {

  public AddTypeTagToFieldIntention() {
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.add.type.tag");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;



    HaxePsiField field = attemptToFindField(editor, file);;
    boolean isMissingTypeTag = field != null && field.getTypeTag() == null;
    if (isMissingTypeTag) {
      ResultHolder type = HaxeExpressionEvaluator.evaluate(field, new HaxeExpressionEvaluatorContext(field), null).result;
      return !(type == null || type.isUnknown());
    }
    return false;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {

    HaxePsiField field = attemptToFindField(editor, file);;
    boolean isMissingTypeTag = field != null && field.getTypeTag() == null;
    if (isMissingTypeTag) {
      ResultHolder type = HaxeExpressionEvaluator.evaluate(field, new HaxeExpressionEvaluatorContext(field), null).result;
      if (!(type == null || type.isUnknown())) {

        SpecificTypeReference typeReference = type.getType();
        String presentationString = typeReference.toPresentationString();

        // object literals needs to be handled in a different way as the Presentation string is usually shortened
        if (typeReference instanceof SpecificHaxeClassReference classReference) {
          HaxeClassModel haxeClassModel = classReference.getHaxeClassModel();
          if(haxeClassModel instanceof HaxeObjectLiteralClassModel objectLiteralModel) {
            presentationString =  objectLiteralModel.buildTypeString();
          }
        }

        HaxeTypeTag tag = createTypeTag(project, presentationString);
        field.addAfter(tag, field.getComponentName());
      }
    }

  }


  private HaxePsiField attemptToFindField(Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeLocalVarDeclarationList varDeclarationList = PsiTreeUtil.getParentOfType(place, HaxeLocalVarDeclarationList.class);

    HaxePsiField field = null;
    if (varDeclarationList != null) {
      List<HaxeLocalVarDeclaration> list = varDeclarationList.getLocalVarDeclarationList();
      if (!list.isEmpty())field = list.get(list.size() - 1);
    } else if (place instanceof HaxePsiField psiField) {
      field = psiField;
    }else {
      field = PsiTreeUtil.getParentOfType(place, HaxePsiField.class);
    }
    return field;
  }


}