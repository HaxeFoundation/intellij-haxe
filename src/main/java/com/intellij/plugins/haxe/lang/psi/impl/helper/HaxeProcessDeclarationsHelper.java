package com.intellij.plugins.haxe.lang.psi.impl.helper;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.lang.psi.stubs.StubPsiTreeUtil.getStubChildrenOfAnyType;


public final class HaxeProcessDeclarationsHelper {

  private HaxeProcessDeclarationsHelper() {}

  public static boolean processDeclarations(@NotNull PsiElement self,
                                             @NotNull PsiScopeProcessor processor,
                                             @NotNull ResolveState state,
                                             PsiElement lastParent,
                                             @NotNull PsiElement place) {

    // makes sure we resolve the local function if referenced from inside
    if (lastParent instanceof HaxeLocalFunctionDeclaration) {
      if (!processor.execute(lastParent, state)) {
        return false;
      }
    }

    for (PsiElement element : getDeclarationElementToProcess(self, lastParent)) {
      if (!processor.execute(element, state)) {
        return false;
      }
    }
    return true;
  }

  private static Set<PsiElement> getDeclarationElementToProcess(@NotNull PsiElement self, PsiElement lastParent) {
    final boolean isBlock = self instanceof HaxeBlockStatement || self instanceof HaxeSwitchCaseBlock;
    final PsiElement stopper = isBlock ? lastParent : null;
    // note using linkedHashSet because order is important here
    final Set<PsiElement> result = new LinkedHashSet<>();


    addDeclarations(result, getStubChildrenOfAnyType(self, HaxeFieldDeclaration.class, HaxeMethodDeclaration.class));

    addLocalVarDeclarations(result, UsefulPsiTreeUtil.getChildrenOfType(self, HaxeLocalVarDeclarationList.class, stopper));
    addDeclarations(result, UsefulPsiTreeUtil.getChildrenOfType(self, HaxeLocalFunctionDeclaration.class, stopper));


    List<PsiElement> topLevelDeclarations = getStubChildrenOfAnyType(self,
            HaxeClassDeclaration.class,
            HaxeExternClassDeclaration.class,
            HaxeInterfaceDeclaration.class,
            HaxeTypedefDeclaration.class,
            HaxeEnumDeclaration.class
    );
    addDeclarations(result, topLevelDeclarations);
    addEnumMembers(result, topLevelDeclarations);

    addFunctionLiteralsWithName(result, UsefulPsiTreeUtil.getChildrenOfType(self, HaxeFunctionLiteral.class, stopper));

    if (self instanceof HaxeSwitchCase switchCase) {
      List<HaxeSwitchCaseExpr> list = switchCase.getSwitchCaseExprList();
      for (HaxeSwitchCaseExpr expr : list) {
        addDeclarations(result, PsiTreeUtil.findChildrenOfType(expr, HaxeEnumExtractedValueReference.class));
        addDeclarations(result, PsiTreeUtil.findChildrenOfType(expr, HaxeSwitchCaseCapture.class));
        addDeclarations(result, PsiTreeUtil.findChildrenOfType(expr, HaxeExtractorMatchAssignExpression.class));
        addDeclarations(result, getObjectLiteralReferences(expr));
        addDeclarations(result, getArrayLiteralReferences(expr));
        addCaptureVariableDeclarations(expr, result);
      }
    }

    final HaxeParameterList parameterList = PsiTreeUtil.getStubChildOfType(self, HaxeParameterList.class);
    if (parameterList != null) {
      result.addAll(parameterList.getParameterList());
    }
    final HaxeOpenParameterList openParameterList = PsiTreeUtil.getChildOfType(self, HaxeOpenParameterList.class);
    if (openParameterList != null) {
      result.add(openParameterList);
    }
    final HaxeGenericParam genericParam = PsiTreeUtil.getStubChildOfType(self, HaxeGenericParam.class);
    if (genericParam != null) {
      result.addAll(genericParam.getGenericListPartList());
    }

    if (self instanceof HaxeForStatement forStatement) {
      HaxeKeyValueIterator keyValueIterator = forStatement.getKeyValueIterator();
      HaxeValueIterator valueIterator = forStatement.getValueIterator();
      if (!(lastParent instanceof HaxeIterable)) {
        if (keyValueIterator != null && keyValueIterator != lastParent) {
          result.add(keyValueIterator.getIteratorkey());
          result.add(keyValueIterator.getIteratorValue());
        }
        else if (valueIterator != null && valueIterator != lastParent) {
          result.add(valueIterator);
        }
      }
    }
    // TODO mlo - looks related to the one above, might want to merge
    if (self instanceof HaxeSwitchCase switchCase) {
      for (HaxeSwitchCaseExpr expr : switchCase.getSwitchCaseExprList()) {
        HaxeSwitchCaseCaptureVar captureVar = expr.getSwitchCaseCaptureVar();
        if (captureVar != null) {
          result.add(captureVar.getComponentName());
        }
        Collection<HaxeEnumArgumentExtractor> extractors = PsiTreeUtil.findChildrenOfType(expr, HaxeEnumArgumentExtractor.class);
        for (HaxeEnumArgumentExtractor extractor : extractors) {
          Collection<HaxeEnumExtractedValueReference> extractedValues =
            PsiTreeUtil.findChildrenOfType(extractor, HaxeEnumExtractedValueReference.class);
          List<HaxeComponentName> nameList = extractedValues.stream()
            .map(HaxeEnumExtractedValueReference::getComponentName)
            .toList();
          result.addAll(nameList);
          Collection<HaxeExtractorMatchExpression> matchExpressions =
            PsiTreeUtil.findChildrenOfType(extractor, HaxeExtractorMatchExpression.class);
          for (HaxeExtractorMatchExpression match : matchExpressions) {
            if (match.getMatch().getExpression() instanceof HaxeReferenceExpression expression) {
              result.add(expression);
            }
          }
        }
      }
    }

    if (self instanceof HaxeSwitchCaseCaptureVar captureVar) {
      HaxeComponentName componentName = captureVar.getComponentName();
      result.add(componentName);
    }
    if (self instanceof HaxeEnumExtractedValueReference extractedValue) {
      HaxeComponentName componentName = extractedValue.getComponentName();
      result.add(componentName);
    }

    if (self instanceof HaxeCatchStatement) {
      final HaxeParameter catchParameter = PsiTreeUtil.getChildOfType(self, HaxeParameter.class);
      if (catchParameter != null) {
        result.add(catchParameter);
      }
    }
    return result;
  }

