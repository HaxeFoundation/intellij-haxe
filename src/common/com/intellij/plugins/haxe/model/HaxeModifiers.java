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
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public abstract class HaxeModifiers implements Iterable<HaxeModifier> {
  public boolean hasAnyModifier(HaxeModifier... modifiers) {
    for (HaxeModifier modifier : modifiers) if (hasModifier(modifier)) return true;
    return false;
  }

  public boolean hasAllModifiers(HaxeModifier... modifiers) {
    for (HaxeModifier modifier : modifiers) if (!hasModifier(modifier)) return false;
    return true;
  }

  public HaxeVisibility getVisibility() {
    if (hasModifier(HaxeVisibility.PUBLIC)) return HaxeVisibility.PUBLIC;
    if (hasModifier(HaxeVisibility.PRIVATE)) return HaxeVisibility.PRIVATE;
    return HaxeVisibility.EMPTY;
  }

  public void replaceVisibility(HaxeVisibility newVisibility) {
    replaceModifier(getVisibility(), newVisibility);
  }

  public boolean hasModifier(HaxeModifier modifier) {
    for (HaxeModifier currentModifier : this) if (Objects.equals(currentModifier, modifier)) return true;
    return false;
  }

  abstract public void removeModifier(HaxeModifier oldModifier);
  abstract public void replaceModifier(@Nullable HaxeModifier oldModifier, HaxeModifier newModifier);
  public void addModifier(HaxeModifier newModifier) {
    replaceModifier(null, newModifier);
  }

  public List<HaxeModifier> getAllModifiers() {
    return Lists.newArrayList(this);
  }

  @Override
  abstract public Iterator<HaxeModifier> iterator();

  public String toStringCombined() {
    // @TODO: Combine all modifiers into a string
    return getVisibility().getKeyword();
  }
}
