/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Hashtable;
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
    // Properties and variables are also part of interface declarations that must be implemented,
    // so they can/should show up in this list.

    // Get all of the names and sort them into lists of class methods and interface declarations.
    List<HaxeNamedComponent> namedComponents = HaxeResolveUtil.findNamedSubComponents(false, null, haxeClass);
    List<HaxeNamedComponent> interfaceMethods = new ArrayList<>();
    Hashtable<String, HaxeNamedComponent> classMethods = new Hashtable<>();
    for (HaxeNamedComponent component : namedComponents) {
      final HaxeClass parentClass = PsiTreeUtil.getParentOfType(component, HaxeClass.class, true);
      if (HaxeComponentType.typeOf(parentClass) == HaxeComponentType.CLASS) {
        classMethods.put(component.getName(), component);
      } else if (HaxeComponentType.typeOf(parentClass) == HaxeComponentType.INTERFACE) {
        interfaceMethods.add(component);
      }
    }

    // Walk the interface names and make sure that a method implementation exists.
    for (HaxeNamedComponent component : interfaceMethods) {
      if (!classMethods.containsKey(component.getName())) {
        candidates.add(component);
      }
    }
  }

  @Override
  protected BaseCreateMethodsFix<HaxeNamedComponent> createFix(HaxeClass haxeClass) {
    return new OverrideImplementMethodFix(haxeClass, false);
  }
}
