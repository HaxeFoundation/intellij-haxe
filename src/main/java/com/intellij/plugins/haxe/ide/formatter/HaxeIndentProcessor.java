/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
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
package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.COMMENTS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.FUNCTION_DEFINITION;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIndentProcessor {
  private final CommonCodeStyleSettings settings;

  public HaxeIndentProcessor(CommonCodeStyleSettings settings) {
    this.settings = settings;
  }

  public Indent getChildIndent(ASTNode node) {
    final IElementType elementType = node.getElementType();
    final ASTNode prevSibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(node);
    final IElementType prevSiblingType = prevSibling == null ? null : prevSibling.getElementType();
    final ASTNode parent = node.getTreeParent();
    final IElementType parentType = parent != null ? parent.getElementType() : null;
    final ASTNode superParent = parent == null ? null : parent.getTreeParent();
    final IElementType superParentType = superParent == null ? null : superParent.getElementType();
    final ASTNode firstChild = node.getFirstChildNode();
    final IElementType firstChildType = firstChild == null ? null : firstChild.getElementType();

    final int braceStyle = FUNCTION_DEFINITION.contains(superParentType) ? settings.METHOD_BRACE_STYLE : settings.BRACE_STYLE;

    if (parent == null || parent.getTreeParent() == null) {
      return Indent.getNoneIndent();
    }
    if (COMMENTS.contains(elementType)) {
      if (settings.KEEP_FIRST_COLUMN_COMMENT && isAtFirstColumn(node)) {
        return Indent.getAbsoluteNoneIndent();
      }
      return Indent.getNormalIndent();
    }
    if (elementType == PLCURLY || elementType == PRCURLY) {
      switch (braceStyle) {
        case CommonCodeStyleSettings.END_OF_LINE:
        case CommonCodeStyleSettings.NEXT_LINE:
        case CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED:
          return Indent.getNoneIndent();
        case CommonCodeStyleSettings.NEXT_LINE_SHIFTED:
        case CommonCodeStyleSettings.NEXT_LINE_SHIFTED2:
          return Indent.getNormalIndent();
        default:
          return Indent.getNoneIndent();
      }
    }
    if (parentType == PARENTHESIZED_EXPRESSION) {
      if (elementType == PLPAREN || elementType == PRPAREN) {
        return Indent.getNoneIndent();
      }
      return Indent.getNormalIndent();
    }
    if (needIndent(parentType, elementType)) {
      final PsiElement psi = node.getPsi();
      if (psi.getParent() instanceof PsiFile) {
        return Indent.getNoneIndent();
      }
      return Indent.getNormalIndent();
    }
    if (FUNCTION_DEFINITION.contains(parentType) || parentType == CALL_EXPRESSION) {
      if (elementType == PARAMETER_LIST || elementType == EXPRESSION_LIST || elementType == CALL_EXPRESSION_LIST) {
        return Indent.getNormalIndent();
      }
    }
    if (parentType == FOR_STATEMENT && prevSiblingType == PRPAREN && elementType != BLOCK_STATEMENT) {
      return Indent.getNormalIndent();
    }
    if (parentType == WHILE_STATEMENT && prevSiblingType == PRPAREN
        && elementType == DO_WHILE_BODY && firstChildType != BLOCK_STATEMENT) {
      return Indent.getNormalIndent();
    }
    if (parentType == DO_WHILE_STATEMENT && prevSiblingType == KDO
        && elementType == DO_WHILE_BODY && firstChildType != BLOCK_STATEMENT) {
      return Indent.getNormalIndent();
    }
    if (parentType == RETURN_STATEMENT &&
        prevSiblingType == KRETURN &&
        elementType != BLOCK_STATEMENT) {
      return Indent.getNormalIndent();
    }
    // IF_STATEMENT statement components
    if ((parentType == GUARDED_STATEMENT || parentType == ELSE_STATEMENT) &&
        elementType != BLOCK_STATEMENT &&
        elementType != KELSE &&
        elementType != IF_STATEMENT) {
      return Indent.getNormalIndent();
    }
    return Indent.getNoneIndent();
  }

  private static boolean needIndent(@Nullable IElementType type, IElementType elementType) {
    if (type == null) {
      return false;
    }
    boolean result = type == BLOCK_STATEMENT;
    result = result || type == CLASS_BODY;
    result = result || type == ABSTRACT_BODY;
    result = result || (type == ARRAY_LITERAL && elementType != PLBRACK && elementType != PRBRACK);
    result = result || type == ANONYMOUS_TYPE_FIELD_LIST || type == TYPE_EXTENDS_LIST;
    result = result || type == OBJECT_LITERAL;
    result = result || type == MAP_INITIALIZER_EXPRESSION;
    result = result || type == MAP_INITIALIZER_FOR_STATEMENT;
    result = result || type == MAP_INITIALIZER_WHILE_STATEMENT;
    result = result || type == EXTERN_CLASS_DECLARATION_BODY;
    result = result || type == ENUM_BODY;
    result = result || type == INTERFACE_BODY;
    result = result || type == SWITCH_BLOCK;
    result = result || type == SWITCH_CASE_BLOCK;
    return result;
  }

  private static boolean isAtFirstColumn(ASTNode node) {
    PsiElement element = node.getPsi();
    if (null == element) {
      return false;
    }
    PsiFile file = element.getContainingFile();
    Project project = element.getProject();
    if (null == file || null == project) {
      return false;
    }
    Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
    if (null == doc) {
      return false;
    }
    int line = doc.getLineNumber(node.getStartOffset());
    int lineStart = doc.getLineStartOffset(line);
    return node.getStartOffset() == lineStart;
  }
}
