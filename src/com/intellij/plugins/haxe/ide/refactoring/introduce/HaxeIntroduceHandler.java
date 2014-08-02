/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.ide.refactoring.introduce;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
@SuppressWarnings("MethodMayBeStatic")
public abstract class HaxeIntroduceHandler implements RefactoringActionHandler {
  @Nullable
  protected static PsiElement findAnchor(PsiElement occurrence) {
    return findAnchor(Arrays.asList(occurrence));
  }

  @Nullable
  protected static PsiElement findAnchor(List<PsiElement> occurrences) {
    PsiElement anchor = occurrences.get(0);
    next:
    do {
      final HaxeBlockStatement block = PsiTreeUtil.getParentOfType(anchor, HaxeBlockStatement.class);

      int minOffset = Integer.MAX_VALUE;
      for (PsiElement element : occurrences) {
        minOffset = Math.min(minOffset, element.getTextOffset());
        if (!PsiTreeUtil.isAncestor(block, element, true)) {
          anchor = block;
          continue next;
        }
      }

      if (block == null) {
        return null;
      }

      PsiElement child = null;
      PsiElement[] children = block.getChildren();
      for (PsiElement aChildren : children) {
        child = aChildren;
        if (child.getTextRange().contains(minOffset)) {
          break;
        }
      }

      return child;
    }
    while (true);
  }

  protected final String myDialogTitle;

  public HaxeIntroduceHandler(@NotNull final String dialogTitle) {
    myDialogTitle = dialogTitle;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    performAction(new HaxeIntroduceOperation(project, editor, file, null));
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
  }

  protected void performAction(HaxeIntroduceOperation operation) {
    final PsiFile file = operation.getFile();
    if (!CommonRefactoringUtil.checkReadOnlyStatus(file)) {
      return;
    }
    final Editor editor = operation.getEditor();
    if (editor.getSettings().isVariableInplaceRenameEnabled()) {
      final TemplateState templateState = TemplateManagerImpl.getTemplateState(operation.getEditor());
      if (templateState != null && !templateState.isFinished()) {
        return;
      }
    }

    PsiElement element1 = null;
    PsiElement element2 = null;
    final SelectionModel selectionModel = editor.getSelectionModel();
    if (selectionModel.hasSelection()) {
      element1 = file.findElementAt(selectionModel.getSelectionStart());
      element2 = file.findElementAt(selectionModel.getSelectionEnd() - 1);
      if (element1 instanceof PsiWhiteSpace) {
        int startOffset = element1.getTextRange().getEndOffset();
        element1 = file.findElementAt(startOffset);
      }
      if (element2 instanceof PsiWhiteSpace) {
        int endOffset = element2.getTextRange().getStartOffset();
        element2 = file.findElementAt(endOffset - 1);
      }
    }
    else {
      if (smartIntroduce(operation)) {
        return;
      }
      final CaretModel caretModel = editor.getCaretModel();
      final Document document = editor.getDocument();
      int lineNumber = document.getLineNumber(caretModel.getOffset());
      if ((lineNumber >= 0) && (lineNumber < document.getLineCount())) {
        element1 = file.findElementAt(document.getLineStartOffset(lineNumber));
        element2 = file.findElementAt(document.getLineEndOffset(lineNumber) - 1);
      }
    }
    final Project project = operation.getProject();
    if (element1 == null || element2 == null) {
      showCannotPerformError(project, editor);
      return;
    }

    element1 = HaxeRefactoringUtil.getSelectedExpression(project, file, element1, element2);
    if (element1 == null) {
      showCannotPerformError(project, editor);
      return;
    }

    if (!checkIntroduceContext(file, editor, element1)) {
      return;
    }
    operation.setElement(element1);
    performActionOnElement(operation);
  }

  protected boolean checkIntroduceContext(PsiFile file, Editor editor, PsiElement element) {
    if (!isValidIntroduceContext(element)) {
      showCannotPerformError(file.getProject(), editor);
      return false;
    }
    return true;
  }

