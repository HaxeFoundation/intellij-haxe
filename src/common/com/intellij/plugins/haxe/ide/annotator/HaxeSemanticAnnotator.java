/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.generation.OverrideImplementMethodFix;
import com.intellij.plugins.haxe.ide.quickfix.CreateGetterSetterQuickfix;
import com.intellij.plugins.haxe.ide.quickfix.HaxeSwitchMutabilityModifier;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.*;
import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.returnTypeMismatch;
import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.typeMismatch;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.*;
import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.canAssignToFrom;
import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.getUnderlyingClassIfAbstractNull;
import static com.intellij.plugins.haxe.model.type.HaxeTypeResolver.getTypeFromFunctionArgument;
import static java.util.stream.Collectors.toList;

public class HaxeSemanticAnnotator implements Annotator, HighlightRangeExtension {

  @Override
  public boolean isForceHighlightParents(@NotNull PsiFile file) {
    // XXX: Maybe more complex logic will be required if we only want this to be true when
    // doing semantic annotations.
    return (file.getLanguage().isKindOf(HaxeLanguage.INSTANCE));
  }

  public static HaxeSemanticAnnotatorInspections.Registrar getInspectionProvider() {
    return new HaxeSemanticAnnotatorInspections.Registrar();
  }

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element.getLanguage() == HaxeLanguage.INSTANCE) {
      analyzeSingle(element, new HaxeAnnotationHolder(holder));
    }
  }

  private static void analyzeSingle(final PsiElement element, HaxeAnnotationHolder holder) {
    // skip a lot of if else on tokens that are not haxe related (whitespace etc)
    if (!(element instanceof HaxePsiCompositeElement))
      return;

    if (element instanceof HaxePackageStatement) {
      PackageChecker.check((HaxePackageStatement)element, holder);
    } else if (element instanceof HaxeMethod) {
      MethodChecker.check((HaxeMethod)element, holder);
    } else if (element instanceof HaxeClass) {
      ClassChecker.check((HaxeClass)element, holder);
    } else if (element instanceof HaxeType) {
      TypeChecker.check((HaxeType)element, holder);
    } else if (element instanceof HaxeFieldDeclaration) {
      FieldChecker.check((HaxeFieldDeclaration)element, holder);
    } else if (element instanceof HaxeLocalVarDeclaration) {
      LocalVarChecker.check((HaxeLocalVarDeclaration)element, holder);
    } else if (element instanceof HaxeStringLiteralExpression) {
      StringChecker.check((HaxeStringLiteralExpression)element, holder);
    } else if (element instanceof HaxeTypeCheckExpr) {
      TypeCheckExpressionChecker.check((HaxeTypeCheckExpr)element, holder);
    } else if (element instanceof HaxeAssignExpression) {
      AssignExpressionChecker.check((HaxeAssignExpression)element, holder);
    } else if (element instanceof HaxeIsTypeExpression) {
      IsTypeExpressionChecker.check((HaxeIsTypeExpression)element, holder);
    } else if (element instanceof HaxeCallExpression) {
      CallExpressionChecker.check((HaxeCallExpression)element, holder);
    }
  }
}