  private static void addFunctionLiteralsWithName(Set<PsiElement> result, @Nullable HaxeFunctionLiteral[] childrenOfType) {
    if (childrenOfType == null) return;
    for (HaxeFunctionLiteral lit : childrenOfType) {
      HaxeComponentName componentName = lit.getComponentName();
      if (componentName != null) {
        result.add(componentName);
      }
    }
  }

  private static void addEnumMembers(Set<PsiElement> result, List<PsiElement> declarations) {
    for (PsiElement decl : declarations) {
      if (decl instanceof HaxeEnumDeclaration enumDeclaration) {
        List<HaxeNamedComponent> list = enumDeclaration.getModel()
                .getMembers(null).stream()
                .map(m -> m.getNamedComponentPsi())
                .filter(Objects::nonNull)
                .toList();
        result.addAll(list);
      }
    }
  }

  private static void addCaptureVariableDeclarations(HaxeSwitchCaseExpr expr, Set<PsiElement> result) {
    List<PsiElement> captureVars = new ArrayList<>();
    for (HaxeReferenceExpression refExpr : PsiTreeUtil.findChildrenOfType(expr, HaxeReferenceExpression.class)) {
      if (HaxeReferenceUtil.isCaptureVar(refExpr)) {
        captureVars.add(refExpr);
      }
    }
    addDeclarations(result, captureVars);
  }

  private static @NotNull Collection<PsiElement> getObjectLiteralReferences(HaxeSwitchCaseExpr expr) {
    Collection<HaxeEnumObjectLiteralElement> objectLiterals = PsiTreeUtil.findChildrenOfType(expr, HaxeEnumObjectLiteralElement.class);
    return objectLiterals.stream()
      .map(HaxeEnumObjectLiteralElement::getExpression)
      .filter(HaxeReferenceExpression.class::isInstance)
      .filter(haxeExpression -> Character.isLowerCase(haxeExpression.getText().charAt(0)))
      .map(HaxeReferenceExpression.class::cast)
      .filter(expression -> expression.getChildren().length == 1)
      .map(PsiElement.class::cast)
      .toList();
  }

  private static @NotNull Collection<PsiElement> getArrayLiteralReferences(HaxeSwitchCaseExpr expr) {
    Collection<HaxeEnumExtractArrayLiteral> arrayLiterals = PsiTreeUtil.findChildrenOfType(expr, HaxeEnumExtractArrayLiteral.class);
    return arrayLiterals.stream()
      .flatMap(literal -> literal.getExpressionList().stream())
      .filter(HaxeReferenceExpression.class::isInstance)
      .map(HaxeReferenceExpression.class::cast)
      .filter(expression -> expression.getChildren().length == 1)
      .map(PsiElement.class::cast)
      .toList();
  }

  private static void addLocalVarDeclarations(@NotNull Set<PsiElement> result,
                                               @Nullable HaxeLocalVarDeclarationList[] items) {
    if (items == null) return;
    List<HaxeLocalVarDeclarationList> declarationLists = Arrays.asList(items);
    Collections.reverse(declarationLists);
    declarationLists.forEach(list -> result.addAll(list.getLocalVarDeclarationList()));
  }

  private static void addVarDeclarations(@NotNull Set<PsiElement> result, @Nullable HaxeFieldDeclaration[] items) {
    if (items == null) return;
    result.addAll(Arrays.asList(items));
  }

  private static void addDeclarations(@NotNull Set<PsiElement> result, @Nullable PsiElement[] items) {
    if (items != null) {
      result.addAll(Arrays.asList(items));
    }
  }

  private static void addDeclarations(@NotNull Set<PsiElement> result, @Nullable Collection<PsiElement> items) {
    if (items != null) {
      result.addAll(items);
    }
  }
}

