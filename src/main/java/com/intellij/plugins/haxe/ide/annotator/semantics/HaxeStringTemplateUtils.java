package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HaxeStringTemplateUtils {

    // These patterns are designed to find likely string templates; they are not intended to be exhaustive.
    public static final String DOUBLE_QUOTE = "\"";
    public static final String SINGE_QUOTE = "'";

    public static final String shortTemplateRegex = "(\\$+)\\w+";
    public static final String longTemplateRegex = "(\\$+)\\{.*}";

    public static final Pattern shortTemplate = Pattern.compile(shortTemplateRegex);
    public static final Pattern longTemplate = Pattern.compile(longTemplateRegex);


    public static boolean isSingleQuotesRequired(HaxeStringLiteralExpression psi) {
        String text = psi.getText();
        return text.startsWith(DOUBLE_QUOTE) && (templateMatches(text, shortTemplate) || templateMatches(text, longTemplate));
    }
    public static boolean isStringWithtemplates(HaxeStringLiteralExpression psi) {
        String text = psi.getText();
        return text.startsWith(SINGE_QUOTE) && (templateMatches(text, shortTemplate) || templateMatches(text, longTemplate));
    }

    public static boolean templateMatches(String text, Pattern pattern) {
        // We need an odd number of dollar signs to avoid detecting escaped dollar signs as templates.
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (matcher.groupCount() == 1 && (matcher.end(1) - matcher.start(1)) % 2 != 0) {
                return true;
            }
        }
        return false;
    }
}
