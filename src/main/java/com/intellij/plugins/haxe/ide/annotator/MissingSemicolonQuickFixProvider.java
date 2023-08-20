/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2020 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.ErrorQuickFixProvider;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.model.fixer.HaxeMissingSemicolonFixer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;

public class MissingSemicolonQuickFixProvider implements ErrorQuickFixProvider {
  private static final String MISSING_SEMICOLON = HaxeBundle.message("parsing.error.missing.semi.colon");


    @Override
  public void registerErrorQuickFix(@NotNull PsiErrorElement errorElement, @NotNull HighlightInfo.Builder builder) {
    if (MISSING_SEMICOLON.equals(errorElement.getErrorDescription())) {

      HaxeMissingSemicolonFixer action = addMissingSemicolon(errorElement);
      builder.registerFix(action, null, "Add semicolon", errorElement.getTextRange(), null);
    }
  }

  @NotNull
  private static HaxeMissingSemicolonFixer addMissingSemicolon(@NotNull PsiElement expr) {
    return new HaxeMissingSemicolonFixer(expr);
  }
}
