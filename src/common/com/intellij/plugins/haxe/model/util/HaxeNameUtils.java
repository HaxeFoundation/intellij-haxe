/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model.util;

import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeNameUtils {
  static private Pattern TAIL_NUMBER = Pattern.compile("\\d+$");

  static public String incrementNumber(String name) {
    final Matcher matcher = TAIL_NUMBER.matcher(name);
    int i = 0;
    if (matcher.matches()) {
      i = Integer.parseInt(matcher.group(0));
      name = matcher.replaceAll("");
    }
    return name + (i + 1);
  }

  static public String getValidIdentifierFromExpression(@NotNull PsiElement e, @NotNull ResultHolder value) {
    final PsiIdentifier identifier = UsefulPsiTreeUtil.getLastDescendantOfType(e, PsiIdentifier.class);
    if (identifier != null) {
      return ensureIdName(identifier.getText());
    }
    return getValidIdFromType(value);
  }

  static public String getValidIdFromType(@NotNull ResultHolder value) {
    return ensureIdName(value.toStringWithoutConstant());
  }

  static public String ensureIdName(String name) {
    name = name.substring(0, 1).toLowerCase() + name.substring(1);
    return name;
  }

  static public String ensureClassName(String name) {
    name = name.substring(0, 1).toUpperCase() + name.substring(1);
    return name;
  }

  static public String makePlural(String name) {
    // @TODO: Here try to:
    // child -> children
    if (name.equals("child")) return "children";
    return name + "List";
  }

  static public String makeSingular(String name) {
    // @TODO: Here try to:
    // remove *List.
    // children -> child
    // thing:s -> thing
    return name + "Element";
  }
}
