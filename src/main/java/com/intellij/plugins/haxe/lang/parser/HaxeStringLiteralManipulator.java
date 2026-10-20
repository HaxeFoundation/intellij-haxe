package com.intellij.plugins.haxe.lang.parser;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeStringLiteralImpl;
import com.intellij.psi.AbstractElementManipulator;
import org.jetbrains.annotations.NotNull;

/** Edits a string literal's content, the text between its quotes, by reparsing the whole literal. */
public class HaxeStringLiteralManipulator extends AbstractElementManipulator<HaxeStringLiteralImpl> {

  /** An unterminated literal's content runs to its end. */
  @Override
  public @NotNull TextRange getRangeInElement(@NotNull HaxeStringLiteralImpl element) {
    String text = element.getText();
    boolean closed = text.length() > 1 && text.charAt(text.length() - 1) == text.charAt(0);
    return new TextRange(1, closed ? text.length() - 1 : text.length());
  }

  // TODO: escape newContent for the literal's quote style (backslash, the quote char, $ inside single quotes)
  @Override
  public HaxeStringLiteralImpl handleContentChange(@NotNull HaxeStringLiteralImpl element, @NotNull TextRange range,
                                                   String newContent) {
    String newText = range.replace(element.getText(), newContent);
    return (HaxeStringLiteralImpl)element.updateText(newText);
  }
}
