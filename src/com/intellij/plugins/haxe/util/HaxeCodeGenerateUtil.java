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

import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.openapi.util.Pair;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCodeGenerateUtil {

  public static final String CLASS_PREFIX = "class Main{";
  public static final String CLASS_SUFFIX = "}";
  public static final String FUNCTION_PREFIX = "function main(){";
  public static final String FUNCTION_SUFFIX = "}";

  public static Pair<String, Integer> wrapStatement(String statement) {
    statement = trimDummy(statement);
    final String function = FUNCTION_PREFIX + statement + FUNCTION_SUFFIX;
    final Pair<String, Integer> pair = wrapFunction(function);
    return new Pair<String, Integer>(pair.getFirst(), pair.getSecond() + FUNCTION_SUFFIX.length());
  }

  public static Pair<String, Integer> wrapFunction(String function) {
    function = trimDummy(function);
    return new Pair<String, Integer>(CLASS_PREFIX + function + CLASS_SUFFIX, CLASS_SUFFIX.length());
  }

  private static String trimDummy(String text) {
    if (text.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) {
      text = text.substring(0, text.length() - CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.length());
    }
    return text;
  }
}
