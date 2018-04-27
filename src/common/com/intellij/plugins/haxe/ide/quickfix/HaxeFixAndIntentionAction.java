/*
 * Copyright 2018-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract public class HaxeFixAndIntentionAction extends LocalQuickFixAndIntentionActionOnPsiElement {
  protected HaxeFixAndIntentionAction(@Nullable PsiElement element) {
    super(element);
  }

  protected HaxeFixAndIntentionAction(@Nullable PsiElement startElement, @Nullable PsiElement endElement) {
    super(startElement, endElement);
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return "semantic";
  }
}