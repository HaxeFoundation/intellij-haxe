package com.intellij.plugins.haxe.buildsystem.hxml.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

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
        VirtualFile currentFile = getContainingFile().getVirtualFile();
        VirtualFileSystem fileSystem = currentFile.getFileSystem();

        String relativePath = getRelativeCanonicalPath(currentFile);
        if(relativePath != null) {
            VirtualFile resolvedFile = fileSystem.findFileByPath(relativePath);
            if (resolvedFile != null) {
                return PsiManager.getInstance(this.getProject()).findFile(resolvedFile);
            }
        }
        // the workdir is usually project root so imported hxml files from subfolders
        // will likely resolve from this directory
        String rootlPath = getProjectRootCanonicalPath(currentFile);
        if(rootlPath != null) {
            VirtualFile resolvedFile = fileSystem.findFileByPath(rootlPath);
            if (resolvedFile != null) {
                return PsiManager.getInstance(this.getProject()).findFile(resolvedFile);
            }
        }

        return null;
    }

    private @Nullable String getRelativeCanonicalPath(VirtualFile currentFile) {
        String fileName = getText();
        VirtualFile parentDir = currentFile.getParent();
        if(parentDir != null) {
          return parentDir.getCanonicalPath() + "/" + fileName;
        }
        return null;
    }

    private @Nullable String getProjectRootCanonicalPath(VirtualFile currentFile) {
        String fileName = getText();
        VirtualFile baseDir = ProjectUtil.guessProjectDir(getProject());
        if(baseDir != null) {
            return baseDir.getCanonicalPath() + "/" + fileName;
        }
        return null;
    }
}

