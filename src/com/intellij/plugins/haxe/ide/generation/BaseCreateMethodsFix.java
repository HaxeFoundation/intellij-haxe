package com.intellij.plugins.haxe.ide.generation;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.HaxeNamedElementNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    if (myHaxeClass instanceof HaxeClassDeclaration) {
      final HaxeClassBody body = ((HaxeClassDeclaration)myHaxeClass).getClassBody();
      assert body != null;
      for (PsiElement child : body.getChildren()) {
        if (child.getTextOffset() > caretOffset) return;
        anchor = child;
      }
      return;
    }
    anchor = file.findElementAt(caretOffset);
  }

  /**
   * must be called not in write action
   */
  public void beforeInvoke(@NotNull final Project project, final Editor editor, final PsiFile file) {
  }

  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    if (!CodeInsightUtilBase.prepareFileForWrite(file)) return;
    evalAnchor(editor, file);
    processElements(project, getElementsToProcess());
  }

  protected void processElements(Project project, Set<T> elementsToProcess) {
    for (T e : elementsToProcess) {
      anchor = doAddMethodsForOne(project, buildFunctionsText(e), anchor);
      modifyElement(e);
    }
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
        if (element instanceof HaxeVarDeclarationPart) {
          element = element.getParent();
        }
        anchor = insert.addAfter(element, anchor);
        anchor = afterAddHandler(element, anchor);
      }
    }
    return anchor;
  }

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
    final T[] objects = (T[])elementsToProcess.toArray(new HaxeNamedComponent[elementsToProcess.size()]);
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
