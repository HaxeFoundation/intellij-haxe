package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertBeforeElementFunction;

public class HaxeIntroduceFunctionIntention extends HaxeUnresolvedSymbolIntentionBase<HaxeCallExpression> {

  private final String methodName;

  public HaxeIntroduceFunctionIntention(HaxeCallExpression callExpression) {
    super(callExpression);
    @NotNull PsiElement[] children = callExpression.getExpression().getChildren();
    methodName = children[children.length-1].getText();
  }


  @Override
  public @IntentionName @NotNull String getText() {
    return "Create local function '" + methodName + "'";
  }


  @Override
  protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
    HaxeBlockStatement block = PsiTreeUtil.getParentOfType(element, HaxeBlockStatement.class);
    if (block != null) {
      PsiElement anchor = findInsertBeforeElementFunction(element, block);
      PsiElement methodDeclaration = generateDeclaration(project).copy();
      anchor.getParent().addBefore(methodDeclaration, anchor);
      anchor.getParent().addBefore(createNewLine(project), anchor);

      CodeStyleManager.getInstance(project).reformat(methodDeclaration);
    }
    return element.getContainingFile();
  }


  private PsiElement generateDeclaration(@NotNull Project project) {
    String returnType = guessReturnElementType();
    String returnStatement = determineReturnStatement(returnType);
    String function = """
      function %s (%s):%s {
        %s
      }
      """
      .formatted(methodName, generateParameterList(), returnType, returnStatement);

    return HaxeElementGenerator.createMethodDeclaration(project, function);
  }

  private String determineReturnStatement(String type) {
    if (Objects.equals(type, SpecificHaxeClassReference.VOID)) return "";
    if (Objects.equals(type, SpecificHaxeClassReference.INT)) return " return 0;";
    if (Objects.equals(type, SpecificHaxeClassReference.FLOAT)) return " return 0.0;";
    if (Objects.equals(type, SpecificHaxeClassReference.BOOL)) return " return false;";
    return "return null;";
  }

  private String guessReturnElementType() {
    HaxeCallExpression element = myPsiElementPointer.getElement();
    if (element.getParent() instanceof  HaxeBlockStatement) return SpecificHaxeClassReference.VOID;
    return guessElementTypeText();
  }

  private String generateParameterList() {
    StringBuilder builder = new StringBuilder();
    HaxeCallExpression element = myPsiElementPointer.getElement();
    HaxeCallExpressionList expressionList = element.getExpressionList();
    if (expressionList!= null) {
      Set<String> used = new HashSet<>();
      @NotNull List<HaxeExpression> list = expressionList.getExpressionList();
      for (int i = 0; i < list.size(); i++) {
        HaxeExpression expression = list.get(i);
        ResultHolder type = HaxeExpressionEvaluator.evaluate(expression, null).result;
        String paramName = "p" + i;
        String typeTag = "";
        if (!type.isUnknown()) {
            List<String> names = HaxeNameSuggesterUtil.getSuggestedNames(expression, false, true, used);
            if (!names.isEmpty()) {
              String name = names.get(0);
              used.add(name);
              paramName = name;
            }
            typeTag = ":" + getTypeName(type);
        }
        builder.append(paramName).append(typeTag);
        if (i + 1 != list.size()) {
          builder.append(",");
        }
      }
    }
    return builder.toString();
  }



}