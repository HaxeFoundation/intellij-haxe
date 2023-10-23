/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifierPsiMixin;
import com.intellij.psi.tree.IElementType;

import java.util.Objects;

/**
 * Created by ebishton on 9/27/14.
 */
public class HaxeIdentifierPsiMixinImpl extends HaxePsiCompositeElementImpl implements HaxeIdentifierPsiMixin {

  private static final String MATCH_ANY_IDENTIFIER = "_";

  public HaxeIdentifierPsiMixinImpl(ASTNode node) {
    super(node);
  }

  @Override
  public IElementType getTokenType() {
    return HaxeTokenTypes.IDENTIFIER;
  }

  @Override
  public boolean isMatchAny() {
    return textMatches(MATCH_ANY_IDENTIFIER);
  }
}
