/*
 * Copyright 2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.lang.util;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HaxeExpressionUtil {
  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  //static {LOG.setLevel(Level.DEBUG);}  // Remove when done debugging.

  private static final ThreadLocal<ArrayList<PsiElement>> constantsInProcess =
    new ThreadLocal<>().withInitial(()->new ArrayList<>());

  @Nullable
  public static IElementType getOperationType(@NotNull HaxeExpression expression) {
    final ASTNode token = expression.getNode().findChildByType(HaxeTokenTypeSets.UNARY_OPERATORS);
    if (token != null) return token.getElementType();
    return null;
  }

  public static boolean isIncrementDecrementOperation(@Nullable PsiElement element) {
    if (element instanceof HaxePrefixExpression) {
      final IElementType operationType = HaxeExpressionUtil.getOperationType((HaxePrefixExpression)element);
      return HaxeTokenTypeSets.UNARY_READ_WRITE_OPERATORS.contains(operationType);
    }
    else return element instanceof HaxeSuffixExpression;
  }

  public static boolean isOnAssignmentLeftHand(@NotNull HaxeExpression expr) {
    final PsiElement parent = PsiTreeUtil.skipParentsOfType(expr, HaxeParenthesizedExpression.class);
    return parent instanceof HaxeAssignExpression &&
           PsiTreeUtil.isAncestor(((HaxeAssignExpression)parent).getExpressionList().get(0), expr, false);
  }

  public static boolean isAccessedForWriting(@NotNull HaxeExpression expr) {
    if (isOnAssignmentLeftHand(expr)) return true;
    final PsiElement parent = UsefulPsiTreeUtil.skipParenthesizedExprUp(expr.getParent());
    return isIncrementDecrementOperation(parent);
  }

  public static boolean isAccessedForReading(@NotNull HaxeExpression expr) {
    final PsiElement parent = PsiTreeUtil.skipParentsOfType(expr, HaxeParenthesizedExpression.class);
    return !(parent instanceof HaxeAssignExpression) ||
           !PsiTreeUtil.isAncestor(((HaxeAssignExpression)parent).getExpressionList().get(0), expr, false) ||
           getAssignOperationElementType((HaxeAssignExpression)parent) != HaxeTokenTypes.OASSIGN;
  }

  @Nullable
  private static IElementType getAssignOperationElementType(@NotNull HaxeAssignExpression element) {
    final ASTNode token = element.getAssignOperation().getNode().findChildByType(HaxeTokenTypeSets.ASSIGN_OPERATORS);
    if (token != null) return token.getElementType();
    return null;
  }

  public static boolean isArrayExpression(HaxeExpression expr) {
    return expr instanceof HaxeArrayLiteral
           || expr instanceof HaxeArrayAccessExpression;
  }

  /**
   * Determine if an expression is a constant expression eligible to be used as the
   * right-hand-side of a "static inline var", such that the compiler will consider
   * the variable to be a constant usable as the default value in parameter lists.
   *
   * NOTE: This is likely going to be an arms race unless we match the algorithm in
   * the compiler.
   *
   * @param expr - the expression to test.
   * @return true, if the expression is considered constant; false, otherwise.
   */
  public static boolean isConstantExpression(HaxeExpression expr) {
    return ConstantClass.NOT_CONSTANT != classifyConstantExpression(expr);
  }

  /**
   * Resolve the the type of the constant, if it *is* a constant.
   * @return The ConstantClass, or NULL if the element is not constant.
   */
  private static ConstantClass classifyConstantExpression(HaxeExpression expr) {
    // Protect from circular references.
    if (constantsInProcess.get().contains(expr)) {
      return ConstantClass.NOT_CONSTANT;
    }
    constantsInProcess.get().add(expr);
    try {

      ConstantClass cc = innerClassifier(expr);
      return cc;

    } finally {
      constantsInProcess.get().remove(expr);
    }
  }

  private static ConstantClass innerClassifier(HaxeExpression expr) {

    if (null == expr) return ConstantClass.NOT_CONSTANT;

    // According to Simn:
    // Functions, arrays, maps, and anonymous objects are NOT considered constant
    // expressions.  The compiler ensures that "inline field == inline field" always
    // holds.  If arrays were allowed, e.g. "static inline var myconstant = []",
    // then "myconstant == myconstant" would come out as "[] == []" and that's false.

    // Compiler's check:
    //
    //    let type_function_arg_value ctx t c do_display =
    //        match c with
    //            | None -> None
    //            | Some e ->
    //                let p = pos e in
    //                let e = if do_display then Display.ExprPreprocessing.process_expr ctx.com e else e in
    //                let e = ctx.g.do_optimize ctx (type_expr ctx e (WithType.with_type t)) in
    //                unify ctx e.etype t p;
    //                let rec loop e = match e.eexpr with
    //                    | TConst _ -> Some e
    //                    | TField({eexpr = TTypeExpr _},FEnum _) -> Some e
    //                    | TCast(e,None) -> loop e
    //                    | _ ->
    //                         if ctx.com.display.dms_kind = DMNone || ctx.com.display.dms_inline && ctx.com.display.dms_error_policy = EPCollect then
    //                             display_error ctx "Parameter default value should be constant" p;
    //                         None
    //               in
    //               loop e

    // Maps, Arrays, Function Literals, Object, Anonymous, Block Statements are
    // Haxe LiteralExpressions.  Booleans, LITINT, LITHEX, LITOCT, LITFLOAT, true, false, null
    // are either HaxeLiteralExpressions OR HaxeConstantExpressions, depending upon
    // the rule that they matched in the BNF.
    if (is_type(expr, HaxeLiteralExpression.class)
    ||  is_type(expr, HaxeConstantExpression.class)) {
      PsiElement childElement = expr.getFirstChild();
      IElementType type = null != childElement ? childElement.getNode().getElementType() : null;
      if (null == type) {  // Optimize failure case.
        return ConstantClass.NOT_CONSTANT;
      }
      if (type == HaxeTokenTypes.KTRUE
          ||  type == HaxeTokenTypes.KFALSE) {
        return ConstantClass.BOOLEAN;
      }
      if (type == HaxeTokenTypes.KNULL) {
        return ConstantClass.NULL;
      }
      if (type == HaxeTokenTypes.LITFLOAT
          ||  type == HaxeTokenTypes.LITINT
          ||  type == HaxeTokenTypes.LITHEX
          ||  type == HaxeTokenTypes.LITOCT) {
        return ConstantClass.NUMERIC;
      }
      if (type == HaxeTokenTypes.STRING_LITERAL_EXPRESSION) {
        return ConstantClass.STRING;
      }
      if (type == HaxeTokenTypes.REGULAR_EXPRESSION_LITERAL) {
        return ConstantClass.REGEX;
      }
      return ConstantClass.NOT_CONSTANT;
    }

    if (is_type(expr, HaxeRegularExpression.class)) { // Is also a HaxeLiteralExpression, so must come first.
      return ConstantClass.REGEX;
    }

    if (is_type(expr, HaxeStringLiteralExpression.class)) {
      // If there are $vars in string, then they are not constants.
      // It doesn't matter whether $var points to a constant, the string is still not constant.
      return ((HaxeStringLiteralExpression)expr).getLongTemplateEntryList().isEmpty() &&
             ((HaxeStringLiteralExpression)expr).getShortTemplateEntryList().isEmpty()
             ? ConstantClass.STRING
             : ConstantClass.NOT_CONSTANT;
    }

    if (is_type(expr, HaxeParenthesizedExpression.class)) {
      return classifyConstantExpression(((HaxeParenthesizedExpression)expr).getExpression());
    }

    // Unary expressions.
    if (is_type(expr, HaxePrefixExpression.class)) {
      // Only, negation ('-'), not ('!'), and bitwise complement ('~') are allowed.
      PsiElement child = expr.getFirstChild();
      IElementType type = null != child ? child.getNode().getElementType() : null;
      if (HaxeTokenTypes.OMINUS.equals(type) ||
          HaxeTokenTypes.ONOT.equals(type) ||
          HaxeTokenTypes.OCOMPLEMENT.equals(type)) {
        return classifyConstantExpression(((HaxePrefixExpression)expr).getExpression());
      }
      return ConstantClass.NOT_CONSTANT;
    }
    // Binary expressions.
    if (is_type(expr, HaxeAdditiveExpression.class)) return areConstant(((HaxeAdditiveExpression)expr).getExpressionList());
    if (is_type(expr, HaxeBitwiseExpression.class)) return areConstant(((HaxeBitwiseExpression)expr).getExpressionList());
    if (is_type(expr, HaxeCompareExpression.class)) return areConstant(((HaxeCompareExpression)expr).getExpressionList());
    if (is_type(expr, HaxeLogicAndExpression.class)) return areConstant(((HaxeLogicAndExpression)expr).getExpressionList());
    if (is_type(expr, HaxeLogicOrExpression.class)) return areConstant(((HaxeLogicOrExpression)expr).getExpressionList());
    if (is_type(expr, HaxeMultiplicativeExpression.class)) return areConstant(((HaxeMultiplicativeExpression)expr).getExpressionList());
    if (is_type(expr, HaxeShiftExpression.class)) return areConstant(((HaxeShiftExpression)expr).getExpressionList());

    // Ternary expression.
    if (is_type(expr, HaxeTernaryExpression.class)) {
      // Even Simple ternaries with static inline comparisions fail compilation.
      // e.g.: static inline var c = 1 > 0 ? true : false;
      return ConstantClass.NOT_CONSTANT;
    }

    if (is_type(expr, HaxeCastExpression.class)) {
      // Casts without a target type are allowed.
      HaxeCastExpression cast = (HaxeCastExpression)expr;
      return null == cast.getFunctionType() &&
             null == cast.getTypeOrAnonymous()
             ? ConstantClass.NOT_CONSTANT
             : classifyConstantExpression(cast.getExpression());
    }

    if (is_type(expr, HaxeReferenceExpression.class)) {
      HaxeReferenceExpression ref = (HaxeReferenceExpression)expr;
      PsiElement resolved = ref.resolve();
      if (resolved instanceof HaxeEnumValueDeclaration) { // sub-class of HaxeFieldDeclaration
        // Only empty (non-parameterized) enumerations are constant.
        HaxeParameterList parameterList = ((HaxeEnumValueDeclaration)resolved).getParameterList();
        List<HaxeParameter> params = null == parameterList ? null : parameterList.getParameterList();
        return null == params || params.isEmpty()
               ? ConstantClass.ENUMERATION
               : ConstantClass.NOT_CONSTANT;
      }
      if (resolved instanceof HaxeFieldDeclaration) {
        HaxeFieldDeclaration fieldDeclaration = (HaxeFieldDeclaration)resolved;
        HaxeFieldModel fieldModel = new HaxeFieldModel(fieldDeclaration);
        return classifyConstantExpression(fieldModel.getInitializerExpression());
      }
      return resolved instanceof HaxeExpression
             ? classifyConstantExpression((HaxeExpression)resolved)
             : ConstantClass.NOT_CONSTANT;
    }

    if (is_type(expr, HaxeCallExpression.class)) {
      // Any arguments that are non-constant make the call non-constant.
      ConstantClass classification = classifyCallExpression((HaxeCallExpression)expr);
      if (ConstantClass.NOT_CONSTANT != classification) {
        HaxeExpressionList params = ((HaxeCallExpression)expr).getExpressionList();
        List<HaxeExpression> expressions = params.getExpressionList();
        for (HaxeExpression expression : expressions) {
          if (!isConstantExpression(expression)) {
            return ConstantClass.NOT_CONSTANT;
          }
        }
      }
      return classification;
    }

    if (is_type(expr, HaxeFunctionLiteral.class)) {
      // Function literals are constants, they just don't necessarily return a
      // constant when called.
      return ConstantClass.FUNCTION;
    }

    LOG.debug("Unexpected expression type being checked for constant-ness:" + expr);
    return ConstantClass.NOT_CONSTANT;
  }

  private static ConstantClass areConstant(List<HaxeExpression> exprList) {
    // Need to check that each is a constant, _and_ that they are of compatible (assignable) types.
    // Luckily, only numeric values are compatible, so we only have to check that each type is numeric, if the
    // types are different.

    // The good news is that we don't need a full resolve.  We can just check the expression types.

    if (null == exprList || exprList.isEmpty()) return ConstantClass.NOT_CONSTANT;

    ConstantClass firstType = null;
    for (HaxeExpression expr : exprList) {
      ConstantClass type = classifyConstantExpression(expr);
      if (type == ConstantClass.NOT_CONSTANT) {
        return ConstantClass.NOT_CONSTANT;
      }
      if (null == firstType) {
        firstType = type;
      } else {
        if (!type.isCompatibleWith(firstType)) {
          return ConstantClass.NOT_CONSTANT;
        }
      }
    }
    return null == firstType ? ConstantClass.NOT_CONSTANT : firstType;
  }

  @NotNull
  private static ConstantClass classifyCallExpression(HaxeCallExpression callExpr) {
    // According to Simn: Some functions are optimized in the compiler if they
    // have static arguments: Type.enumIndex, String.fromCharCode,
    // Std.string, Std.int, Math.ceil, Math.floor, Std.is.

    HaxeExpression expr = callExpr.getExpression();
    PsiReference ref = expr.getReference();
    PsiElement resolved = null != ref ? ref.resolve() : null;
    String methodName = resolved instanceof HaxeMethod ? ((HaxeMethod)resolved).getName() : null;

    PsiElement parentClass = UsefulPsiTreeUtil.getParentOfType(resolved, HaxeClass.class);
    String className = parentClass instanceof HaxeClass ? ((HaxeClass)parentClass).getName() : null;

    if ("Std".equals(className)) {

      if ("int".equals(methodName)) return ConstantClass.NUMERIC;
      if ("string".equals(methodName)) return ConstantClass.STRING;
      if ("is".equals(methodName)) return ConstantClass.BOOLEAN;

    } else if ("Type".equals(className)) {

      if ("enumIndex".equals(methodName)) return ConstantClass.NUMERIC;

    } else if ("String".equals(className)) {

      if ("fromCharCode".equals(methodName)) return ConstantClass.STRING;

    } else if ("Math".equals(className)) {

      if ("ceil".equals(methodName)) return ConstantClass.NUMERIC;
      if ("floor".equals(methodName)) return ConstantClass.NUMERIC;

    }

    return ConstantClass.NOT_CONSTANT;
  }

  enum ConstantClass {
    NOT_CONSTANT,
    NUMERIC,
    STRING,
    REGEX,
    NULL,
    BOOLEAN,
    ENUMERATION,
    FUNCTION;

    public boolean isCompatibleWith(ConstantClass right) {
      switch (this) {
        case NOT_CONSTANT:  return false;
        case NULL:          return false; // Never compatible in combinations.
        case STRING:
        case REGEX:
          switch (right) {
            case STRING:
            case REGEX:     return true;
            default:        return false;
          }
        default:
          return this == right;
      }
    }
  }

  private static <T> boolean is_type(final PsiElement element, final Class<T> clazz) {
    if (clazz.isInstance(element)) {
      LOG.debug(()->"Element " + element + " is a " + clazz);
      return true;
    }
    LOG.debug(()->"Element " + element + "is not a " +clazz);
    return false;
  }

}
