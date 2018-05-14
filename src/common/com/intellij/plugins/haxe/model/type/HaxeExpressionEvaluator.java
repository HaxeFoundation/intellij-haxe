/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.quickfix.HaxeAddCastFix;
import com.intellij.plugins.haxe.ide.quickfix.HaxeCreateConstructorFromCallFix;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.ObjectUtils;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.intellij.plugins.haxe.model.type.SpecificFunctionReference.Argument;

public class HaxeExpressionEvaluator {
  static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  private static final String BUILTIN_CHARACTER_CODE = "code";

  static {
    LOG.setLevel(Level.INFO);
  }

  @NotNull
  static public HaxeExpressionEvaluatorContext evaluate(PsiElement element, HaxeExpressionEvaluatorContext context) {
    context.result = handle(element, context);
    return context;
  }

  @NotNull
  static private ResultHolder handle(final PsiElement element, final HaxeExpressionEvaluatorContext context) {
    try {
      return _handle(element, context);
    }
    catch (NullPointerException e) {
      // Make sure that these get into the log, because the GeneralHighlightingPass swallows them.
      LOG.error("Error evaluating expression type for element " + element.toString(), e);
      throw e;
    }
    catch (ProcessCanceledException e) {
      // Don't log these, because they are common, but DON'T swallow them, either; it makes things unresponsive.
      throw e;
    }
    catch (Throwable t) {
      // XXX: Watch this.  If it happens a lot, then maybe we shouldn't log it unless in debug mode.
      LOG.warn("Error evaluating expression type for element " + element.toString(), t);
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }
  }

  @NotNull
  static private ResultHolder _handle(final PsiElement element, final HaxeExpressionEvaluatorContext context) {
    if (element == null) {
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }
    LOG.debug("Handling element: " + element);
    if (element instanceof PsiCodeBlock) {
      context.beginScope();
      ResultHolder type = SpecificHaxeClassReference.getUnknown(element).createHolder();
      boolean deadCode = false;
      for (PsiElement childElement : element.getChildren()) {
        type = handle(childElement, context);
        if (deadCode) {
          //context.addWarning(childElement, "Unreachable statement");
          context.addUnreachable(childElement);
        }
        if (childElement instanceof HaxeReturnStatement) {
          deadCode = true;
        }
      }
      context.endScope();
      return type;
    }

    if (element instanceof HaxeReturnStatement) {
      PsiElement[] children = element.getChildren();
      ResultHolder result = SpecificHaxeClassReference.getVoid(element).createHolder();
      if (children.length >= 1) {
        result = handle(children[0], context);
      }
      context.addReturnType(result, element);
      return result;
    }

    if (element instanceof HaxeIterable) {
      return handle(((HaxeIterable)element).getExpression(), context);
    }

    if (element instanceof HaxeForStatement) {
      final HaxeComponentName name = ((HaxeForStatement)element).getComponentName();
      final HaxeIterable iterable = ((HaxeForStatement)element).getIterable();
      final PsiElement body = element.getLastChild();
      context.beginScope();
      try {
        final SpecificTypeReference iterableValue = handle(iterable, context).getType();
        SpecificTypeReference type = iterableValue.getIterableElementType(iterableValue).getType();
        if (iterableValue.isConstant()) {
          final Object constant = iterableValue.getConstant();
          if (constant instanceof HaxeRange) {
            type = type.withRangeConstraint((HaxeRange)constant);
          }
        }
        if (name != null) {
          context.setLocal(name.getText(), new ResultHolder(type));
        }
        return handle(body, context);
      }
      finally {
        context.endScope();
      }
    }

    if (element instanceof HaxeSwitchStatement) {
      // TODO: Evaluating result of switch statement should properly implemented
    }

    if (element instanceof HaxeNewExpression) {
      ResultHolder typeHolder = HaxeTypeResolver.getTypeFromType(((HaxeNewExpression)element).getType());
      if (typeHolder.getType() instanceof SpecificHaxeClassReference) {
        final HaxeClassModel clazz = ((SpecificHaxeClassReference)typeHolder.getType()).getHaxeClassModel();
        if (clazz != null) {
          HaxeMethodModel constructor = clazz.getConstructor();
          if (constructor == null) {
            context.addError(element, HaxeBundle.message("haxe.semantic.no.constructor", clazz.getName()))
              .registerFix(new HaxeCreateConstructorFromCallFix((HaxeNewExpression)element));
          } else {
            checkParameters(element, constructor, ((HaxeNewExpression)element).getExpressionList(), context);
          }
        }
      }
      return typeHolder.duplicate();
    }

    if (element instanceof HaxeThisExpression) {
      //PsiReference reference = element.getReference();
      //HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(element);
      HaxeClass ancestor = UsefulPsiTreeUtil.getAncestor(element, HaxeClass.class);
      if (ancestor == null) return SpecificTypeReference.getDynamic(element).createHolder();
      HaxeClassModel model = ancestor.getModel();
      if (model.isAbstract()) {
        HaxeTypeOrAnonymous type = model.getUnderlyingType();
        if (type != null) {
          HaxeClass aClass = HaxeResolveUtil.tryResolveClassByQName(type);
          if (aClass != null) {
            return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(aClass.getModel(), element), element).createHolder();
          }
        }
      }
      return SpecificHaxeClassReference.primitive(ancestor.getQualifiedName(), element).createHolder();
    }

