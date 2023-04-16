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
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeMetadataListOwnerImpl implements com.intellij.plugins.haxe.metadata.psi.HaxeMetadataListOwner {
  ASTNode owner;

  public HaxeMetadataListOwnerImpl(@NotNull ASTNode owner) {
    // Can't use getPsi() in this constructor because it causes a stack overflow.
    this.owner = owner;
  }

  public HaxeMetadataListOwnerImpl(@NotNull PsiElement owner) {
    this.owner = owner.getNode();
  }

  @NotNull
  @Override
  public HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.getMetadataList(owner.getPsi(), metadataType);
  }

  @Override
  public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
    return false;
  }
}
