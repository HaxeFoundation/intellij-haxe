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
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataContent;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class HaxeAbstractClassModel extends HaxeClassModel {
  private static final Logger LOG = Logger.getLogger("#HaxeAbstractClassModel");

  public HaxeAbstractClassModel(@NotNull HaxeAbstractTypeDeclaration haxeClass) {
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
          ResultHolder holder = resolver.resolveTypeParameter(params.get(0).getTypeParameter());
          if(holder != null && holder.getClassType() != null )return holder.getClassType().getHaxeClass();
        }
      }
      return null;
    }
    HaxeTypeOrAnonymous anonymous = underlyingTypePsi.getTypeOrAnonymous();
    final HaxeType underlyingType = anonymous != null ? anonymous.getType() : null;
    if (underlyingType != null) {
      HaxeReferenceExpression referenceExpression = underlyingType.getReferenceExpression();
      PsiElement resolve = referenceExpression.resolve();
      if(resolve instanceof HaxeTypeParameterDeclaration typeParameter) {
        ResultHolder holder = resolver == null ? null : resolver.resolveTypeParameter(typeParameter);
        if(holder != null && holder.getClassType() != null )return holder.getClassType().getHaxeClass();
      }else if (resolve instanceof  HaxeClass underlHaxeClass) {
        if(resolver != null) {
          ResultHolder resolvedUnderlyingClass = resolver.resolve(underlHaxeClass);
          if(resolvedUnderlyingClass != null && !resolvedUnderlyingClass.isUnknown()) {
            if(resolvedUnderlyingClass.getClassType() != null) {
              return resolvedUnderlyingClass.getClassType().getHaxeClass();
            }
          }
        }
        return  underlHaxeClass;
      }
    }

    return null;
  }


  public HaxeAbstractTypeDeclaration getAbstractClass() {
    return (HaxeAbstractTypeDeclaration)getBasePsi();
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
    return allEmpty;
  }
}
