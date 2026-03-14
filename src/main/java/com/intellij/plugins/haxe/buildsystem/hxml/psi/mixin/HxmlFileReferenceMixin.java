package com.intellij.plugins.haxe.buildsystem.hxml.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HxmlFileReferenceMixin extends HxmlReference implements PsiReference {

    public HxmlFileReferenceMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement resolve() {
        return resolveFileReference();
    }

    @Override
    public PsiReference getReference() {
        return this;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if(element instanceof PsiFile file) {
            return file == resolveFileReference();
        }
        return false;
    }

    public @Nullable PsiFile resolveFileReference() {
        String fileName = getText();
        VirtualFile parentDir = getContainingFile().getVirtualFile().getParent();
        String canonicalPath = parentDir.getCanonicalPath() +"/"+ fileName;

        VirtualFileSystem fileSystem = parentDir.getFileSystem();
        VirtualFile resolvedFile = fileSystem.findFileByPath(canonicalPath);
        if(resolvedFile != null) {
            return PsiManager.getInstance(this.getProject()).findFile(resolvedFile);
        }
        return null;
    }
}

