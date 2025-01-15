package com.intellij.plugins.haxe.ide.inlay.all;

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.ide.hint.types.*;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AllInlayHintsProvider implements InlayHintsProvider {

    List<InlayHintsProvider> inlayHintsProviders = List.of(
            new HaxeInlayCaptureVariableHintsProvider(),
            new HaxeInlayEnumExtractorHintsProvider(),
            new HaxeInlayFieldHintsProvider(),
            new HaxeInlayForLoopHintsProvider(),
            new HaxeInlayLocalVariableHintsProvider(),
            new HaxeInlayReturnTypeHintsProvider(),
            new HaxeInlayUntypedParameterHintsProvider()
    );

    @Override
    public @Nullable InlayHintsCollector createCollector(@NotNull PsiFile psiFile, @NotNull Editor editor) {
        List<HaxeSharedBypassCollector> inlayHintsCollectors = inlayHintsProviders.stream()
                .map(inlayHintsProvider -> inlayHintsProvider.createCollector(psiFile, editor))
                .map(HaxeSharedBypassCollector.class::cast)
                .toList();

        return (SharedBypassCollector) (psiElement, inlayTreeSink) -> {
            inlayHintsCollectors.forEach(collector -> collector.collectFromElement(psiElement, inlayTreeSink));
        };

    }
}
