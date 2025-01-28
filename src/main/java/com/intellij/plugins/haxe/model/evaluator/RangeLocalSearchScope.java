package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.scope.RangeBasedLocalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RangeLocalSearchScope extends RangeBasedLocalSearchScope {

    final PsiElement [] elements;
    final TextRange [] ranges;


    public RangeLocalSearchScope(PsiElement element, PsiElement parent) {
        super("", true);
        List<? super @NotNull PsiElement> list = new ArrayList<>();
        int start = element.getTextRange().getEndOffset();
        int end = parent.getTextRange().getEndOffset();

        collectPsiElementsAtRange(element.getContainingFile(), list, start, end);

        this.elements = list.toArray(PsiElement.EMPTY_ARRAY);
        this.ranges = new TextRange[] {new TextRange(start,end)};
    }

    @Override
    public @NotNull TextRange @NotNull [] getRanges(@NotNull VirtualFile file) {
        return ranges;
    }

    @Override
    protected @NotNull PsiElement @NotNull [] getPsiElements() {
         return elements;
    }
}
