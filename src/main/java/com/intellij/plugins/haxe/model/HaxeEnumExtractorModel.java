package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.evaluate;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorHandlers.searchForIteratorType;

public class HaxeEnumExtractorModel implements HaxeModel {

  private final HaxeEnumArgumentExtractor extractor;

  public HaxeEnumExtractorModel(@NotNull HaxeEnumArgumentExtractor extractor) {
    this.extractor = extractor;
  }

  @Nullable
  public HaxeEnumValueModel getEnumValueModel() {
    PsiElement resolve = resolveEnumValueCached();
    if (resolve instanceof HaxeEnumValueDeclaration declaration) {
      if (declaration.getModel() instanceof HaxeEnumValueModel model) return  model;
    }
    return null;
  }

  @Nullable
  private PsiElement resolveEnumValueCached() {
    return CachedValuesManager.getProjectPsiDependentCache(extractor, HaxeEnumExtractorModel::computeResolveEnumValue);
  }


  public int findExtractValueIndex(PsiElement value) {
      PsiElement[] extractorArguments = getChildrenCached();
      for (int i = 0; i < extractorArguments.length; i++) {
        PsiElement argument = extractorArguments[i];
        if (argument instanceof  HaxeEnumExtractedValue extractedValue) {
          if (extractedValue.getEnumExtractedValueReference() == value) {
            return i;
          }
        }
      }
    return -1;
  }
  public int findExtractValueParentIndex(PsiElement value) {
      PsiElement[] extractorArguments = getChildrenCached();
      for (int i = 0; i < extractorArguments.length; i++) {
        PsiElement argument = extractorArguments[i];
        if(value == argument) {
          return i;
        }
      }
    return -1;
  }
  public int findArgumentIndex(PsiElement value, boolean deepSearch) {
      PsiElement[] extractorArguments = getChildrenCached();
      for (int i = 0; i < extractorArguments.length; i++) {
        PsiElement argument = extractorArguments[i];
        PsiElement ref = value;
        while (ref != null) {
          if (ref == argument) return i;
          if (!deepSearch)break;
          ref = ref.getParent();
        }
      }
    return -1;
  }


  public HaxeGenericResolver getGenericResolver() {
    HaxeEnumValueModel model = getEnumValueModel();
    if (model == null) return  new HaxeGenericResolver();
    HaxeClassModel anEnum = model.getDeclaringEnum();
    HaxeGenericResolver resolver = anEnum != null ? anEnum.getGenericResolver(null) : new HaxeGenericResolver();
    HaxeEnumArgumentExtractor parentExtractor = PsiTreeUtil.getParentOfType(extractor, HaxeEnumArgumentExtractor.class);
    if (parentExtractor != null) {
      HaxeEnumExtractorModel extractorModel = (HaxeEnumExtractorModel)parentExtractor.getModel();
      HaxeGenericResolver parentResolver = extractorModel.getGenericResolver();

      int index = extractorModel.findExtractValueIndex(extractor);
      HaxeEnumValueModel valueModel = extractorModel.getEnumValueModel();
      if(valueModel  instanceof  HaxeEnumValueConstructorModel constructorModel) {
        ResultHolder parameterType = constructorModel.getParameterType(index, parentResolver);
        if (parameterType != null && parameterType.isClassType()) {
          return parameterType.getClassType().getGenericResolver();
        }
      }
    }else {
      SpecificHaxeClassReference reference = HaxeResolveUtil.resolveExtractorEnum(extractor);
      if (reference != null) {
        return reference.getGenericResolver();
      }
    }
    return resolver != null ?  resolver : new HaxeGenericResolver();
  }

  private @NotNull PsiElement[] getChildrenCached() {
    return CachedValuesManager.getProjectPsiDependentCache(extractor, HaxeEnumExtractorModel::computeChildren);
  }

  private static PsiElement[] computeChildren(HaxeEnumArgumentExtractor extractor) {
    return extractor.getEnumExtractorArgumentList().getChildren();
  }

  private static PsiElement computeResolveEnumValue(HaxeEnumArgumentExtractor extractor) {
    return extractor.getEnumValueReference().getReferenceExpression().resolve();
  }