class CallExpressionChecker {
  public static void check(
    final HaxeCallExpression expr,
    final HaxeAnnotationHolder holder
  ) {

    final HaxeReference reference = (HaxeReference)expr.getExpression();
    final PsiElement target = reference.resolve();

    //TODO unify logic for parameter checking for Function and Methods

    if (target instanceof HaxePsiField) {

      HaxeTypeTag tag = ((HaxePsiField)target).getTypeTag();

      if(tag.getFunctionType() != null) {
        List<HaxeFunctionArgument> argumentList = tag.getFunctionType().getFunctionArgumentList();

        List<HaxeExpression> expressionArgList = new LinkedList<>();
        HaxeExpressionList referenceParameterList = expr.getExpressionList();
        if (referenceParameterList != null) {
          expressionArgList.addAll(referenceParameterList.getExpressionList());
        }

        long minArgs = argumentList.stream().filter(argument -> argument.getOptionalMark() == null).count();
        long maxArgs = argumentList.size();

        if (expressionArgList.size() < minArgs) {
          TextRange range;
          if(expressionArgList.size()  == 0 ) {
            PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(expr.getExpression());
            PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
            range = TextRange.create(first.getTextOffset(), second.getTextOffset()+1);

          }else {
            range = referenceParameterList.getTextRange();
          }
          String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgs, expressionArgList.size());
          holder.createErrorAnnotation(range, message);
          return;
        }

        if (expressionArgList.size() > maxArgs) {
          String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgs, expressionArgList.size());
          holder.createErrorAnnotation(referenceParameterList.getTextRange(), message);
          return;
        }


        //TODO handle required after optionals
        for (int i = 0; i < expressionArgList.size(); i++) {
          HaxeExpression expression = expressionArgList.get(i);
          ResultHolder expressionType = findExpressionType(expression);

          HaxeFunctionArgument functionArgument = argumentList.get(i);
          ResultHolder parameterType = getTypeFromFunctionArgument(functionArgument);


          // if expression is enumValue we need to resolve the underlying enumType type to test assignment
          if(expressionType.getType() instanceof SpecificEnumValueReference) {
            SpecificHaxeClassReference enumType = ((SpecificEnumValueReference)expressionType.getType()).getEnumClass();
            expressionType = enumType.createHolder();
          }

          if (!canAssignToFrom(parameterType, expressionType)) {
            String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch",
                                                parameterType.toPresentationString(),
                                                expressionType.toPresentationString());
            holder.createErrorAnnotation(expression.getTextRange(), message);
          }
        }

      }

    }

    if (target instanceof HaxeMethod) {
      HaxeMethod method = (HaxeMethod)target;
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

      if (expressionArgList.size() < minArgCount - (isStaticExtension ? 1 : 0) ) {
        TextRange range;
        if((expressionArgList.size()  - (isStaticExtension ? 1 : 0)) == 0 ) {
          PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(expr.getExpression());
          PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
          range = TextRange.create(first.getTextOffset(), second.getTextOffset()+1);

        }else {
          range = referenceParameterList.getTextRange();
        }
        String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgCount, expressionArgList.size());
        holder.createErrorAnnotation(range, message);
        return;
      }

      if (expressionArgList.size() > maxArgCount - (isStaticExtension ? 1 : 0)) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgCount, expressionArgList.size());
        holder.createErrorAnnotation(referenceParameterList.getTextRange(), message);
        return;
      }

      HaxeGenericResolver resolver = findGenericResolverFromVariable(expr);
      if(resolver == null && specialization != null) {
        resolver = specialization.toGenericResolver(expr);
      }

      //TODO handle required after optionals
      for (int i = 0; i < expressionArgList.size(); i++) {
        HaxeExpression expression = expressionArgList.get(i);


        //TODO add type check for haxe.extern.Rest arguments

        int parameterIndex = i + (isStaticExtension ? 1 : 0);// skip one if static extension (first arg is caller
        // var args accept all types so no need to do any more validation
        if (parameterIndex >= parameters.size()  || isVarArg(parameters.get(parameterIndex))) return;

        //TODO fix method generics (fun<T>(value:T):T)
        if (method.getModel().getGenericParams().size()> 0) return;

        HaxeParameterModel parameterModel = parameters.get(parameterIndex);
        ResultHolder parameterType = parameterModel.getType(resolver);

        ResultHolder expressionType = null;

        if(expression instanceof  HaxeReferenceExpression) {
            HaxeClassResolveResult resolveHaxeClass = ((HaxeReferenceExpression)expression).resolveHaxeClass();
            if (resolveHaxeClass.getHaxeClass() instanceof HaxeSpecificFunction) {
              SpecificHaxeClassReference parameterClassType = parameterType.getClassType();
              if(parameterClassType != null) {
                if (parameterClassType.isFunction()) {
                  // parameter type `Function` accepts all functions
                  continue;
                } else {
                  PsiElement resolvedExpression = ((HaxeReferenceExpression)expression).resolve();
                  if (resolvedExpression instanceof HaxeMethodDeclaration) {
                    HaxeMethodDeclaration resolve = (HaxeMethodDeclaration)resolvedExpression;
                    SpecificFunctionReference type = resolve.getModel().getFunctionType(null);
                    expressionType = type.createHolder();

                    // make sure that if  parameter type is typedef  try to convert to function so we can compare with argument
                    if (parameterClassType.getHaxeClass().getModel().isTypedef()) {
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

        expressionType = expressionType != null ? expressionType :  findExpressionType(expression);

        // if expression is enumValue we need to resolve the underlying enumType type to test assignment
        if(expressionType.getType() instanceof SpecificEnumValueReference) {
          SpecificHaxeClassReference enumType = ((SpecificEnumValueReference)expressionType.getType()).getEnumClass();
          expressionType = enumType.createHolder();
        }

        if (!canAssignToFrom(parameterType, expressionType)) {
          String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch",
                                              parameterType.toPresentationString(),
                                              expressionType.toPresentationString());
          holder.createErrorAnnotation(expression.getTextRange(), message);
        }
      }
    }
  }


  private static HaxeGenericResolver findGenericResolverFromVariable(HaxeCallExpression expr) {
    HaxeReference[] type = UsefulPsiTreeUtil.getChildrenOfType(expr.getExpression(), HaxeReference.class, null);

    if(type!= null && type.length> 0) {
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
    return parametersList.stream().anyMatch(CallExpressionChecker::isVarArg);
  }

  private static boolean isVarArg(HaxeParameterModel model) {
    // TODO : this is a bit of a hack to avoid having to resolve Array Expr and Rest, should probably resolve and compare these properly
    // haxe.extern.Rest<Float>
    // Array<haxe.macro.Expr>

    if (model.getType().getType() instanceof  SpecificHaxeClassReference) {
      SpecificHaxeClassReference classType = (SpecificHaxeClassReference)model.getType().getType();
      if(classType.getHaxeClass() != null) {
        ResultHolder[] specifics = classType.getSpecifics();
        if (specifics.length == 1) {
          SpecificHaxeClassReference specificType = (SpecificHaxeClassReference)specifics[0].getType();
          if (specificType.getHaxeClass() != null) {
            // Array<haxe.macro.Expr>
            if (classType.isArray() && specificType.isExpr() ) {
              return true;
            }
            // haxe.extern.Rest<>
            if (classType.isRest()) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }
}

class IsTypeExpressionChecker {
  public static void check(
    final HaxeIsTypeExpression expr,
    final HaxeAnnotationHolder holder
  ) {
    if (!IS_TYPE_INSPECTION.isEnabled(expr)) return;

    PsiElement rhsType = getRightHandType(expr);
    if (null != rhsType) {
      annotateTypeError(rhsType, holder);
    } else {
      PsiElement rhs = getRightHandElement(expr);
      holder.createErrorAnnotation(null != rhs ? rhs : expr, HaxeBundle.message("haxe.semantic.is.operator.rhs.must.be.type"));
    }

    if (IS_TYPE_INSPECTION_4dot1_COMPATIBLE.isEnabled(expr)) {

      PsiElement lhs = expr.getLeftExpression();
      if (isComplexExpression(lhs)) {
        Annotation annotation =
          holder.createErrorAnnotation(lhs, HaxeBundle.message("haxe.semantic.is.operator.lhs.cannot.be.complex.expression"));
        annotation.registerFix(wrapLhsFixer(lhs));
        annotation.registerFix(wrapInnerIsFixer(expr));
      }

      PsiElement parent = expr.getParent();
      if (parent instanceof HaxeAssignExpression) {
        // "a = b is expression" parses (in our parser) as "a = (b is expression)", so the parent is actually the assignment.
        TextRange assignMarkerRange = new TextRange(parent.getTextOffset(), lhs.getTextRange().getEndOffset());

        Annotation annotation =
          holder.createErrorAnnotation(assignMarkerRange, HaxeBundle.message("haxe.semantic.is.operator.4_1.lhs.cannot.be.assignment"));
        annotation
          .registerFix(HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.assignment.with.parenthesis"), expr, assignMarkerRange));
        annotation.registerFix(wrapExpressionFixer(expr));
      } else if (parent instanceof HaxeVarInit) {
        TextRange initMarkerRange = new TextRange(parent.getTextOffset(), lhs.getTextRange().getEndOffset());
        Annotation annotation =
          holder.createErrorAnnotation(initMarkerRange, HaxeBundle.message("haxe.semantic.is.operator.4_1.lhs.cannot.be.var.init"));
        annotation.registerFix(wrapInnerIsFixer(expr));

        HaxeExpression initExpression = ((HaxeVarInit)parent).getExpression();
        if (null != initExpression) {
          annotation.registerFix(wrapExpressionFixer(initExpression));
        }
      } else if ( parent instanceof HaxeBinaryExpression
                  || parent instanceof HaxeSwitchStatement
                  || parent instanceof HaxeExpressionList
                  || parent instanceof HaxeMapInitializerExpressionList
                  || parent instanceof HaxeTernaryExpression
                  || parent instanceof HaxeReturnStatement
                  || parent instanceof HaxeDoWhileBody
                  || parent instanceof HaxeTryStatement
                  || parent instanceof HaxeFunctionLiteral
                  || parent instanceof HaxeForStatement
                  || parent instanceof HaxeMapInitializerExpression
                  || parent instanceof HaxeBlockStatement
                  || parent instanceof HaxeThrowStatement
      ) {
        annotateIs(holder, expr, HaxeBundle.message("haxe.semantic.unparenthesized.is.expression.cannot.be.used.here.pre.4.2.semantics"));
      } else if (parent instanceof HaxeGuard
               ||parent instanceof HaxeWhileStatement) {
        annotateIs(holder, expr,
                   HaxeBundle.message(
                     "haxe.semantic.is.expression.requires.double.parenthesis.when.used.as.a.guard.expression.pre.4.2.semantics"));
      }
    }

    // Recolor the keyword, because error markings revert the color.
    recolorIsKeyword(holder, expr);
  }

  private static void recolorIsKeyword(AnnotationHolder holder, HaxeIsTypeExpression element) {
    HaxeIsOperator op = element.getOperator();
    HaxeColorAnnotator.colorizeKeyword(holder, op);
  }

  @NotNull
  private static Annotation annotateIs(HaxeAnnotationHolder holder, HaxeIsTypeExpression expr, String message) {
    if (null == message) {
      message = HaxeBundle.message("haxe.semantic.unparenthesized.is.expression.cannot.be.used.here");
    }
    Annotation annotation = holder.createErrorAnnotation(expr, message);
    annotation.registerFix(wrapInnerIsFixer(expr));
    return annotation;
  }

  @NotNull
  private static HaxeSurroundFixer wrapInnerIsFixer(@NotNull HaxeIsTypeExpression expr) {
    PsiElement lhs = expr.getLeftExpression();
    if (lhs instanceof HaxeBinaryExpression) {
      PsiElement lhsrhs = ((HaxeBinaryExpression)lhs).getRightExpression();
      if (null != lhsrhs) {
        PsiElement rhs = getRightHandElement(expr);
        if (null == rhs) rhs = UsefulPsiTreeUtil.getLastChild(expr, HaxePsiCompositeElement.class);
        if (null != rhs) {
          return wrapIsFixer(lhsrhs, rhs);
        }
      }
    }
    return wrapIsFixer(expr);
  }

  @NotNull
  private static HaxeSurroundFixer wrapIsFixer(@NotNull HaxeIsTypeExpression expr) {
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.is.expression.with.parenthesis"), expr, expr.getTextRange());
  }

  @NotNull
  private static HaxeSurroundFixer wrapIsFixer(@NotNull PsiElement first, @NotNull PsiElement last) {
    TextRange range = first.getTextRange().union(last.getTextRange());
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.is.expression.with.parenthesis"), first, range);
  }

  @NotNull
  private static HaxeSurroundFixer wrapExpressionFixer(@NotNull PsiElement expr) {
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.expression.with.parenthesis"), expr, expr.getTextRange());
  }

  @NotNull
  private static HaxeSurroundFixer wrapLhsFixer(@NotNull PsiElement expr) {
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.left.hand.side.with.parenthesis"), expr, expr.getTextRange());
  }


  @Nullable
  private static PsiElement getRightHandType(HaxeIsTypeExpression expr) {
    PsiElement el = expr.getFunctionType();
    if (null == el) {
      HaxeTypeOrAnonymous toa = expr.getTypeOrAnonymous();
      if (null != toa) {
        el = toa.getAnonymousType();
        if (null == el) el = toa.getType();
      }
    }
    if (null == el) {
      PsiElement rhs = getRightHandElement(expr);
      if (rhs instanceof HaxeObjectLiteral) el = rhs;
    }
    return el;
  }

  @Nullable
  private static PsiElement getRightHandElement(HaxeIsTypeExpression expr) {
    List<HaxeExpression> exprs = expr.getExpressionList();
    if (exprs.size() > 1) {
      return exprs.get(1);
    }
    return null;
  }

  private static void annotateTypeError(PsiElement type, HaxeAnnotationHolder holder) {
    if (type instanceof HaxeType) {
      HaxeType hx = (HaxeType)type;
      if (null != hx.getTypeParam()) {
        holder.createErrorAnnotation(type, HaxeBundle.message("haxe.semantic.is.operator.type.cannot.have.parameters"));
      }
      HaxeReferenceExpression ref = hx.getReferenceExpression();
      PsiElement found = ref.resolve();
      if (found instanceof HaxeLocalVarDeclaration) {
        holder.createErrorAnnotation(type, HaxeBundle.message("haxe.semantic.is.operator.rhs.must.be.type"));
      }
    } else {
      holder.createErrorAnnotation(type, HaxeBundle.message("haxe.semantic.is.operator.type.not.supported" ));
    }
  }

  private static boolean isComplexExpression(PsiElement expr) {
    return expr instanceof HaxeBinaryExpression
        || expr instanceof HaxeTernaryExpression
        || expr instanceof HaxeIsTypeExpression;
  }
}

class TypeCheckExpressionChecker {
  public static void check(
    final HaxeTypeCheckExpr expr,
    final HaxeAnnotationHolder holder
  ) {
    if (!INCOMPATIBLE_TYPE_CHECKS.isEnabled(expr)) return;

    final PsiElement[] children = expr.getChildren();
    if (children.length == 2) {
      final HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expr);
      final ResultHolder statementResult = HaxeTypeResolver.getPsiElementType(children[0], expr, resolver);
      ResultHolder assertionResult = SpecificTypeReference.getUnknown(expr).createHolder();
      if (children[1] instanceof HaxeTypeOrAnonymous) {
        assertionResult = HaxeTypeResolver.getTypeFromTypeOrAnonymous((HaxeTypeOrAnonymous)children[1]);
        ResultHolder resolveResult = resolver.resolve(assertionResult.getType().toStringWithoutConstant());
        if (null != resolveResult) {
          assertionResult = resolveResult;
        }
      }
      if (!assertionResult.canAssign(statementResult)) {
        final HaxeDocumentModel document = HaxeDocumentModel.fromElement(expr);
        Annotation annotation = holder.createErrorAnnotation(children[0],
                                                             HaxeBundle.message("haxe.semantic.statement.does.not.unify.with.asserted.type",
                                                                                statementResult.getType().toStringWithoutConstant(),
                                                                                assertionResult.getType().toStringWithoutConstant()));
        annotation.registerFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.type.check")) {
          @Override
          public void run() {
            document.replaceElementText(expr, children[0].getText());
          }
        });
        annotation.registerFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.change.type.check.to.0", statementResult.toStringWithoutConstant())) {
          @Override
          public void run( ) {
            document.replaceElementText(children[1], statementResult.toStringWithoutConstant());
          }
        });
        // TODO: Add type conversion fixers. (eg. Wrap with Std.int(), wrap with Std.toString())
      }
    }
  }
}



class TypeTagChecker {
  public static void check(
    final PsiElement erroredElement,
    final HaxeTypeTag tag,
    final HaxeVarInit initExpression,
    boolean requireConstant,
    final HaxeAnnotationHolder holder
  ) {
    final ResultHolder varType = HaxeTypeResolver.getTypeFromTypeTag(tag, erroredElement);
    final ResultHolder initType = getTypeFromVarInit(initExpression);

    if (!varType.canAssign(initType)) {

      HaxeAnnotation annotation = typeMismatch(erroredElement, initType.toStringWithoutConstant(),
                                               varType.toStringWithoutConstant());
      if (null != initType.getClassType()) {
        annotation.withFix(new HaxeTypeTagChangeFixer(HaxeBundle.message("haxe.quickfix.change.variable.type"), tag, initType.getClassType()));
      }

      annotation.withFix(new HaxeRemoveElementFixer(HaxeBundle.message("haxe.quickfix.remove.initializer"), initExpression))
        .withFixes(HaxeExpressionConversionFixer.createStdTypeFixers(initExpression.getExpression(),
                                                                     initType.getType(), varType.getType()));
      holder.addAnnotation(annotation);

    } else if (requireConstant && initType.getType().getConstant() == null) {
      holder.createErrorAnnotation(erroredElement, HaxeBundle.message("haxe.semantic.parameter.default.type.should.be.constant", initType));
    }
  }

  @NotNull
  private static ResultHolder getTypeFromVarInit(@NotNull HaxeVarInit init) {
    HaxeExpression initExpression = init.getExpression();
    HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(initExpression);

    final ResultHolder abstractEnumFieldInitType = HaxeAbstractEnumUtil.getStaticMemberExpression(initExpression, resolver);
    if (abstractEnumFieldInitType != null) {
      return abstractEnumFieldInitType;
    }

    // fallback to simple init expression
    return null != initExpression ? HaxeTypeResolver.getPsiElementType(initExpression, init, resolver)
                                  : SpecificTypeReference.getInvalid(init).createHolder();
  }
}

class LocalVarChecker {
  public static void check(final HaxeLocalVarDeclaration var, final HaxeAnnotationHolder holder) {
    if (!INCOMPATIBLE_INITIALIZATION.isEnabled(var)) return;

    HaxeLocalVarModel local = new HaxeLocalVarModel(var);
    if (local.hasInitializer() && local.hasTypeTag()) {
      TypeTagChecker.check(local.getBasePsi(), local.getTypeTagPsi(), local.getInitializerPsi(), false, holder);
    }
  }
}


class FieldChecker {
  public static void check(final HaxeFieldDeclaration var, final HaxeAnnotationHolder holder) {
    HaxeFieldModel field = new HaxeFieldModel(var);
    if (field.isProperty()) {
      checkProperty(field, holder);
    } else {
      if (FINAL_FIELD_IS_INITIALIZED.isEnabled(var)) {
        if (field.isFinal()) {
          if (!field.hasInitializer()) {
            if (!isParentInterface(var)) {
              if (field.isStatic()) {
                holder.createErrorAnnotation(var, HaxeBundle.message("haxe.semantic.final.static.var.init", field.getName()));
              } else if (!isFieldInitializedInTheConstructor(field)) {
                holder.createErrorAnnotation(var, HaxeBundle.message("haxe.semantic.final.var.init", field.getName()));
              }
            }
          } else {
            if (isParentInterface(var)) {
              holder.createErrorAnnotation(var, HaxeBundle.message("haxe.semantic.final.static.var.init.interface", field.getName()));
            }
          }
        }
      }
    }

    if (field.hasInitializer() && field.hasTypeTag()) {
      TypeTagChecker.check(field.getBasePsi(), field.getTypeTagPsi(), field.getInitializerPsi(), false, holder);
    }


    // Checking for variable redefinition.
    if (FIELD_REDEFINITION.isEnabled(var)) {
      HashSet<HaxeClassModel> classSet = new HashSet<>();
      HaxeClassModel fieldDeclaringClass = field.getDeclaringClass();
      classSet.add(fieldDeclaringClass);
      while (fieldDeclaringClass != null) {
        fieldDeclaringClass = fieldDeclaringClass.getParentClass();
        if (classSet.contains(fieldDeclaringClass)) {
          break;
        }
        else {
          classSet.add(fieldDeclaringClass);
        }
        if (fieldDeclaringClass != null) {
          for (HaxeFieldModel parentField : fieldDeclaringClass.getFields()) {
            if (parentField.getName().equals(field.getName())) {
              String message;
              if (parentField.isStatic()) {
                message = HaxeBundle.message("haxe.semantic.static.field.override", field.getName());
                holder.createWeakWarningAnnotation(field.getNameOrBasePsi(), message);
              }
              else {
                message = HaxeBundle.message("haxe.semantic.variable.redefinition", field.getName(), fieldDeclaringClass.getName());
                holder.createErrorAnnotation(field.getBasePsi(), message);
              }
              break;
            }
          }
        }
      }
    }
  }

  private static boolean isParentInterface(HaxeFieldDeclaration var) {
    return var.getParent() instanceof HaxeInterfaceBody;
  }

  private static boolean isFieldInitializedInTheConstructor(HaxeFieldModel field) {
    HaxeClassModel declaringClass = field.getDeclaringClass();
    if (declaringClass == null) return false;
    HaxeMethodModel constructor = declaringClass.getConstructor(null);
    if (constructor == null) return false;
    PsiElement body = constructor.getBodyPsi();
    if (body == null) return false;

    final InitVariableVisitor visitor = new InitVariableVisitor(field.getName());
    body.accept(visitor);
    return visitor.result;
  }

  private static void checkProperty(final HaxeFieldModel field, final HaxeAnnotationHolder holder) {
    final HaxeDocumentModel document = field.getDocument();

    PsiElement fieldBasePsi = field.getBasePsi();
    if (PROPERTY_ACCESSOR_VALID.isEnabled(fieldBasePsi)) {
// TODO: Bug here.  (set,get) are being marked as errors.
      if (field.getGetterPsi() != null && !field.getGetterType().isValidGetter()) {
        holder.createErrorAnnotation(field.getGetterPsi(), "Invalid getter accessor");
      }

      if (field.getSetterPsi() != null && !field.getSetterType().isValidSetter()) {
        holder.createErrorAnnotation(field.getSetterPsi(), "Invalid setter accessor");
      }
    }

    if (field.isFinal()) {
      if (PROPERTY_CANNOT_BE_FINAL.isEnabled(fieldBasePsi)) {
        holder
          .createErrorAnnotation(fieldBasePsi, HaxeBundle.message("haxe.semantic.property.cant.be.final"))
          .registerFix(new HaxeSwitchMutabilityModifier((HaxeFieldDeclaration)fieldBasePsi));
      }
    } else {
      if (PROPERTY_IS_NOT_REAL_VARIABLE.isEnabled(fieldBasePsi)) {
        final HaxeVarInit initializerPsi = field.getInitializerPsi();
        if (field.isProperty() && !field.isRealVar() && null != initializerPsi) {
          Annotation annotation = holder.createErrorAnnotation(
            initializerPsi,
            "This field cannot be initialized because it is not a real variable"
          );
          annotation.registerFix(new HaxeFixer("Remove init") {
            @Override
            public void run() {
              document.replaceElementText(initializerPsi, "", StripSpaces.BEFORE);
            }
          });
          annotation.registerFix(new HaxeFixer("Add @:isVar") {
            @Override
            public void run() {
              field.getModifiers().addModifier(IS_VAR);
            }
          });
          if (field.getSetterPsi() != null) {
            annotation.registerFix(new HaxeFixer("Make setter null") {
              @Override
              public void run() {
                document.replaceElementText(field.getSetterPsi(), "null");
              }
            });
          }
        }
      }
    }
    checkPropertyAccessorMethods(field, holder);
  }

  private static void checkPropertyAccessorMethods(final HaxeFieldModel field, final HaxeAnnotationHolder holder) {
    if (!PROPERTY_ACCESSOR_EXISTENCE.isEnabled(field.getBasePsi())) {
      return;
    }

    if (field.getDeclaringClass().isInterface()) {
      return;
    }

    if (field.getGetterType() == HaxeAccessorType.GET) {
      final String methodName = "get_" + field.getName();

      HaxeMethodModel method = field.getDeclaringClass().getMethod(methodName, null);
      if (method == null && field.getGetterPsi() != null) {
        holder
          .createErrorAnnotation(field.getGetterPsi(), "Can't find method " + methodName)
          .registerFix(new CreateGetterSetterQuickfix(field.getDeclaringClass(), field, true));
      }
    }

    if (field.getSetterType() == HaxeAccessorType.SET) {
      final String methodName = "set_" + field.getName();

      HaxeMethodModel method = field.getDeclaringClass().getMethod(methodName, null);
      if (method == null && field.getSetterPsi() != null) {
        holder
          .createErrorAnnotation(field.getSetterPsi(), "Can't find method " + methodName)
          .registerFix(new CreateGetterSetterQuickfix(field.getDeclaringClass(), field, false));
      }
    }
  }
}

class TypeChecker {
  static public void check(final HaxeType type, final HaxeAnnotationHolder holder) {
    checkValidClassName(type.getReferenceExpression().getIdentifier(), holder);
    checkValidTypeParameters(type, holder);
  }

  static public void checkValidClassName(final PsiIdentifier identifier, final HaxeAnnotationHolder holder) {
    if (identifier == null) return;
    if (!INVALID_TYPE_NAME.isEnabled(identifier)) return;

    final String typeName = getTypeName(identifier);
    if (!HaxeClassModel.isValidClassName(typeName)) {
      Annotation annotation = holder.createErrorAnnotation(identifier, "Type name must start by upper case");
      annotation.registerFix(new HaxeFixer("Change name") {
        @Override
        public void run() {
          HaxeDocumentModel.fromElement(identifier).replaceElementText(
            identifier,
            typeName.substring(0, 1).toUpperCase() + typeName.substring(1)
          );
        }
      });
    }
  }
  static public void checkValidTypeParameters(final HaxeType type, final HaxeAnnotationHolder holder) {
    if(type.getContext() != null && type.getContext().getParent() instanceof HaxeTypeTag) {
      SpecificHaxeClassReference haxeClassReference = HaxeTypeResolver.getTypeFromType(type).getClassType();

      if (haxeClassReference != null ) {
        HaxeClass haxeClass = haxeClassReference.getHaxeClass();
        if(haxeClass != null) {
          // Dynamic is special and does not require Type parameter to de specified
          if (DYNAMIC.equalsIgnoreCase(haxeClass.getName())) return;

          int typeParameterCount = countTypeParameters(haxeClassReference);
          int classParameterCount = countTypeParameters(haxeClass);

          if (typeParameterCount != classParameterCount) {
            String typeName = getTypeName(type.getReferenceExpression().getIdentifier());
            holder.createErrorAnnotation(type, "Invalid number of type parameters for " + typeName);
          }
        }
      }
    }
  }

  static private int countTypeParameters(HaxeType type) {
    HaxeTypeParam param = type.getTypeParam();
    if(param == null) return 0;
    return param.getTypeList().getTypeListPartList().size();
  }
  static private int countTypeParameters(SpecificHaxeClassReference reference) {
    return (int)Stream.of(reference.getSpecifics()).filter(holder -> !holder.isUnknown()).count();
    //HaxeGenericParam param = haxeClass.getGenericParam();
    //if(param == null) return 0;
    //return param.getGenericListPartList().size();
  }
  static private int countTypeParameters(HaxeClass haxeClass) {
    HaxeGenericParam param = haxeClass.getGenericParam();
    if(param == null) return 0;
    return param.getGenericListPartList().size();
  }
  private static String getTypeName(PsiIdentifier identifier) {
    return identifier.getText();
  }

}

class ClassChecker {
  static public void check(final HaxeClass clazzPsi, final HaxeAnnotationHolder holder) {
    HaxeClassModel clazz = clazzPsi.getModel();
    checkModifiers(clazz, holder);
    checkDuplicatedFields(clazz, holder);
    checkClassName(clazz, holder);
    checkExtends(clazz, holder);
    if(!clazzPsi.isInterface()) {
      checkInterfaces(clazz, holder);
      checkInterfacesMethods(clazz, holder);
      checkInterfacesFields(clazz, holder);
    }
  }

  static private void checkModifiers(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!DUPLICATE_CLASS_MODIFIERS.isEnabled(clazz.getBasePsi())) return;

    HaxeClassModifierList modifiers = clazz.getModifiers();
    if (null != modifiers) {
      List<HaxeClassModifier> list = modifiers.getClassModifierList();
      checkForDuplicateModifier(holder, "private",
                                list.stream()
                                  .filter((modifier)->!Objects.isNull(modifier.getPrivateKeyWord()))
                                  .collect(toList()));
      checkForDuplicateModifier(holder, "final",
                                list.stream()
                                  .filter((modifier)->!Objects.isNull(modifier.getFinalKeyWord()))
                                  .collect(toList()));
      if (modifiers instanceof HaxeExternClassModifierList) {
        checkForDuplicateModifier(holder, "extern", ((HaxeExternClassModifierList)modifiers).getExternKeyWordList());
      }
    }
  }

  private static void checkForDuplicateModifier(@NotNull HaxeAnnotationHolder holder, @NotNull String modifier, @Nullable List<? extends PsiElement> elements) {
    if (null != elements && elements.size() > 1) {
      for (int i = 1; i < elements.size(); ++i) {
        reportDuplicateModifier(holder, modifier, elements.get(i));
      }
    }
  }

  private static void reportDuplicateModifier(HaxeAnnotationHolder holder, String modifier, final PsiElement element) {
    String message = HaxeBundle.message("haxe.semantic.key.must.not.be.repeated.for.class.declaration", modifier);
    Annotation annotation = holder.createErrorAnnotation(element, message);
    final HaxeDocumentModel document = HaxeDocumentModel.fromElement(element);
    if (null != document) {
      annotation.registerFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.duplicate", modifier)) {
        @Override
        public void run() {
          document.replaceElementText(element, "", StripSpaces.AFTER);
        }
      });
    }
  }


  static private void checkDuplicatedFields(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!DUPLICATE_FIELDS.isEnabled(clazz.getBasePsi())) return;

    Map<String, HaxeMemberModel> map = new HashMap<>();
    Set<HaxeMemberModel> repeatedMembers = new HashSet<>();
    for (HaxeMemberModel member : clazz.getMembersSelf()) {
      final String memberName = member.getName();
      HaxeMemberModel repeatedMember = map.get(memberName);
      if (repeatedMember != null) {
        repeatedMembers.add(member);
        repeatedMembers.add(repeatedMember);
      } else {
        map.put(memberName, member);
      }
    }

    for (HaxeMemberModel member : repeatedMembers) {
      holder.createErrorAnnotation(member.getNameOrBasePsi(), "Duplicate class field declaration : " + member.getName());
    }


    //Duplicate class field declaration
  }

  static private void checkClassName(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    TypeChecker.checkValidClassName(clazz.getNamePsi(), holder);
  }

  private static void checkExtends(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!SUPERCLASS_TYPE_COMPATIBILITY.isEnabled(clazz.getBasePsi())) return;

    HaxeClassModel reference = clazz.getParentClass(); // Get first in extends list, not PSI parent.
    // TODO: Need to loop over all interfaces or types.
    if (reference != null) {
      if (isAnonymousType(clazz)) {
        if (!isAnonymousType(reference)) {
          // @TODO: Move to bundle
          holder.createErrorAnnotation(clazz.haxeClass.getHaxeExtendsList().get(0), "Not an anonymous type");
        }
      } else if (clazz.isInterface()) {
        if (!reference.isInterface()) {
          // @TODO: Move to bundle
          holder.createErrorAnnotation(reference.getPsi(), "Not an interface");
        }
      } else if (!reference.isClass()) {
        // @TODO: Move to bundle
        holder.createErrorAnnotation(reference.getPsi(), "Not a class");
      }

      final String qname1 = reference.haxeClass.getQualifiedName();
      final String qname2 = clazz.haxeClass.getQualifiedName();
      if (qname1.equals(qname2)) {
        // @TODO: Move to bundle
        holder.createErrorAnnotation(clazz.haxeClass.getHaxeExtendsList().get(0), "Cannot extend self");
      }
    }
  }

  static private boolean isAnonymousType(HaxeClassModel clazz) {
    if (clazz != null && clazz.haxeClass != null) {
      HaxeClass haxeClass = clazz.haxeClass;
      if (haxeClass instanceof HaxeAnonymousType) {
        return true;
      }
      if (haxeClass instanceof HaxeTypedefDeclaration) {
        HaxeTypeOrAnonymous anonOrType = ((HaxeTypedefDeclaration)haxeClass).getTypeOrAnonymous();
        if (anonOrType != null) {
          return anonOrType.getAnonymousType() != null;
        }
      }
    }
    return false;
  }

  private static void checkInterfaces(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!SUPERINTERFACE_TYPE.isEnabled(clazz.getBasePsi())) return;

    for (HaxeClassReferenceModel interfaze : clazz.getImplementingInterfaces()) {
      HaxeClassModel interfazeClass = interfaze.getHaxeClass();
      boolean isDynamic = null != interfazeClass ? SpecificHaxeClassReference.withoutGenerics(interfazeClass.getReference()).isDynamic() : false;
      if (interfazeClass == null || !(interfazeClass.isInterface() || isDynamic) ) {
        holder.createErrorAnnotation(interfaze.getPsi(), HaxeBundle.message("haxe.semantic.interface.error.message"));
      }
    }
  }

  private static void checkInterfacesMethods(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    PsiElement clazzPsi = clazz.getPsi();
    boolean checkMissingInterfaceMethods = MISSING_INTERFACE_METHODS.isEnabled(clazzPsi);
    boolean checkInterfaceMethodSignature = INTERFACE_METHOD_SIGNATURE.isEnabled(clazzPsi);
    boolean checkInheritedInterfaceMethodSignature = INHERITED_INTERFACE_METHOD_SIGNATURE.isEnabled(clazzPsi);

    if (!checkMissingInterfaceMethods && !checkInterfaceMethodSignature && !checkInheritedInterfaceMethodSignature) {
      return;
    }

    for (HaxeClassReferenceModel reference : clazz.getImplementingInterfaces()) {
      checkInterfaceMethods(clazz, reference, holder, checkMissingInterfaceMethods, checkInterfaceMethodSignature, checkInheritedInterfaceMethodSignature);
    }
  }
  private static void checkInterfacesFields(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    //TODO add settings for this feature

    for (HaxeClassReferenceModel reference : clazz.getImplementingInterfaces()) {
      checkInterfaceFields(clazz, reference, holder);
    }
  }

  private static void checkInterfaceFields(
    final HaxeClassModel clazz,
    final HaxeClassReferenceModel intReference,
    final HaxeAnnotationHolder holder) {

    final List<HaxeFieldModel> missingFields = new ArrayList<>();
    final List<String> missingFieldNames = new ArrayList<>();

    if (intReference.getHaxeClass() != null) {
      List<HaxeFieldDeclaration> fields = clazz.haxeClass.getAllHaxeFields(HaxeComponentType.CLASS, HaxeComponentType.ENUM);
      for (HaxeFieldModel intField : intReference.getHaxeClass().getFields()) {
        if (!intField.isStatic()) {


          Optional<HaxeFieldDeclaration> fieldResult = fields.stream()
            .filter(method -> intField.getName().equals(method.getName()))
            .findFirst();

          if (!fieldResult.isPresent()) {
              missingFields.add(intField);
              missingFieldNames.add(intField.getName());
          } else {
            final HaxeFieldDeclaration fieldDeclaration = fieldResult.get();

            if(intField.getPropertyDeclarationPsi() != null) {
              HaxePropertyAccessor intGetter = intField.getGetterPsi();
              HaxePropertyAccessor intSetter = intField.getSetterPsi();
              HaxePropertyDeclaration propertyDeclaration = fieldDeclaration.getPropertyDeclaration();

              if(propertyDeclaration == null) {
                // some combinations are compatible with normal variables
                if (intGetter.getText().equals("default") && (intSetter.getText().equals("never") || intSetter.getText().equals("null"))) {
                  continue;
                }
                if (intGetter.getText().equals("never") && (intSetter.getText().equals("null"))) {
                  continue;
                }

                // @TODO: Move error messages to  bundle
                holder.createErrorAnnotation(fieldDeclaration.getOriginalElement(), "Field " +fieldDeclaration.getName()
                                                                    + " has different property access than in  "
                                                                    + intReference.getHaxeClass().getName());
              }else {
                HaxePropertyAccessor getter = propertyDeclaration.getPropertyAccessorList().get(0);
                HaxePropertyAccessor setter = propertyDeclaration.getPropertyAccessorList().get(1);


                if (intGetter != null && getter != null) {
                  if (!intGetter.getText().equals(getter.getText())) {
                    holder.createErrorAnnotation(getter.getElement(), "Field " +fieldDeclaration.getName()
                                                                        + " has different property access than in  "
                                                                        + intReference.getHaxeClass().getName());
                  }
                }

                if (intSetter != null && setter != null) {
                  if (!intSetter.getText().equals(setter.getText())) {
                    holder.createErrorAnnotation(setter.getElement(), "Field " +fieldDeclaration.getName()
                                                                        + " has different property access than in  "
                                                                        + intReference.getHaxeClass().getName());
                  }
                }
              }
            }

            HaxeFieldDeclaration intFieldDeclaration = (HaxeFieldDeclaration)intField.getPsiField();
            HaxeMutabilityModifier modifier = intFieldDeclaration.getMutabilityModifier();

            HaxeMutabilityModifier mutabilityModifier = fieldDeclaration.getMutabilityModifier();
            if(!modifier.getText().equals(mutabilityModifier.getText())) {
              holder.createErrorAnnotation(fieldDeclaration.getNode(), "Field " +fieldDeclaration.getName()
                                                                + " has different mutability than in  "
                                                                + intReference.getHaxeClass().getName());
            }

            HaxeFieldModel model = new HaxeFieldModel(fieldDeclaration);
            HaxeGenericResolver classFieldResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(model.getBasePsi());
            HaxeGenericResolver interfaceFieldResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(intField.getBasePsi());

            boolean typesAreCompatible = canAssignToFrom(intField.getResultType(interfaceFieldResolver), model.getResultType(classFieldResolver));

            if(!typesAreCompatible) {
              holder.createErrorAnnotation(fieldDeclaration.getNode(), "Field " +fieldDeclaration.getName()
                                                                       + " has different type than in  "
                                                                       + intReference.getHaxeClass().getName());
            }
          }
      }
    }

    if (missingFields.size() > 0) {
      // @TODO: Move to bundle
      Annotation annotation = holder.createErrorAnnotation(
        intReference.getPsi(),
        "Not implemented fields: " + StringUtils.join(missingFieldNames, ", ")
      );
      annotation.registerFix(new HaxeFixer("Implement fields") {
        @Override
        public void run() {
          OverrideImplementMethodFix fix = new OverrideImplementMethodFix(clazz.haxeClass, false);
          for (HaxeFieldModel field : missingFields) {
            fix.addElementToProcess(field.getPsiField());
          }

          PsiElement basePsi = clazz.getBasePsi();
          Project p = basePsi.getProject();
          fix.invoke(p, FileEditorManager.getInstance(p).getSelectedTextEditor(), basePsi.getContainingFile());
        }
      });
    }
  }
}


  private static void checkInterfaceMethods(
    final HaxeClassModel clazz,
    final HaxeClassReferenceModel intReference,
    final HaxeAnnotationHolder holder,
    final boolean checkMissingInterfaceMethods,
    final boolean checkInterfaceMethodSignature,
    final boolean checkInheritedInterfaceMethodSignature
  ) {
    final List<HaxeMethodModel> missingMethods = new ArrayList<HaxeMethodModel>();
    final List<String> missingMethodsNames = new ArrayList<String>();

    if (intReference.getHaxeClass() != null) {
      List<HaxeMethodModel> methods = clazz.haxeClass.getAllHaxeMethods(HaxeComponentType.CLASS, HaxeComponentType.ENUM).stream()
        .map(HaxeMethodPsiMixin::getModel)
        .collect(toList());

      for (HaxeMethodModel intMethod : intReference.getHaxeClass().getMethods(null)) {
        if (!intMethod.isStatic()) {

          Optional<HaxeMethodModel> methodResult = methods.stream()
            .filter(method -> intMethod.getName().equals(method.getName()))
            .findFirst();



          if (!methodResult.isPresent()) {
            if (checkMissingInterfaceMethods) {
              missingMethods.add(intMethod);
              missingMethodsNames.add(intMethod.getName());
            }
          } else {
            final HaxeMethodModel methodModel = methodResult.get();

            // We should check if signature in inherited method differs from method provided by interface
            if (methodModel.getDeclaringClass() != clazz) {
              if(methodModel.getDeclaringClass().isInterface()) {
                missingMethods.add(methodModel);
                missingMethodsNames.add(intMethod.getName());
              } else {
                if (checkInheritedInterfaceMethodSignature && MethodChecker.checkIfMethodSignatureDiffers(methodModel, intMethod)) {
                  final HaxeClass parentClass = methodModel.getDeclaringClass().haxeClass;

                  final String errorMessage = HaxeBundle.message(
                    "haxe.semantic.implemented.super.method.signature.differs",
                    methodModel.getName(),
                    parentClass.getQualifiedName(),
                    intMethod.getPresentableText(HaxeMethodContext.NO_EXTENSION),
                    methodModel.getPresentableText(HaxeMethodContext.NO_EXTENSION)
                  );

                  holder.createErrorAnnotation(intReference.getPsi(), errorMessage);
                }
              }
            } else {
              if (checkInterfaceMethodSignature) {
                MethodChecker.checkMethodsSignatureCompatibility(methodModel, intMethod, holder);
              }
            }
          }
        }
      }
    }

    if (missingMethods.size() > 0) {
      // @TODO: Move to bundle
      Annotation annotation = holder.createErrorAnnotation(
        intReference.getPsi(),
        "Not implemented methods: " + StringUtils.join(missingMethodsNames, ", ")
      );
      annotation.registerFix(new HaxeFixer("Implement methods") {
        @Override
        public void run() {
          OverrideImplementMethodFix fix = new OverrideImplementMethodFix(clazz.haxeClass, false);
          for (HaxeMethodModel mm : missingMethods) {
            fix.addElementToProcess(mm.getMethodPsi());
          }

          PsiElement basePsi = clazz.getBasePsi();
          Project p = basePsi.getProject();
          fix.invoke(p, FileEditorManager.getInstance(p).getSelectedTextEditor(), basePsi.getContainingFile());
        }
      });
    }
  }
}

