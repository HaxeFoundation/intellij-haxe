/*
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

class HaxeInitVariableVisitor extends HaxeVisitor {
  public boolean result = false;

  private final String fieldName;

  HaxeInitVariableVisitor(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public void visitElement(PsiElement element) {
    super.visitElement(element);
    if (result) return;
    if (element instanceof HaxeIdentifier || element instanceof HaxePsiToken || element instanceof HaxeStringLiteralExpression) return;
    element.acceptChildren(this);
  }

  @Override
  public void visitAssignExpression(@NotNull HaxeAssignExpression o) {
    HaxeExpression expression = (o.getExpressionList()).get(0);
    if (expression instanceof HaxeReferenceExpression) {
      final HaxeReferenceExpression reference = (HaxeReferenceExpression)expression;
      final HaxeIdentifier identifier = reference.getIdentifier();

      if (identifier.getText().equals(fieldName)) {
        PsiElement firstChild = reference.getFirstChild();
        if (firstChild instanceof HaxeThisExpression || firstChild == identifier) {
          this.result = true;
          return;
        }
      }
    }

    super.visitAssignExpression(o);
  }
}