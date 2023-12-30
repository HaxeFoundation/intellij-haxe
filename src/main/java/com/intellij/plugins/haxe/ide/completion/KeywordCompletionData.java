package com.intellij.plugins.haxe.ide.completion;


import com.intellij.psi.tree.IElementType;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class KeywordCompletionData {

  public static final String CARET = "<CARET>";

  IElementType keyword;
  String template;
  boolean bold;
  boolean italic;

  public static KeywordCompletionData keywordWithSpace(IElementType keyword) {
    return new  KeywordCompletionData (keyword, keyword + " ", true, false);
  }
  public static KeywordCompletionData keywordWithSpace(IElementType keyword,  boolean bold, boolean italic) {
    return new  KeywordCompletionData (keyword, keyword + " ", bold, italic);
  }
  public static KeywordCompletionData keywordOnly(IElementType keyword) {
    return new  KeywordCompletionData (keyword, null, true, false);
  }
  public static KeywordCompletionData keywordParentheses(IElementType keyword) {
    return new  KeywordCompletionData (keyword, keyword.toString() + " ("+CARET+")", true, false);
  }
  public static KeywordCompletionData keywordCurlyBrackets(IElementType keyword) {
    return new  KeywordCompletionData (keyword, keyword.toString() + " {\n"+CARET+"\n}", true, false);
  }
  public static KeywordCompletionData keywordTemplate(IElementType keyword, String template) {
    return new  KeywordCompletionData (keyword, template, true, false);
  }
  public static KeywordCompletionData keywordData(IElementType keyword, String template, boolean bold, boolean italic) {
    return new  KeywordCompletionData (keyword, template, bold, italic);
  }
}
