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
