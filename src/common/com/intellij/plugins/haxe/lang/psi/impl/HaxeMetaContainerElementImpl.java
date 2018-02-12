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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeMacroClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMacroClassList;
import com.intellij.plugins.haxe.lang.psi.HaxeMetaContainerElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HaxeMetaContainerElementImpl extends HaxePsiCompositeElementImpl implements HaxeMetaContainerElement {
  private static final String OPEN_BRACE = "(";

  public HaxeMetaContainerElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public boolean hasMeta(String name) {
    return getMetaStream()
      .anyMatch(item -> {
        String text = item.getText();
        return (text.equals(name) || text.startsWith(name + OPEN_BRACE));
      });
  }

  public HaxeMacroClass getMeta(String name) {
    return getMetaStream()
      .filter(item -> {
        String text = item.getText();
        return (text.equals(name) || text.startsWith(name + OPEN_BRACE));
      }).findFirst().orElse(null);
  }

  @NotNull
  public List<HaxeMacroClass> getMetaList() {
    return getMetaStream().collect(Collectors.toList());
  }

  @NotNull
  public Stream<HaxeMacroClass> getMetaStream() {
    HaxeMacroClassList macroListPsi = PsiTreeUtil.findChildOfType(this, HaxeMacroClassList.class);
    if (macroListPsi != null) {
      return macroListPsi.getMacroClassList().stream();
    }

    return Stream.empty();
  }
}
