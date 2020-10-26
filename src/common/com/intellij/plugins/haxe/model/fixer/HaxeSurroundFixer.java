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
import com.intellij.plugins.haxe.model.StripSpaces;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;

public class HaxeSurroundFixer extends HaxeFixer {

  final TextRange range;
  final PsiElement element;
  final HaxeElementType left;
  final HaxeElementType right;

  public HaxeSurroundFixer(@NotNull String text, @NotNull PsiElement el, @NotNull HaxeElementType left, @NotNull HaxeElementType right) {
    this(text, el, el.getTextRange(), left, right);
  }

  public HaxeSurroundFixer(@NotNull String text, @NotNull PsiElement el, @NotNull TextRange range, @NotNull HaxeElementType left, @NotNull HaxeElementType right) {
    super(text);
    this.element = el;
    this.range= range;
    this.left = left;
    this.right = right;
  }

  @Override
  public void run() {
    if (!element.isValid()) return;

    HaxeDocumentModel doc = HaxeDocumentModel.fromElement(element);
    doc.wrapTextRange(range, left.asCode(), right.asCode(), StripSpaces.NONE);
  }

  public static HaxeSurroundFixer withParens(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.parenthesis"), el, (HaxeElementType)PLPAREN, (HaxeElementType)PRPAREN);
  }

  public static HaxeSurroundFixer withParens(String fixTitle, PsiElement el, TextRange range) {
    return new HaxeSurroundFixer(fixTitle, el, range, (HaxeElementType)PLPAREN, (HaxeElementType)PRPAREN);
  }

  public static HaxeSurroundFixer withBrackets(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.brackets"), el, (HaxeElementType)PLBRACK, (HaxeElementType)PRBRACK);
  }

  public static HaxeSurroundFixer withBraces(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.curly.braces"), el, (HaxeElementType)PLCURLY, (HaxeElementType)PRCURLY);
  }

  public static HaxeSurroundFixer withQuotes(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.double.quotation.marks"), el, (HaxeElementType)DOUBLE_QUOTE, (HaxeElementType)DOUBLE_QUOTE);
  }

  public static HaxeSurroundFixer withSingleQuotes(PsiElement el) {
    return new HaxeSurroundFixer(HaxeBundle.message("haxe.quickfix.surround.with.single.quotation.marks"), el, (HaxeElementType)SINGLE_QUOTE, (HaxeElementType)SINGLE_QUOTE);
  }
}
