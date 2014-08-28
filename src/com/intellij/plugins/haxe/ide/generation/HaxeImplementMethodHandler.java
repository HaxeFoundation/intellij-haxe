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

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionPrototypeDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeImplementMethodHandler extends BaseHaxeGenerateHandler {
  @Override
  protected String getTitle() {
    return HaxeBundle.message("haxe.implement.method");
  }

  @Override
  void collectCandidates(HaxeClass haxeClass, List<HaxeNamedComponent> candidates) {
    for (HaxeNamedComponent haxeNamedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      final boolean prototype = haxeNamedComponent instanceof HaxeFunctionPrototypeDeclarationWithAttributes;
      final HaxeClass parentClass = PsiTreeUtil.getParentOfType(haxeNamedComponent, HaxeClass.class, true);
      final boolean interfaceField = HaxeComponentType.typeOf(haxeNamedComponent) == HaxeComponentType.FIELD &&
                                     HaxeComponentType.typeOf(parentClass) == HaxeComponentType.INTERFACE;
      if (!prototype && !interfaceField) continue;

      candidates.add(haxeNamedComponent);
    }
  }

  @Override
  protected BaseCreateMethodsFix createFix(HaxeClass haxeClass) {
    return new OverrideImplementMethodFix(haxeClass, false);
  }
}
