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
package com.intellij.plugins.haxe.ide;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCommenter implements CodeDocumentationAwareCommenter {

  public static final String SINGLE_LINE_PREFIX = "//";
  public static final String BLOCK_COMMENT_PREFIX = "/*";
  public static final String BLOCK_COMMENT_SUFFIX = "*/";
  public static final String DOC_COMMENT_PREFIX = "/**";
  public static final String DOC_COMMENT_LINE_PREFIX = "*";
  public static final String DOC_COMMENT_SUFFIX = "**/";

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
    return HaxeTokenTypeSets.LINE_COMMENT;
  }

  @Override
  public IElementType getBlockCommentTokenType() {
    return HaxeTokenTypeSets.BLOCK_COMMENT;
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

  @Override
  public String getDocumentationCommentSuffix() {
    return DOC_COMMENT_SUFFIX;
  }

  @Override
  public boolean isDocumentationComment(PsiComment element) {
    return element.getTokenType() == HaxeTokenTypeSets.DOC_COMMENT;
  }
}
