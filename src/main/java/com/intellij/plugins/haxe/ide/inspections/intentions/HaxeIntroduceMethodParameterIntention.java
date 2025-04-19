package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeIntroduceMethodParameterIntention extends HaxeUnresolvedSymbolIntentionBase<HaxeReferenceExpression> {

  private final String myDeclarationName;
  private final String expressionText;


  public HaxeIntroduceMethodParameterIntention(HaxeReferenceExpression expression, HaxeMethodDeclaration declaration) {
    super(expression);
    @NotNull PsiElement[] children = expression.getChildren();
    expressionText = children[children.length-1].getText();
    myDeclarationName = declaration.getName();
  }


  @Override
  public @IntentionName @NotNull String getText() {
    return "Create parameter  '" + expressionText + "' in " + myDeclarationName;
  }



  @Override
  protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
    HaxeMethodDeclaration methodDeclaration = PsiTreeUtil.getParentOfType(element, HaxeMethodDeclaration.class);

    if (methodDeclaration != null) {
      HaxeParameterList parameterList = methodDeclaration.getParameterList();
      HaxeParameter parameter = (HaxeParameter)generateParameter(project).copy();
      List<HaxeParameter> list = parameterList.getParameterList();
      if (list.isEmpty()) {
        parameterList.add(parameter);
      }else {
        PsiUtilCore.ensureValid(parameterList);
        PsiElement seperator = generateComma(project);
        parameterList.add(seperator);
        parameterList.add(parameter);

        CodeStyleManager.getInstance(project).reformat(parameterList);
      }
    }
    return element.getContainingFile();
  }


  private HaxeParameter generateParameter(@NotNull Project project) {
    return HaxeElementGenerator.createParameter(project, expressionText + ":" + guessElementTypeText() );
  }
  private PsiElement generateComma(@NotNull Project project) {
    return HaxeElementGenerator.createComma(project);
  }


}
