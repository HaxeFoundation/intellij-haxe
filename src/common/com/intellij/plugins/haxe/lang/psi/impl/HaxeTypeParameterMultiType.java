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
  private final HaxeAnonymousTypeBodyImpl body;

  public HaxeTypeParameterMultiType(@NotNull ASTNode node, @NotNull List<HaxeType> typeList) {
    super(node);
    this.body = new HaxeAnonymousTypeBodyImpl(node);
    this.typeList = typeList;
  }

  public List<HaxeType> getHaxeExtendsList() {
    return typeList;
  }

  @NotNull
  @Override
  public HaxeAnonymousTypeBody getAnonymousTypeBody() {
    return body;
  }

}