class MethodChecker {
  static public void check(final HaxeMethod methodPsi, final HaxeAnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    checkTypeTagInInterfacesAndExternClass(currentMethod, holder);
    checkMethodArguments(currentMethod, holder);
    checkOverride(methodPsi, holder);
    if (HaxeSemanticAnnotatorConfig.ENABLE_EXPERIMENTAL_BODY_CHECK) {
      MethodBodyChecker.check(methodPsi, holder);
    }
    //currentMethod.getBodyPsi()
  }

  private static void checkTypeTagInInterfacesAndExternClass(final HaxeMethodModel currentMethod, final HaxeAnnotationHolder holder) {
    if (!MISSING_TYPE_TAG_ON_EXTERN_AND_INTERFACE.isEnabled(currentMethod.getBasePsi())) return;

    HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    if (currentClass.isExtern() || currentClass.isInterface()) {
      if (currentMethod.getReturnTypeTagPsi() == null && !currentMethod.isConstructor()) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), HaxeBundle.message("haxe.semantic.type.required"));
      }
      for (final HaxeParameterModel param : currentMethod.getParameters()) {
        if (param.getTypeTagPsi() == null) {
          holder.createErrorAnnotation(param.getBasePsi(), HaxeBundle.message("haxe.semantic.type.required"));
        }
      }
    }
  }

  private static void checkMethodArguments(final HaxeMethodModel currentMethod, final HaxeAnnotationHolder holder) {
    PsiElement methodPsi = currentMethod.getBasePsi();
    boolean checkOptionalWithInit = OPTIONAL_WITH_INITIALIZER.isEnabled(methodPsi);
    boolean checkParameterInitializers = PARAMETER_INITIALIZER_TYPES.isEnabled(methodPsi);
    boolean checkParameterOrdering = PARAMETER_ORDERING_CHECK.isEnabled(methodPsi);
    boolean checkRepeatedParameterName = REPEATED_PARAMETER_NAME_CHECK.isEnabled(methodPsi);

    if (!checkOptionalWithInit
    &&  !checkParameterInitializers
    &&  !checkParameterOrdering
    &&  !checkRepeatedParameterName) {
      return;
    }

    boolean hasOptional = false;
    HashMap<String, PsiElement> argumentNames = new HashMap<String, PsiElement>();
    for (final HaxeParameterModel param : currentMethod.getParameters()) {
      String paramName = param.getName();

      if (checkOptionalWithInit) {
        if (param.hasOptionalPsi() && param.getVarInitPsi() != null) {
          // @TODO: Move to bundle
          holder.createWarningAnnotation(param.getOptionalPsi(), "Optional not needed when specified an init value");
        }
      }

      if (checkParameterInitializers) {
        if (param.getVarInitPsi() != null && param.getTypeTagPsi() != null) {
          TypeTagChecker.check(
            param.getBasePsi(),
            param.getTypeTagPsi(),
            param.getVarInitPsi(),
            true,
            holder
          );
        }
      }

      if (checkParameterOrdering) {
        if (param.isOptional()) {
          hasOptional = true;
        } else if (hasOptional) {
          // @TODO: Move to bundle
          holder.createWarningAnnotation(param.getBasePsi(), "Non-optional argument after optional argument");
        }
      }

      if (checkRepeatedParameterName) {
        if (argumentNames.containsKey(paramName)) {
          // @TODO: Move to bundle
          holder.createWarningAnnotation(param.getNamePsi(), "Repeated argument name '" + paramName + "'");
          holder.createWarningAnnotation(argumentNames.get(paramName), "Repeated argument name '" + paramName + "'");
        } else {
          argumentNames.put(paramName, param.getNamePsi());
        }
      }
    }
  }

  private static final String[] OVERRIDE_FORBIDDEN_MODIFIERS = {FINAL, INLINE, STATIC};
  private static void checkOverride(final HaxeMethod methodPsi, final HaxeAnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    final HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    final HaxeModifiersModel currentModifiers = currentMethod.getModifiers();

    final HaxeClassModel parentClass = (currentClass != null) ? currentClass.getParentClass() : null;
    final HaxeMethodModel parentMethod = parentClass != null ? parentClass.getMethod(currentMethod.getName(), null) : null;
    final HaxeModifiersModel parentModifiers = (parentMethod != null) ? parentMethod.getModifiers() : null;

    if (!METHOD_OVERRIDE_CHECK.isEnabled(methodPsi)) { // TODO: This check is not granular enough.
      // If the rest of the checks are disabled, we don't want to inhibit the signature check.
      if (null != parentMethod) {
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder);
      }
      return;
    }

    boolean requiredOverride = false;

    if (currentMethod.isConstructor()) {
      if (currentModifiers.hasModifier(STATIC)) {
        // @TODO: Move to bundle
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Constructor can't be static").registerFix(
          new HaxeModifierRemoveFixer(currentModifiers, STATIC)
        );
      }
    } else if (currentMethod.isStaticInit()) {
      if (!currentModifiers.hasModifier(STATIC)) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "__init__ must be static").registerFix(
          new HaxeModifierAddFixer(currentModifiers, STATIC)
        );
      }
    } else if (parentMethod != null) {
      if (parentMethod.isStatic()) {
        holder.createWarningAnnotation(currentMethod.getNameOrBasePsi(), "Method '" + currentMethod.getName()
                                                                         + "' overrides a static method of a superclass");
      } else {
        requiredOverride = true;

        if (parentModifiers.hasAnyModifier(OVERRIDE_FORBIDDEN_MODIFIERS)) {
          Annotation annotation =
            holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Can't override static, inline or final methods");
          for (String modifier : OVERRIDE_FORBIDDEN_MODIFIERS) {
            if (parentModifiers.hasModifier(modifier)) {
              annotation.registerFix(
                new HaxeModifierRemoveFixer(parentModifiers, modifier, "Remove " + modifier + " from " + parentMethod.getFullName())
              );
            }
          }
        }

        if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
          Annotation annotation = holder.createWarningAnnotation(
            currentMethod.getNameOrBasePsi(),
            "Field " +
            currentMethod.getName() +
            " has less visibility (public/private) than superclass one."
          );
          annotation.registerFix(
            new HaxeModifierReplaceVisibilityFixer(currentModifiers, parentModifiers.getVisibility(), "Change current method visibility"));
          annotation.registerFix(
            new HaxeModifierReplaceVisibilityFixer(parentModifiers, currentModifiers.getVisibility(), "Change parent method visibility"));
        }
      }
    }

    //System.out.println(aClass);
    if (currentModifiers.hasModifier(OVERRIDE) && !requiredOverride) {
      holder.createErrorAnnotation(currentModifiers.getModifierPsi(OVERRIDE), "Overriding nothing").registerFix(
        new HaxeModifierRemoveFixer(currentModifiers, OVERRIDE)
      );
    } else if (requiredOverride) {
      if (!currentModifiers.hasModifier(OVERRIDE)) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Must override").registerFix(
          new HaxeModifierAddFixer(currentModifiers, OVERRIDE)
        );
      } else {
        // It is rightly overriden. Now check the signature.
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder);
      }
    }
  }

  static void checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod,
    final HaxeAnnotationHolder holder
  ) {
    if (!METHOD_SIGNATURE_COMPATIBILITY.isEnabled(currentMethod.getBasePsi())) return;

    final HaxeDocumentModel document = currentMethod.getDocument();

    List<HaxeParameterModel> currentParameters = currentMethod.getParameters();
    final List<HaxeParameterModel> parentParameters = parentMethod.getParameters();
    int minParameters = Math.min(currentParameters.size(), parentParameters.size());

    if (currentParameters.size() > parentParameters.size()) {
      for (int n = minParameters; n < currentParameters.size(); n++) {
        final HaxeParameterModel currentParam = currentParameters.get(n);
        holder.createErrorAnnotation(currentParam.getBasePsi(), "Unexpected argument").registerFix(
          new HaxeFixer("Remove argument") {
            @Override
            public void run() {
              currentParam.remove();
            }
          });
      }
    } else if (currentParameters.size() != parentParameters.size()) {
      holder.createErrorAnnotation(
        currentMethod.getNameOrBasePsi(),
        "Not matching arity expected " +
        parentParameters.size() +
        " arguments but found " +
        currentParameters.size()
      );
    }

    HaxeGenericResolver scopeResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(currentMethod.getBasePsi());

    for (int n = 0; n < minParameters; n++) {
      final HaxeParameterModel currentParam = currentParameters.get(n);
      final HaxeParameterModel parentParam = parentParameters.get(n);

      // We cannot simply check that the two types are the same when they have type arguments;
      // the arguments may not resolve to the same thing.  So, we need to resolve the element
      // in the super-class before we can check assignment compatibility.
      SpecificHaxeClassReference resolvedParent = resolveSuperclassElement(scopeResolver, currentParam, parentParam);

      // Order of assignment compatibility is to parent, from subclass.
      ResultHolder currentParamType = currentParam.getType(scopeResolver);
      ResultHolder parentParamType = parentParam.getType(null == resolvedParent ? scopeResolver : resolvedParent.getGenericResolver());
      if (!canAssignToFrom(parentParamType, currentParamType)) {
        HaxeAnnotation annotation =
          typeMismatch(currentParam.getBasePsi(), currentParamType.toString(), parentParamType.toString())
          .withFix(HaxeFixer.create(HaxeBundle.message("haxe.semantic.change.type"), ()->{
            document.replaceElementText(currentParam.getTypeTagPsi(), parentParam.getTypeTagPsi().getText());
          }));
        holder.addAnnotation(annotation);
      }

      if (currentParam.hasOptionalPsi() != parentParam.hasOptionalPsi()) {
        final boolean removeOptional = currentParam.hasOptionalPsi();

        String errorMessage;
        if (parentMethod.getDeclaringClass().isInterface()) {
          errorMessage = removeOptional ? "haxe.semantic.implemented.method.parameter.required"
                                        : "haxe.semantic.implemented.method.parameter.optional";
        } else {
          errorMessage = removeOptional ? "haxe.semantic.overwritten.method.parameter.required"
                                        : "haxe.semantic.overwritten.method.parameter.optional";
        }

        errorMessage = HaxeBundle.message(errorMessage, parentParam.getPresentableText(),
                                          parentMethod.getDeclaringClass().getName() + "." + parentMethod.getName());

        final Annotation annotation = holder.createErrorAnnotation(currentParam.getBasePsi(), errorMessage);
        final String localFixName = HaxeBundle.message(removeOptional ? "haxe.semantic.method.parameter.optional.remove"
                                                                      : "haxe.semantic.method.parameter.optional.add");

        annotation.registerFix(
          new HaxeFixer(localFixName) {
            @Override
            public void run() {
              if (removeOptional) {
                currentParam.getOptionalPsi().delete();
              } else {
                PsiElement element = currentParam.getBasePsi();
                document.addTextBeforeElement(element.getFirstChild(), "?");
              }
            }
          }
        );
      }
    }

    // Check the return type...

    // Again, the super-class may resolve with different/incompatible type arguments.
    SpecificHaxeClassReference resolvedParent = resolveSuperclassElement(scopeResolver, currentMethod, parentMethod);

    ResultHolder currentResult = currentMethod.getResultType(scopeResolver);
    ResultHolder parentResult = parentMethod.getResultType(resolvedParent != null ? resolvedParent.getGenericResolver() : scopeResolver);

    // Order of assignment compatibility is to parent, from subclass.
    if (!canAssignToFrom(parentResult.getType(), currentResult.getType())) {
      PsiElement psi = currentMethod.getReturnTypeTagOrNameOrBasePsi();
      HaxeAnnotation annotation =
        returnTypeMismatch(psi, currentResult.getType().toStringWithoutConstant(), parentResult.getType().toStringWithConstant())
          .withFix(HaxeFixer.create(HaxeBundle.message("haxe.semantic.change.type"), ()->{
            document.replaceElementText(currentResult.getElementContext(), parentResult.toStringWithoutConstant());
          }));
      holder.addAnnotation(annotation);
    }
  }

  @Nullable
  private static SpecificHaxeClassReference resolveSuperclassElement(HaxeGenericResolver scopeResolver,
                                                                     HaxeModel currentElement,
                                                                     HaxeModel parentParam) {
    HaxeGenericSpecialization scopeSpecialization = HaxeGenericSpecialization.fromGenericResolver(currentElement.getBasePsi(), scopeResolver);
    HaxeClassResolveResult superclassResult = HaxeResolveUtil.getSuperclassResolveResult(parentParam.getBasePsi(),
                                                                                         currentElement.getBasePsi(),
                                                                                         scopeSpecialization);
    if (superclassResult == HaxeClassResolveResult.EMPTY) {
      // TODO: Create Unresolved annotation??
      HaxeDebugLogger.getLogger().warn("Couldn't resolve a parameter type from a subclass for " + currentElement.getName());
    }

    SpecificHaxeClassReference resolvedParent = null;
    HaxeGenericResolver superResolver = superclassResult.getGenericResolver();
    HaxeClass superClass = superclassResult.getHaxeClass();
    if (null != superClass) {
      HaxeClassReference superclassReference = new HaxeClassReference(superClass.getModel(), currentElement.getBasePsi());
      resolvedParent = SpecificHaxeClassReference.withGenerics(superclassReference, superResolver.getSpecificsFor(superClass));
    }
    return resolvedParent;
  }

  // Fast check without annotations
  static boolean checkIfMethodSignatureDiffers(HaxeMethodModel source, HaxeMethodModel prototype) {
    final List<HaxeParameterModel> sourceParameters = source.getParameters();
    final List<HaxeParameterModel> prototypeParameters = prototype.getParameters();

    if (sourceParameters.size() != prototypeParameters.size()) {
      return true;
    }

    final int parametersCount = sourceParameters.size();

    for (int n = 0; n < parametersCount; n++) {
      final HaxeParameterModel sourceParam = sourceParameters.get(n);
      final HaxeParameterModel prototypeParam = prototypeParameters.get(n);
      if (!canAssignToFrom(sourceParam.getType(), prototypeParam.getType()) ||
          sourceParam.isOptional() != prototypeParam.isOptional()) {
        return true;
      }
    }

    ResultHolder currentResult = source.getResultType();
    ResultHolder prototypeResult = prototype.getResultType();

    return !currentResult.canAssign(prototypeResult);
  }
}

