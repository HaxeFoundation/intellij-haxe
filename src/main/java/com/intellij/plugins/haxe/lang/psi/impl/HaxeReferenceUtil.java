package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;
import java.util.Optional;

import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.CLASS;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.ENUM;

public class HaxeReferenceUtil {

    public static boolean isStaticExtension(HaxeReferenceExpression referenceExpression) {
                PsiElement method = referenceExpression.resolve();
                if (method instanceof HaxeMethodDeclaration haxeMethod) {
                    // a few fast checks before we dive into using imports
                    if (!haxeMethod.isStatic()) return false; // only static methods can be extensions (compiler: Cannot access static field XXX from a class instance)
                    if (haxeMethod.getParameterList().isEmpty()) return false; // must have minimum 1 parameter
                    PsiElement ChainBeforeMethod = referenceExpression.getChildren()[0];
                    if (ChainBeforeMethod instanceof HaxeIdentifier) return false; // not chain, got method identifier

                    // check the important part, was this reference imported with using statement (or one of the compiler included using refs)
                    if (ChainBeforeMethod instanceof HaxeReferenceExpressionImpl parentReferenceExpression) {
                        PsiElement caller = parentReferenceExpression.resolve();
                        if (caller == method) return false; // probably a function bind or similar

                        ResultHolder callerType = HaxeExpressionEvaluator.evaluateWithRecursionGuard(parentReferenceExpression).result;

                        SpecificHaxeClassReference classType = callerType.getClassType();
                        if(classType != null && !classType.isUnknown()) {
                            HaxeClass haxeClass = classType.getHaxeClass();
                            // checking if references starts with a class references.
                            // staticExtensions are allowed on classes (if parameter is Class<T>/Enum<T>)
                            boolean callieIsAClass = haxeClass != null && caller == haxeClass && parentReferenceExpression.isClassReferenceOf(haxeClass);




                            HaxeClassModel haxeClassModel = classType.getHaxeClassModel();
                            if(haxeClassModel  != null) {

                                // check if callie has @:using
                                List<HaxeMethodModel> extensionMethodsFromMeta = haxeClassModel.getExtensionMethodsFromMeta();
                                Optional<HaxeMethodModel> extensionMethodFromMeta = extensionMethodsFromMeta.stream()
                                        .filter(model -> model.getBasePsi() == haxeMethod).findFirst();

                                if(extensionMethodFromMeta.isPresent()) {
                                    HaxeMethodModel extModel = extensionMethodFromMeta.get();
                                    List<HaxeParameterModel> parameters = extModel.getParameters();
                                    if(!parameters.isEmpty()) {
                                        // Extension methods on Class references is allowed but param type must be Class<T>.
                                        // we check this here so we don't accidentally treat a static method as a static extension.
                                        if (callieIsAClass) {
                                            ResultHolder type = parameters.getFirst().getType();
                                            SpecificHaxeClassReference paramClass = type.getClassType();
                                            if (paramClass != null) {
                                                return paramClass.isEnumClass() || paramClass.isClass();
                                            }
                                        }else {
                                            return true;
                                        }
                                    }
                                }

                                // make sure  there's no method on callie type with the same name
                                HaxeBaseMemberModel member = haxeClassModel.getMember(haxeMethod.getName(), null);
                                if (member != null) return false;
                            }
                        }

                        boolean defaultExtension = HaxeResolveUtil.isDefaultExtension(haxeMethod);
                        if(defaultExtension) return true;

                        boolean inUsingImports = HaxeResolveUtil.isInUsingImports(referenceExpression, haxeMethod);
                        if(inUsingImports) return true;

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

    public static boolean isCaptureVar(HaxeReferenceExpression expression) {
        PsiElement resolved = expression.resolve();
        if(resolved != null) {
            HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(expression, HaxeSwitchStatement.class);
            if (switchStatement != null) {
                HaxeExpression switchStatementExpression = switchStatement.getExpression();
                return resolved == switchStatementExpression || PsiTreeUtil.isAncestor(switchStatementExpression, resolved, true);
            }
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
