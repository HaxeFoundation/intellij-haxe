package com.intellij.plugins.haxe.spellchecker;

import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeStringTemplateUtils;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.spellchecker.inspections.WordSplitter;
import com.intellij.spellchecker.tokenizer.EscapeSequenceTokenizer;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class HaxeStringLiteralTokenizer extends EscapeSequenceTokenizer<HaxeStringLiteralExpression> {
    @Override
    public void tokenize(@NonNull HaxeStringLiteralExpression element, @NotNull TokenConsumer consumer) {
        if (HaxeStringTemplateUtils.isStringWithtemplates(element)) {
            consumer.consumeToken(element, HaxeStringTextSplitter.getInstance());
        } else {
            consumer.consumeToken(element, PlainTextSplitter.getInstance());
        }
    }
}
