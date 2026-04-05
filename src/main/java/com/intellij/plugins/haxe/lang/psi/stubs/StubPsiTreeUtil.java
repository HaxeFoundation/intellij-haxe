package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.StubElement;

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

}
