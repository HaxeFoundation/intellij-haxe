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

import com.intellij.plugins.haxe.model.HaxeTypesModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class ResultHolder {
  static public ResultHolder[] EMPTY = new ResultHolder[0];

  @NotNull
  private SpecificTypeReference type;

  private boolean canMutate = true;
  private int mutationCount = 0;

  @Nullable
  public PsiElement element = null;

  @Nullable
  public String name;

  public ResultHolder(@NotNull SpecificTypeReference type) {
    this.type = type;
    check(false);
  }

  private void check(boolean type) {
    /*
    if (isUnknown()) {
      System.out.println("Holder set to unknown (" + type + "): " + System.identityHashCode(this));
    }
    */
  }

  @NotNull
  public SpecificTypeReference getType() {
    return type;
  }

  @Nullable
  public SpecificFunctionReference getFunctionType() {
    return (type instanceof SpecificFunctionReference) ? (SpecificFunctionReference)type : null;
  }

  @Nullable
  public SpecificHaxeClassReference getClassType() {
    return (type instanceof SpecificHaxeClassReference) ? (SpecificHaxeClassReference)type : null;
  }

  public boolean isUnknown() {
    return type.isUnknown();
  }

  public ResultHolder setType(@Nullable SpecificTypeReference type) {
    return setType(type, null);
  }

  public ResultHolder setType(@Nullable SpecificTypeReference type, @Nullable PsiElement element) {
    if (type == null) {
      type = HaxeTypesModel.fromElement(this.getElementContext()).DYNAMIC;
    }
    this.type = type;
    this.element = element;
    mutationCount++;
    check(true);
    return this;
  }


  public void disableMutating() {
    this.canMutate = false;
  }

  public boolean hasMutated() {
    return this.mutationCount > 0;
  }

  public boolean canMutate() {
    return this.canMutate;
  }

  public boolean isImmutable() {
    return !this.canMutate;
  }

  static public List<SpecificTypeReference> types(List<ResultHolder> holders) {
    LinkedList<SpecificTypeReference> out = new LinkedList<SpecificTypeReference>();
    for (ResultHolder holder : holders) {
      out.push(holder.type);
    }
    return out;
  }

  public boolean canAssign(ResultHolder that) {
    return HaxeTypeCompatible.canAssignToFrom(this, that);
  }

  public void removeConstant() {
    setType(getType().withoutConstantValue(), null);
  }

  public String toString() {
    return this.getType().toString();
  }

  public String toStringWithoutConstant() {
    return this.getType().toStringWithoutConstant();
  }

  public String toStringWithConstant(boolean withConstant) {
    if (withConstant) {
      return this.getType().toStringWithConstant();
    } else {
      return this.getType().toStringWithoutConstant();
    }
  }

  public ResultHolder duplicate() {
    // Don't duplicate unknown holders
    return isUnknown() ? this : new ResultHolder(this.getType().duplicate());
  }

  public ResultHolder withConstantValue(Object constantValue) {
    return duplicate().setType(getType().withConstantValue(constantValue), null);
  }

  public ResultHolder withoutConstantValue() {
    return withConstantValue(null);
  }

  public Object getConstant() {
    return getType().getConstant();
  }

  public PsiElement getElementContext() {
    return type.getElementContext();
  }

  public ResultHolder applySpecifics(HaxeGenericResolver generic) {
    if (this.isUnknown()) return this;
    final ResultHolder result = generic.resolve(this.toStringWithoutConstant());
    if (result != null) {
      if (result.isUnknown() || result.getType().isUnknown()) {
        System.out.println("Nope!");
      }
      this.setType(result.getType());
    } else {
      getType().applyGenerics(generic);
    }
    return this;
  }
}
