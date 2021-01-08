/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2021 AS3Boyan
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

import com.intellij.plugins.haxe.lang.psi.HaxeObjectLiteral;
import org.jetbrains.annotations.NotNull;

public class SpecificObjectReference extends SpecificTypeReference {


  public SpecificObjectReference(@NotNull HaxeObjectLiteral context) {
    super(context);
  }


  @Override
  public SpecificObjectReference withConstantValue(Object constantValue) {
    return this; // TODO
  }


  public String toPresentationString() {
    return toString();
  }

  @Override
  public String toString() {
    return context.getText();
  }

  @Override
  public String toStringWithoutConstant() {
    return toPresentationString();
  }

  @Override
  public String toStringWithConstant() {
    return toPresentationString();
  }

  @Override
  public boolean canBeTypeVariable() {
    return false;
  }
}

