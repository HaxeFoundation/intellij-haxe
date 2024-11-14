package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.evaluate;

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
  public ResultHolder resolveExtractedValueType(@NotNull HaxeEnumExtractedValueReference extractedValue, @NotNull HaxeGenericResolver parentResolver) {
    HaxeEnumValueModel enumValueModel = getEnumValueModel();
    if (enumValueModel instanceof HaxeEnumValueConstructorModel constructorModel) {
      // check if in literal array (inside an extractor)
      HaxeSwitchExtractorExpressionArrayLiteral arrayLiteral =
        PsiTreeUtil.getParentOfType(extractedValue, HaxeSwitchExtractorExpressionArrayLiteral.class, true, HaxeEnumArgumentExtractor.class);
      if (arrayLiteral != null) {

        int index = findExtractValueParentIndex(arrayLiteral); // find arrays index
        HaxeGenericResolver extractorResolver = getGenericResolver();
        ResultHolder parameterType = constructorModel.getParameterType(index, extractorResolver);
        if (parameterType != null && parameterType.getClassType() != null) {
          HaxeGenericResolver paramResolver = parameterType.getClassType().getGenericResolver();
          @NotNull ResultHolder[] specifics = paramResolver.getSpecifics();
          if (specifics.length != 0) return specifics[0];
        }
      }
      else {
        int index = findExtractValueIndex(extractedValue);
        HaxeGenericResolver extractorResolver = getGenericResolver();
        ResultHolder parameterType = constructorModel.getParameterType(index, extractorResolver);
        if (parameterType != null) {

          HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(extractedValue, HaxeSwitchStatement.class);
          if (switchStatement == null) return createUnknown(extractedValue);

          HaxeExpression switchStatementExpression = switchStatement.getExpression();

          // remove common Parenthesis wrapping
          while (switchStatementExpression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
            switchStatementExpression = parenthesizedExpression.getExpression();
          }
          PsiElement lookupElement = switchStatementExpression;
          // if  literal, we must  find the related expression and get resolver from that expression
          if (switchStatementExpression instanceof HaxeArrayLiteral || switchStatementExpression instanceof HaxeObjectLiteral) {
            List<Object> list = createSwitchExpressionPath(extractor);
            lookupElement = searchSwitchExpressionPath(switchStatementExpression, list);
          }
          // if nested extraction (ex "case MyEnumVal(MYOtherEnumVal(ref)):" ref) we need to get the correct enum generics
          List<HaxeEnumArgumentExtractor> parentExtractors =
            PsiTreeUtil.collectParents(extractedValue, HaxeEnumArgumentExtractor.class, false, (e) -> e instanceof HaxeSwitchCase);

          if (parentExtractors.size() > 1) {
            ResultHolder switchExpressionType = evaluate(switchStatementExpression, parentResolver).result;
            HaxeGenericResolver pathResolver =  switchExpressionType.getClassType().getGenericResolver();
            SpecificHaxeClassReference classType = switchExpressionType.getClassType();
            if (classType != null) {
              //unwrap null so we can resolve members correctly
              if(classType.isNullType() && classType.unwrapNullType() instanceof SpecificHaxeClassReference classReference) {
                classType = classReference;
              }
              // LinkedHashMap because order matters
              LinkedHashMap<String, String> memberPath = createSwitchExpressionMemberPath(extractedValue);
              for (Map.Entry<String, String> path : memberPath.entrySet()) {
                String enumValueName = path.getKey();
                String parameterName = path.getValue();
                if (classType != null && classType.getHaxeClassModel() instanceof HaxeEnumModel model) {
                HaxeEnumValueModel value = model.getValue(enumValueName);
                if (value instanceof  HaxeEnumValueConstructorModel valueConstructorModel) {
                  Optional<HaxeParameterModel> first =
                    valueConstructorModel.getParameters().stream().filter(p -> p.getName().equals(parameterName)).findFirst();
                  if (first.isPresent()) {
                    pathResolver = classType.getGenericResolver();
                    HaxeParameterModel parameterModel = first.get();
                    ResultHolder resolved = pathResolver.withoutUnknowns().resolve(parameterModel.getType());
                    if(resolved == null) return createUnknown(extractedValue);
                    classType = resolved.getClassType();
                  }
                }
                }
              }
            }
            ResultHolder resolve = pathResolver.resolve(parameterType);
            if(resolve != null && !resolve.isUnknown()) return resolve;
            return parameterType;
          }

          ResultHolder result = evaluate(lookupElement, parentResolver).result;
          SpecificHaxeClassReference classType = result.getClassType();
          if (!result.isUnknown() && classType != null) {
            // TODO null safety, resolveTypeDefClass can be null if model is unknown
            if(classType.isTypeDefOfClass()) classType = classType.resolveTypeDefClass();
            ResultHolder resolve = classType.getGenericResolver().withoutUnknowns().resolve(parameterType);
            if(resolve != null && !resolve.isUnknown()) {
              return resolve;
            }else {
              return parameterType;
            }
          }
          else {
            return parameterType;
          }
        }
      }
    }
    return createUnknown(extractedValue);
  }

  private static ResultHolder createUnknown(@NotNull HaxeEnumExtractedValueReference extractedValue) {
    return SpecificHaxeClassReference.getUnknown(extractedValue).createHolder();
  }

  // linked hashmap because order matters
  private LinkedHashMap<String, String> createSwitchExpressionMemberPath(HaxeEnumExtractedValueReference value) {


    List<String> parameterNames = new ArrayList<>();
    List<String> EnumValueNames = new ArrayList<>();
    PsiElement ref = value;
    HaxeEnumArgumentExtractor parent = PsiTreeUtil.getParentOfType(ref, HaxeEnumArgumentExtractor.class);
    while (parent != null && parent.getModel() instanceof  HaxeEnumExtractorModel model){
    int index = model.findArgumentIndex(ref, true);
      HaxeEnumValueModel enumValueModel = model.getEnumValueModel();
      if (enumValueModel != null) {
        HaxeEnumValueDeclaration valueDeclaration = enumValueModel.getEnumValuePsi();
        if (valueDeclaration != null) {
          HaxeModel valueModel = valueDeclaration.getModel();
          if (valueModel instanceof HaxeEnumValueConstructorModel constructorModel) {
            List<HaxeParameterModel> parameters = constructorModel.getParameters();
            if (index > -1 && index < parameters.size()) {
              HaxeParameterModel parameterModel = parameters.get(index);
              parameterNames.add(parameterModel.getName());
              EnumValueNames.add(valueModel.getName());
            }
          }
        }
      }
      ref = parent;
      parent =  PsiTreeUtil.getParentOfType(parent, HaxeEnumArgumentExtractor.class, true, HaxeSwitchCase.class);
    }
    Collections.reverse(parameterNames);
    Collections.reverse(EnumValueNames);
    LinkedHashMap<String,String> enumAndParamMap = new LinkedHashMap<>();
    for (int i = 0, size = EnumValueNames.size(); i < size; i++) {
      enumAndParamMap.put(EnumValueNames.get(i), parameterNames.get(i));
    }

    return enumAndParamMap;
  }

  private static @Nullable PsiElement searchSwitchExpressionPath(HaxeExpression switchStatementExpression, List<Object> list) {
    PsiElement lookupElement = switchStatementExpression;
    for (Object o : list) {
      if (o instanceof  Integer ix && lookupElement instanceof HaxeArrayLiteral literal) {
        HaxeExpressionList arrayList = literal.getExpressionList();
        if (arrayList == null) {
          lookupElement = null;
          break;
        }else {
          lookupElement = arrayList.getExpressionList().get(ix);
        }
      }else if (o instanceof  String name && lookupElement instanceof HaxeObjectLiteral literal) {
        Optional<HaxeExpression> first = literal.getObjectLiteralElementList().stream()
          .filter(ol -> ol.getComponentName() != null)
          .filter(ol -> ol.getExpression() != null)
          .filter(ol -> ol.getComponentName().getIdentifier().textMatches(name))
          .map(HaxeObjectLiteralElement::getExpression)
          .findFirst();
        if (first.isPresent()) {
          lookupElement = first.get();
        }else {
          lookupElement = null;
          break;
        }
      }else {
        lookupElement = null;
        break;
      }
    }
    return lookupElement;
  }

  private static @NotNull List<Object> createSwitchExpressionPath(HaxeEnumArgumentExtractor extractor) {
    List<Object> list = new ArrayList<>();
    PsiElement child = extractor;
    PsiElement parent = extractor.getParent();
    while (parent != null && !(parent instanceof HaxeSwitchCaseExpr)) {
      if (parent instanceof  HaxeSwitchCaseExprArray  exprArray) {
        // array look up
        int arrayIndex = exprArray.getExpressionList().indexOf(child);
        list.add(arrayIndex);
      } else if (parent instanceof  HaxeEnumExtractArrayLiteral arrayLiteral){
        // array look up
        int arrayIndex = arrayLiteral.getExpressionList().indexOf(child);
        list.add(arrayIndex);
      }else if (parent instanceof  HaxeEnumObjectLiteralElement objectLiteral){
        // object lookup
        list.add(objectLiteral.getComponentName().getIdentifier().getText());
        parent = parent.getParent();// need extra parent to get out of objectLiteral
      }

      child = parent;
      parent = parent.getParent();
    }
    Collections.reverse(list);
    return list;
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
