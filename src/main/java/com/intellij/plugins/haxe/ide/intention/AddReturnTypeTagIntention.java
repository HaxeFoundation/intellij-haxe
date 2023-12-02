package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
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

import static com.intellij.plugins.haxe.util.HaxeElementGenerator.createTypeTag;
import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.findParentOfTypeButStopIfTypeIs;

public class AddReturnTypeTagIntention extends BaseIntentionAction {

  private HaxeMethod myMethod;
  private ResultHolder returnType;

  public AddReturnTypeTagIntention() {
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
    return HaxeBundle.message("haxe.quickfix.add.return.type");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;

    attemptToFindMethod(editor, file);

    boolean isMissingTypeTag = myMethod != null
                               && myMethod.getModel() != null
                               && !myMethod.isConstructor()
                               && myMethod instanceof HaxeMethodDeclaration declaration
                               && declaration.getTypeTag() == null;
    if (isMissingTypeTag) {
      HaxeGenericResolver resolver = myMethod.getModel().getGenericResolver(null);
      resolver = resolver.withTypeParametersAsType(myMethod.getModel().getGenericParams());
      returnType = myMethod.getModel().getReturnType(resolver);
      return !(returnType == null);
    }
    return false;
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {

    String typeText = returnType.isUnknown() ? SpecificTypeReference.VOID : returnType.getType().toPresentationString();
    HaxeTypeTag tag = createTypeTag(project, typeText);
    PsiParameterList list = myMethod.getParameterList();
    PsiElement element = PsiTreeUtil.nextVisibleLeaf(list);
    myMethod.addAfter(tag, element);

  }


  private void attemptToFindMethod(Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
   if (place instanceof HaxeMethod method) {
     myMethod = method;
    }else if (place != null){
     myMethod = findParentOfTypeButStopIfTypeIs(place, HaxeMethod.class, HaxeBlockStatement.class);
    }
  }



}