class PackageChecker {
  static public void check(final HaxePackageStatement element, final HaxeAnnotationHolder holder) {

    HaxeFile file = (HaxeFile)element.getContainingFile();
    if (element != file.getPackageStatement()) {  // If it's not the first one...
      holder.createErrorAnnotation(element, "Multiple package names are not allowed.")
        .registerFix(new HaxeRemoveElementFixer("Remove extra package declaration", element, StripSpaces.BOTH));
    }

    if (!PACKAGE_NAME_CHECK.isEnabled(element)) return;

    final HaxeReferenceExpression expression = element.getReferenceExpression();
    String packageName = (expression != null) ? expression.getText() : "";
    PsiDirectory fileDirectory = file.getParent();
    if (fileDirectory == null) return;
    List<PsiFileSystemItem> fileRange = PsiFileUtils.getRange(PsiFileUtils.findRoot(fileDirectory), fileDirectory);
    fileRange.remove(0);
    String actualPath = PsiFileUtils.getListPath(fileRange);
    final String actualPackage = actualPath.replace('/', '.');
    final String actualPackage2 = HaxeResolveUtil.getPackageName(file);
    // @TODO: Should use HaxeResolveUtil

    for (String s : StringUtils.split(packageName, '.')) {
      if (!s.substring(0, 1).toLowerCase().equals(s.substring(0, 1))) {
        //HaxeSemanticError.addError(element, new HaxeSemanticError("Package name '" + s + "' must start with a lower case character"));
        // @TODO: Move to bundle
        holder.createErrorAnnotation(element, "Package name '" + s + "' must start with a lower case character");
      }
    }

    if (!packageName.equals(actualPackage)) {
      holder.createErrorAnnotation(
        element,
        "Invalid package name! '" + packageName + "' should be '" + actualPackage + "'").registerFix(
        new HaxeFixer("Fix package") {
          @Override
          public void run() {
            Document document =
              PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());

            if (expression != null) {
              TextRange range = expression.getTextRange();
              document.replaceString(range.getStartOffset(), range.getEndOffset(), actualPackage);
            } else {
              int offset =
                element.getNode().findChildByType(HaxeTokenTypes.OSEMI).getTextRange().getStartOffset();
              document.replaceString(offset, offset, actualPackage);
            }
          }
        }
      );
    }
  }
}

