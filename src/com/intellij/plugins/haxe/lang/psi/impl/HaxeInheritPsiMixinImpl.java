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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeInheritPsiMixin;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeList;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeParam;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.ArrayFactory;
import com.intellij.util.IncorrectOperationException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * HaxeInherit is analogous to PsiJavaCodeReferenceElement
 * Created by ebishton on 10/8/14.
 */
public class HaxeInheritPsiMixinImpl extends HaxePsiCompositeElementImpl implements HaxeInheritPsiMixin {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.HaxeInheritPsiMixinImpl");

  {
    // Turn on all local messages.
    LOG.setLevel(Level.DEBUG);
  }

  /**
   * The empty array of PSI Java code references which can be reused to avoid unnecessary allocations.
   */
  HaxeInheritPsiMixin[] EMPTY_ARRAY = new HaxeInheritPsiMixinImpl[0];

  ArrayFactory<HaxeInheritPsiMixin> ARRAY_FACTORY = new ArrayFactory<HaxeInheritPsiMixin>() {
    @NotNull
    @Override
    public HaxeInheritPsiMixin[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new HaxeInheritPsiMixin[count];
    }
  };


  public HaxeInheritPsiMixinImpl(ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiClassType[] getReferencedTypes() {
    return new PsiClassType[0];
  }

  @NotNull
  @Override
  public PsiJavaCodeReferenceElement[] getReferenceElements() {
    return new PsiJavaCodeReferenceElement[0];
  }

  @Override
  public Role getRole() {
    return null;
  }
}
