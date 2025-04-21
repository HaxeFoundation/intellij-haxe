package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeIntroduceUtil {
    public static @NotNull PsiElement findInsertAfterElementForMethod(@NotNull PsiElement startElement, HaxeClass aClass, boolean readOnly) {
        // try to add after method if element is inside a method declaration and belongs to same class
        if(PsiTreeUtil.isAncestor(aClass, startElement, true)) {
            HaxeMethodDeclaration parentMethod = PsiTreeUtil.getParentOfType(startElement, HaxeMethodDeclaration.class, HaxeModuleMethodDeclaration.class);
            if(parentMethod != null) return parentMethod;
        }
        // else if in class, find where to put it
        if (aClass != null) {
            if (readOnly) aClass = copyFileAndReturnClonedPsiElement(aClass);
            // method after the last method in class
            List<HaxeMethod> methodList = aClass.getHaxeMethodsSelf(null);
            if (!methodList.isEmpty()) return methodList.getLast();

            // else add method at the very end of class body
            PsiElement rBrace = aClass.getRBrace();
            if (rBrace != null) {
                PsiElement prevSibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(rBrace, true);
                if(prevSibling != null)return prevSibling;
            }
        }
        // if no success on any of the above, append as last item in the module
        HaxeModule module = PsiTreeUtil.getParentOfType(startElement, HaxeModule.class);
        return module.getLastChild();
    }



    public static @NotNull PsiElement findInsertBeforeElementFunction(@NotNull PsiElement startElement, HaxeBlockStatement block) {
        PsiElement insertBeforeElement = startElement;
        PsiElement parent = startElement.getParent();

        while (parent != null && parent != block) {
            insertBeforeElement = parent;
            parent = parent.getParent();
        }
        return insertBeforeElement;
    }

    public static @NotNull PsiElement findInsertBeforeElementForVariable(@NotNull PsiElement startElement, HaxeBlockStatement block) {
        PsiElement insertBeforeElement = startElement;
        PsiElement parent = startElement.getParent();

        while (parent != null && parent != block) {
            insertBeforeElement = parent;
            parent = parent.getParent();
        }
        return insertBeforeElement;
    }


    public static <T extends PsiElement> T copyFileAndReturnClonedPsiElement(T psiElement) {
        PsiFile originalFile = psiElement.getContainingFile();
        PsiFile fileCopy = (PsiFile) originalFile.copy();
        PsiElement element = fileCopy.findElementAt(psiElement.getTextOffset());
        while (element != null
               && (!element.getTextRange().equals(psiElement.getTextRange())
                   || element.getClass() != psiElement.getClass())
        ) {
            element = element.getParent();
        }
        return (T) element;
    }
}
