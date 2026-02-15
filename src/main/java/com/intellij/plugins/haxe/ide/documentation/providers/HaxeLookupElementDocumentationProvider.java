package com.intellij.plugins.haxe.ide.documentation.providers;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.documentation.LookupElementDocumentationTargetProvider;
import com.intellij.plugins.haxe.ide.lookup.HaxeMetadataLookupElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeLookupElementDocumentationProvider implements LookupElementDocumentationTargetProvider {

    @Override
    public @Nullable DocumentationTarget documentationTarget(@NotNull PsiFile psiFile, @NotNull LookupElement element, int offset) {
        if(element instanceof  HaxeMetadataLookupElement lookupElement) {
            return new HaxeMetadataDocumentationTarget(psiFile, new TextRange(offset, offset), lookupElement);
        }
        return null;
    }
}
