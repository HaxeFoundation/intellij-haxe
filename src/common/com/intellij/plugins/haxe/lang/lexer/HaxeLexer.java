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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.LookAheadLexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;

public class HaxeLexer extends LookAheadLexer {
  private static final TokenSet tokensToMerge = TokenSet.create(
    MSL_COMMENT,
    MML_COMMENT,
    WSNLS
  );

  @Nullable
  private Project myProject;

  public HaxeLexer(Project project) {
    super(new HaxeMetaCoalescingLexerAdapter(new MergingLexerAdapter(new HaxeFlexLexer(project), tokensToMerge)));
    myProject = project;
  }
}