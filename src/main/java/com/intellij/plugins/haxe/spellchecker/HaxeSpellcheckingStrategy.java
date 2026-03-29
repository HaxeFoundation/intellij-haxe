package com.intellij.plugins.haxe.spellchecker;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.spellchecker.DocCommentTokenizer;
import com.intellij.spellchecker.NamedElementTokenizer;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.DOUBLE_QUOTE;


public class HaxeSpellcheckingStrategy extends SpellcheckingStrategy implements DumbAware {
    private final NamedElementTokenizer namedElementTokenizer = new NamedElementTokenizer();
    private final HaxeStringLiteralTokenizer stringLiteralTokenizer = new HaxeStringLiteralTokenizer();
    private final DocCommentTokenizer myDocCommentTokenizer = new DocCommentTokenizer();

    @Override
    public @NotNull Tokenizer getTokenizer(@NotNull PsiElement element, @NotNull Set<SpellCheckingInspection.SpellCheckingScope> scope) {
        return super.getTokenizer(element, scope);
    }

    @Override
    public @NotNull Tokenizer getTokenizer(PsiElement element) {

        if (element instanceof PsiMethod psiMethod && psiMethod.isConstructor()) return EMPTY_TOKENIZER;

        if (element instanceof PsiDocComment) {
            return useTextLevelSpellchecking() ? EMPTY_TOKENIZER : myDocCommentTokenizer;
        }

        if (element instanceof HaxeStringLiteralExpression literalExpression) {
            return useTextLevelSpellchecking() ? EMPTY_TOKENIZER : stringLiteralTokenizer;
        }

        if (element instanceof PsiNamedElement) {
            return namedElementTokenizer;
        }
        if (shouldIgnore(element)) {
            return EMPTY_TOKENIZER;
        }

        return super.getTokenizer(element);
    }

    private boolean shouldIgnore(PsiElement element) {
        // ignoring other types of comments if grazie spellchecking is enabled
        //  comments are handled by grazie through `HaxeGrazieTextExtractor`
        return element instanceof PsiComment comment && useTextLevelSpellchecking();
    }


    @Override
    public boolean useTextLevelSpellchecking() {
        return Registry.is("spellchecker.grazie.enabled", false);
    }
}
