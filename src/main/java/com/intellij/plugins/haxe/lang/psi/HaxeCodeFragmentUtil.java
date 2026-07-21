package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class HaxeCodeFragmentUtil {

    // used to check if we are in a code fragment, a psi element typically used for expression evaluation when debuggig etc.
    public static boolean isInCodeFragment(PsiElement psiElement) {
        HaxeExpressionCodeFragment condFragment = PsiTreeUtil.getParentOfType(psiElement, HaxeExpressionCodeFragment.class);
        return condFragment != null;
    }
}
