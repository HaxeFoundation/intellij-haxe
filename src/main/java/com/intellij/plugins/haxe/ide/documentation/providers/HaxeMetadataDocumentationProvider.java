package com.intellij.plugins.haxe.ide.documentation.providers;

import com.intellij.openapi.util.TextRange;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.documentation.DocumentationTargetProvider;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxePsiToken;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeMetadataDocumentationProvider implements DocumentationTargetProvider {


    @Override
    public @NotNull List<? extends @NotNull DocumentationTarget> documentationTargets(@NotNull PsiFile file, int offset) {
        if (file instanceof HaxeFile haxeFile) {
            List<HaxeMetadataDocumentationTarget> metadataDocs = getHaxeCompileTimeMetadataDocs(offset, haxeFile);
            if (metadataDocs != null) return metadataDocs;
        }
        return List.of();
    }

    private static @Nullable List<HaxeMetadataDocumentationTarget> getHaxeCompileTimeMetadataDocs(int offset, HaxeFile haxeFile) {
        PsiElement elementAt = haxeFile.findElementAt(offset);
        if (elementAt instanceof HaxePsiToken token) {
            HaxeMetadataCompileTimeMeta parentOfType = PsiTreeUtil.getParentOfType(token, HaxeMetadataCompileTimeMeta.class);
            if (parentOfType != null) {
                HaxeMetadataType type = parentOfType.getType();
                if (type != null) {
                    return List.of(new HaxeMetadataDocumentationTarget(haxeFile, new TextRange(offset, offset), type.getText()));
                }
            }
        }
        return null;
    }
}
