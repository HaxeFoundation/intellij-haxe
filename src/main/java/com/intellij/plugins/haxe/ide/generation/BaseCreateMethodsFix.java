/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.HaxeNamedElementNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.PRCURLY;

/**
 * @author: Fedor.Korotkov
 */
abstract public class BaseCreateMethodsFix<T extends HaxeNamedComponent> {
  private final Set<T> elementsToProcess = new LinkedHashSet<T>();
  protected final HaxeClass myHaxeClass;
  protected final HaxeGenericSpecialization specializations;
  protected PsiElement anchor = null;

  public BaseCreateMethodsFix(final HaxeClass haxeClass) {
    myHaxeClass = haxeClass;
    specializations = HaxeClassResolveResult.create(haxeClass).getSpecialization();
  }

  protected void evalAnchor(@Nullable Editor editor, PsiFile file) {
    if (editor == null) return;
    final int caretOffset = editor.getCaretModel().getOffset();
    PsiElement bodyPsi = null;
    if (myHaxeClass instanceof HaxeClassDeclaration) {
      bodyPsi = ((HaxeClassDeclaration)myHaxeClass).getClassBody();
    }
    else if (myHaxeClass instanceof HaxeInterfaceDeclaration) {
      bodyPsi = ((HaxeInterfaceDeclaration)myHaxeClass).getInterfaceBody();
    }
    if (null != bodyPsi) {
      for (PsiElement child : bodyPsi.getChildren()) {
        if (child.getTextOffset() > caretOffset) break;
        anchor = child;
      }
      if (null != anchor) return;

      // If we got here, either the caret wasn't on an element in the class body, or
      // the class is empty (of composite elements). Place ourselves at the whitespace
      // just before the closing bracket, or after the last detected position if there
      // is no closing bracket.
      ASTNode lastChild = bodyPsi.getNode().getLastChildNode();
      if (null != lastChild && lastChild.getElementType() == PRCURLY) {
        ASTNode prev = lastChild.getTreePrev();
        if (null != prev) {
          lastChild = prev;
        }
      }
      if (null != lastChild) {
        anchor = lastChild.getPsi();
        if (null != anchor) return;
      }
    }

    anchor = file.findElementAt(caretOffset);
  }

  /**
   * must be called not in write action
   */
  public void beforeInvoke(@NotNull final Project project, final Editor editor, final PsiFile file) {
  }

  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
    evalAnchor(editor, file);
    processElements(project, getElementsToProcess());
  }

  protected void processElements(Project project, Set<T> elementsToProcess) {
    for (T e : elementsToProcess) {
      anchor = doAddMethodsForOne(project, buildFunctionsText(e), anchor);
      modifyElement(e);
    }
    anchor = addNewlineIfNoneBeforeEndCurly(anchor);
  }

  protected void modifyElement(T element) {
  }

  protected abstract String buildFunctionsText(T e);

  public PsiElement doAddMethodsForOne(final Project project, final String functionsText, PsiElement anchor)
    throws IncorrectOperationException {
    if (functionsText != null && functionsText.length() > 0) {
      List<HaxeNamedComponent> elements = HaxeElementGenerator.createNamedSubComponentsFromText(project, functionsText);
      final PsiElement insert = myHaxeClass instanceof HaxeClassDeclaration ?
                                ((HaxeClassDeclaration)myHaxeClass).getClassBody() : myHaxeClass;
      assert insert != null;
      for (PsiElement element : elements) {
        anchor = insert.addAfter(element, anchor);
        anchor = afterAddHandler(element, anchor);
      }
    }
    return anchor;
  }

  protected PsiElement addNewlineIfAnchorIsAtOpenCurly(PsiElement anchor) {
    if (anchor.getNode().getElementType() == HaxeTokenTypes.PLCURLY) {
      final PsiElement endingNewLineNode =
              PsiParserFacade.SERVICE.getInstance(anchor.getProject()).createWhiteSpaceFromText("\n");
      anchor = anchor.getParent().addAfter(endingNewLineNode, anchor);
    }
    return anchor;
  }

  protected PsiElement addNewlineIfNoneBeforeEndCurly(PsiElement anchor) {
    ASTNode next = anchor.getNode().getTreeNext();
    if (next.getElementType() == PRCURLY) {
      final PsiElement endingNewLineNode =
              PsiParserFacade.SERVICE.getInstance(anchor.getProject()).createWhiteSpaceFromText("\n");
      anchor = anchor.getParent().addAfter(endingNewLineNode, anchor);
    }
    return anchor;
  }

  /** Called for *each* element that is added. */
  protected PsiElement afterAddHandler(PsiElement element, PsiElement anchor) {
    final PsiElement newLineNode =
      PsiParserFacade.SERVICE.getInstance(element.getProject()).createWhiteSpaceFromText("\n\n");
    anchor.getParent().addBefore(newLineNode, anchor);
    return anchor;
  }

  public void addElementToProcess(final T function) {
    elementsToProcess.add(function);
  }

  public void addElementsToProcessFrom(@Nullable final Collection<HaxeNamedElementNode> selectedElements) {
    if (selectedElements == null) {
      return;
    }
    for (HaxeNamedElementNode el : selectedElements) {
      addElementToProcess((T)el.getPsiElement());
    }
  }

  public Set<T> getElementsToProcess() {
    final T[] objects = (T[])elementsToProcess.toArray(new HaxeNamedComponent[0]);
    final Comparator<T> tComparator = new Comparator<T>() {
      public int compare(final T o1, final T o2) {
        return o1.getTextOffset() - o2.getTextOffset();
      }
    };

    final int size = elementsToProcess.size();
    final LinkedHashSet<T> result = new LinkedHashSet<T>(size);
    final List<T> objectsFromSameFile = new ArrayList<T>();
    PsiFile containingFile = null;

    for (int i = 0; i < size; ++i) {
      final T object = objects[i];
      final PsiFile currentContainingFile = object.getContainingFile();

      if (currentContainingFile != containingFile) {
        if (containingFile != null) {
          Collections.sort(objectsFromSameFile, tComparator);
          result.addAll(objectsFromSameFile);
          objectsFromSameFile.clear();
        }
        containingFile = currentContainingFile;
      }

      objectsFromSameFile.add(object);
    }

    Collections.sort(objectsFromSameFile, tComparator);
    result.addAll(objectsFromSameFile);

    elementsToProcess.clear();
    elementsToProcess.addAll(result);
    return elementsToProcess;
  }
}
