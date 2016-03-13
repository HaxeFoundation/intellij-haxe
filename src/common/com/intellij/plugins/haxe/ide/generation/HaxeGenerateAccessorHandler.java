/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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

      if (!myStrategy.accept(haxeNamedComponent.getName(), componentMap.keySet())) continue;

      candidates.add(haxeNamedComponent);
    }
  }
}
