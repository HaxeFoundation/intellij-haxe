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

import com.intellij.plugins.haxe.lang.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeAbstractClassModel extends HaxeClassModel {

  public static final String FORWARD = "@:forward";

  public HaxeAbstractClassModel(@NotNull HaxeAbstractClassDeclaration haxeClass) {
    super(haxeClass);
  }

  @Nullable
  @Override
  public HaxePsiCompositeElement getBodyPsi() {
    return getAbstractClassBody();
  }

  public boolean hasForwards() {
    return hasMeta(FORWARD);
  }

  public HaxeClass getUnderlyingClass() {
    HaxeUnderlyingType underlyingTypePsi = getAbstractClass().getUnderlyingType();
    if (underlyingTypePsi == null) return null;
    HaxeTypeOrAnonymous anonymous = underlyingTypePsi.getTypeOrAnonymous();
    final HaxeType underlyingType = anonymous != null ? anonymous.getType() : null;
    if (underlyingType != null) {
      final HaxeClassResolveResult result = underlyingType.getReferenceExpression().resolveHaxeClass();
      return result.getHaxeClass();
    }

    return null;
  }

  public HaxeAbstractClassDeclaration getAbstractClass() {
    return (HaxeAbstractClassDeclaration)getBasePsi();
  }

  protected HaxeAbstractBody getAbstractClassBody() {
    return getAbstractClass().getAbstractBody();
  }

  public boolean isForwarded(String name) {
    final HaxeMacroClass forwardMeta = getMeta(FORWARD);
    if (forwardMeta != null) {
      final HaxeCustomMeta customMeta = forwardMeta.getCustomMeta();
      final HaxeExpressionList expressionList = customMeta.getExpressionList();
      if (expressionList == null) return true;
      final List<HaxeExpression> list = expressionList.getExpressionList();
      if (list.isEmpty()) return true;
      for (HaxeExpression expression : list) {
        if (expression.getText().equals(name)) return true;
      }
    }
    return false;
  }
}
