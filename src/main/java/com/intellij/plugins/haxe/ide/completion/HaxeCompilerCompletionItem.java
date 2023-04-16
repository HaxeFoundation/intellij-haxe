/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.ide.completion;

import com.google.common.base.Joiner;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class HaxeCompilerCompletionItem {
  public static final List<HaxeCompilerCompletionItem> EMPTY_LIST = new ArrayList<HaxeCompilerCompletionItem>(0);

  private String name;
  private List<String> parameters = null;
  private String retType = null;
  private String memberType = null;
  private String documentation = null;


  public HaxeCompilerCompletionItem(String name) {
    this.name = name;
  }

  public void setDocumentation(@Nullable String documentation) {
    this.documentation = documentation;
  }

  public void setMemberType(@Nullable String memberType) {
    this.memberType = memberType;
  }

  @Nullable
  public List<String> getParameters() {
    return this.parameters;
  }

  public void setParameters(@Nullable List<String> parameters) {
    this.parameters = parameters;
  }

  public void setReturnType(@Nullable String retType) {
    this.retType = retType;
  }

  @NotNull
  public LookupElement toLookupElement() {

    StringBuilder presentableText = new StringBuilder()
        .append(this.name)
        .append('(')
        .append(Joiner.on(", ").join(this.parameters))
        .append("):")
        .append(this.retType);

    Icon icon = "var".equals(this.memberType) ? HaxeIcons.Field : HaxeIcons.Method;

    LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(this, this.name)
        .withIcon(icon)
        .withPresentableText(presentableText.toString())
        .withTailText(" " + this.memberType, true)
        .withBoldness(true)
        .withTypeText(this.documentation, true);

    return lookupElementBuilder;
  }

  public String toString() {
    return this.name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxeCompilerCompletionItem item = (HaxeCompilerCompletionItem)o;

    if (name != null ? !name.equals(item.name) : item.name != null) return false;
    if (parameters != null ? !parameters.equals(item.parameters) : item.parameters != null) return false;
    if (retType != null ? !retType.equals(item.retType) : item.retType != null) return false;
    if (memberType != null ? !memberType.equals(item.memberType) : item.memberType != null) return false;
    return documentation != null ? documentation.equals(item.documentation) : item.documentation == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    result = 31 * result + (retType != null ? retType.hashCode() : 0);
    result = 31 * result + (memberType != null ? memberType.hashCode() : 0);
    result = 31 * result + (documentation != null ? documentation.hashCode() : 0);
    return result;
  }
}
