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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.TokenSet;
import java.util.Map;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;

public class HaxeLexer extends LookAheadLexer {
  // PPBODY: the flex lexer runs the FULL rule set over inactive conditional
  // branches and remaps every token (whitespace included) to PPBODY, so a
  // branch is a contiguous run; merging folds it into one token per region
  // between directives, the chameleon HaxeInactiveBodyElementType parses
  private static final TokenSet tokensToMerge = TokenSet.create(
    MSL_COMMENT,
    MML_COMMENT,
    WSNLS,
    PPBODY
  );

  @Nullable
  private Project myProject;

  public HaxeLexer(Project project) {
    this(project, null, true);
  }

  private HaxeLexer(Project project, @Nullable Supplier<Map<String, String>> definitions, boolean remapInactiveToPpbody) {
    super(new HaxeMetaCoalescingLexerAdapter(new MergingLexerAdapter(new HaxeFlexLexer(project, definitions, remapInactiveToPpbody), tokensToMerge)));
    myProject = project;
  }

  /**
   * A lexer whose {@code #if} evaluation follows the file's own resolve
   * context ({@link HaxeConditionalExpression#definitionsForLexing(Project, PsiFile)});
   * the project-wide set without a file.
   */
  public static HaxeLexer forFile(@NotNull Project project, @Nullable PsiFile file) {
    return new HaxeLexer(project, () -> HaxeConditionalExpression.definitionsForLexing(project, file), true);
  }

  /**
   * Lexes inactive conditional branches with their REAL token types instead
   * of PPBODY, so the editor's token-stream mechanics (brace matching and
   * auto-close, enter between braces) work in dead code. Only for the editor
   * highlighter - the parser needs the PPBODY blobs.
   */
  public static HaxeLexer forHighlighting(Project project) {
    return forHighlighting(project, null);
  }

  /** The highlighting lexer of one file: {@code #if} follows the file's own context; a null project (color settings page) has no defines. */
  public static HaxeLexer forHighlighting(@Nullable Project project, @Nullable VirtualFile file) {
    Supplier<Map<String, String>> definitions = project == null ? null : () -> HaxeConditionalExpression.definitionsForLexing(project, file);
    return new HaxeLexer(project, definitions, false);
  }
}