  private void showCannotPerformError(Project project, Editor editor) {
    CommonRefactoringUtil.showErrorHint(
      project,
      editor,
      HaxeBundle.message("refactoring.introduce.selection.error"),
      myDialogTitle,
      "refactoring.extractMethod"
    );
  }

  protected boolean isValidIntroduceContext(PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, HaxeParameterList.class) == null;
  }

  private boolean smartIntroduce(final HaxeIntroduceOperation operation) {
    final Editor editor = operation.getEditor();
    final PsiFile file = operation.getFile();
    int offset = editor.getCaretModel().getOffset();
    PsiElement elementAtCaret = file.findElementAt(offset);
    if (!checkIntroduceContext(file, editor, elementAtCaret)) return true;
    final List<HaxeExpression> expressions = new ArrayList<HaxeExpression>();
    while (elementAtCaret != null) {
      if (elementAtCaret instanceof HaxeFile) {
        break;
      }
      if (elementAtCaret instanceof HaxeExpression) {
        expressions.add((HaxeExpression)elementAtCaret);
      }
      elementAtCaret = elementAtCaret.getParent();
    }
    if (expressions.size() == 1 || ApplicationManager.getApplication().isUnitTestMode()) {
      operation.setElement(expressions.get(0));
      performActionOnElement(operation);
      return true;
    }
    else if (expressions.size() > 1) {
      IntroduceTargetChooser.showChooser(
        editor,
        expressions,
        new Pass<HaxeExpression>() {
          @Override
          public void pass(HaxeExpression expression) {
            operation.setElement(expression);
            performActionOnElement(operation);
          }
        }, new Function<HaxeExpression, String>() {
          public String fun(HaxeExpression expression) {
            return expression.getText();
          }
        }
      );
      return true;
    }
    return false;
  }

  private void performActionOnElement(HaxeIntroduceOperation operation) {
    if (!checkEnabled(operation)) {
      return;
    }
    final PsiElement element = operation.getElement();

    final HaxeExpression initializer = (HaxeExpression)element;
    operation.setInitializer(initializer);

    operation.setOccurrences(getOccurrences(element, initializer));
    operation.setSuggestedNames(getSuggestedNames(initializer));
    if (operation.getOccurrences().size() == 0) {
      operation.setReplaceAll(false);
    }

    performActionOnElementOccurrences(operation);
  }

  protected void performActionOnElementOccurrences(final HaxeIntroduceOperation operation) {
    final Editor editor = operation.getEditor();
    if (editor.getSettings().isVariableInplaceRenameEnabled()) {
      ensureName(operation);
      if (operation.isReplaceAll() != null) {
        performInplaceIntroduce(operation);
      }
      else {
        OccurrencesChooser.simpleChooser(editor).showChooser(
          operation.getElement(),
          operation.getOccurrences(),
          new Pass<OccurrencesChooser.ReplaceChoice>() {
            @Override
            public void pass(OccurrencesChooser.ReplaceChoice replaceChoice) {
              operation.setReplaceAll(replaceChoice == OccurrencesChooser.ReplaceChoice.ALL);
              performInplaceIntroduce(operation);
            }
          });
      }
    }
    else {
      performIntroduceWithDialog(operation);
    }
  }

  protected boolean checkEnabled(HaxeIntroduceOperation operation) {
    return true;
  }

  protected static void ensureName(HaxeIntroduceOperation operation) {
    if (operation.getName() == null) {
      final Collection<String> suggestedNames = operation.getSuggestedNames();
      if (suggestedNames.size() > 0) {
        operation.setName(suggestedNames.iterator().next());
      }
      else {
        operation.setName("x");
      }
    }
  }


  protected List<PsiElement> getOccurrences(PsiElement element, @NotNull final HaxeExpression expression) {
    PsiElement context = element;
    HaxeComponentType type = null;
    do {
      context = PsiTreeUtil.getParentOfType(context, HaxeComponent.class, true);
      type = HaxeComponentType.typeOf(context);
    }
    while (type != null && notFunctionMethodClass(type));
    if (context == null) {
      context = expression.getContainingFile();
    }
    return HaxeRefactoringUtil.getOccurrences(expression, context);
  }

  private static boolean notFunctionMethodClass(HaxeComponentType type) {
    final boolean isFunctionMethodClass = type == HaxeComponentType.METHOD ||
                                          type == HaxeComponentType.FUNCTION ||
                                          type == HaxeComponentType.INTERFACE ||
                                          type == HaxeComponentType.CLASS;
    return !isFunctionMethodClass;
  }

  protected Collection<String> getSuggestedNames(final HaxeExpression expression) {
    Collection<String> candidates = new LinkedHashSet<String>();
    String text = expression.getText();
    if (expression instanceof HaxeCallExpression) {
      final HaxeExpression callee = ((HaxeCallExpression)expression).getExpression();
      text = callee.getText();
    }

    if (text != null) {
      candidates.addAll(HaxeNameSuggesterUtil.generateNames(text));
    }

    // todo: add suggestions

    final Set<String> usedNames = HaxeRefactoringUtil.collectUsedNames(expression);
    final Collection<String> result = new ArrayList<String>();

    for (String candidate : candidates) {
      int index = 0;
      String suffix = "";
      while (usedNames.contains(candidate + suffix)) {
        suffix = Integer.toString(++index);
      }
      result.add(candidate + suffix);
    }

    return result;
  }

  protected void performIntroduceWithDialog(HaxeIntroduceOperation operation) {
    final Project project = operation.getProject();
    if (operation.getName() == null) {
      HaxeIntroduceDialog dialog = new HaxeIntroduceDialog(project, myDialogTitle, operation);
      dialog.show();
      if (!dialog.isOK()) {
        return;
      }
      operation.setName(dialog.getName());
      operation.setReplaceAll(dialog.doReplaceAllOccurrences());
    }

    PsiElement declaration = performRefactoring(operation);
    if (declaration == null) {
      return;
    }
    final Editor editor = operation.getEditor();
    editor.getCaretModel().moveToOffset(declaration.getTextRange().getEndOffset());
    editor.getSelectionModel().removeSelection();
  }

  protected void performInplaceIntroduce(HaxeIntroduceOperation operation) {
    final PsiElement statement = performRefactoring(operation);
    final HaxeComponent target = PsiTreeUtil.findChildOfType(statement, HaxeComponent.class);
    if (target == null) {
      return;
    }
    final List<PsiElement> occurrences = operation.getOccurrences();
    final PsiElement occurrence = HaxeRefactoringUtil.findOccurrenceUnderCaret(occurrences, operation.getEditor());
    final PsiElement elementForCaret = occurrence != null ? occurrence : target;
    operation.getEditor().getCaretModel().moveToOffset(elementForCaret.getTextRange().getStartOffset());
    final InplaceVariableIntroducer<PsiElement> introducer =
      new HaxeInplaceVariableIntroducer(target.getComponentName(), operation, occurrences);
    introducer.performInplaceRefactoring(new LinkedHashSet<String>(operation.getSuggestedNames()));
  }

  @Nullable
  protected PsiElement performRefactoring(HaxeIntroduceOperation operation) {
    PsiElement declaration = createDeclaration(operation);
    if (declaration == null) {
      showCannotPerformError(operation.getProject(), operation.getEditor());
      return null;
    }

    declaration = performReplace(declaration, operation);
    declaration = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(declaration);
    return declaration;
  }

  @Nullable
  public PsiElement createDeclaration(HaxeIntroduceOperation operation) {
    final Project project = operation.getProject();
    final HaxeExpression initializer = operation.getInitializer();
    InitializerTextBuilder builder = new InitializerTextBuilder();
    initializer.accept(builder);
    String assignmentText = "var " + operation.getName() + " = " + builder.result() + ";";
    PsiElement anchor = operation.isReplaceAll()
                        ? findAnchor(operation.getOccurrences())
                        : findAnchor(initializer);
    return createDeclaration(project, assignmentText, anchor);
  }

  @Nullable
  protected PsiElement createDeclaration(Project project, String text, PsiElement anchor) {
    return HaxeElementGenerator.createStatementFromText(project, text);
  }

  private PsiElement performReplace(@NotNull final PsiElement declaration, final HaxeIntroduceOperation operation) {
    final HaxeExpression expression = operation.getInitializer();
    final Project project = operation.getProject();
    return new WriteCommandAction<PsiElement>(project, expression.getContainingFile()) {
      protected void run(final Result<PsiElement> result) throws Throwable {
        final PsiElement createdDeclaration = addDeclaration(operation, declaration);
        result.setResult(createdDeclaration);
        if (createdDeclaration != null) {
          modifyDeclaration(createdDeclaration);
        }

        PsiElement newExpression = createExpression(project, operation.getName());

        if (operation.isReplaceAll()) {
          List<PsiElement> newOccurrences = new ArrayList<PsiElement>();
          for (PsiElement occurrence : operation.getOccurrences()) {
            final PsiElement replaced = replaceExpression(occurrence, newExpression, operation);
            if (replaced != null) {
              newOccurrences.add(replaced);
            }
          }
          operation.setOccurrences(newOccurrences);
        }
        else {
          final PsiElement replaced = replaceExpression(expression, newExpression, operation);
          operation.setOccurrences(Collections.singletonList(replaced));
        }

        postRefactoring(operation.getElement());
      }
    }.execute().getResultObject();
  }

  protected void modifyDeclaration(@NotNull PsiElement declaration) {
    final PsiElement newLineNode = PsiParserFacade.SERVICE.getInstance(declaration.getProject()).createWhiteSpaceFromText("\n");
    final PsiElement parent = declaration.getParent();
    parent.addAfter(newLineNode, declaration);
  }

  @Nullable
  protected HaxeReference createExpression(Project project, String name) {
    return HaxeElementGenerator.createReferenceFromText(project, name);
  }

  @Nullable
  protected PsiElement replaceExpression(PsiElement expression, PsiElement newExpression, HaxeIntroduceOperation operation) {
    return expression.replace(newExpression);
  }


  protected void postRefactoring(PsiElement element) {
  }

  @Nullable
  public PsiElement addDeclaration(HaxeIntroduceOperation operation, PsiElement declaration) {
    final PsiElement expression = operation.getInitializer();
    return addDeclaration(expression, declaration, operation);
  }

  @Nullable
  protected abstract PsiElement addDeclaration(@NotNull final PsiElement expression,
                                               @NotNull final PsiElement declaration,
                                               @NotNull HaxeIntroduceOperation operation);


  private static class HaxeInplaceVariableIntroducer extends InplaceVariableIntroducer<PsiElement> {
    private final HaxeComponentName myTarget;

    public HaxeInplaceVariableIntroducer(HaxeComponentName target,
                                         HaxeIntroduceOperation operation,
                                         List<PsiElement> occurrences) {
      super(target, operation.getEditor(), operation.getProject(), "Introduce Variable",
            occurrences.toArray(new PsiElement[occurrences.size()]), null);
      myTarget = target;
    }

    @Override
    protected PsiElement checkLocalScope() {
      return myTarget.getContainingFile();
    }
  }

  private static class InitializerTextBuilder extends PsiRecursiveElementVisitor {
    private final StringBuilder myResult = new StringBuilder();

    @Override
    public void visitWhiteSpace(PsiWhiteSpace space) {
      myResult.append(space.getText().replace('\n', ' '));
    }

    @Override
    public void visitElement(PsiElement element) {
      if (element.getChildren().length == 0) {
        myResult.append(element.getText());
      }
      else {
        super.visitElement(element);
      }
    }

    public String result() {
      return myResult.toString();
    }
  }
}
