/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Created by as3boyan on 25.11.14.
 */
public class HXMLCompletionArgument extends HaxeCompletionItem {
  final public boolean doubleDash;

  @Nullable final public String options;

  public HXMLCompletionArgument(boolean doubleDash,
                                @NotNull String name,
                                @NotNull String description,
                                @Nullable String options) {
    super(name, description);
    this.doubleDash = doubleDash;
    this.options = options;
  }

  @Override
  public LookupElement getLookupElement() {
    StringBuilder sb = new StringBuilder();
    if (options != null && !options.isEmpty()) sb.append(' ').append(options);
    if (!description.isEmpty()) sb.append(' ').append(description);

    return LookupElementBuilder
      .create(name)
      .withTailText(sb.toString(), true);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HXMLCompletionArgument item = (HXMLCompletionArgument)o;
    return Objects.equals(name, item.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
