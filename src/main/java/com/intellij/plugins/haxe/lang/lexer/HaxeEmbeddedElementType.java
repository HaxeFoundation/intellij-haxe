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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An element type for embedded languages (metadata, dox) occurring in the Haxe language..
 * The HaxeMetaElementType is a Haxe "wrapping" element that occurs in the Haxe AST.
 * Its *contents* are parsed via other languages, e.g. MetadataLexer and MetadataParser
 * (which emit HaxeMetadataElementType elements).
 */
public class HaxeEmbeddedElementType extends HaxeLazyParseableElementType {

  public HaxeEmbeddedElementType(@NotNull String debugName,
                                 @NotNull Language language) {
    super(debugName, language);
  }

  @Override
  public boolean isParsable(@NotNull CharSequence buffer, @NotNull Language fileLanguage, @NotNull Project project) {
    // Don't allow a reparse of the data inside independently of the reset of the file.
    // If you do, then adding a character which changes the meaning of the node (such
    // as adding a paren to metadata to close it early) will not reparse the portions
    // of the file that are no longer a valid part of the node -- and the screen isn't
    // updated as expected.
    return false;
  }

  @Nullable
  @Override
  public ASTNode createNode(CharSequence text) {
    return new LazyParseablePsiElement(this, text);
  }

}
