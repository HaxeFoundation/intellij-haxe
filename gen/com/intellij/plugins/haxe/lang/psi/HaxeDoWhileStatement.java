/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeDoWhileStatement extends HaxeStatementPsiMixin {

  @NotNull
  List<HaxeBlockStatement> getBlockStatementList();

  @NotNull
  List<HaxeBreakStatement> getBreakStatementList();

  @NotNull
  List<HaxeContinueStatement> getContinueStatementList();

  @NotNull
  List<HaxeDoWhileStatement> getDoWhileStatementList();

  @NotNull
  List<HaxeExpression> getExpressionList();

  @NotNull
  List<HaxeForStatement> getForStatementList();

  @NotNull
  List<HaxeIfStatement> getIfStatementList();

  @NotNull
  List<HaxeLocalFunctionDeclaration> getLocalFunctionDeclarationList();

  @NotNull
  List<HaxeLocalVarDeclaration> getLocalVarDeclarationList();

  @Nullable
  HaxeMacroClassList getMacroClassList();

  @NotNull
  List<HaxeReturnStatement> getReturnStatementList();

  @NotNull
  List<HaxeSwitchStatement> getSwitchStatementList();

  @NotNull
  List<HaxeThrowStatement> getThrowStatementList();

  @NotNull
  List<HaxeTryStatement> getTryStatementList();

  @NotNull
  List<HaxeWhileStatement> getWhileStatementList();

}
