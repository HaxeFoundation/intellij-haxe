package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.CLASS;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.ENUM;

public class HaxeReferenceUtil {

    public static boolean isStaticExtension(HaxeReferenceExpression referenceExpression) {
                PsiElement method = referenceExpression.resolve();
                if (method instanceof HaxeMethod haxeMethod) {
                    if (!haxeMethod.isStatic()) return false; // only static methods can be extensions (compiler: Cannot access static field XXX from a class instance)

                    PsiElement ChainBeforeMethod = referenceExpression.getChildren()[0];
                    if (ChainBeforeMethod instanceof HaxeIdentifier) return false; // not chain, got method identifier
                    if (ChainBeforeMethod instanceof HaxeReferenceExpression referenceExpression1) {
                        PsiElement caller = referenceExpression1.resolve();
                        if (caller == method) return false; // probably a function bind or similar
                        ResultHolder callerType = HaxeExpressionEvaluator.evaluateWithRecursionGuard(referenceExpression1).result;
                        if(callerType.getClassType() != null) {
                            HaxeClassModel haxeClassModel = callerType.getClassType().getHaxeClassModel();
                            if(haxeClassModel  != null) {
                                HaxeBaseMemberModel member = haxeClassModel.getMember(((HaxeMethod) method).getName(), null);
                                if (member == null) return true;
                            }
                        }
                        return !(caller instanceof HaxeClass || caller instanceof HaxeImportAlias);
                    }else {
                        return true;
                    }
                }
        return false;
    }

    public static boolean isStaticExtension(HaxeCallExpression callExpression) {
        PsiReference referenceChain = callExpression.getFirstChild().getReference();
        if (referenceChain instanceof HaxeReferenceExpression referenceExpression) {
            return isStaticExtension(referenceExpression);
        }
        return false;
    }


    public static ResultHolder wrapTypeInClassOrEnum(PsiElement element, HaxeClass haxeClass) {
        // wrap in Class<> or Enum<>
        SpecificHaxeClassReference originalClass = SpecificHaxeClassReference.withoutGenerics(haxeClass.getModel().getReference());
        SpecificHaxeClassReference wrappedClass =
                SpecificHaxeClassReference.getStdClass(haxeClass.isEnum() ? ENUM : CLASS, element,
                        new ResultHolder[]{new ResultHolder(originalClass)});
        return wrappedClass.createHolder();
    }
}
