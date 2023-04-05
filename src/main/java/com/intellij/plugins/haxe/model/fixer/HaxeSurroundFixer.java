/*
 * Copyright 2020 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.model.fixer;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.DOUBLE_QUOTE;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.SINGLE_QUOTE;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeSurroundFixer extends HaxeFixer {

  final TextRange range;
  final PsiElement element;
  final HaxeElementType left;
  final HaxeElementType right;
  final HaxeElementType oldLeft;
  final HaxeElementType oldRight;

  public HaxeSurroundFixer(@NotNull String text, @NotNull PsiElement el, @NotNull HaxeElementType left, @NotNull HaxeElementType right) {
    this(text, el, el.getTextRange(), left, right);
  }

  public HaxeSurroundFixer(@NotNull String text, @NotNull PsiElement el, @NotNull TextRange range, @NotNull HaxeElementType left, @NotNull HaxeElementType right) {
    this(text, el, range, left, right, null, null);
  }

  public HaxeSurroundFixer(@NotNull String text, @NotNull PsiElement el, @NotNull TextRange range,
                           @NotNull HaxeElementType left, @NotNull HaxeElementType right,
                           @Nullable HaxeElementType oldLeft, @Nullable HaxeElementType oldRight) {
    super(text);
    this.element = el;
    this.range= range;
    this.left = left;
    this.right = right;
    this.oldLeft = oldLeft;
    this.oldRight = oldRight;
  }

  @Override
  public void run() {
    if (!element.isValid()) return;

    HaxeDocumentModel doc = HaxeDocumentModel.fromElement(element);
    doc.rewrapElement(element, range, left.asCode(), right.asCode(),
                      null != oldLeft ? oldLeft.asCode() : null,
                      null != oldRight ? oldRight.asCode() : null);
  }

  public static HaxeSurroundFixer withParens(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.parenthesis"), el,
                                 (HaxeElementType)ENCLOSURE_PARENTHESIS_LEFT, (HaxeElementType)ENCLOSURE_PARENTHESIS_RIGHT);
  }

  public static HaxeSurroundFixer withParens(String fixTitle, PsiElement el, TextRange range) {
    return new HaxeSurroundFixer(fixTitle, el, range, (HaxeElementType)ENCLOSURE_PARENTHESIS_LEFT,
                                 (HaxeElementType)ENCLOSURE_PARENTHESIS_RIGHT);
  }

  public static HaxeSurroundFixer withBrackets(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.brackets"), el, (HaxeElementType)ENCLOSURE_BRACKET_LEFT,
                                 (HaxeElementType)ENCLOSURE_BRACKET_RIGHT);
  }

  public static HaxeSurroundFixer withBraces(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.curly.braces"), el,
                                 (HaxeElementType)ENCLOSURE_CURLY_BRACKET_LEFT, (HaxeElementType)ENCLOSURE_CURLY_BRACKET_RIGHT);
  }

  public static HaxeSurroundFixer withQuotes(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.double.quotation.marks"), el, (HaxeElementType)DOUBLE_QUOTE, (HaxeElementType)DOUBLE_QUOTE);
  }

  public static HaxeSurroundFixer withSingleQuotes(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.single.quotation.marks"), el, (HaxeElementType)SINGLE_QUOTE, (HaxeElementType)SINGLE_QUOTE);
  }

  public static HaxeSurroundFixer replaceQuotesWithSingleQuotes(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.single.quotation.marks"), el, el.getTextRange(),
                                 (HaxeElementType)SINGLE_QUOTE, (HaxeElementType)SINGLE_QUOTE,
                                 (HaxeElementType)DOUBLE_QUOTE, (HaxeElementType)DOUBLE_QUOTE);
  }

  public static HaxeSurroundFixer exchangeQuoteType(PsiElement el) {
    String text = el.getText();
    if (null != text && text.startsWith("'")) {
      return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.double.quotation.marks"), el, el.getTextRange(),
                                   (HaxeElementType)DOUBLE_QUOTE, (HaxeElementType)DOUBLE_QUOTE,
                                   (HaxeElementType)SINGLE_QUOTE, (HaxeElementType)SINGLE_QUOTE);
    }
    return replaceQuotesWithSingleQuotes(el);
  }
}
