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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModifierType;
import com.intellij.plugins.haxe.model.HaxeModifiersModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.log4j.Level;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeOverrideMethodHandler extends BaseHaxeGenerateHandler {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.ide.generation.HaxeOverrideMethodHandler");
  {
    LOG.info("Loaded HaxeOverrideMethodHandler");
    LOG.setLevel(Level.DEBUG);
  }

  @Override
  protected String getTitle() {
    return HaxeBundle.message("haxe.override.method");
  }

  @Override
  void collectCandidates(HaxeClass haxeClass, List<HaxeNamedComponent> candidates) {
    HaxeClassModel clazz = haxeClass.getModel();

    for (HaxeMethodModel method : clazz.getAncestorMethods()) {
      // Only add methods that doesn't have @:final or static modifiers and also that are not constructors
      if (
        !method.getModifiers().hasAnyModifier(
          HaxeModifierType.FINAL,
          HaxeModifierType.STATIC
        ) &&
        !method.isConstructor()
      ) {
        candidates.add(method.getMethodPsi());
      }
    }
  }

  @Override
  protected BaseCreateMethodsFix createFix(HaxeClass haxeClass) {
    return new OverrideImplementMethodFix(haxeClass, true);
  }
}
