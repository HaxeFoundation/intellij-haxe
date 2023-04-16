/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2019 AS3Boyan
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
package com.intellij.plugins.haxe.model.fixer;

import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.StripSpaces;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeRemoveElementFixer extends HaxeFixer {

  PsiElement element;
  StripSpaces stripSpaces;

  public HaxeRemoveElementFixer(@Nullable String text, @NotNull PsiElement element, StripSpaces strips) {
    super(text == null ? "HaxeRemoveElementFixer" : text);
    this.element = element;
    this.stripSpaces = strips;
  }

  public HaxeRemoveElementFixer(@Nullable String text, @NotNull PsiElement element) {
    this(text, element, StripSpaces.NONE);
  }

  public void run() {
    HaxeDocumentModel document = HaxeDocumentModel.fromElement(element);
    document.replaceElementText(element, "", stripSpaces);
  }
}
