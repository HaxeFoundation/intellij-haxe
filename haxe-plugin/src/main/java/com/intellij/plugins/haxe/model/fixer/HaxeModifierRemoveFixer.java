/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model.fixer;

import com.intellij.plugins.haxe.model.HaxeModifierType;
import com.intellij.plugins.haxe.model.HaxeModifiersModel;

public class HaxeModifierRemoveFixer extends HaxeFixer {
  private HaxeModifiersModel modifiers;
  private HaxeModifierType modifier;

  public HaxeModifierRemoveFixer(HaxeModifiersModel modifiers, HaxeModifierType modifier) {
    this(modifiers, modifier, "Remove " + modifier.s);
  }

  public HaxeModifierRemoveFixer(HaxeModifiersModel modifiers, HaxeModifierType modifier, String string) {
    super(string);
    this.modifiers = modifiers;
    this.modifier = modifier;
  }

  @Override
  public void run() {
    modifiers.removeModifier(modifier);
  }
}
