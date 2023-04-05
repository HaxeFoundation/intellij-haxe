/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.metadata.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeCompileTimeMetadata;
import com.intellij.plugins.haxe.lang.psi.HaxeMetadataContent;
import com.intellij.plugins.haxe.lang.psi.HaxeMetadataType;
import com.intellij.plugins.haxe.lang.psi.HaxeRunTimeMetadata;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiCompositeElementImpl;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@CustomLog
public class HaxeMetaImpl extends HaxePsiCompositeElementImpl implements HaxeMeta {


  public HaxeMetaImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public HaxeMetadataType getType() {
    return findNotNullChildByType(HaxeTokenTypes.METADATA_TYPE);
  }

  @Override
  public HaxeMetadataContent getContent() {
    return findChildByType(HaxeTokenTypes.METADATA_CONTENT);
  }

  @Override
  @NotNull
  public PsiElement getContainer() {
    PsiElement container = UsefulPsiTreeUtil.getParent(this, HaxeTokenTypes.METADATA_DECLARATION);
    if (null == container) {
      log.error("Could not find EMBEDDED_META parent for " + getDebugName());
    }
    return container;
  }

  @Override
  public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
    // Metadata does not have it's own metadata.  All meta is attributed to the next Haxe element (class, method, statement, etc.).
    return false;
  }

  @Override
  public boolean isType(HaxeMetadataTypeName type) {
    if (null == type) return false;
    return type.matches(this.getType());
  }

  @Override
  public boolean isType(HaxeMeta meta) {
    if (null == meta) return false;
    return HaxeMetadataTypeName.matches(this.getType(), meta.getType());
  }

  @Override
  public boolean isType(String name) {
    if (null == name || name.isEmpty()) return false;

    int trim = 0;
    if ('@' == name.charAt(0)) {
      trim++;
      if (':' == name.charAt(1)) {
        trim++;
      }
    }
    if (trim > 0) {
      name = name.substring(trim);
    }

    return this.getType().textMatches(name);
  }

  @Override
  public boolean isRunTimeMeta() {
    return this instanceof HaxeRunTimeMetadata;
  }

  @Override
  public boolean isCompileTimeMeta() {
    return this instanceof HaxeCompileTimeMetadata;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // NOTE: equals and hashcode are used by HaxeMetadataList to find metadata of a given type (regardless of content).
  //       It breaks the normal use of equals, so be careful when using it.  Most code should use isType() instead.
  @Override
  public boolean equals(Object o) {
    return null != o && o.getClass() == this.getClass() && isType((HaxeMeta)o);
  }
  @Override
  public int hashCode() {
    return Objects.hash(getClass(), getType());
  }
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
