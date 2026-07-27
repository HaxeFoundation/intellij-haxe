package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class HaxeCodeFragmentUtil {

    // checks for a code fragment, a psi element typically used for expression evaluation when debugging etc.
    public static boolean isInCodeFragment(PsiElement psiElement) {
        HaxeExpressionCodeFragment condFragment = PsiTreeUtil.getParentOfType(psiElement, HaxeExpressionCodeFragment.class);
        return condFragment != null;
    }
}
