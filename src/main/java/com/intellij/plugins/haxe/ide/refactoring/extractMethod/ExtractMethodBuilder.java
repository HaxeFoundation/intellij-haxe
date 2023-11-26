package com.intellij.plugins.haxe.ide.refactoring.extractMethod;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ExtractMethodBuilder {

  private  int  startOffset;
  private  int  endOffset;

  private  String  selectedText;
  private  List<HaxePsiCompositeElement> expressions;
  private  List<HaxeAssignExpression> assignExpressions;
  private  List<HaxeReferenceExpression> assignExpressionsOutsideSelection;
  private List<HaxeReferenceExpression> referencesUsed;
  private List<HaxeReferenceExpression> referencesToParameters;
  private List<HaxeLocalVarDeclaration> localVariables;
  private Map<PsiReference, HaxeLocalVarDeclaration> localUsedOutside;

  private Map<String, String> parametersMap;
  public ExtractMethodBuilder expressions(List<HaxePsiCompositeElement> expressions) {
    this.expressions = expressions;
    return this;
  }
  public ExtractMethodBuilder startOffset(int offset) {
    startOffset = offset;
    return this;
  }
  public ExtractMethodBuilder endOffset(int offset) {
    endOffset = offset;
    return this;
  }

  public ExtractMethodBuilder selectedText(@Nullable @NlsSafe String text) {
    selectedText = text;
    return this;
  }


  public void validateAndProcessExpressions() {

    assignExpressions = findAssignExpressions();
    assignExpressionsOutsideSelection = findAssignsOutsideSelection(assignExpressions);

    referencesUsed = findReferencesUsed(expressions, assignExpressions);
    referencesToParameters = findOutsideReferencesForParameterList(referencesUsed);

    localVariables = findLocalVariables();
    localUsedOutside = findLocalVarsUsedOutside(localVariables);

    parametersMap = buildParameterList(referencesToParameters);

    // check if we can extract method
    if (moreThanOneLocalVarReferencedOutsideSelection()) {
      throw  new UnableToExtractMethodException();
    } else if (localVarDeclarationPartiallyOutsideOfSelection()) {
      throw  new UnableToExtractMethodException();
    } else if (moreThanOneLocalVariableUsedOutsideSelection()) {
      throw  new UnableToExtractMethodException();
    } else if (requireMoreThanOneReturnValue()) {
      throw  new UnableToExtractMethodException();
    }else if (selectionContainsMethod()) {
      throw  new UnableToExtractMethodException();
    }
  }

  private boolean localVarDeclarationPartiallyOutsideOfSelection() {
    TextRange range = new TextRange(startOffset, endOffset);
    for (HaxeLocalVarDeclaration variable : localVariables) {
      if (variable.getParent() instanceof  HaxeLocalVarDeclarationList list) {
        if (!range.contains(list.getTextRange())) return true;
      }
    }
    return false;
  }

  private boolean requireMoreThanOneReturnValue() {
    return assignExpressionsOutsideSelection.size() + localUsedOutside.size() > 1;
  }

  private boolean moreThanOneLocalVariableUsedOutsideSelection() {
    return localUsedOutside.size() > 1;
  }

  private boolean moreThanOneLocalVarReferencedOutsideSelection() {
    return assignExpressionsOutsideSelection.size() > 1;
  }

  private List<HaxeAssignExpression> findAssignExpressions() {
    List<HaxeAssignExpression> assignExpressions = new ArrayList<>();
    for (HaxePsiCompositeElement expression : expressions) {
      if (expression instanceof HaxeAssignExpression assignExpression) assignExpressions.add(assignExpression);
      assignExpressions.addAll(PsiTreeUtil.findChildrenOfType(expression, HaxeAssignExpression.class));
    }
    return assignExpressions;
  }

  private List<HaxeReferenceExpression> findAssignsOutsideSelection(List<HaxeAssignExpression> assignExpressions) {
    List<HaxeReferenceExpression> outside = new ArrayList<>();
    for (HaxeAssignExpression expression : assignExpressions) {
      HaxeExpression leftExpression = expression.getLeftExpression();
      if (leftExpression instanceof HaxeReferenceExpression referenceExpression) {
        PsiElement resolve = referenceExpression.resolve();
        if (resolve == null) {
          // todo error
          break;
        }
        if (resolve instanceof HaxeLocalVarDeclaration) {
          if (!resolve.getTextRange().intersects(startOffset, endOffset)) {
            outside.add(referenceExpression);
          }
        }
      }
    }
    return outside;
  }


  private List<HaxeReferenceExpression> findReferencesUsed(List<HaxePsiCompositeElement> expressions,
                                                           List<HaxeAssignExpression> ignoreList) {
    List<HaxeReferenceExpression> referenceExpressions = new ArrayList<>();
    for (HaxePsiCompositeElement expression : expressions) {
      //todo filter out members, we only need local variables
      if (expression instanceof HaxeReferenceExpression haxeReferenceExpression) referenceExpressions.add(haxeReferenceExpression);
      referenceExpressions.addAll(PsiTreeUtil.findChildrenOfType(expression, HaxeReferenceExpression.class));
    }
    for (HaxeAssignExpression expression : ignoreList) {
      referenceExpressions.remove(expression.getLeftExpression());
    }
    return referenceExpressions;
  }

  private List<HaxeReferenceExpression> findOutsideReferencesForParameterList(List<HaxeReferenceExpression> referencesUsed) {
    List<HaxeReferenceExpression> outside = new ArrayList<>();
    for (HaxeReferenceExpression referenceExpression : referencesUsed) {
      PsiElement resolve = referenceExpression.resolve();
      if (resolve instanceof HaxeLocalVarDeclaration) {
        if (!resolve.getTextRange().intersects(startOffset, endOffset)) {
          outside.add(referenceExpression);
        }
      }
    }
    return outside;
  }

  private List<HaxeLocalVarDeclaration> findLocalVariables() {
    List<HaxeLocalVarDeclaration> localVarList = new ArrayList<>();
    for (HaxePsiCompositeElement expression : expressions) {
      if (expression instanceof HaxeLocalVarDeclarationList varDeclarationList) {
        localVarList.addAll(varDeclarationList.getLocalVarDeclarationList());
      }
      else if (expression instanceof HaxeLocalVarDeclaration varDeclaration) {
        localVarList.add(varDeclaration);
      }
    }
    return localVarList;
  }

  private Map<PsiReference, HaxeLocalVarDeclaration> findLocalVarsUsedOutside(List<HaxeLocalVarDeclaration> localVariables) {
    Map<PsiReference, HaxeLocalVarDeclaration> outside = new HashMap<>();
    TextRange markedText = new TextRange(startOffset, endOffset);
    for (HaxeLocalVarDeclaration varDeclaration : localVariables) {
      HaxeComponentName componentName = varDeclaration.getComponentName();
      SearchScope useScope = PsiSearchHelper.getInstance(componentName.getProject()).getUseScope(componentName);


      List<PsiReference> references = new ArrayList<>(ReferencesSearch.search(componentName, useScope).findAll());
      for (PsiReference reference : references) {
        TextRange range = reference.getElement().getTextRange();
        if (!markedText.contains(range)) {
          outside.put(reference, varDeclaration);
        }
      }
    }
    return outside;
  }

  private boolean selectionContainsMethod() {
    for (HaxePsiCompositeElement expression : expressions) {
      if (expression instanceof HaxeMethodDeclaration) {
        return true;
      }
    }
    return false;
  }

  protected List<String> getSuggestedNames(HaxePsiCompositeElement compositeElement) {
    Set<String> usedNames = HaxeRefactoringUtil.collectUsedNames(compositeElement);
    List<String> candidates = new ArrayList<>();
    if (compositeElement instanceof HaxeExpression expression) {
      candidates.addAll(HaxeNameSuggesterUtil.getSuggestedNames(expression, false));
    }else {
      candidates.add("extracted");
    }

    List<String> result = new ArrayList<>();
    for (String candidate : candidates) {
      int index = 0;
      String suffix = "";
      while (usedNames.contains(candidate + suffix)) {
        suffix = Integer.toString(++index);
      }
      result.add(candidate + suffix);
    }

    return result;
  }

  public HaxeMethodDeclaration buildExtractedMethod(@Nullable Project project) {



    HaxePsiCompositeElement fistElement = expressions.get(0);
    String suggestedName = getSuggestedNames(fistElement).get(0);

    boolean firstStatementReturn = PsiTreeUtil.getParentOfType(fistElement, HaxeBinaryExpression.class) != null;

    String block = buildMethodContent(selectedText, firstStatementReturn);
    String method = buildMethod(suggestedName, block, parametersMap);

    return  HaxeElementGenerator.createMethodDeclaration(project, method);
  }

  private String buildMethod(String suggestedName, String block, Map<String, String> parameters) {
    StringBuilder builder = new StringBuilder();
    builder.append("\nfunction "+suggestedName+" (");
    parameters.forEach((paramName, paramType) -> {
      builder.append(paramName);
      if (paramType != null && !paramType.isBlank()) {
        builder.append(":").append(paramType);
      }
    });
    builder.append(")");
    builder.append(block);
    builder.append("\n\n");
    return builder.toString();
  }

  private String buildMethodContent(String selectedText, boolean firstStatementReturn) {
    if (firstStatementReturn) {
      if (selectedText.trim().endsWith(";")) {
        return "{\n return " + selectedText + "\n}\n";
      }else {
        return "{\n return " + selectedText + ";\n}\n";
      }
    }else {
      return "{\n" + selectedText + "\n}\n";
    }
  }

  private Map<String, String> buildParameterList(List<HaxeReferenceExpression> RefrencesForparameters) {
    Map<String, String> parameterNameTypeMap = new HashMap<>();
    for (HaxeReferenceExpression ref : RefrencesForparameters) {
      ResultHolder type = HaxeExpressionEvaluator.evaluate(ref, null).result;
      if (type.isUnknown()) {
        parameterNameTypeMap.put(ref.getReferenceName(), null);
      }
      else {
        parameterNameTypeMap.put(ref.getReferenceName(), type.toPresentationString());
      }
    }
    return parameterNameTypeMap;
  }





  public PsiElement buildReplacementExpression(@Nullable Project project, HaxeMethodDeclaration methodDeclaration) {

    boolean containsReturnStatements = PsiTreeUtil.getChildOfType(methodDeclaration, HaxeReturnStatement.class) != null;

    String methodName = methodDeclaration.getComponentName().getText();
    String methodCall = methodName + "(" + String.join(", ", parametersMap.keySet()) + ")";

    if (!assignExpressionsOutsideSelection.isEmpty()) {
      // TODO find type of assign // TODO this is wrong  check local vars used outside for return/assign
      methodCall = assignExpressionsOutsideSelection.get(0).getText() + " = " + methodCall;
    }
    else if (!localUsedOutside.isEmpty() && !containsReturnStatements) {
      Set<PsiReference> references = localUsedOutside.keySet();
      String varName = localUsedOutside.get(references.iterator().next()).getText();
      methodCall = "var " + varName + " = " + methodCall;

      HaxeBlockStatement blockStatement = methodDeclaration.getBlockStatement();
      PsiElement newLine = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n\n").copy();
      PsiElement returnStatement = HaxeElementGenerator.createStatementFromText(project, "return " + varName + ";").copy();
      List<HaxeExpression> expressionList = blockStatement.getExpressionList();
      if (expressionList.isEmpty()) {
        throw  new UnableToExtractMethodException();
      }
      PsiElement lastExpression = expressionList.get(expressionList.size() - 1);
      if (lastExpression.getNextSibling().textMatches(";")) lastExpression = lastExpression.getNextSibling();
      blockStatement.addAfter(returnStatement, lastExpression);
      blockStatement.addAfter(newLine, lastExpression);
      //CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(blockStatement);
      // TODO add return value
    }
    else if (!localUsedOutside.isEmpty()) {
      // can not return value and assign as the method already got return statments
      throw  new UnableToExtractMethodException();
    }

    return  HaxeElementGenerator.createStatementFromText(project, methodCall);

  }


}
