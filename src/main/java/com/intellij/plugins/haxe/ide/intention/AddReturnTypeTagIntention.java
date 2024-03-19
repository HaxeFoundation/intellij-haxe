package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.util.HaxeElementGenerator.createTypeTag;
import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.findParentOfTypeButStopIfTypeIs;

public class AddReturnTypeTagIntention extends BaseIntentionAction {


  public AddReturnTypeTagIntention() {
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
    return HaxeBundle.message("haxe.quickfix.add.return.type");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;

    HaxeMethod method = attemptToFindMethod(editor, file);

    boolean isMissingTypeTag = method != null
                               && method.getModel() != null
                               && !method.isConstructor()
                               && method instanceof HaxeMethodDeclaration declaration
                               && declaration.getTypeTag() == null;
    if (isMissingTypeTag) {
      return !( getReturnType(method) == null);
    }
    return false;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxeMethod method = attemptToFindMethod(editor, file);
    if (method != null) {
      ResultHolder returnType = getReturnType(method);

      String typeText = returnType.isUnknown() ? SpecificTypeReference.VOID : returnType.getType().toPresentationString();
      HaxeTypeTag tag = createTypeTag(project, typeText);
      PsiParameterList list = method.getParameterList();
      PsiElement element = PsiTreeUtil.nextVisibleLeaf(list);
      method.addAfter(tag, element);
    }
  }

  private static ResultHolder getReturnType(HaxeMethod method) {
    HaxeMethodModel model = method.getModel();
    HaxeGenericResolver resolver = model.getGenericResolver(null);
    resolver = resolver.withTypeParametersAsType(model.getGenericParams());
    return model.getReturnType(resolver);
  }


  private @Nullable HaxeMethod attemptToFindMethod(Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
   if (place instanceof HaxeMethod method) {
     return method;
    }else if (place != null){
     return findParentOfTypeButStopIfTypeIs(place, HaxeMethod.class, HaxeBlockStatement.class);
    }
    return null;
  }



}