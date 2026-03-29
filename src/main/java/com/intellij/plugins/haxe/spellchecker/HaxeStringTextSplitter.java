package com.intellij.plugins.haxe.spellchecker;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeStringTemplateUtils;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.util.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeStringTextSplitter extends PlainTextSplitter {

    private static final HaxeStringTextSplitter INSTANCE = new HaxeStringTextSplitter();

    public static HaxeStringTextSplitter getInstance() {
        return INSTANCE;
    }


    @Override
    public void split(@Nullable String text, @NotNull TextRange range, @NotNull Consumer<TextRange> consumer) {
        String TextWithoutTemplate = getTextWithoutTemplate(text, HaxeStringTemplateUtils.longTemplate);
        TextWithoutTemplate = getTextWithoutTemplate(TextWithoutTemplate, HaxeStringTemplateUtils.shortTemplate);
        super.split(TextWithoutTemplate, range, consumer);
    }

    private @NonNull String getTextWithoutTemplate(String text, Pattern templatePattern) {
        Matcher matcher = templatePattern.matcher(text);
        return matcher.replaceAll(this::spaceFill);
    }

    private String spaceFill(MatchResult matchResult) {
        String group = matchResult.group();
        return StringUtils.leftPad("", group.length());
    }
}
