package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO find optimal way to do SearchScope (test LocalSearchScope vs RangeLocalSearchScope )
// if it lags  try adding checkCanceled() to custom scope class
public class HaxeExpressionEvaluatorSearchUtil {
    public static LocalSearchScope getSmallestPossibleSearchScope(PsiElement element, @Nullable PsiElement suggestedSearchScope) {
        PsiElement parent = element.getParent();
        if (parent instanceof HaxeLocalVarDeclarationList
            || parent instanceof HaxeLocalVarDeclaration
        ) {
            PsiElement scopePsi = findBlock(parent);
            if (scopePsi != null) return createScope(scopePsi);
//            if (scopePsi != null) return new RangeLocalSearchScope(element, scopePsi);
        }
        if (parent instanceof HaxeFieldDeclaration fieldDeclaration) {
            PsiClass scopePsi = fieldDeclaration.getContainingClass();
            if (scopePsi != null) return createScope(scopePsi);
//            if (containingClass != null) return scope(containingClass.getScope());
        }

        if (parent instanceof HaxeObjectLiteralElement) {
            PsiElement scopePsi = findBlock(parent);
            if (scopePsi != null) return createScope(scopePsi);
//            if (block != null) return new RangeLocalSearchScope(element, block);
        }

        if(parent instanceof HaxeParameter) {
            PsiElement scopePsi = findBlock(parent);
            if (scopePsi != null) return createScope(scopePsi);
//            if (block != null) return new RangeLocalSearchScope(element, block);
        }

        // if no custom rules use suggested scope
        if (suggestedSearchScope != null) {
            return  createScope(suggestedSearchScope);
        }

        // failed to find local scope, and no suggestions, we have to search the entire file
        return  createScope(element.getContainingFile());
    }

    private static @Nullable HaxePsiCompositeElement findBlock(PsiElement parent) {
        return PsiTreeUtil.getParentOfType(parent, HaxeSwitchCaseBlock.class, HaxeBlockStatement.class, HaxeMethod.class);
    }

    private static @NotNull LocalSearchScope createScope(PsiElement block) {
        return new LocalSearchScope(block);
    }
}
