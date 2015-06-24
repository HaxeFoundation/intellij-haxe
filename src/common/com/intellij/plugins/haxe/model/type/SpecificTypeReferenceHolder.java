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
package com.intellij.plugins.haxe.model.type;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class SpecificTypeReferenceHolder {
  @NotNull
  private SpecificTypeReference type;

  public SpecificTypeReferenceHolder(@NotNull SpecificTypeReference type) {
    this.type = type;
  }

  @NotNull
  public SpecificTypeReference getType() {
    return type;
  }

  public boolean isUnknown() {
    return type.isUnknown();
  }

  public void setType(@NotNull SpecificTypeReference type) {
    this.type = type;
  }

  static public List<SpecificTypeReference> types(List<SpecificTypeReferenceHolder> holders) {
    LinkedList<SpecificTypeReference> out = new LinkedList<SpecificTypeReference>();
    for (SpecificTypeReferenceHolder holder : holders) {
      out.push(holder.type);
    }
    return out;
  }

  public boolean canAssign(SpecificTypeReferenceHolder that) {
    return HaxeTypeCompatible.isAssignable(this, that);
  }

  public void removeConstant() {
    setType(getType().withoutConstantValue());
  }
}
