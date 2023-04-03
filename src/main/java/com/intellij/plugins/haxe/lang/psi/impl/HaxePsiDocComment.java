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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.javadoc.PsiDocCommentImpl;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;


/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxePsiDocComment extends PsiDocCommentImpl implements PsiDocComment {

  private HaxeNamedComponent mOwnerComponent;

  public HaxePsiDocComment(@NotNull HaxeNamedComponent owner, @NotNull PsiComment comment) {
    super(comment.getText());
    mOwnerComponent = owner;
  }

  @Override
  public PsiDocCommentOwner getOwner() {
    final PsiElement parent = mOwnerComponent;
    if (parent instanceof PsiDocCommentOwner) {
      final PsiDocCommentOwner owner = (PsiDocCommentOwner) parent;
      if (owner.getDocComment() == this) {
        return owner;
      }
    }
    // HaxeXXX component owners that are not yet adapted to PsiXXX
    return null;
  }

  @Override
  public PsiElement getParent() {
    return mOwnerComponent;
  }

  @Override
  public PsiFile getContainingFile() {
    return mOwnerComponent.getContainingFile();
  }

  @Override
  public boolean isWritable() {
    return SharedImplUtil.isWritable(mOwnerComponent.getNode());
  }

  @NotNull
  @Override
  public Project getProject() {
    return mOwnerComponent.getProject();
  }

  @Override
  public boolean canNavigateToSource() {
    return mOwnerComponent.canNavigateToSource();
  }

  @Override
  public boolean canNavigate() {
    return mOwnerComponent.canNavigate();
  }

  @NotNull
  @Override
  public Language getLanguage() {
    return mOwnerComponent.getLanguage();
  }

  @NotNull
  @Override
  public ASTNode getNode() {
    return mOwnerComponent.getNode();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public String toString() {
    return "HaxePsiDocComment";
  }
}
