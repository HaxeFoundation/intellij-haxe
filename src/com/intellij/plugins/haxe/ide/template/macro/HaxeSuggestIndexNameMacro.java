package com.intellij.plugins.haxe.ide.template.macro;

import com.intellij.codeInsight.template.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.util.HaxeMacroUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSuggestIndexNameMacro extends Macro {
  @Override
  public String getName() {
    return "haxeSuggestIndexName";
  }

  @Override
  public String getPresentableName() {
    return HaxeBundle.message("macro.haxe.index.name");
  }

  @NotNull
  @Override
  public String getDefaultValue() {
    return "i";
  }

  @Override
  public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
    final PsiElement at = context.getPsiElementAtStartOffset();
    final Set<HaxeComponentName> variables = HaxeMacroUtil.findVariables(at);
    final Set<String> names = new THashSet<String>(ContainerUtil.map(variables, new Function<HaxeComponentName, String>() {
      @Override
      public String fun(HaxeComponentName name) {
        return name.getName();
      }
    }));
    for (char i = 'i'; i < 'z'; ++i) {
      if (!names.contains(Character.toString(i))) {
        return new TextResult(Character.toString(i));
      }
    }
    return null;
  }
}
