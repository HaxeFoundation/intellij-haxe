/*
 * Copyright 2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class HaxeAbstractClassModel extends HaxeClassModel {
  private static final Logger LOG = Logger.getLogger("#HaxeAbstractClassModel");

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

  public HaxeClass getUnderlyingClass(@Nullable final HaxeGenericResolver resolver) {
    HaxeUnderlyingType underlyingTypePsi = getAbstractClass().getUnderlyingType();
    if (underlyingTypePsi == null) {
      // Null is a special type with no declared underlying class, but the generic
      // declares the underlying type.  (e.g. Underlying class for "Null<String>" is "String").
      if ("Null".equals(getName()) && isCoreType()) {
        if (null == resolver) {
          LOG.warning("Can't find underlying class for Null<> because there is no resolver/specialization.");
          return null;
        }
        List<HaxeGenericParamModel> params = getGenericParams();
        if (!params.isEmpty()) {
          return resolveGeneric(params.get(0).getName(), resolver);
        }
      }
      return null;
    }
    HaxeTypeOrAnonymous anonymous = underlyingTypePsi.getTypeOrAnonymous();
    final HaxeType underlyingType = anonymous != null ? anonymous.getType() : null;
    if (underlyingType != null) {
      HaxeReferenceExpression referenceExpression = underlyingType.getReferenceExpression();
      final HaxeClassResolveResult result = referenceExpression.resolveHaxeClass();
      HaxeClass resultClass = result.getHaxeClass();
      return null != resultClass ? resultClass : resolveGeneric(referenceExpression.getIdentifier().getText(), resolver);
    }

    return null;
  }

  @Nullable
  private HaxeClass resolveGeneric(@Nullable String name, @Nullable HaxeGenericResolver resolver) {
    if (null == name || null == resolver) return null;
    ResultHolder result = resolver.resolve(name);
    SpecificHaxeClassReference ref = null != result ? result.getClassType() : null;
    HaxeClass underlyingClass = ref != null ? ref.getHaxeClass() : null;
    return underlyingClass;
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
