/*
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
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor;
import com.intellij.plugins.haxe.model.HaxeUsingModel;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeUsingStatementPsiMixinImpl extends HaxeStatementPsiMixinImpl implements HaxeUsingStatement {
  public HaxeUsingStatementPsiMixinImpl(ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public HaxeUsingModel getModel() {
    return new HaxeUsingModel(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) {
      ((HaxeVisitor)visitor).visitUsingStatement(this);
    }
    else {
      super.accept(visitor);
    }
  }

  @Override
  @Nullable
  public HaxeReferenceExpression getReferenceExpression() {
    return findChildByClass(HaxeReferenceExpression.class);
  }
}
