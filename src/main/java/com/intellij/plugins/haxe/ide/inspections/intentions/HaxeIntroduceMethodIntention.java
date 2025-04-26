package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertAfterElementForMethod;
import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findTypesRequiringImportsAndAddToFile;

public class HaxeIntroduceMethodIntention
  extends HaxeUnresolvedSymbolIntentionBase<HaxeCallExpression>
  implements HighPriorityAction {

  protected final @NotNull SmartPsiElementPointer<HaxeClass> myPsiTargetPointer;

  private final String methodName;

  public HaxeIntroduceMethodIntention(@NotNull HaxeCallExpression callExpression, @NotNull HaxeClass targetClass) {
    super(callExpression);
    @NotNull PsiElement[] children = callExpression.getExpression().getChildren();
    methodName = children[children.length-1].getText();
    myPsiTargetPointer  = createPointer(targetClass);
  }


  @Override
  public @IntentionName @NotNull String getText() {
    return "Create method '" + methodName + "'";
  }

  @Override
  protected String getPreviewName() {
    HaxeClass aClass = myPsiTargetPointer.getElement();
    return  aClass == null ? null : aClass.getQualifiedName();
  }

  @Override
  protected PsiElement getTargetPsi() {
    return findInsertAfterElementForMethod(myPsiElementPointer.getElement(), myPsiTargetPointer.getElement(),false);
  }

  @Override
  protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
    PsiElement anchor = findInsertAfterElementForMethod(element, myPsiTargetPointer.getElement(), preview);


    PsiElement methodDeclaration = generateDeclaration(project).copy();
    methodDeclaration = anchor.getParent().addAfter(methodDeclaration, anchor);
    anchor.getParent().addBefore(createNewLine(project), methodDeclaration);

//    generateMissingImports()

    methodDeclaration = CodeStyleManager.getInstance(project).reformat(methodDeclaration);
    if(!preview) {
      if(methodDeclaration instanceof HaxeMethodDeclaration declaration) {
        HaxeMethodModel newModel = declaration.getModel();
        List<HaxeParameterModel> parameters = newModel.getParameters();
        ResultHolder returnType = newModel.getReturnType(null);

        ResultHolder knownReturnType = guessElementType(myPsiElementPointer.getElement());
        if(knownReturnType.isDynamic() || knownReturnType.isUnknown()) knownReturnType = null;
        findTypesRequiringImportsAndAddToFile(parameters, getKnownParameterTypeList(), returnType, knownReturnType, anchor.getContainingFile());
      }
    }
    return anchor.getContainingFile();
  }




  private PsiElement generateDeclaration(@NotNull Project project) {
    String returnType = guessReturnElementType();
    String returnStatement = determineReturnStatement(returnType);
    String optionalStaticKeyword = needsToBeStatic() ? "static" : "";
    String privateKeyword = needsToBePublic() ? "public" : "private";
    String function = """
      %s %s function %s (%s):%s {
        %s
      }
      """
      .formatted(privateKeyword, optionalStaticKeyword, methodName, generateParameterList(), returnType, returnStatement);

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

  private List<ResultHolder> getKnownParameterTypeList() {
    HaxeCallExpression element = myPsiElementPointer.getElement();
    if(element == null) return List.of();
    HaxeCallExpressionList expressionList = element.getExpressionList();
    List<ResultHolder>  parameterTypes = new ArrayList<>();
    if (expressionList!= null) {
      @NotNull List<HaxeExpression> list = expressionList.getExpressionList();
        for (HaxeExpression expression : list) {
            ResultHolder type = HaxeExpressionEvaluator.evaluate(expression, null).result;
            parameterTypes.add(type);
        }
    }
    return parameterTypes;
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
            List<String> names = HaxeNameSuggesterUtil.getSuggestedNames(expression, false, false, used);
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