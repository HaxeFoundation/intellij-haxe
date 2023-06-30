package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.ide.annotator.semantics.TypeParameterUtil.*;
import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.canAssignToFrom;
import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.getUnderlyingClassIfAbstractNull;
import static com.intellij.plugins.haxe.model.type.HaxeTypeResolver.getTypeFromFunctionArgument;

public class HaxeCallExpressionAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeCallExpression expression) {
      check(expression, holder);
    }
  }

  public static void check(final HaxeCallExpression expr, final AnnotationHolder holder) {

    if (expr.getExpression() instanceof HaxeReference reference) {
      final PsiElement target = reference.resolve();

      //TODO unify logic for parameter checking for Function and Methods

      if (target instanceof HaxePsiField field) {

        HaxeTypeTag tag = field.getTypeTag();

        if (tag != null && tag.getFunctionType() != null) {
          List<HaxeFunctionArgument> argumentList = tag.getFunctionType().getFunctionArgumentList();

          List<HaxeExpression> expressionArgList = new LinkedList<>();
          HaxeExpressionList referenceParameterList = expr.getExpressionList();
          if (referenceParameterList != null) {
            expressionArgList.addAll(referenceParameterList.getExpressionList());
          }

          long minArgs = findMinArgsCounts(argumentList);
          long maxArgs = argumentList.size();

          int expressionArgListSize = expressionArgList.size();
          if (expressionArgListSize < minArgs) {
            TextRange range;
            if (expressionArgList.isEmpty()) {
              PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(expr.getExpression());
              PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
              range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
            }
            else {
              range = referenceParameterList.getTextRange();
            }
            String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgs, expressionArgListSize);
            holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create();
            return;
          }

          if (expressionArgListSize > maxArgs) {
            String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgs, expressionArgListSize);
            holder.newAnnotation(HighlightSeverity.ERROR, message).range(referenceParameterList.getTextRange()).create();
            return;
          }


          //TODO handle required after optionals
          for (int i = 0; i < expressionArgListSize; i++) {
            HaxeExpression expression = expressionArgList.get(i);
            ResultHolder expressionType = findExpressionType(expression);

            HaxeFunctionArgument functionArgument = argumentList.get(i);
            ResultHolder parameterType = getTypeFromFunctionArgument(functionArgument);


            // if expression is enumValue we need to resolve the underlying enumType type to test assignment
            if (expressionType.getType() instanceof SpecificEnumValueReference) {
              SpecificHaxeClassReference enumType = ((SpecificEnumValueReference)expressionType.getType()).getEnumClass();
              expressionType = enumType.createHolder();
            }

            if (!canAssignToFrom(parameterType, expressionType)) {
              annotateTypeMismatch(holder, parameterType, expressionType, expression);
            }
          }
        }
      }

      if (target instanceof HaxeMethod method) {
        boolean isStaticExtension = expr.resolveIsStaticExtension();

        HaxeGenericSpecialization specialization = expr.getSpecialization();
        List<HaxeParameterModel> parameters = method.getModel().getParameters();

        long minArgCount = countRequiredArguments(parameters);
        long maxArgCount = hasVarArgs(parameters) ? Long.MAX_VALUE : parameters.size();

        List<HaxeExpression> expressionArgList = new LinkedList<>();
        HaxeExpressionList referenceParameterList = expr.getExpressionList();
        if (referenceParameterList != null) {
          expressionArgList.addAll(referenceParameterList.getExpressionList());
        }

        if (expressionArgList.size() < minArgCount - (isStaticExtension ? 1 : 0)) {
          TextRange range;
          if ((expressionArgList.size() - (isStaticExtension ? 1 : 0)) == 0) {
            PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(expr.getExpression());
            PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
            range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
          }
          else {
            range = referenceParameterList.getTextRange();
          }
          String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgCount, expressionArgList.size());
          holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create();
          return;
        }

        if (expressionArgList.size() > maxArgCount - (isStaticExtension ? 1 : 0)) {
          String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgCount, expressionArgList.size());
          holder.newAnnotation(HighlightSeverity.ERROR, message).range(referenceParameterList.getTextRange()).create();
          return;
        }

        HaxeGenericResolver resolver = findGenericResolverFromVariable(expr);
        if (resolver == null && specialization != null) {
          resolver = specialization.toGenericResolver(expr);
        }

        Map<String, ResultHolder> typeParamMap = createTypeParameterConstraintMap(method, resolver);

        if (method.getModel().isOverload()) {
          //TODO implement support for overloaded methods
          return; //(stopping here to avoid marking arguments as type mismatch)
        }

        //TODO handle required after optionals
        for (int i = 0; i < expressionArgList.size(); i++) {
          HaxeExpression expression = expressionArgList.get(i);


          //TODO add type check for haxe.extern.Rest arguments

          int parameterIndex = i + (isStaticExtension ? 1 : 0);// skip one if static extension (first arg is caller
          // var args accept all types so no need to do any more validation
          if (parameterIndex >= parameters.size() || isVarArg(parameters.get(parameterIndex))) return;

          HaxeParameterModel parameterModel = parameters.get(parameterIndex);

          // add constraints from method
          resolver.addAll(method.getModel().getGenericResolver(null).withoutUnknowns());

          ResultHolder parameterType = parameterModel.getType(resolver.withoutUnknowns());

          ResultHolder expressionType = null;

          if (expression instanceof HaxeReferenceExpression) {
            HaxeClassResolveResult resolveHaxeClass = ((HaxeReferenceExpression)expression).resolveHaxeClass();
            if (resolveHaxeClass.getHaxeClass() instanceof HaxeSpecificFunction) {
              SpecificHaxeClassReference parameterClassType = parameterType.getClassType();
              if (parameterClassType != null) {
                if (parameterClassType.isFunction()) {
                  // parameter type `Function` accepts all functions
                  continue;
                }
                else {
                  PsiElement resolvedExpression = ((HaxeReferenceExpression)expression).resolve();
                  if (resolvedExpression instanceof HaxeMethodDeclaration resolve) {
                    SpecificFunctionReference type = resolve.getModel().getFunctionType(null);
                    expressionType = type.createHolder();

                    // make sure that if  parameter type is typedef  try to convert to function so we can compare with argument
                    HaxeClass aClass = parameterClassType.getHaxeClass();
                    if (aClass != null && aClass.getModel().isTypedef()) {
                      SpecificFunctionReference functionReference = parameterClassType.resolveTypeDefFunction();
                      if (functionReference != null) {
                        parameterType = functionReference.createHolder();
                      }
                    }
                  }
                }
              }
            }
          }

          expressionType = expressionType != null ? expressionType : findExpressionType(expression);

          // if expression is enumValue we need to resolve the underlying enumType type to test assignment
          if (expressionType.getType() instanceof SpecificEnumValueReference type) {
            SpecificHaxeClassReference enumType = type.getEnumClass();
            expressionType = enumType.createHolder();
          }
          if (parameterType.getType() instanceof SpecificHaxeClassReference classReference  ) {
            boolean containsTypeParameter = containsTypeParameter(parameterType, typeParamMap);
            if (containsTypeParameter) {
            String className = classReference.getClassName();
            //if (typeParamNames.contains(className)) {


              //ResultHolder constraint =  typeParamMap.get(className);
              Optional<ResultHolder> optionalConstraint = findConstraintForTypeParameter(parameterType, typeParamMap);
              if (optionalConstraint.isPresent()) {
                ResultHolder constraint = optionalConstraint.get();
                if (!canAssignToFrom(constraint, expressionType)) {
                  annotateTypeMismatch(holder, constraint, expressionType, expression);
                }
              }else {
                // checks if we have used a unconstrained generic type, and then apply that type to any other arguments with that value
                ResultHolder resolved = resolver.resolve(className);
                if(resolved == null) {
                    resolver.add(className, expressionType);
                    typeParamMap.put(className, expressionType);
                }else {
                  if (!canAssignToFrom(resolved, expressionType)) {
                    annotateTypeMismatch(holder, resolved, expressionType, expression);
                  }
                }
              }
            }else {
              if (parameterType.isClassType() && !containsTypeParameter(parameterType, typeParamMap)) {
                if (!canAssignToFrom(parameterType, expressionType)) {
                  annotateTypeMismatch(holder, parameterType, expressionType, expression);
                }
              }
            }
          }else {
            if (!canAssignToFrom(parameterType, expressionType)) {
              annotateTypeMismatch(holder, parameterType, expressionType, expression);
            }
          }
        }
      }
    }
  }




  private static void annotateTypeMismatch(AnnotationHolder holder, ResultHolder expected, ResultHolder got, HaxeExpression expression) {
    String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch",
                                        expected.toPresentationString(),
                                        got.toPresentationString());

    holder.newAnnotation(HighlightSeverity.ERROR, message).range(expression.getTextRange()).create();
  }

  private static int findMinArgsCounts(List<HaxeFunctionArgument> argumentList) {
    int count = 0;
    for (HaxeFunctionArgument argument : argumentList) {
      if (argument.getOptionalMark() == null) {
        if (!isVoidArgument(argument)) count++;
      }
    }
    return count;
  }

  private static boolean isVoidArgument(HaxeFunctionArgument argument) {
    HaxeTypeOrAnonymous toa = argument.getTypeOrAnonymous();
    HaxeType t = null != toa ? toa.getType() : null;
    String name = t != null ? t.getText() : null;
    return SpecificHaxeClassReference.VOID.equals(name);
  }


  private static HaxeGenericResolver findGenericResolverFromVariable(HaxeCallExpression expr) {
    HaxeReference[] type = UsefulPsiTreeUtil.getChildrenOfType(expr.getExpression(), HaxeReference.class, null);

    if (type != null && type.length > 0) {
      HaxeReference expression = type[0];
      HaxeClassResolveResult resolveResult = expression.resolveHaxeClass();
      SpecificHaxeClassReference reference = resolveResult.getSpecificClassReference(expression.getElement(), null);
      SpecificHaxeClassReference finalReference = getUnderlyingClassIfAbstractNull(reference);
      return finalReference.getGenericResolver();
    }
    return null;
  }


  @NotNull
  private static ResultHolder findExpressionType(HaxeExpression expression) {
    return HaxeExpressionEvaluator
      .evaluate(expression, new HaxeExpressionEvaluatorContext(expression, null),
                HaxeGenericResolverUtil.generateResolverFromScopeParents(expression)).result;
  }

  private static long countRequiredArguments(List<HaxeParameterModel> parametersList) {
    return parametersList.stream()
      .filter(p -> !p.isOptional() && !p.hasInit() && !isVarArg(p))
      .count();
  }

  private static boolean hasVarArgs(List<HaxeParameterModel> parametersList) {
    return parametersList.stream().anyMatch(HaxeCallExpressionAnnotator::isVarArg);
  }

  private static boolean isVarArg(HaxeParameterModel model) {
    if(model.isRest()) {
      return true;
    }
    //Legacy solutions for rest arguments
    // Array<haxe.macro.Expr>
    // haxe.extern.Rest<Float>
    // TODO : this is a bit of a hack to avoid having to resolve Array<Expr> and Rest<> Class, should probably resolve and compare these properly
    if (model.getType().getType() instanceof SpecificHaxeClassReference classType) {
      if (classType.getHaxeClass() != null) {
        ResultHolder[] specifics = classType.getSpecifics();
        if (specifics.length == 1) {
          SpecificTypeReference type = specifics[0].getType();
          if (type instanceof SpecificHaxeClassReference specificType) {
            if (specificType.getHaxeClass() != null) {
              // Array<haxe.macro.Expr>
              if (classType.isArray() && isMacroExpr(specificType)) {
                return true;
              }
              // haxe.extern.Rest<>
              return isExternRestClass(classType);
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean isMacroExpr( SpecificHaxeClassReference classReference) {
    return classReference.getHaxeClass().getQualifiedName().equals("haxe.macro.Expr");
  }

  private static boolean isExternRestClass(SpecificHaxeClassReference classReference) {
    return classReference.getHaxeClass().getQualifiedName().equals("haxe.extern.Rest");
  }
}
