/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeNameSuggesterUtil {
  private HaxeNameSuggesterUtil() {
  }

  private static String deleteNonLetterFromString(@NotNull final String string) {
    Pattern pattern = Pattern.compile("[^a-zA-Z_]+");
    Matcher matcher = pattern.matcher(string);
    return matcher.replaceAll("_");
  }

  @NotNull
  public static Collection<String> generateNames(@NotNull String name) {
    name = StringUtil.decapitalize(deleteNonLetterFromString(StringUtil.unquoteString(name.replace('.', '_'))));
    if (name.startsWith("get")) {
      name = name.substring(3);
    }
    else if (name.startsWith("is")) {
      name = name.substring(2);
    }
    while (name.startsWith("_")) {
      name = name.substring(1);
    }
    while (name.endsWith("_")) {
      name = name.substring(0, name.length() - 1);
    }
    final int length = name.length();
    final Collection<String> possibleNames = new LinkedHashSet<String>();
    for (int i = 0; i < length; i++) {
      if (Character.isLetter(name.charAt(i)) &&
          (i == 0 || name.charAt(i - 1) == '_' || (Character.isLowerCase(name.charAt(i - 1)) && Character.isUpperCase(name.charAt(i))))) {
        final String candidate = StringUtil.decapitalize(name.substring(i));
        if (candidate.length() < 25) {
          possibleNames.add(candidate);
        }
      }
    }
    // prefer shorter names
    ArrayList<String> reversed = new ArrayList<String>(possibleNames);
    Collections.reverse(reversed);
    return reversed;
  }
}
