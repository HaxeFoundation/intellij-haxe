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
package com.intellij.plugins.haxe.util;

import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeClassBody;
import com.intellij.plugins.haxe.lang.util.HaxeAstUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCodeGenerateUtil {

  public enum Position {
    PRECEDING,
    FOLLOWING,
    WRAPPING,
    CHILD
  }

  public static final String CLASS_PREFIX = "class Main{";
  public static final String CLASS_SUFFIX = "}";
  public static final String INTERFACE_PREFIX = "interface Main{";
  public static final String INTERFACE_SUFFIX = "}";
  public static final String FUNCTION_PREFIX = "function main(){";
  public static final String FUNCTION_SUFFIX = "}";

  public static Pair<String, Integer> wrapStatement(String statement) {
    statement = trimDummy(statement);
    final String function = FUNCTION_PREFIX + statement + FUNCTION_SUFFIX;
    final Pair<String, Integer> pair = wrapFunction(function);
    return new Pair<String, Integer>(pair.getFirst(), pair.getSecond() + FUNCTION_SUFFIX.length());
  }

  public static Pair<String, Integer> wrapFunction(String function) {
    function = trimDummy(function);
    return new Pair<String, Integer>(CLASS_PREFIX + function + CLASS_SUFFIX, CLASS_SUFFIX.length());
  }

  public static Pair<String, Integer> wrapInterfaceFunction(String function) {
    function = trimDummy(function);
    return new Pair<String, Integer>(INTERFACE_PREFIX + function + INTERFACE_SUFFIX, INTERFACE_SUFFIX.length());
  }

  private static String trimDummy(String text) {
    if (text.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) {
      text = text.substring(0, text.length() - CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.length());
    }
    return text;
  }

  private static boolean delimitersInOrder(ASTNode left, ASTNode right) {
    return null == left || null == right || left.getStartOffset() < right.getStartOffset();
  }

  /**
   * Gets the wrapping delimiter pair for an element.
   * <p>
   * Note that this search may find delimiters in unexpected positions if the search element or its parent
   * contains incomplete (or unparsed) sub-elements.
   *
   * @param position - Position to look for the delimeters relative to the element (PRECEDING, FOLLOWING, WRAPPING, CHILD).
   * @param element  - Anchor element
   * @param left     - left-hand delimiter
   * @param right    - right-hand delimiter
   * @return A Pair of delimiting elements found in the relative position.  Pair.first is left; Pair.second is right; either
   * or both may be null, if not found.
   */
  @NotNull
  public static Pair<PsiElement, PsiElement> getWrappingDelimiters(Position position,
                                                                   PsiElement element,
                                                                   IElementType left,
                                                                   IElementType right) {
    PsiElement lfound = null;
    PsiElement rfound = null;

    if (null != element) {
      ASTNode anchor = element.getNode();
      ASTNode lnode = null;
      ASTNode rnode = null;

      switch (position) {
        case PRECEDING:
          lnode = UsefulPsiTreeUtil.getPrevSiblingOfType(anchor, left);
          rnode = UsefulPsiTreeUtil.getPrevSiblingOfType(anchor, right);
          if (!delimitersInOrder(lnode, rnode)) {
            rnode = null;
          }
          break;
        case FOLLOWING:
          lnode = UsefulPsiTreeUtil.getNextSiblingOfType(anchor, left);
          rnode = UsefulPsiTreeUtil.getNextSiblingOfType(anchor, right);
          if (!delimitersInOrder(lnode, rnode)) {
            lnode = null;
          }
          break;
        case WRAPPING:
          lnode = UsefulPsiTreeUtil.getPrevSiblingOfType(anchor, left);
          rnode = UsefulPsiTreeUtil.getNextSiblingOfType(anchor, right);
          break;
        case CHILD:
          ASTNode child = anchor.getFirstChildNode();
          if (null != child) {
            lnode = HaxeAstUtil.isOfType(child, left) ? child : UsefulPsiTreeUtil.getNextSiblingOfType(child, left);
            rnode = HaxeAstUtil.isOfType(child, right) ? child : UsefulPsiTreeUtil.getNextSiblingOfType(child, right);
            if (!delimitersInOrder(lnode, rnode)) {
              lnode = null;
            }
          }
          break;
        default:
          throw new HaxeDebugUtil.InvalidCaseException("Unknown Position");
      }

      lfound = null == lnode ? null : lnode.getPsi();
      rfound = null == rnode ? null : rnode.getPsi();
    }
    return new Pair<>(lfound, rfound);
  }

  @NotNull
  public static Pair<PsiElement, PsiElement> getParens(Position position, PsiElement element) {
    return getWrappingDelimiters(position, element, HaxeTokenTypes.PLPAREN, HaxeTokenTypes.PRPAREN);
  }

  @NotNull
  public static Pair<PsiElement, PsiElement> getCurlyBrackets(Position position, PsiElement element) {
    return getWrappingDelimiters(position, element, HaxeTokenTypes.PLCURLY, HaxeTokenTypes.PRCURLY);
  }

  @NotNull
  public static Pair<PsiElement, PsiElement> getSquareBrackets(Position position, PsiElement element) {
    return getWrappingDelimiters(position, element, HaxeTokenTypes.PLBRACK, HaxeTokenTypes.PRBRACK);
  }

  /**
   * Wrap an element with delimiters, if it doesn't already have them.
   *
   * @param editor  - Editor in which the element resides.
   * @param element - element to wrap.
   * @param left    - left-hand delimiter
   * @param right   - right-hand delimiter
   * @return true if any elements were added; false if nothing was added.
   */
  public static boolean addMissingDelimiters(@NotNull Editor editor, PsiElement element, IElementType left, IElementType right) {
    boolean documentModified = false;

    if (null != element) {
      Pair<PsiElement, PsiElement> delims = getWrappingDelimiters(Position.WRAPPING, element, left, right);
      if (null == delims.first && null == delims.second) {
        delims = getWrappingDelimiters(Position.CHILD, element, left, right);
      }
      TextRange range = element.getTextRange();
      if (delims.second == null) {
        editor.getDocument().insertString(range.getEndOffset(), right.toString());
        documentModified = true;
      }
      if (delims.first == null) {
        editor.getDocument().insertString(range.getStartOffset(), left.toString());
        documentModified = true;
      }
    }
    return documentModified;
  }

  /**
   * Add a string at the error element, if one occurs as a sub-element to the given element.
   *
   * @param editor  - editor containing the element
   * @param element - element to find an error sub-element in.
   * @param insert  - new string to insert.
   * @return true if the string was inserted; false, otherwise (e.g. no error element found).
   */
  public static boolean addStringAtErrorElement(@NotNull Editor editor, @Nullable PsiElement element, @Nullable String insert) {
    if (null != element && null != insert && !insert.isEmpty()) {
      PsiErrorElement errorElement = UsefulPsiTreeUtil.getChild(element, PsiErrorElement.class);
      if (null != errorElement) {
        int offset = errorElement.getTextRange().getStartOffset();
        editor.getDocument().insertString(offset, insert);
        editor.getCaretModel().moveToOffset(offset);
        return true;
      }
    }
    return false;
  }
}