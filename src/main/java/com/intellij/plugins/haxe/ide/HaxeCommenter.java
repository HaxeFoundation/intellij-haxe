/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
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
package com.intellij.plugins.haxe.ide;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/// Comment syntax for Haxe. Doc comments come in two styles:
///
/// ```haxe
/// /**
///     Haxe style: bare lines, closed by two stars.
/// **/
///
/// /**
///  * Javadoc style: a star on each line.
///  */
/// ```
///
/// The code style picks the style of new comments ([#usesLeadingAsterisks]).
///
/// @author: Fedor.Korotkov
public class HaxeCommenter implements CodeDocumentationAwareCommenter {

  public static final String SINGLE_LINE_PREFIX = "//";

  public static final String BLOCK_COMMENT_PREFIX = "/*";
  public static final String BLOCK_COMMENT_SUFFIX = "*/";

  public static final String DOC_COMMENT_PREFIX = "/**";
  public static final String DOC_COMMENT_LINE_PREFIX = "*";
  public static final String HAXE_DOC_COMMENT_SUFFIX = "**/";
  public static final String JAVADOC_COMMENT_SUFFIX = "*/";

  // a Javadoc-style line puts its star under the opener's first star, one column in
  public static final String JAVADOC_LINE_START = " " + DOC_COMMENT_LINE_PREFIX + " ";
  public static final String JAVADOC_CLOSER_LINE = " " + JAVADOC_COMMENT_SUFFIX;

  /** Whether new doc comments in the file are Javadoc style, with a star on each line, rather than Haxe style. */
  public static boolean usesLeadingAsterisks(@NotNull PsiFile file) {
    return CodeStyleManager.getInstance(file.getProject()).getDocCommentSettings(file).isLeadingAsteriskEnabled();
  }

  /** Whether the comment is Javadoc style; one without lines of its own yet takes the style of new comments. */
  public static boolean usesLeadingAsterisks(@NotNull HaxePsiDocCommentImpl docComment) {
    return docComment.hasInteriorLines() ? docComment.isStarStyle() : usesLeadingAsterisks(docComment.getContainingFile());
  }

  /** The closing line a new doc comment in the file gets, after its indent. */
  public static @NotNull String newDocCommentCloserLine(@NotNull PsiFile file) {
    return usesLeadingAsterisks(file) ? JAVADOC_CLOSER_LINE : HAXE_DOC_COMMENT_SUFFIX;
  }

  @Override
  public String getLineCommentPrefix() {
    return SINGLE_LINE_PREFIX;
  }

  @Override
  public String getBlockCommentPrefix() {
    return BLOCK_COMMENT_PREFIX;
  }

  @Override
  public String getBlockCommentSuffix() {
    return BLOCK_COMMENT_SUFFIX;
  }

  @Override
  public String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public String getCommentedBlockCommentSuffix() {
    return null;
  }

  @Override
  public IElementType getLineCommentTokenType() {
    return HaxeTokenTypeSets.MSL_COMMENT;
  }

  @Override
  public IElementType getBlockCommentTokenType() {
    return HaxeTokenTypeSets.MML_COMMENT;
  }

  @Override
  public IElementType getDocumentationCommentTokenType() {
    return HaxeTokenTypeSets.DOC_COMMENT;
  }

  @Override
  public String getDocumentationCommentPrefix() {
    return DOC_COMMENT_PREFIX;
  }

  @Override
  public String getDocumentationCommentLinePrefix() {
    return DOC_COMMENT_LINE_PREFIX;
  }

  // the platform closes the doc comments it creates with this; the Enter handler closes Javadoc-style ones itself
  @Override
  public String getDocumentationCommentSuffix() {
    return HAXE_DOC_COMMENT_SUFFIX;
  }

  @Override
  public boolean isDocumentationComment(PsiComment element) {
    return element.getTokenType() == HaxeTokenTypeSets.DOC_COMMENT;
  }
}
