/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineOverride;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Toggles a conditional-compilation flag by editing the IDE overrides of the
 * build the file is analysed under (its resolve context, else the active
 * build), so the change shows up under that build's IDE Overrides and
 * composes with the build file: a flag the build file defines is masked with
 * a REMOVE entry, one it doesn't gets a SET entry, and toggling back simply
 * drops the override. The store mutation publishes the build-settings topic,
 * which drives the reparse and the tool window refresh.
 */
public class HaxeDefineIntention implements IntentionAction {
  private final String myWord;
  private final boolean isDefined;
  /** The build the toggle edits, named in the text; found by {@link #isAvailable}. */
  private @Nullable String buildFileName;

  public HaxeDefineIntention(@Nls String word, boolean contains) {
    myWord = word;
    isDefined = contains;
  }

  @NotNull
  @Override
  public String getText() {
    if (buildFileName == null) {
      return HaxeBundle.message(isDefined ? "haxe.intention.undefine" : "haxe.intention.define", myWord);
    }
    String key = isDefined ? "haxe.intention.undefine.for.build" : "haxe.intention.define.for.build";
    return HaxeBundle.message(key, myWord, buildFileName);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return HaxeBundle.message("quick.fixes.family");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    // invoke() edits the IDE overrides of the build defining the file; without one it would silently do nothing
    if (HaxeDefineContextService.isCompilerIdentityDefine(myWord)) return false;
    VirtualFile buildFile = definingBuildFile(project, file);
    buildFileName = buildFile == null ? null : buildFile.getName();
    return buildFile != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    VirtualFile buildFile = definingBuildFile(project, file);
    if (buildFile == null) return;

    HaxeIdeOverridesStore store = HaxeIdeOverridesStore.getInstance(project);
    List<DefineOverride> defines = new ArrayList<>(store.getDefines(buildFile.getPath()));
    defines.removeIf(define -> define.name().equals(myWord));
    // an override entry is only needed when the build context disagrees with
    // the wanted state; otherwise dropping the previous override suffices
    boolean definedByBuildContext = HaxeDefineContextService.getInstance(project).isDefinedWithoutOverrides(buildFile, myWord);
    if (isDefined && definedByBuildContext) {
      defines.add(new DefineOverride(myWord, "", DefineEffect.REMOVE));
    }
    else if (!isDefined && !definedByBuildContext) {
      defines.add(new DefineOverride(myWord, "", DefineEffect.SET));
    }
    store.setDefines(buildFile.getPath(), defines);
  }

  @Nullable
  private static VirtualFile definingBuildFile(@NotNull Project project, @Nullable PsiFile file) {
    VirtualFile physical = HaxeDefineContextService.physicalFileOf(file);
    return physical == null ? null : HaxeDefineContextService.getInstance(project).definingBuildFile(physical);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
