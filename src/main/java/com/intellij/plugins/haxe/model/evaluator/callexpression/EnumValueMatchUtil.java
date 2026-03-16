package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeEnumValueFieldModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext.countRequiredArguments;

public class EnumValueMatchUtil {


    public static @NotNull HaxeCallExpressionEvaluation evaluateMatchCall(HaxeCallExpressionContext context, HaxeCallExpressionEvaluation evaluation, boolean trackErrors) {
        checkPatternArguments(context, evaluation, trackErrors);
        return evaluation;
    }

    public static @NotNull HaxeCallExpressionEvaluation evaluatePatterns(HaxeCallExpressionContext context, HaxeCallExpressionEvaluation evaluation, boolean trackErrors) {
        checkPatterMatchingArgumentCount(context, evaluation, trackErrors);
        return evaluation;
    }

    private static void checkPatternArguments(HaxeCallExpressionContext context, HaxeCallExpressionEvaluation evaluation, boolean trackErrors) {
        for (CallExpressionArgumentModel argument : context.arguments) {
            PsiElement argElement = argument.getPsiElement();
            if(argElement instanceof HaxeReferenceExpression referenceExpression) {
                PsiElement resolve = referenceExpression.resolve();
                if(resolve instanceof HaxeEnumValueDeclarationField) {
                    continue; // OK
                }
                // got field when constructor expected
                if(resolve instanceof HaxeEnumValueDeclarationConstructor) {
                    evaluation.addError("Invalid match: Not enough patterns", argElement);
                    continue;
                }
                // check if field is in abstract enum
                if(resolve instanceof HaxeFieldDeclaration fieldDeclaration) {
                    if(fieldDeclaration.getModel() instanceof HaxeEnumValueFieldModel) {
                    continue; // OK
                    }
                }
                evaluation.addError("Unrecognized pattern", argElement);

            }else if (argElement instanceof HaxeCallExpression callExpression) {
                if(callExpression.getExpression() instanceof HaxeReferenceExpression callRef) {
                    PsiElement resolve = callRef.resolve();
                    if (!(resolve instanceof HaxeEnumValueDeclarationConstructor)) {
                        evaluation.addError("Unrecognized pattern", argElement);
                    }
                }
            }
        }
    }

    public static boolean isInsidePatternMatcher(@NotNull PsiElement reference) {
        HaxeCallExpression parentOfType = PsiTreeUtil.getParentOfType(reference.getParent(), HaxeCallExpression.class);
        if(parentOfType != null && parentOfType.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
            if(referenceExpression.resolve() instanceof HaxeMethod method) {
                return isPatternMatcher(method);
            }
        }
        return false;
    }

    public static boolean isPatternMatcher(HaxeMethod method) {
        if(method == null) return false;
        FullyQualifiedInfo info = method.getModel().getQualifiedInfo();
        return info != null
                && Objects.equals(info.memberName, "match")
                && Objects.equals(info.className, "EnumValue")
                && Objects.equals(info.moduleName, "EnumValue")
                && Objects.equals(info.packagePath, "");
    }

    public static HaxeCallExpressionEvaluation checkPatternMatchingOutsideMatchFunction(HaxeCallExpressionContext context, HaxeCallExpressionEvaluation evaluation, boolean trackErrors) {
        if (trackErrors) {
            for (CallExpressionArgumentModel argument : context.arguments) {
                if(argument.psiElement instanceof HaxeExtractorMatchExpression matchExpression) {
                    String message = "Pattern matching not allowed here";
                    evaluation.addError(message, matchExpression);
                    return evaluation.validationFailed();
                }
            }
        }
        return evaluation;
    }

    private static  HaxeCallExpressionEvaluation checkPatterMatchingArgumentCount(HaxeCallExpressionContext context, HaxeCallExpressionEvaluation evaluation,  boolean trackErrors) {
        int argumentCount = context.arguments.size();
        int parameterCount = context.parameters.size();
        int min = countRequiredArguments(context.parameters);

        // EnumValue pattern matching does not require all arguments, but minimum 1 (unless optional)
        if (argumentCount < Math.min(min,1)) {
            if (trackErrors) context.addToFewArgumentError(evaluation, min);
            return evaluation.validationFailed();
        }
        //max arg check
        if (argumentCount > parameterCount) {
            if (trackErrors) context.addToManyArgumentError(evaluation, parameterCount);
            return evaluation.validationFailed();
        }
        return evaluation;
    }


}
