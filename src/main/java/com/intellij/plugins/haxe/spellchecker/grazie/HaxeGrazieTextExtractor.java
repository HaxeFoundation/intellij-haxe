package com.intellij.plugins.haxe.spellchecker.grazie;

import com.intellij.grazie.text.TextContent;
import com.intellij.grazie.text.TextContentBuilder;
import com.intellij.grazie.text.TextExtractor;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeStringTemplateUtils;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.psi.HaxeLongTemplateEntry;
import com.intellij.plugins.haxe.lang.psi.HaxeShortTemplateEntry;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class HaxeGrazieTextExtractor extends TextExtractor {

    @Override
    protected @Nullable TextContent buildTextContent(@NotNull PsiElement element, @NotNull Set<TextContent.TextDomain> allowedDomains) {
        if (element instanceof PsiComment comment) {
            IElementType tokenType = comment.getTokenType();
            if (tokenType == HaxeTokenTypeSets.MSL_COMMENT) {
                return TextContent.builder().build(element, TextContent.TextDomain.COMMENTS);
            }

            if (tokenType == HaxeTokenTypeSets.MML_COMMENT) {
                return TextContent.builder()
                        .removingIndents(" \t*")
                        .removingLineSuffixes(" \t*")
                        .build(element, TextContent.TextDomain.COMMENTS);
            }

            if (tokenType == HaxeTokenTypeSets.DOC_COMMENT) {
                return TextContent.builder()
                        .removingIndents(" \t*")
                        .removingLineSuffixes(" \t*")
                        .build(element, TextContent.TextDomain.DOCUMENTATION);
            }

        }else if (element instanceof HaxeStringLiteralExpression stringLiteral) {
            return TextContent.builder()
                    .withUnknown(e -> e instanceof HaxeLongTemplateEntry)
                    .withUnknown(e -> e instanceof HaxeShortTemplateEntry)
                    .build(element, TextContent.TextDomain.LITERALS);
        }
        return null;
    }
}
