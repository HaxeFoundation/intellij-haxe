package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.HXMLFile;
import com.intellij.plugins.haxe.hxml.psi.HXMLHxmlFile;
import com.intellij.plugins.haxe.hxml.psi.HXMLPath;
import com.intellij.plugins.haxe.hxml.psi.HXMLQualifiedName;
import com.intellij.plugins.haxe.hxml.psi.HXMLUnknownValue;
import com.intellij.plugins.haxe.ide.highlight.HXMLSyntaxHighlighter;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.XML_SUB_TAG_START;


public class HaxeHxmlFastColorAnnotator implements Annotator, DumbAware {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        switch (element) {
            case HXMLQualifiedName psi -> {
                colorize(holder, psi, HXMLSyntaxHighlighter.CLASS_NAME);
            }
            case HXMLPath psi -> {
                colorize(holder, psi, HXMLSyntaxHighlighter.INCLUDE);
            }
            case HXMLHxmlFile psi -> {
                colorize(holder, psi, HXMLSyntaxHighlighter.INCLUDE);
            }
            case HXMLUnknownValue psi -> {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Unknown HXML structure")
                        .range(element)
                        .create();
            }
            default -> {}
        }

    }


    private static void colorize(AnnotationHolder holder, PsiElement  element, TextAttributesKey attribute) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(attribute).create();
    }
}
