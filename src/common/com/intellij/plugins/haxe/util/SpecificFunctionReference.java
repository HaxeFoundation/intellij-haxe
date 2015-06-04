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
package com.intellij.plugins.haxe.util;

import com.intellij.psi.PsiElement;

import java.util.List;

public class SpecificFunctionReference implements SpecificTypeReference {
  SpecificTypeReference[] items;

  public SpecificFunctionReference(SpecificTypeReference[] items) {
    this.items = items;
  }

  @Override
  public SpecificTypeReference withConstantValue(Object constantValue) {
    return new SpecificFunctionReference(items);
  }

  @Override
  public Object getConstant() {
    return null;
  }

  @Override
  public PsiElement getElementContext() {
    if (items.length == 0) return null;
    return items[0].getElementContext();
  }

  public SpecificTypeReference getReturnType() {
    if (items.length == 0) return null;
    return items[items.length - 1];
  }

  @Override
  public String toString() {
    String out = "";
    for (int n = 0; n < items.length; n++) {
      if (n > 0) out += " -> ";
      out += items[n];
    }
    //return toStringWithoutConstant();
    return out;
  }

  @Override
  public SpecificTypeReference access(String name) {
    return null;
  }
}
