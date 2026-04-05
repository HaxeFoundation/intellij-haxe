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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeImportStub;
import com.intellij.plugins.haxe.model.HaxeImportModel;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class HaxeImportStatementPsiMixinImpl extends HaxeStubBasedPsiElementBase<HaxeImportStub>
  implements HaxeImportStatement, HaxeStatementPsiMixin {

  private HaxeImportModel myImportModel = null;

  public HaxeImportStatementPsiMixinImpl(ASTNode node) {
    super(node);
  }

  public HaxeImportStatementPsiMixinImpl(HaxeImportStub stub, IStubElementType<?, ?> nodeType) {
    super(stub, nodeType);
  }

  @NotNull
  @Override
  public HaxeImportModel getModel() {
    if (myImportModel == null || !myImportModel.isValid()) {
      myImportModel = new HaxeImportModel(this);
    }
    return myImportModel;
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) {
      ((HaxeVisitor)visitor).visitImportStatement(this);
    } else {
      super.accept(visitor);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof HaxeImportStatement other)) return false;

    final HaxeReferenceExpression reference = getReferenceExpression();
    final HaxeReferenceExpression otherReference = other.getReferenceExpression();

    if (reference == null || otherReference == null) return false;
    if (!Objects.equals(reference.getText(), otherReference.getText())) return false;

    final boolean hasWildCard = getWildcard() != null;
    final boolean otherHasWildCard = other.getWildcard() != null;

    if (hasWildCard != otherHasWildCard) return false;

    final boolean hasAlias = getAlias() != null;
    final boolean otherHasAlias = other.getAlias() != null;

    if (hasAlias != otherHasAlias) return false;

    final String aliasText = hasAlias ? getAlias().getText() : null;
    final String otherAliasText = otherHasAlias ? other.getAlias().getText() : null;

    return Objects.equals(aliasText, otherAliasText);
  }

  @Override
  public int hashCode() {
    String text;
    if(ApplicationManager.getApplication().isReadAccessAllowed()) {
      text = getText();
    }else {
      text = ApplicationManager.getApplication().runReadAction((Computable<String>)this::getText);
    }
    return Objects.hash(text);
  }
}
