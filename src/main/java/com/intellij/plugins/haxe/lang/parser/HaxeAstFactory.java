/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.ASTFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiTokenImpl;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.PsiCommentImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 10/24/14.
 */
public class HaxeAstFactory extends ASTFactory {

  // Default implementations of the base class return null, which
  // forces the DefaultFactoryHolder.DEFAULT factory to be used.
  // (Meaning things get created anyway.)  Since that's the normal
  // functionality, it's fine for most things.

  public HaxeAstFactory() {
    super();
  }

  @Nullable
  @Override
  public LazyParseableElement createLazy(ILazyParseableElementType type, CharSequence text) {
    return super.createLazy(type, text);
  }

  @Nullable
  @Override
  public CompositeElement createComposite(IElementType type) {
    return super.createComposite(type);
  }

  @Nullable
  @Override
  public LeafElement createLeaf(IElementType type, CharSequence text) {
    if (HaxeTokenTypeSets.COMMENTS.contains(type) && !typeIsMeta(type)) {
      return new PsiCommentImpl(type, text);
    }

    return new HaxePsiTokenImpl(type, text);
  }

  private boolean typeIsMeta(IElementType type) {
    return type == HaxeTokenTypes.EMBEDDED_META;
  }
}
