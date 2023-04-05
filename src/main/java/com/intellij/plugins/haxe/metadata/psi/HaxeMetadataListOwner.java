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
package com.intellij.plugins.haxe.metadata.psi;

import com.intellij.plugins.haxe.lang.psi.HaxeCompileTimeMetadata;
import com.intellij.plugins.haxe.lang.psi.HaxeRunTimeMetadata;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HaxeMetadataListOwner {

  /**
   * Gets the metadata for this element.
   *
   * @param metadataType The type of metadata to retrieve; null for all types.
   * @return A list of metadata items for this element.
   */
  @NotNull
  HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType);

  boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType);

  default boolean hasRuntimeMetadata(HaxeMetadataTypeName name) {
    return hasMetadata(name, HaxeRunTimeMetadata.class);
  }

  default boolean hasCompileTimeMetadata(HaxeMetadataTypeName name) {
    return hasMetadata(name, HaxeCompileTimeMetadata.class);
  }
}
