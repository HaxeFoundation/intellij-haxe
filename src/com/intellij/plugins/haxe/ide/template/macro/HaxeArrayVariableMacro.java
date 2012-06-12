package com.intellij.plugins.haxe.ide.template.macro;

import com.intellij.codeInsight.template.*;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeClassResolveResult;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.util.HaxeMacroUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeArrayVariableMacro extends Macro {
  @Override
  public String getName() {
    return "haxeArrayVariable";
  }

  @Override
  public String getPresentableName() {
    return HaxeBundle.message("macro.haxe.array.variable");
  }

  @Override
  public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
    final PsiElement at = HaxeMacroUtil.findElementAt(context);
    final Set<HaxeComponentName> variables = HaxeMacroUtil.findVariables(at);
    final List<HaxeComponentName> filtered = ContainerUtil.filter(variables, new Condition<HaxeComponentName>() {
      @Override
      public boolean value(HaxeComponentName name) {
        final HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(name.getParent());
        final HaxeClass haxeClass = result.getHaxeClass();
        return haxeClass != null && "Array".equalsIgnoreCase(haxeClass.getQualifiedName());
      }
    });
    return filtered.isEmpty() ? null : new PsiElementResult(filtered.iterator().next());
  }
}