class MethodBodyChecker {
  public static void check(HaxeMethod psi, HaxeAnnotationHolder holder) {
    final HaxeMethodModel method = psi.getModel();
    // Note: getPsiElementType runs a number of checks while determining the type.
    HaxeTypeResolver.getPsiElementType(method.getBodyPsi(), holder, generateConstraintResolver(method));
  }

  @NotNull
  private static HaxeGenericResolver generateConstraintResolver(HaxeMethodModel method) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (HaxeGenericParamModel param : method.getGenericParams()) {
      ResultHolder constraint = param.getConstraint(resolver);
      if (null == constraint) {
        constraint = new ResultHolder(SpecificHaxeClassReference.getDynamic(param.getPsi()));
      }
      resolver.add(param.getName(), constraint);
    }
    return resolver;
  }
}

class StringChecker {
  // These patterns are designed to find likely string templates; they are not intended to be exhaustive.
  private static Pattern shortTemplate = Pattern.compile("(\\$+)\\w+");
  private static Pattern longTemplate = Pattern.compile("(\\$+)\\{.*}");

  public static void check(HaxeStringLiteralExpression psi, HaxeAnnotationHolder holder) {
    if (!STRING_INTERPOLATION_QUOTE_CHECK.isEnabled(psi)) return;

    if (isSingleQuotesRequired(psi)) {
      holder.createWarningAnnotation(psi, HaxeBundle
        .message("haxe.semantic.inspection.message.expression.that.contains.string.interpolation.should.be.wrapped.with.single.quotes"))
        .registerFix(HaxeSurroundFixer.replaceQuotesWithSingleQuotes(psi));

    }
  }

