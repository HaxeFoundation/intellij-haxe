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
package com.intellij.plugins.haxe.ide.rearranger;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementEntry;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettingsSerializer;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created by as3boyan on 10.09.14.
 */
public class HaxeRearrangerModel implements Rearranger {
  @Nullable
  @Override
  public Pair parseWithNew(@NotNull PsiElement element,
                           @Nullable Document document,
                           @NotNull Collection collection,
                           @NotNull PsiElement element2,
                           @Nullable ArrangementSettings settings) {
    return null;
  }

  @NotNull
  @Override
  public List parse(@NotNull PsiElement element,
                    @Nullable Document document,
                    @NotNull Collection collection,
                    @Nullable ArrangementSettings settings) {
    return null;
  }

  @Override
  public int getBlankLines(@NotNull CodeStyleSettings settings,
                           @Nullable ArrangementEntry entry,
                           @Nullable ArrangementEntry entry2,
                           @NotNull ArrangementEntry entry3) {
    return 0;
  }

  @NotNull
  @Override
  public ArrangementSettingsSerializer getSerializer() {
    return null;
  }
}
