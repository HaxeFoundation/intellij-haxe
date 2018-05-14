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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeRenameElementFix extends HaxeFixAndIntentionAction {
  private final String properTypeName;

  public HaxeRenameElementFix(PsiElement o, String properName) {
    super(o);

    this.properTypeName = properName;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    PsiElement elementToRename = startElement;
    if (!(elementToRename instanceof HaxeNamedComponent)) {
      elementToRename = UsefulPsiTreeUtil.getParentOfType(elementToRename, HaxeNamedComponent.class);
    }

    if (elementToRename != null) {
      tryRename(project, elementToRename);
    }
  }

  private void tryRename(@NotNull Project project, PsiElement elementToRename) {
    try {
      RenameProcessor processor = new RenameProcessor(project, elementToRename, properTypeName, false, false);
      processor.run();
    }
    catch (Throwable error) {
      LOG.warn(error);
    }
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.rename.to", properTypeName);
  }
}
