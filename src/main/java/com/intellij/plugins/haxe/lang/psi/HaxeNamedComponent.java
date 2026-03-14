/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeNamedComponent extends HaxePsiCompositeElement {
  @Nullable
  HaxeComponentName getComponentName();

  @Nullable
  HaxeNamedComponent getTypeComponent();

  boolean isPublic();

  boolean isStatic();

  boolean isOverride();

  boolean isOverload();

  boolean isInline();

  String filterName();

  PsiElement getModiferPsi(IElementType tokenType);

  // Added the slowdowns from HaxeComponentType.typeOf()
  // se method description for explanation description
  HaxeComponentType getComponentType();

  default boolean hasMeta(HaxeMetadataTypeName meta) {
    return HaxeMetadataUtils.hasMeta(this, meta);
  }

  default boolean hasCompileTimeMeta(HaxeMetadataTypeName meta) {
    return HaxeMetadataUtils.hasMeta(this, HaxeMetadataCompileTimeMeta.class, meta);
  }

  default HaxeMetadataList getCompileTimeMeta(HaxeMetadataTypeName meta) {
    return HaxeMetadataUtils.getMetadataList(this, HaxeMetadataCompileTimeMeta.class, meta);
  }
}