  @NotNull
  public ResultHolder resolveExtractedValueType(@NotNull HaxeEnumExtractedValueReference extractedValue) {

    HaxeEnumValueModel enumValueModel = getEnumValueModel();
    if (enumValueModel instanceof HaxeEnumValueConstructorModel) {
      HaxeExpression switchStatement = findSwitchExpressionType(extractedValue);
      if (switchStatement == null) return createUnknown(extractedValue);
      ResultHolder switchType = evaluate(switchStatement).result;
      if (switchType.getClassType() != null) {
        return getTypeForExtractedValue(extractedValue, switchStatement, switchType);
      }
    }
    // unable to determine type
    return createUnknown(extractedValue);
  }

  private HaxeExpression findSwitchExpressionType(@NotNull HaxeEnumExtractedValueReference extractedValue) {
    HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(extractedValue, HaxeSwitchStatement.class);
    if (switchStatement == null) return null;

    HaxeExpression switchStatementExpression = switchStatement.getExpression();

    // remove common Parenthesis wrapping
    while (switchStatementExpression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
      switchStatementExpression = parenthesizedExpression.getExpression();
    }
    return switchStatementExpression;
  }

  private ResultHolder getTypeForExtractedValue(@NotNull HaxeEnumExtractedValueReference extractedValue, HaxeExpression switchStatement, ResultHolder switchType) {
    SpecificHaxeClassReference switchTypeClass = switchType.getClassType();
    HaxeGenericResolver switchExpressionResolver = switchTypeClass.getGenericResolver();

    List<ExtractorHierarchyElement> extractorHierarchy = getExtractorHierarchy(extractedValue);
    extractorHierarchy = extractorHierarchy.reversed();

    SpecificTypeReference loopType = switchTypeClass;
    HaxeGenericResolver loopResolver = switchExpressionResolver;
    HaxeExpression loopPsi = switchStatement;



    for (ExtractorHierarchyElement element : extractorHierarchy) {
      switch(element.type) {
        case ENUM_VALUE: {
          HaxeEnumValueConstructorModel model = (HaxeEnumValueConstructorModel)element.model();
          ResultHolder result = model.getParameterType((Integer) element.key, loopResolver);
          if(result != null && !result.isUnknown()) {
            loopType = result.getType();
            if(loopType instanceof  SpecificHaxeClassReference classReference) {
              loopResolver = classReference.getGenericResolver();
            }
          }

        }
        break;
        case OBJECT_LITERAL: {
          if (loopPsi instanceof HaxeObjectLiteral objectLiteral) {
            List<HaxeObjectLiteralElement> objectLiteralElementList = objectLiteral.getObjectLiteralElementList();
            for (HaxeObjectLiteralElement literalElement : objectLiteralElementList) {
              if(literalElement.getComponentName().getIdentifier().textMatches((String) element.key)) {
                loopPsi = literalElement.getExpression();
                ResultHolder result = evaluate(loopPsi).result;
                if(result != null && !result.isUnknown()) {
                  loopType = result.getType();
                  if(loopType instanceof  SpecificHaxeClassReference classReference) {
                    loopResolver = classReference.getGenericResolver();
                  }
                }
                break;
              }
            }

          } else {
            HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(extractedValue);
            ResultHolder result = loopType.access((String) element.key, context, switchExpressionResolver);
            if (result != null && !result.isUnknown()) {
              loopType = result.getType();
              if(loopType instanceof  SpecificHaxeClassReference classReference) {
                loopResolver = classReference.getGenericResolver();
              }
            }
          }
        }
        break;
        case ARRAY_LITERAL: {
          if(loopPsi instanceof HaxeArrayLiteral arrayLiteral) {
            HaxeExpressionList expressionList = arrayLiteral.getExpressionList();
            if(expressionList != null) {
              HaxeExpression haxeExpression = expressionList.getExpressionList().get((Integer) element.key);
              loopPsi = haxeExpression;
              ResultHolder result = evaluate(haxeExpression).result;
              if (result != null && !result.isUnknown()) {
                loopType = result.getType();
                if (loopType instanceof SpecificHaxeClassReference classReference) {
                  loopResolver = classReference.getGenericResolver();
                }
              }
            }
          } else {
            if(loopType instanceof  SpecificHaxeClassReference classReference) {
              ResultHolder iteratorResult = searchForIteratorType(classReference, "iterator", extractedValue);
              if (iteratorResult != null && iteratorResult.getClassType() != null) {
                SpecificHaxeClassReference iterator = iteratorResult.getClassType();
                HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(extractedValue);
                ResultHolder access = iterator.access("next", context, iterator.getGenericResolver());
                if (access != null && access.getFunctionType() != null) {
                  SpecificFunctionReference functionType = access.getFunctionType();
                  ResultHolder result = functionType.getReturnType();
                  if (result != null && !result.isUnknown()) {
                    loopType = result.getType();
                    if (loopType instanceof SpecificHaxeClassReference loopClass) {
                      loopResolver = loopClass.getGenericResolver();
                    }
                  }
                }
              }
            }
          }
        }
        break;
      }
    }

    return loopType.createHolder();
  }

