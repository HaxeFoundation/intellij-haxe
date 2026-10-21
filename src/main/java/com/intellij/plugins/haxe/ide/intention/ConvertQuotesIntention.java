/*
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.model.fixer.HaxeSurroundFixer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Switches a string literal between single and double quotes. One instance
 * serves every editor, so nothing about the caret's literal is kept between
 * the calls: {@link #isAvailable} sets the label for the literal it found and
 * {@link #invoke} finds the literal again.
 */
public class ConvertQuotesIntention extends BaseIntentionAction {
  private static final String DOUBLE_QUOTES = "\"";

  public ConvertQuotesIntention() {
    setText(getFamilyName());
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return HaxeBundle.message("haxe.intention.convert.quotes.family");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    HaxeStringLiteralExpression expression = stringLiteralAt(editor, file);
    if (expression == null) return false;
    setText(labelFor(expression));
    return true;
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxeStringLiteralExpression expression = stringLiteralAt(editor, file);
    if (expression == null) return;
    String commandName = HaxeBundle.message("haxe.intention.convert.quotes.command");
    WriteCommandAction.runWriteCommandAction(project, commandName, null, HaxeSurroundFixer.exchangeQuoteType(expression));
  }

  /** The label says what the switch does to a {@code $}: the two quote kinds differ in exactly that. */
  @NotNull
  private static String labelFor(@NotNull HaxeStringLiteralExpression expression) {
    if (isWrappedWithDoubleQuotes(expression)) {
      return hasDollar(expression)
             ? HaxeBundle.message("haxe.quickfix.convert.to.single.quotes.interpolating")
             : HaxeBundle.message("haxe.quickfix.surround.with.single.quotation.marks");
    }
    return hasInterpolation(expression)
           ? HaxeBundle.message("haxe.quickfix.convert.to.double.quotes.plain.text")
           : HaxeBundle.message("haxe.quickfix.surround.with.double.quotation.marks");
  }

  /** The string literal at the caret of a Haxe file, or null. */
  @Nullable
  static HaxeStringLiteralExpression stringLiteralAt(@NotNull Editor editor, @NotNull PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return null;
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
    return PsiTreeUtil.getParentOfType(place, HaxeStringLiteralExpression.class);
  }

  /** Whether a single-quoted string interpolates something; only the lexer of single quotes produces template entries. */
  static boolean hasInterpolation(HaxeStringLiteralExpression expression) {
    return !expression.getLongTemplateEntryList().isEmpty() || !expression.getShortTemplateEntryList().isEmpty();
  }

  /** Whether a double-quoted string holds a {@code $} that single quotes would start interpolating. */
  static boolean hasDollar(HaxeStringLiteralExpression expression) {
    return expression.getText().indexOf('$') >= 0;
  }

  static boolean isWrappedWithDoubleQuotes(@NotNull HaxeStringLiteralExpression expression) {
    PsiElement opening = expression.getFirstChild();
    return opening != null && opening.textMatches(DOUBLE_QUOTES);
  }
}