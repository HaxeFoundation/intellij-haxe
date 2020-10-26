/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.surroundWith;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeBlockStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeDoWhileStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeGuardedStatement;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeStatementUtils;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDoWhileSurrounder extends HaxeManyStatementsSurrounder {
  @NotNull
  @Override
  protected PsiElement doSurroundElements(PsiElement[] elements, PsiElement parent) {
    final HaxeDoWhileStatement whileStatement =
      (HaxeDoWhileStatement)HaxeElementGenerator.createStatementFromText(elements[0].getProject(), "do {\n} while(a);");
    List<HaxeBlockStatement> blockStatementList = whileStatement.getBody().getBlockStatementList();
    addStatements(blockStatementList.get(0), elements);
    return whileStatement;
  }

  @Override
  protected TextRange getSurroundSelectionRange(PsiElement element) {
    final List<HaxeExpression> expressionList = HaxeStatementUtils.getExpressionList(element);
    return expressionList.get(expressionList.size() - 1).getTextRange();
  }

  @Override
  public String getTemplateDescription() {
    return HaxeBundle.message("haxe.surrounder.do.while");
  }
}
