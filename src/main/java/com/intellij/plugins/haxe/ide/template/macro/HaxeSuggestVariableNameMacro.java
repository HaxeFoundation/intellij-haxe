/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide.template.macro;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSuggestVariableNameMacro extends Macro {
  @Override
  public String getName() {
    return "haxeSuggestVariableName";
  }

  @Override
  public String getPresentableName() {
    return HaxeBundle.message("macro.haxe.variable.name");
  }

  @NotNull
  @Override
  public String getDefaultValue() {
    return "o";
  }

  @Override
  public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
    return null;
  }
}