  private static boolean isSingleQuotesRequired(HaxeStringLiteralExpression psi) {
    String text = psi.getText();
    return text.startsWith("\"") && (templateMatches(text, shortTemplate)|| templateMatches(text, longTemplate));
  }

  private static boolean templateMatches(String text, Pattern p) {
    // We need an odd number of dollar signs to avoid detecting escaped dollar signs as templates.
    Matcher m = p.matcher(text);
    while (m.find()) {
      if (m.groupCount() == 1 && (m.end(1) - m.start(1)) % 2 != 0) {
        return true;
      }
    }
    return false;
  }
}

class InitVariableVisitor extends HaxeVisitor {
  public boolean result = false;

  private final String fieldName;

  InitVariableVisitor(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public void visitElement(PsiElement element) {
    super.visitElement(element);
    if (result) return;
    if (element instanceof HaxeIdentifier || element instanceof HaxePsiToken || element instanceof HaxeStringLiteralExpression) return;
    element.acceptChildren(this);
  }

  @Override
  public void visitAssignExpression(@NotNull HaxeAssignExpression o) {
    HaxeExpression expression = (o.getExpressionList()).get(0);
    if (expression instanceof HaxeReferenceExpression) {
      final HaxeReferenceExpression reference = (HaxeReferenceExpression)expression;
      final HaxeIdentifier identifier = reference.getIdentifier();

      if (identifier.getText().equals(fieldName)) {
        PsiElement firstChild = reference.getFirstChild();
        if (firstChild instanceof HaxeThisExpression || firstChild == identifier) {
          this.result = true;
          return;
        }
      }
    }

    super.visitAssignExpression(o);
  }
}

class AssignExpressionChecker {
  public static void check(HaxeAssignExpression psi, HaxeAnnotationHolder holder) {
    if (!ASSIGNMENT_TYPE_COMPATIBILITY_CHECK.isEnabled(psi)) return;

    // TODO: Think about how to use models to do this instead. :/
    PsiElement lhs = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(psi);
    PsiElement assignOperation = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(lhs);
    PsiElement rhs = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(assignOperation);

    HaxeGenericResolver lhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(lhs);
    HaxeGenericResolver rhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(rhs);
    ResultHolder lhsType = HaxeTypeResolver.getPsiElementType(lhs, psi, lhsResolver);
    ResultHolder rhsType = HaxeTypeResolver.getPsiElementType(rhs, psi, rhsResolver);

    if (!lhsType.canAssign(rhsType)) {
      HaxeAnnotation anno = typeMismatch(rhs, rhsType.toPresentationString(), lhsType.toPresentationString())
        .withFixes(HaxeExpressionConversionFixer.createStdTypeFixers(rhs, rhsType.getType(), lhsType.getType()));
      holder.addAnnotation(anno);
    }
    if (lhsType.isImmutable()) {
      // TODO: Think about providing a quick-fix for immutability; remember final markings come from metadata, too.
      holder.createErrorAnnotation(psi, HaxeBundle.message("haxe.semantic.cannot.assign.value.to.final.variable"));
    }
  }
}