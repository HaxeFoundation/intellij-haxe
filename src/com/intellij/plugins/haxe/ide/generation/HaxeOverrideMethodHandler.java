package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeOverrideMethodHandler extends BaseHaxeGenerateHandler {
  @Override
  protected String getTitle() {
    return HaxeBundle.message("haxe.override.method");
  }

  @Override
  void collectCandidates(HaxeClass haxeClass, List<HaxeNamedComponent> candidates) {
    for (HaxeNamedComponent haxeNamedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      if (!(haxeNamedComponent instanceof HaxeFunctionDeclarationWithAttributes)) continue;
      if (haxeNamedComponent.isStatic()) continue;
      // already
      if (haxeNamedComponent.isOverride() && PsiTreeUtil.getParentOfType(haxeNamedComponent, HaxeClass.class) == haxeClass) continue;
      // constructor
      if ("new".equals(haxeNamedComponent.getName())) continue;

      candidates.add(haxeNamedComponent);
    }
  }

  @Override
  protected BaseCreateMethodsFix createFix(HaxeClass haxeClass) {
    return new OverrideImplementMethodFix(haxeClass, true);
  }
}
