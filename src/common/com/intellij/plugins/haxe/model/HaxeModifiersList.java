/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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
package com.intellij.plugins.haxe.model;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class HaxeModifiersList extends HaxeModifiers {
  private final List<HaxeModifier> modifiers;

  public HaxeModifiersList(List<HaxeModifier> modifiers) {
    this.modifiers = modifiers;
  }

  public HaxeModifiersList(Iterable<HaxeModifier> modifiers) {
    this.modifiers = Lists.newArrayList(modifiers);
  }

  public HaxeModifiersList(HaxeModifier... modifiers) {
    this.modifiers = Arrays.asList(modifiers);
  }

  @Override
  public Iterator<HaxeModifier> iterator() {
    return modifiers.iterator();
  }

  @Override
  public boolean hasModifier(HaxeModifier modifier) {
    return modifiers.contains(modifier);
  }

  @Override
  public void removeModifier(HaxeModifier oldModifier) {
    modifiers.remove(oldModifier);
  }

  @Override
  public void replaceModifier(HaxeModifier oldModifier, HaxeModifier newModifier) {
    int index = modifiers.indexOf(oldModifier);
    if (index >= 0) {
      modifiers.set(index, newModifier);
    }
    else {
      modifiers.add(newModifier);
    }
  }
}
