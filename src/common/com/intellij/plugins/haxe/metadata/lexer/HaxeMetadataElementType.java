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
package com.intellij.plugins.haxe.metadata.lexer;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeEmbeddedElementType;
import com.intellij.plugins.haxe.metadata.HaxeMetadataLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * An element type for lexemes emitted from the MetadataLexer and used in the MetadataParser.
 */
public class HaxeMetadataElementType extends IElementType {
  public HaxeMetadataElementType(@NotNull @NonNls String debugName) {
    super(debugName, HaxeMetadataLanguage.INSTANCE);
  }

  /**
   * Factory method to create new
   *
   * @param name
   * @return
   */
  public static IElementType createToken(String name) {
    if ("HAXE_CODE".equals(name)
        || "CT_META_ARGS".equals(name)
        || "RT_META_ARGS".equals(name)) {
      return new HaxeEmbeddedElementType(name, HaxeLanguage.INSTANCE);
    }
    return new HaxeMetadataElementType(name);
  }
}
