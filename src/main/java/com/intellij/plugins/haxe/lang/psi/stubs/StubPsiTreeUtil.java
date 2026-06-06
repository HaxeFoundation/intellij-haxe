package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

import static com.intellij.psi.util.PsiTreeUtil.findChildrenOfAnyType;
import static com.intellij.psi.util.PsiTreeUtil.getChildrenOfAnyType;

public class StubPsiTreeUtil {

    public static <T extends PsiElement> T getStubOrPsiParentOfType(PsiElement element, Class<? extends T>... classes) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            for (Class<? extends T> aClass : classes) {
                if (aClass.isInstance(parent)) return (T)parent;
            }
            if (parent instanceof StubBasedPsiElement<?> stubBase) {
                StubElement<?> stub = stubBase.getStub();
                if (stub != null) {
                    StubElement<?> parentStub = stub.getParentStub();
                    parent = parentStub.getPsi();
                    continue;
                }
            }
            parent = parent.getParent();
        }
        return null;
    }
    public static <T extends PsiElement> T getStubOrPsiParentOfType(PsiElement element, Class<? extends T> cls, boolean strict) {
        PsiElement parent =  strict ? element.getParent() : element;
        while (parent != null) {
            if (cls.isInstance(parent)) return (T)parent;
            if (parent instanceof StubBasedPsiElement<?> stubBase) {
                StubElement<?> stub = stubBase.getStub();
                if (stub != null) {
                    return stub.getParentStubOfType(cls);
                }
            }
            parent = parent.getParent();
        }
        return null;
    }

    public static @Unmodifiable @NotNull <T extends PsiElement> List<T> getStubChildrenOfAnyType(@Nullable PsiElement element, @NotNull Class<? extends T> @NotNull ... classes) {
        if (element == null) return Collections.emptyList();

        StubElement<?> stub = element instanceof StubBasedPsiElement ? ((StubBasedPsiElement<?>)element).getStub() : null;
        if (stub == null) return getChildrenOfAnyType(element, classes);

        List<T> result = new SmartList<>();
        for (StubElement<?> childStub : stub.getChildrenStubs()) {
            PsiElement child = childStub.getPsi();
            if (PsiTreeUtil.instanceOf(child, classes)) {
                result.add((T)child);
            }
        }
        return result;
    }

}
