/*
 * Copyright 2018 Ilya Malanin
 * Copyright 2019-2020 Eric Bishton
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
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataContent;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class HaxeAbstractClassModel extends HaxeClassModel {
  private static final Logger LOG = Logger.getLogger("#HaxeAbstractClassModel");

  public HaxeAbstractClassModel(@NotNull HaxeAbstractClassDeclaration haxeClass) {
    super(haxeClass);
  }

  @Nullable
  @Override
  public HaxePsiCompositeElement getBodyPsi() {
    return getAbstractClassBody();
  }

  public boolean hasForwards() {
    return hasCompileTimeMeta(HaxeMeta.FORWARD);
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
    if (null == name) return false;

    boolean allEmpty = true;
    HaxeMetadataList forwardMetaList = HaxeMetadataUtils.getMetadataList(getBasePsi(), HaxeMeta.COMPILE_TIME, HaxeMeta.FORWARD);
    for (HaxeMeta forward : forwardMetaList) {
      HaxeMetadataContent content = forward.getContent();
      if (null != content) {
        List<HaxeExpression> expressions = HaxeMetadataUtils.getCompileTimeExpressions(content);
        for (HaxeExpression expression : expressions) {
          allEmpty = false;
          if (expression.getText().equals(name)) {
            return true;
          }
        }
      }
    }
    return allEmpty ? true : false;
  }
}
