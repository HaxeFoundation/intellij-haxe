/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2020 AS3Boyan
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousTypeBody;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This class is a helper class to handle type parameters with constrains from multiple types
 * https://haxe.org/manual/type-system-type-parameter-constraints.html
 *
 * It makes sure auto-completion and  method/field  resolves works.
 *  NOTE: It does NOT check it the types can actually be unified and is not intended to be used
 *  for anything else other than generics with multiple constraints
 */
public class HaxeTypeParameterMultiType extends AnonymousHaxeTypeImpl {

  private final List<HaxeType> typeList;
  private final List<HaxeAnonymousTypeBody> anonymousTypeBodyList;

  public static HaxeTypeParameterMultiType withTypeList(@NotNull ASTNode node, @NotNull List<HaxeType> typeList) {
    return  new HaxeTypeParameterMultiType(node, typeList, null);
  }
  public static HaxeTypeParameterMultiType withAnonymousList(@NotNull ASTNode node, @NotNull List<HaxeAnonymousTypeBody> anonymousTypeBodyList) {
    return  new HaxeTypeParameterMultiType(node, null, anonymousTypeBodyList);
  }

  public HaxeTypeParameterMultiType(@NotNull ASTNode node, List<HaxeType> typeList, List<HaxeAnonymousTypeBody> anonymousTypeBodyList) {
    super(node);
    this.typeList = typeList != null ? typeList : List.of();
    this.anonymousTypeBodyList = anonymousTypeBodyList  != null  ? anonymousTypeBodyList : List.of();
  }

  public List<HaxeType> getHaxeExtendsList() {
    return typeList;
  }


  @Override
  public @NotNull List<HaxeAnonymousTypeBody> getAnonymousTypeBodyList() {
    return anonymousTypeBodyList;
  }

  @Override
  public @NotNull List<HaxeType> getTypeList() {
    return List.of();
  }

}
