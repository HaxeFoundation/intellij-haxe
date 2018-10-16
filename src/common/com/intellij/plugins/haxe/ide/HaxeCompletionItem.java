/*
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.NotNull;

public class HaxeCompletionItem {
  @NotNull public final String name;
  @NotNull public final String description;

  private LookupElement myLookupElement;

  public HaxeCompletionItem(@NotNull String name, @NotNull String description) {
    this.name = name;
    this.description = description;
  }

  public LookupElement getLookupElement() {
    if (myLookupElement == null) {
      myLookupElement = LookupElementBuilder.create(name).withTailText(" " + description, true);
    }
    return myLookupElement;
  }
}
