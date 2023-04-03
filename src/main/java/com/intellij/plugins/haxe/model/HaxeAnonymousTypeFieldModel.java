/*
 * Copyright 2018 Ilya Malanin
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

import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousTypeField;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.Nullable;

public class HaxeAnonymousTypeFieldModel extends HaxeMemberModel {
  public HaxeAnonymousTypeFieldModel(HaxeAnonymousTypeField basePsi) {
    super(basePsi);
  }

  public HaxeAnonymousTypeField getAnonymousTypeFieldPsi() {
    return (HaxeAnonymousTypeField)this.getBasePsi();
  }

  public ResultHolder getResultType() {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.getBasePsi());
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getDeclaringClass();
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    return super.getQualifiedInfo();
  }
}
