package com.intellij.plugins.haxe.buildsystem.hxml.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;

public class HxmlParserUtil  extends GeneratedParserUtilBase {


    // We perform a "lookahead" in the lexer to determine if "hxml" is an identifier or the file extension
    // this will result in any "hxml" at the end of a file to become an identifier as there is no character
    // after this and so the "lookahead" will never be successful.
    public static boolean eofHxmlExtension(PsiBuilder builder, int level) {

        String tokenText = builder.getTokenText();
        if(tokenText != null && tokenText.equalsIgnoreCase("hxml")) {
            PsiBuilder.Marker mark = builder.mark();
            consumeToken(builder, builder.getTokenType());
            boolean eof = builder.eof();
            if(eof) {
                mark.drop();
                return true;
            }else {
                mark.rollbackTo();
                return false;
            }
        }
        return false;
    }
}
