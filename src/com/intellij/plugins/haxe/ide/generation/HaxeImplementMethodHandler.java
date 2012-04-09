package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionPrototypeDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeImplementMethodHandler extends BaseHaxeGenerateHandler {
  @Override
  protected String getTitle() {
    return HaxeBundle.message("haxe.implement.method");
  }

  @Override
  void collectCandidates(HaxeClass haxeClass, Collection<HaxeNamedComponent> candidates) {
    for (HaxeNamedComponent haxeNamedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      if (!(haxeNamedComponent instanceof HaxeFunctionPrototypeDeclarationWithAttributes)) continue;

      candidates.add(haxeNamedComponent);
    }
  }

  @Override
  protected BaseCreateMethodsFix createFix(HaxeClass haxeClass) {
    return new OverrideImplementMethodFix(haxeClass, false);
  }
}
