/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.util.HaxeUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDefineIntention implements IntentionAction {
  private final String myWord;
  private final boolean isDefined;

  public HaxeDefineIntention(@Nls String word, boolean contains) {
    myWord = word;
    isDefined = contains;
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message(isDefined ? "haxe.intention.undefine" : "haxe.intention.define", myWord);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return HaxeBundle.message("quick.fixes.family");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final HaxeProjectSettings projectSettings = HaxeProjectSettings.getInstance(file.getProject());
    final Set<String> definitions = projectSettings.getUserCompilerDefinitionsAsSet();
    projectSettings.setUserCompilerDefinitions(changeDefinitions(definitions));
    HaxeUtil.reparseProjectFiles(project);
  }

  private String[] changeDefinitions(Set<String> definitions) {
    if (isDefined) {
      definitions.remove(myWord);
    }
    else {
      definitions.add(myWord);
    }
    return ArrayUtil.toStringArray(definitions);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
