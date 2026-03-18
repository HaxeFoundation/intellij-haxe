package com.intellij.plugins.haxe.ide.documentation.providers;

import com.intellij.icons.AllIcons;
import com.intellij.model.Pointer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.util.text.Strings;
import com.intellij.platform.backend.documentation.DocumentationResult;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.plugins.haxe.ide.documentation.HaxeDocumentationRenderer;
import com.intellij.plugins.haxe.ide.lookup.HaxeMetadataLookupElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class HaxeMetadataDocumentationTarget implements DocumentationTarget {
    private final @NotNull PsiFile psiFile;
    private final TextRange rangeInFile;
    private final HaxeMetadataLookupElement element;
    private final String name;

    public HaxeMetadataDocumentationTarget(@NotNull PsiFile psiFile, @NotNull TextRange rangeInFile, @NotNull HaxeMetadataLookupElement element) {
        this.name = element.name;

        this.psiFile = psiFile;
        this.rangeInFile = rangeInFile;
        this.element = element;
    }
    public HaxeMetadataDocumentationTarget(@NotNull PsiFile psiFile, @NotNull TextRange rangeInFile, String name) {
        this.name = name;

        this.psiFile = psiFile;
        this.rangeInFile = rangeInFile;
        this.element = null;
    }

    @Override
    public @NotNull TargetPresentation computePresentation() {
        return TargetPresentation.builder("").presentation();
    }

    @Override
    public @NotNull Pointer<? extends DocumentationTarget> createPointer() {
        if(element != null) {
            return Pointer.fileRangePointer(psiFile, rangeInFile, (psiFile1, rangeInFile1) -> new HaxeMetadataDocumentationTarget(psiFile1, rangeInFile1, element));
        }else {
            return Pointer.fileRangePointer(psiFile, rangeInFile, (psiFile1, rangeInFile1) -> new HaxeMetadataDocumentationTarget(psiFile1, rangeInFile1, name));
        }
    }

    @Override
    public @Nullable DocumentationResult computeDocumentation() {
        HaxeMetadataDocumentations.MetadataInfo docs = HaxeMetadataDocumentations.getDocsFor(name);
        if(docs == null) return  null;
        return DocumentationResult.documentation(render(docs));
    }

    private  String render(HaxeMetadataDocumentations.MetadataInfo info) {
        HaxeDocumentationRenderer renderer = psiFile.getProject().getService(HaxeDocumentationRenderer.class);
        return new HtmlBuilder()
                .append(HtmlChunk.icon("AllIcons.Nodes.Annotationtype", AllIcons.Nodes.Annotationtype)).nbsp(1)
                .append(info.getMetadata()).append(renderArgs(info.getArguments()))
                .hr()
                .br()
                .append(HtmlChunk.raw(renderer.parseAndRender(info.getDescription())))
                .br()
                .hr()
                .append(createPlatformFooter(info))
                .toString();
    }

    private static @NotNull HtmlChunk createPlatformFooter(HaxeMetadataDocumentations.MetadataInfo info) {
        return HtmlChunk.span().addText("Platforms: " + Strings.join(info.getPlatforms(),",")).italic();
    }

    private static @NotNull HtmlChunk renderArgs(List<String> arguments) {
        if(arguments.isEmpty()) return HtmlChunk.empty();
        return HtmlChunk.span().addText(" ( "+Strings.join(arguments, ", ")+" )").italic();
    }

}
