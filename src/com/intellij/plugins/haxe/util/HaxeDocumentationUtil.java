package com.intellij.plugins.haxe.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDocumentationUtil {
  @NotNull
  public static String unwrapCommentDelimiters(@NotNull String text){
    if(text.startsWith("/**")) text = text.substring("/**".length());
    if(text.startsWith("/*")) text = text.substring("/*".length());
    if(text.startsWith("//")) text = text.substring("//".length());
    if(text.endsWith("**/")) text = text.substring(0, text.length() - "**/".length());
    if(text.endsWith("*/")) text = text.substring(0, text.length() - "*/".length());
    return text;
  }
}
