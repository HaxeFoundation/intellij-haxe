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
import com.intellij.plugins.haxe.lang.psi.HaxeStatementPsiMixin;
import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeUsingStub;
import com.intellij.plugins.haxe.model.HaxeUsingModel;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class HaxeUsingStatementPsiMixinImpl extends HaxeStubBasedPsiElementBase<HaxeUsingStub>
  implements HaxeUsingStatement, HaxeStatementPsiMixin {


  private final AtomicReference<HaxeUsingModel> haxeUsingModel = new AtomicReference<>();

  public HaxeUsingStatementPsiMixinImpl(ASTNode node) {
    super(node);
  }

  public HaxeUsingStatementPsiMixinImpl(HaxeUsingStub stub, IElementType nodeType) {
    super(stub, nodeType);
  }

  @NotNull
  @Override
  public HaxeUsingModel getModel() {
    HaxeUsingModel model = haxeUsingModel.get();
    if (model != null && model.isValid()) {
      return model;
    }
    HaxeUsingModel newValue = new HaxeUsingModel(this);
    if (haxeUsingModel.compareAndSet(null, newValue)) {
      return newValue;
    } else {
      return haxeUsingModel.get();
    }
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