    if (element instanceof HaxeIdentifier) {
      ResultHolder holder = context.get(element.getText());

      if (holder == null) {
        return SpecificTypeReference.getUnknown(element).createHolder();
      }

      return holder;
    }

    if (element instanceof HaxeCastExpression) {
      handle(((HaxeCastExpression)element).getExpression(), context);
      HaxeTypeOrAnonymous anonymous = ((HaxeCastExpression)element).getTypeOrAnonymous();
      if (anonymous != null) {
        return HaxeTypeResolver.getTypeFromTypeOrAnonymous(anonymous);
      } else {
        return SpecificHaxeClassReference.getUnknown(element).createHolder();
      }
    }

    if (element instanceof HaxeWhileStatement) {
      List<HaxeExpression> list = ((HaxeWhileStatement)element).getExpressionList();
      SpecificTypeReference type = null;
      HaxeExpression lastExpression = null;
      for (HaxeExpression expression : list) {
        type = handle(expression, context).getType();
        lastExpression = expression;
      }
      if (type == null) {
        type = SpecificTypeReference.getDynamic(element);
      }
      if (!type.isBool() && lastExpression != null) {
        context
          .addError(lastExpression, HaxeBundle.message("haxe.semantic.condition.must.be.boolean"))
          .registerFix(new HaxeAddCastFix(lastExpression, type, SpecificTypeReference.getBool(element)));
      }

      PsiElement body = element.getLastChild();
      if (body != null) {
        //return SpecificHaxeClassReference.createArray(result); // @TODO: Check this
        return handle(body, context);
      }

      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLocalVarDeclarationList) {
      for (HaxeLocalVarDeclaration part : ((HaxeLocalVarDeclarationList)element).getLocalVarDeclarationList()) {
        handle(part, context);
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeAssignExpression) {
      final PsiElement left = element.getFirstChild();
      final PsiElement right = element.getLastChild();
      if (left != null && right != null) {
        final ResultHolder leftResult = handle(left, context);
        final ResultHolder rightResult = handle(right, context);

        if (leftResult.isUnknown()) {
          leftResult.setType(rightResult.getType());
        }
        leftResult.removeConstant();

        final SpecificTypeReference leftValue = leftResult.getType();
        final SpecificTypeReference rightValue = rightResult.getType();

        //leftValue.mutateConstantValue(null);
        if (!leftResult.canAssign(rightResult)) {
          context.addError(element, "Can't assign " + rightValue + " to " + leftValue, new HaxeCastFixer(right, rightValue, leftValue));
        }

        if (leftResult.isImmutable()) {
          context.addError(element, "Trying to change an immutable value");
        }

        return rightResult;
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLocalVarDeclaration) {
      final HaxeComponentName name = ((HaxeLocalVarDeclaration)element).getComponentName();
      final HaxeVarInit init = ((HaxeLocalVarDeclaration)element).getVarInit();
      final HaxeTypeTag typeTag = ((HaxeLocalVarDeclaration)element).getTypeTag();
      ResultHolder result = SpecificHaxeClassReference.getUnknown(element).createHolder();
      if (init != null) {
        result = handle(init, context);
      }
      if (typeTag != null) {
        result = HaxeTypeResolver.getTypeFromTypeTag(typeTag, element);
      }

      if (typeTag != null) {
        final ResultHolder tag = HaxeTypeResolver.getTypeFromTypeTag(typeTag, element);
        if (!tag.canAssign(result)) {
          result = tag.duplicate();

          context.addError(
            element,
            "Can't assign " + result + " to " + tag,
            new HaxeTypeTagChangeFixer(typeTag, result.getType()),
            new HaxeTypeTagRemoveFixer(typeTag)
          );
        }
      }

      if (name != null) {
        context.setLocal(name.getText(), result);
      }

      return result;
    }

    if (element instanceof HaxeVarInit) {
      final HaxeExpression expression = ((HaxeVarInit)element).getExpression();
      if (expression == null) {
        return SpecificTypeReference.getInvalid(element).createHolder();
      }
      return handle(expression, context);
    }

    if (element instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression referenceExpression = (HaxeReferenceExpression)element;
      if (referenceExpression.getFirstChild() instanceof HaxeIdentifier) {
        PsiElement resolvedElement = referenceExpression.resolve();
        if (resolvedElement instanceof HaxeFieldDeclaration ||
            resolvedElement instanceof HaxeMethodDeclaration ||
            resolvedElement instanceof HaxeParameter) {
          ResultHolder resultType = handle(referenceExpression.getIdentifier(), context);
          if (resultType.isUnknown()) {
            HaxeBaseMemberModel model = (HaxeBaseMemberModel)((HaxeModelTarget)resolvedElement).getModel();
            return model.getResultType();
          }
          return resultType;
        } else if (resolvedElement instanceof HaxeClass) {
          return SpecificHaxeClassReference
            .withoutGenerics(new HaxeClassReference(((HaxeClass)resolvedElement).getModel(), element))
            .createHolder();
        } else {
          return handle(referenceExpression.getIdentifier(), context);
        }
      } else {
        ResultHolder typeHolder = handle(referenceExpression.getFirstChild(), context);
        final SpecificTypeReference type = typeHolder.getType();
        final String memberName = referenceExpression.getIdentifier().getText();
        if (referenceExpression.getIdentifier().getText().equals(BUILTIN_CHARACTER_CODE)) {
          HaxeStringLiteralExpression stringLiteral = ObjectUtils.tryCast(referenceExpression.getQualifier(), HaxeStringLiteralExpression.class);
          Character character = HaxeStringLiteralUtil.getCharacter(stringLiteral);
          if (character != null) {
            return SpecificTypeReference.getInt(element, (int)character).createHolder();
          } else {
            context.addError(referenceExpression.getFirstChild(), "String must be a single UTF8 char");
          }
        } else {
          ResultHolder memberType = type.access(memberName, context);
          if (memberType == null) {
            Annotation annotation =
              context.addError(referenceExpression.getIdentifier(), "Can't resolve '" + memberName + "' in " + typeHolder.getType());
            annotation.registerFix(new HaxeCreateLocalVariableFixer(memberName, element));
            annotation.registerFix(new HaxeCreateMethodFixer(memberName, element));
            annotation.registerFix(new HaxeCreateFieldFixer(memberName, element));
          } else {
            return memberType;
          }
        }
      }
      return SpecificTypeReference.getDynamic(element).createHolder();
    }

    if (element instanceof HaxeCallExpression) {
      HaxeCallExpression callelement = (HaxeCallExpression)element;
      HaxeExpression callLeft = ((HaxeCallExpression)element).getExpression();
      SpecificTypeReference functionType = handle(callLeft, context).getType();

      // @TODO: this should be innecessary when code is working right!
      if (functionType.isUnknown()) {
        if (callLeft instanceof HaxeReference) {
          PsiReference reference = callLeft.getReference();
          if (reference != null) {
            PsiElement subelement = reference.resolve();
            if (subelement instanceof HaxeMethod) {
              functionType = ((HaxeMethod)subelement).getModel().getFunctionType();
            }
          }
        }
      }

      if (functionType.isUnknown()) {
        LOG.debug("Couldn't resolve " + callLeft.getText());
      }

      List<HaxeExpression> parameterExpressions = null;
      if (callelement.getExpressionList() != null) {
        parameterExpressions = callelement.getExpressionList().getExpressionList();
      } else {
        parameterExpressions = Collections.emptyList();
      }

      if (functionType instanceof SpecificFunctionReference) {
        SpecificFunctionReference ftype = (SpecificFunctionReference)functionType;
        HaxeExpressionEvaluator.checkParameters(callelement, ftype, parameterExpressions, context);

        return ftype.getReturnType().duplicate();
      }

      if (functionType.isDynamic()) {
        for (HaxeExpression expression : parameterExpressions) {
          handle(expression, context);
        }

        return functionType.withoutConstantValue().createHolder();
      }

      // @TODO: resolve the function type return type
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLiteralExpression) {
      return handle(element.getFirstChild(), context);
    }

    if (element instanceof HaxeStringLiteralExpression) {
      // @TODO: check if it has string interpolation inside, in that case text is not constant
      return SpecificHaxeClassReference.primitive(
        "String",
        element,
        HaxeStringUtil.unescapeString(element.getText())
      ).createHolder();
    }

    if (element instanceof HaxeExpressionList) {
      ArrayList<ResultHolder> references = new ArrayList<ResultHolder>();
      for (HaxeExpression expression : ((HaxeExpressionList)element).getExpressionList()) {
        references.add(handle(expression, context));
      }
      return HaxeTypeUnifier.unifyHolders(references, element);
    }

    if (element instanceof HaxeMapLiteral) {
      HaxeMapInitializerExpressionList listElement = ((HaxeMapLiteral)element).getMapInitializerExpressionList();
      List<HaxeExpression> initializers = new ArrayList<>();

      // In maps, comprehensions don't have expression lists, but they do have one single initializer.
      if (null == listElement) {
        HaxeMapInitializerForStatement forStatement = ((HaxeMapLiteral)element).getMapInitializerForStatement();
        HaxeMapInitializerWhileStatement whileStatement = ((HaxeMapLiteral)element).getMapInitializerWhileStatement();
        HaxeExpression fatArrow = null;
        while (null != forStatement || null != whileStatement) {
          if (null != forStatement) {
            fatArrow = forStatement.getMapInitializer();
            whileStatement = forStatement.getMapInitializerWhileStatement();
            forStatement = forStatement.getMapInitializerForStatement();
          } else {
            fatArrow = whileStatement.getMapInitializer();
            forStatement = whileStatement.getMapInitializerForStatement();
            whileStatement = whileStatement.getMapInitializerWhileStatement();
          }
        }
        if (null != fatArrow) {
          initializers.add(fatArrow);
        } else {
          LOG.error("Didn't find an initializer in a map comprehension: " + element.toString(),
                    new HaxeDebugUtil.InvalidValueException(element.toString() + '\n' + HaxeDebugUtil.elementLocation(element)));
        }
      } else {
        initializers.addAll(listElement.getExpressionList());
      }

      ArrayList<SpecificTypeReference> keyReferences = new ArrayList<>(initializers.size());
      ArrayList<SpecificTypeReference> valueReferences = new ArrayList<>(initializers.size());
      for (HaxeExpression ex : initializers) {
        HaxeFatArrowExpression fatArrow = (HaxeFatArrowExpression)ex;
        SpecificTypeReference keyType = handle(fatArrow.getFirstChild(), context).getType();
        keyReferences.add(keyType);
        SpecificTypeReference valueType = handle(fatArrow.getLastChild(), context).getType();
        valueReferences.add(valueType);
      }

      // XXX: Maybe track and add constants to the type references, like arrays do??
      //      That has implications on how they're displayed (e.g. not as key=>value,
      //      but as separate arrays).
      ResultHolder keyTypeHolder = HaxeTypeUnifier.unify(keyReferences, element).withoutConstantValue().createHolder();
      ResultHolder valueTypeHolder = HaxeTypeUnifier.unify(valueReferences, element).withoutConstantValue().createHolder();

      SpecificTypeReference result = SpecificHaxeClassReference.createMap(keyTypeHolder, valueTypeHolder);
      ResultHolder holder = result.createHolder();
      return holder;
    } // end HaxeMapLiteral

    if (element instanceof HaxeArrayLiteral) {
      HaxeExpressionList list = ((HaxeArrayLiteral)element).getExpressionList();

      // Check if it's a comprehension.
      if (list != null) {
        final List<HaxeExpression> list1 = list.getExpressionList();
        if (list1.isEmpty()) {
          final PsiElement child = list.getFirstChild();
          if ((child instanceof HaxeForStatement) || (child instanceof HaxeWhileStatement)) {
            return SpecificTypeReference.createArray(handle(child, context)).createHolder();
          }
        }
      }

      ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
      ArrayList<Object> constants = new ArrayList<Object>();
      boolean allConstants = true;
      if (list != null) {
        for (HaxeExpression expression : list.getExpressionList()) {
          SpecificTypeReference type = handle(expression, context).getType();
          if (!type.isConstant()) {
            allConstants = false;
          } else {
            constants.add(type.getConstant());
          }
          references.add(type);
        }
      }

      ResultHolder elementTypeHolder = references.isEmpty()
                                       ? SpecificTypeReference.getUnknown(element).createHolder()
                                       : HaxeTypeUnifier.unify(references, element).withoutConstantValue().createHolder();

      SpecificTypeReference result = SpecificHaxeClassReference.createArray(elementTypeHolder);
      if (allConstants) result = result.withConstantValue(constants);
      ResultHolder holder = result.createHolder();
      return holder;
    }

    if (element instanceof PsiJavaToken) {
      IElementType type = ((PsiJavaToken)element).getTokenType();

      if (type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITOCT) {
        return SpecificHaxeClassReference.primitive("Int", element, Long.decode(element.getText())).createHolder();
      } else if (type == HaxeTokenTypes.LITFLOAT) {
        return SpecificHaxeClassReference.primitive("Float", element, Double.parseDouble(element.getText())).createHolder();
      } else if (type == HaxeTokenTypes.KFALSE || type == HaxeTokenTypes.KTRUE) {
        return SpecificHaxeClassReference.primitive("Bool", element, type == HaxeTokenTypes.KTRUE).createHolder();
      } else if (type == HaxeTokenTypes.KNULL) {
        return SpecificHaxeClassReference.primitive("Dynamic", element, HaxeNull.instance).createHolder();
      } else {
        LOG.debug("Unhandled token type: " + type);
        return SpecificHaxeClassReference.getDynamic(element).createHolder();
      }
    }

    if (element instanceof HaxeSuperExpression) {
      /*
      LOG.debug("-------------------------");
      final HaxeExpressionList list = HaxePsiUtils.getChildWithText(element, HaxeExpressionList.class);
      LOG.debug(element);
      LOG.debug(list);
      final List<HaxeExpression> parameters = (list != null) ? list.getExpressionList() : Collections.<HaxeExpression>emptyList();
      final HaxeMethodModel method = HaxeJavaUtil.cast(HaxeMethodModel.fromPsi(element), HaxeMethodModel.class);
      if (method == null) {
        context.addError(element, "Not in a method");
      }
      if (method != null) {
        final HaxeMethodModel parentMethod = method.getParentMethod();
        if (parentMethod == null) {
          context.addError(element, "Calling super without parent constructor");
        } else {
          LOG.debug(element);
          LOG.debug(parentMethod.getFunctionType());
          LOG.debug(parameters);
          checkParameters(element, parentMethod.getFunctionType(), parameters, context);
          //LOG.debug(method);
          //LOG.debug(parentMethod);
        }
      }
      return SpecificHaxeClassReference.getVoid(element);
      */
      final HaxeMethodModel method = HaxeJavaUtil.cast(HaxeBaseMemberModel.fromPsi(element), HaxeMethodModel.class);
      final HaxeMethodModel parentMethod = (method != null) ? method.getParentMethod() : null;
      if (parentMethod != null) {
        return parentMethod.getFunctionType().createHolder();
      }
      context.addError(element, "Calling super without parent constructor");
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeIteratorExpression) {
      final List<HaxeExpression> list = ((HaxeIteratorExpression)element).getExpressionList();
      if (list.size() >= 2) {
        final SpecificTypeReference left = handle(list.get(0), context).getType();
        final SpecificTypeReference right = handle(list.get(1), context).getType();
        Object constant = null;
        if (left.isConstant() && right.isConstant()) {
          constant = new HaxeRange(
            HaxeTypeUtils.getIntValue(left.getConstant()),
            HaxeTypeUtils.getIntValue(right.getConstant())
          );
        }
        return SpecificHaxeClassReference.getIterator(SpecificHaxeClassReference.getInt(element)).withConstantValue(constant)
          .createHolder();
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeArrayAccessExpression) {
      final List<HaxeExpression> list = ((HaxeArrayAccessExpression)element).getExpressionList();
      if (list.size() >= 2) {
        final SpecificTypeReference left = handle(list.get(0), context).getType();
        final SpecificTypeReference right = handle(list.get(1), context).getType();
        if (left.isArray()) {
          Object constant = null;
          if (left.isConstant()) {
            List array = (List)left.getConstant();
            final HaxeRange constraint = right.getRangeConstraint();
            HaxeRange arrayBounds = new HaxeRange(0, array.size());
            if (right.isConstant()) {
              final int index = HaxeTypeUtils.getIntValue(right.getConstant());
              if (arrayBounds.contains(index)) {
                constant = array.get(index);
              } else {
                context.addWarning(element, "Out of bounds " + index + " not inside " + arrayBounds);
              }
            } else if (constraint != null) {
              if (!arrayBounds.contains(constraint)) {
                context.addWarning(element, "Out of bounds " + constraint + " not inside " + arrayBounds);
              }
            }
          }
          return left.getArrayElementType().getType().withConstantValue(constant).createHolder();
        }
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeFunctionLiteral) {
      HaxeFunctionLiteral function = (HaxeFunctionLiteral)element;
      HaxeParameterList params = function.getParameterList();
      if (params == null) {
        return SpecificHaxeClassReference.getInvalid(function).createHolder();
      }
      LinkedList<Argument> arguments = new LinkedList<>();
      ResultHolder returnType = null;
      context.beginScope();
      try {
        if (params instanceof HaxeOpenParameterList) {
          // Arrow function with a single, unparenthesized, parameter.
          HaxeOpenParameterList openParamList = ((HaxeOpenParameterList)params);

          // TODO: Infer the type from first usage in the function body.
          ResultHolder argumentType = SpecificTypeReference.getUnknown(function).createHolder();
          String argumentName = openParamList.getComponentName().getName();
          context.setLocal(argumentName, argumentType);
          arguments.add(new Argument(0, false, argumentType, argumentName));
        } else {
          List<HaxeParameter> list = params.getParameterList();
          for (int i = 0; i < list.size(); i++) {
            HaxeParameter parameter = list.get(i);
            ResultHolder argumentType = HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), function);
            String argumentName = parameter.getName();
            if (argumentName != null) {
              context.setLocal(argumentName, argumentType);
            }
            arguments.add(new Argument(i, parameter.getOptionalMark() != null, argumentType, argumentName));
          }
        }
        context.addLambda(context.createChild(function.getLastChild()));
        HaxeTypeTag tag = (function.getTypeTag());
        if (null != tag) {
          returnType = HaxeTypeResolver.getTypeFromTypeTag(tag, function);
        } else {
          // If there was no type tag on the function, then we try to infer the value:
          // If there is a block to this method, then return the type of the block.  (See PsiBlockStatement above.)
          // If there is not a block, but there is an expression, then return the type of that expression.
          // If there is not a block, but there is a statement, then return the type of that statement.
          HaxeBlockStatement block = function.getBlockStatement();
          if (null != block) {
            returnType = handle(block, context);
          } else if (null != function.getExpression()) {
            returnType = handle(function.getExpression(), context);
          } else {
            // Only one of these can be non-null at a time.
            PsiElement possibleStatements[] = {function.getDoWhileStatement(), function.getForStatement(), function.getIfStatement(),
              function.getReturnStatement(), function.getThrowStatement(), function.getWhileStatement()};
            for (PsiElement statement : possibleStatements) {
              if (null != statement) {
                returnType = handle(statement, context);
              }
            }
          }
        }
      }
      finally {
        context.endScope();
      }

      return new SpecificFunctionReference(arguments, returnType, null, function).createHolder();
    }

    if (element instanceof HaxeIfStatement) {
      PsiElement[] children = element.getChildren();
      if (children.length >= 1) {
        SpecificTypeReference expr = handle(children[0], context).getType();
        if (!SpecificTypeReference.getBool(element).canAssign(expr)) {
          context.addError(
            children[0],
            "If expr " + expr + " should be bool",
            new HaxeCastFixer(children[0], expr, SpecificHaxeClassReference.getBool(element))
          );
        }

        if (expr.isConstant()) {
          context.addWarning(children[0], "If expression constant");
        }

        if (children.length < 2) return SpecificHaxeClassReference.getUnknown(element).createHolder();
        PsiElement eTrue = null;
        PsiElement eFalse = null;
        eTrue = children[1];
        if (children.length >= 3) {
          eFalse = children[2];
        }
        SpecificTypeReference tTrue = null;
        SpecificTypeReference tFalse = null;
        if (eTrue != null) tTrue = handle(eTrue, context).getType();
        if (eFalse != null) tFalse = handle(eFalse, context).getType();
        if (expr.isConstant()) {
          if (expr.getConstantAsBool()) {
            if (tFalse != null) {
              context.addUnreachable(eFalse);
            }
          } else {
            if (tTrue != null) {
              context.addUnreachable(eTrue);
            }
          }
        }
        return HaxeTypeUnifier.unify(tTrue, tFalse, element).createHolder();
      }
    }

    if (element instanceof HaxeParenthesizedExpression) {
      return handle(element.getChildren()[0], context);
    }

    if (element instanceof HaxeTernaryExpression) {
      HaxeExpression[] list = ((HaxeTernaryExpression)element).getExpressionList().toArray(new HaxeExpression[0]);
      return HaxeTypeUnifier.unify(handle(list[1], context).getType(), handle(list[2], context).getType(), element).createHolder();
    }

    if (element instanceof HaxePrefixExpression) {
      HaxeExpression expression = ((HaxePrefixExpression)element).getExpression();
      if (expression == null) {
        return handle(element.getFirstChild(), context);
      } else {
        ResultHolder typeHolder = handle(expression, context);
        SpecificTypeReference type = typeHolder.getType();
        if (type.getConstant() != null) {
          String operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
          return type.withConstantValue(HaxeTypeUtils.applyUnaryOperator(type.getConstant(), operatorText)).createHolder();
        }
        return typeHolder;
      }
    }

    if (
      (element instanceof HaxeAdditiveExpression) ||
      (element instanceof HaxeBitwiseExpression) ||
      (element instanceof HaxeShiftExpression) ||
      (element instanceof HaxeLogicAndExpression) ||
      (element instanceof HaxeLogicOrExpression) ||
      (element instanceof HaxeCompareExpression) ||
      (element instanceof HaxeMultiplicativeExpression)
      ) {
      PsiElement[] children = element.getChildren();
      String operatorText;
      if (children.length == 3) {
        operatorText = children[1].getText();
        return HaxeOperatorResolver.getBinaryOperatorResult(
          element, handle(children[0], context).getType(), handle(children[2], context).getType(),
          operatorText, context
        ).createHolder();
      } else {
        operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
        return HaxeOperatorResolver.getBinaryOperatorResult(
          element, handle(children[0], context).getType(), handle(children[1], context).getType(),
          operatorText, context
        ).createHolder();
      }
    }

    LOG.debug("Unhandled " + element.getClass());
    return SpecificHaxeClassReference.getDynamic(element).createHolder();
  }

  static private void checkParameters(
    final PsiElement callelement,
    final HaxeMethodModel method,
    final List<HaxeExpression> arguments,
    final HaxeExpressionEvaluatorContext context
  ) {
    checkParameters(callelement, method.getFunctionType(), arguments, context);
  }

  static private void checkParameters(
    PsiElement callelement,
    SpecificFunctionReference ftype,
    List<HaxeExpression> parameterExpressions,
    HaxeExpressionEvaluatorContext context
  ) {
    List<Argument> parameterTypes = ftype.getArguments();
    int len = Math.min(parameterTypes.size(), parameterExpressions.size());
    for (int n = 0; n < len; n++) {
      ResultHolder type = parameterTypes.get(n).getType();
      HaxeExpression expression = parameterExpressions.get(n);
      ResultHolder value = handle(expression, context);

      if (!type.canAssign(value)) {
        context.addError(
          expression,
          "Can't assign " + value + " to " + type,
          new HaxeCastFixer(expression, value.getType(), type.getType())
        );
      }
    }

    //LOG.debug(ftype.getDebugString());
    // More parameters than expected
    if (parameterExpressions.size() > parameterTypes.size()) {
      for (int n = parameterTypes.size(); n < parameterExpressions.size(); n++) {
        context.addError(parameterExpressions.get(n), "Unexpected argument");
      }
    }
    // Less parameters than expected
    else if (parameterExpressions.size() < ftype.getNonOptionalArgumentsCount()) {
      context.addError(callelement, "Less arguments than expected");
    }
  }

  static private String getOperator(PsiElement element, TokenSet set) {
    ASTNode operatorNode = element.getNode().findChildByType(set);
    if (operatorNode == null) return "";
    return operatorNode.getText();
  }
}
