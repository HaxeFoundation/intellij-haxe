package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeVarDeclarationPart;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;

import java.util.List;
import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public abstract class HaxeGenerateAccessorHandler extends BaseHaxeGenerateHandler {

  private final CreateGetterSetterFix.Strategy myStrategy;

  protected HaxeGenerateAccessorHandler(CreateGetterSetterFix.Strategy strategy) {
    myStrategy = strategy;
  }

  @Override
  protected BaseCreateMethodsFix createFix(HaxeClass haxeClass) {
    return new CreateGetterSetterFix(haxeClass, myStrategy);
  }

  @Override
  void collectCandidates(HaxeClass haxeClass, List<HaxeNamedComponent> candidates) {
    final List<HaxeNamedComponent> subComponents = HaxeResolveUtil.getNamedSubComponents(haxeClass);
    final Map<String, HaxeNamedComponent> componentMap = HaxeResolveUtil.namedComponentToMap(subComponents);

    for (HaxeNamedComponent haxeNamedComponent : subComponents) {
      if (!(haxeNamedComponent instanceof HaxeVarDeclarationPart)) continue;
      if (haxeNamedComponent.isStatic()) continue;

      if (!myStrategy.accept(haxeNamedComponent.getName(), componentMap.keySet())) continue;

      candidates.add(haxeNamedComponent);
    }
  }
}
