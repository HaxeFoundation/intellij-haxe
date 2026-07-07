package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.XML_SUB_TAG_START;


public class HaxeXmlFastColorAnnotator implements Annotator, DumbAware {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PsiWhiteSpace) return;

        if (isEndOfTag(element)) {
            colorize(holder, element, HaxeSyntaxHighlighterColors.INLINE_XML);
            return;
        }
        if (element instanceof HaxeXmlFragment fragment) {
            colorizeXmlFragment(fragment, holder);
            return;
        }
    }


    static boolean isEndOfTag(@NonNull PsiElement element) {
        if (element.getParent() instanceof HaxeXmlLiteralExpression xmlExpression) {
            if(element.textMatches(">")) {
                PsiElement next = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(element);
                if (next instanceof HaxeXmlMarkupElement|| next.getNode().getElementType() == XML_SUB_TAG_START) {
                    return true;
                }
            }
        }
        if (element.getParent() instanceof HaxeXmlSubTagContainerStart tag) {
            boolean isLastChild = tag.getLastChild() == element;
            if (isLastChild && element.textMatches(">")) {
                return true;
            }
        }
        return false;
    }


    private static void colorizeXmlFragment(@NotNull HaxeXmlFragment fragment, @NotNull AnnotationHolder holder) {

        if (fragment instanceof HaxeMarkupAttributeValueString) {
            colorize(holder, fragment, HaxeSyntaxHighlighterColors.STRING);
        }

        if (fragment instanceof HaxeXmlMarkupAttributeName) {
            colorize(holder, fragment, HaxeSyntaxHighlighterColors.INLINE_XML_ATTRIBUTE_NAME);
        }


        if (fragment instanceof HaxeMarkupAttributeValueCurlyBlock block) {
            TextRange fullRange = fragment.getTextRange();
            int length = fullRange.getLength();

            TextRange start = fullRange.cutOut(TextRange.create(0, 1));
            TextRange end = fullRange.cutOut(TextRange.create(length - 1, length -1));

            colorize(holder, start, HaxeSyntaxHighlighterColors.INLINE_XML);
            colorize(holder, end, HaxeSyntaxHighlighterColors.INLINE_XML);

        }
    }


    private static void colorize(AnnotationHolder holder, PsiElement  element, TextAttributesKey attribute) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(attribute).create();
    }
    private static void colorize(AnnotationHolder holder, TextRange range, TextAttributesKey attribute) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range)
                .textAttributes(attribute).create();
    }
}
