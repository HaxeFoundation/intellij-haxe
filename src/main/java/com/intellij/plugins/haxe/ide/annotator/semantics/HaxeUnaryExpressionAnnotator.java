package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeUnaryExpressionAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if(!element.isValid()) return;

        if (element instanceof HaxeUnaryExpression postfixExpression) {
            checkPostfixExpression(postfixExpression, holder);
        }
    }

    private void checkPostfixExpression(@NotNull HaxeUnaryExpression unaryExpression, @NotNull AnnotationHolder holder) {
        // ignore if  inside meta, ex @:op(A++)
        if (unaryExpression.getParent() instanceof HaxeCompiletimeMetaArg) return;

        HaxeOperator operator = unaryExpression.getOperator();
        HaxeExpression expression = unaryExpression.getExpression();

        if (operator.textMatches("++") || operator.textMatches("--")) {

            if (expression instanceof HaxeLiteralExpression || expression instanceof HaxeStringLiteralExpression) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Invalid assign").range(unaryExpression).create();
                return;
            }
            if (expression instanceof HaxeCallExpression ) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Invalid assign").range(unaryExpression).create();
                return;
            }
            if (expression instanceof HaxeNewExpression  newExpression) {
                ResultHolder resultHolder = HaxeTypeResolver.getTypeFromType(newExpression.getType());
                if (!resultHolder.isUnknown()) {
                    // abstract types can have operator overloads, a "new BigInt(0)++" (abstract) might be allowed
                    // but other classes does not support that and  "a new MyClass()++" does not make sense
                    if(!resultHolder.getType().isAbstractType()) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Invalid assign").range(unaryExpression).create();
                    }
                }
                return;
            }


            if (expression instanceof HaxeArrayAccessExpression) {
                ResultHolder result = HaxeExpressionEvaluator.evaluate(expression).result;
                checkIfPostOrPrefixIsValid(unaryExpression, holder, result);
                return;

            }
            if (expression instanceof HaxeReferenceExpression referenceExpression) {
                ResultHolder result = HaxeExpressionEvaluator.evaluate(expression).result;

                boolean isWritable = checkIfPropertyWritable(referenceExpression);
                if (!isWritable) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "This expression cannot be accessed for writing")
                            .range(unaryExpression)
                            .create();
                    return;
                }

                // shortcut for common types
                SpecificTypeReference type = result.getType();
                if (!result.isImmutable() && isWritable && (type.isInt() || type.isFloat() || type.isSingle())) return;

                checkIfPostOrPrefixIsValid(unaryExpression, holder, result);
            }
        }
    }

    private static void checkIfPostOrPrefixIsValid(@NotNull HaxeUnaryExpression unaryExpression, @NotNull AnnotationHolder holder, @NotNull ResultHolder result) {
        if (result.isUnknown()) return;

        SpecificTypeReference type = result.getType();

        //  resolve is typedef before checking
        if(result.isTypeDef() && result.getClassType() != null) {
            type = result.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
        }

        if (result.isImmutable()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Cannot assign to immutable reference").range(unaryExpression).create();
        } else if (!type.isAbstractType()) {
            holder.newAnnotation(HighlightSeverity.ERROR, type.toPresentationString() + " should be Int").range(unaryExpression).create();
        } else {

            if (type.isBool()) {
                holder.newAnnotation(HighlightSeverity.ERROR, "This expression cannot be accessed for writing").range(unaryExpression).create();
            }
            if (type.isString()) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Invalid assign").range(unaryExpression).create();
            }
            if (!type.isAbstractType()) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Invalid assign").range(unaryExpression).create();
            }
            if(type instanceof SpecificHaxeClassReference classReference && classReference.isAbstractType()) {
                if (!classReference.isCoreType()) {
                    List<HaxeMethodModel> overloads = classReference.getOperatorOverloads(unaryExpression.getOperator());
                    if (overloads.isEmpty()) {
                        String operator = unaryExpression.getOperator().getText();
                        holder.newAnnotation(HighlightSeverity.ERROR, "No overload for " + operator + " found").range(unaryExpression).create();
                    }
                }
            }
        }
    }

    private static boolean isNonRealVariableWrite(HaxeReferenceExpression referenceExpression) {
        PsiElement refResolve = referenceExpression.resolve();
        if (refResolve instanceof HaxeFieldDeclaration declaration) {
            if (declaration.getModel() instanceof HaxeFieldModel model && model.isProperty()) {

                HaxeMethodModel setterMethod = model.getSetterMethod();
                HaxeMethodModel getterMethod = model.getGetterMethod();
                boolean inAccessor = PsiTreeUtil.isAncestor( setterMethod != null ? setterMethod.getMethodPsi() : null, referenceExpression, true)
                                     || PsiTreeUtil.isAncestor(getterMethod != null ? getterMethod.getMethodPsi() : null, referenceExpression, true) ;
                // physical fields can be accessed from inside field accessors
                // this check checks if we try to write to a non-physical/real variable
                if (!model.isWritableFromPropertyAccessor() && inAccessor) return true;
            }
        }
        return false;
    }
    private static boolean checkIfPropertyWritable(HaxeReferenceExpression referenceExpression) {
        PsiElement refResolve = referenceExpression.resolve();
        if (refResolve instanceof HaxeFieldDeclaration declaration) {
            if (declaration.getModel() instanceof HaxeFieldModel model && model.isProperty()) {
                if (model.isWritableFromOutside()) return true;

                boolean sameScope = isInsideSameClassOrFile(referenceExpression, declaration);
                return model.isWritableFromInside() && sameScope;
            }
        }
        return true;
    }

    private static boolean isInsideSameClassOrFile(HaxeReferenceExpression referenceExpression, HaxeFieldDeclaration declaration) {
        HaxeClass haxeClass = PsiTreeUtil.getStubOrPsiParentOfType(referenceExpression, HaxeClass.class);
        if (haxeClass != null) return haxeClass == declaration.getContainingClass();

        HaxeModule haxeModule = PsiTreeUtil.getStubOrPsiParentOfType(referenceExpression, HaxeModule.class);
        if (haxeModule != null) return haxeModule.getContainingFile() == declaration.getContainingFile();

        return false;
    }
}