  private enum HierarchyType {
    OBJECT_LITERAL,
    ARRAY_LITERAL,
    ENUM_VALUE;
  }
  private record ExtractorHierarchyElement(HierarchyType type, Object key, HaxeModel model){}

  private static List<ExtractorHierarchyElement> getExtractorHierarchy(@NotNull HaxeEnumExtractedValueReference extractedValueRef) {
    PsiElement element = extractedValueRef;
    PsiElement argumentListElement = null;
    List<ExtractorHierarchyElement> parentsToResolve = new ArrayList<>();

    do {
      PsiElement parent = element.getParent();
      if(parent instanceof HaxeSwitchStatement) break;

      if (parent instanceof HaxeEnumExtractorArgumentList) {
        argumentListElement = element;
      }
      if (parent instanceof HaxeEnumObjectLiteralElement  literal) {
        String id = literal.getComponentName().getIdentifier().getText();
        parentsToResolve.add( new ExtractorHierarchyElement(HierarchyType.OBJECT_LITERAL, id, null));
      }

      if(parent instanceof HaxeSwitchCaseExprArray caseExprArray) {
        @NotNull PsiElement[] children = caseExprArray.getChildren();
        int index = List.of(children).indexOf(element);
        parentsToResolve.add(new ExtractorHierarchyElement(HierarchyType.ARRAY_LITERAL, index, null));
      }

      if(parent instanceof HaxeEnumExtractArrayLiteral arrayLiteral) {
        int index = arrayLiteral.getExpressionList().indexOf(element);
        parentsToResolve.add(new ExtractorHierarchyElement(HierarchyType.ARRAY_LITERAL, index, null));
      }



      if(parent instanceof HaxeEnumArgumentExtractor extractor) {
        if (extractor.getModel() instanceof  HaxeEnumExtractorModel model){
          PsiElement ref = argumentListElement != null ?  argumentListElement : element;
          int argumentIndex = model.findArgumentIndex(ref, true);
          if (argumentIndex != -1) {
            if(model.getEnumValueModel() instanceof HaxeEnumValueConstructorModel constructorModel) {
              PsiParameter parameter = constructorModel.getConstructorParameters().getParameter(argumentIndex);
              parentsToResolve.add( new ExtractorHierarchyElement(HierarchyType.ENUM_VALUE, argumentIndex, constructorModel));
            }
          }
          // remove any extractedValue references as we exit an extractor
          argumentListElement = null;
        }
      }

      element = parent;
    }while(element.getParent() != null);
    return parentsToResolve;
  }


  private static ResultHolder createUnknown(@NotNull HaxeEnumExtractedValueReference extractedValue) {
    return SpecificHaxeClassReference.getUnknown(extractedValue).createHolder();
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public @NotNull PsiElement getBasePsi() {
    return extractor;
  }

  @Override
  public @Nullable HaxeExposableModel getExhibitor() {
    return null;
  }

  @Override
  public @Nullable FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }
}
