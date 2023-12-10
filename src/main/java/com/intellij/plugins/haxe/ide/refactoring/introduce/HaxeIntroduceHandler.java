/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2020 Eric Bishton
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
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.Function;
import org.intellij.lang.regexp.RegExpFile;
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
    return findAnchor(Collections.singletonList(occurrence));
  }

  @Nullable
  protected static PsiElement findAnchor(List<PsiElement> occurrences) {
    PsiElement anchor = occurrences.get(0);
    next:
    do {
      final PsiElement block = PsiTreeUtil.getParentOfType(anchor, HaxeBlockStatement.class, HaxeClassBody.class);

      int minOffset = Integer.MAX_VALUE;
      for (PsiElement element : occurrences) {
        minOffset = Math.min(minOffset, element.getTextOffset());
        if (!PsiTreeUtil.isAncestor(block, element, true)) {
          anchor = block;
          if (anchor == null) return null;
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
      if (element2 == null || elementIsSemicolon(element2)) {
        element2 = file.findElementAt(selectionModel.getSelectionEnd() - 2);
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
    if (element1 instanceof PsiWhiteSpace) {
      int startOffset = element1.getTextRange().getEndOffset();
      element1 = file.findElementAt(startOffset);
    }
    if (element2 instanceof PsiWhiteSpace) {
      int endOffset = element2.getTextRange().getStartOffset();
      element2 = file.findElementAt(endOffset - 1);
    }


    final Project project = operation.getProject();
    if (element1 == null || element2 == null) {
      showCannotPerformError(project, editor);
      return;
    }

    element1 = HaxeRefactoringUtil.getSelectedExpression(project, file, element1, element2);
    if (!isValidForExtraction(element1)) {
      showCannotPerformError(project, editor);
      return;
    }

    if (!checkIntroduceContext(file, editor, element1)) {
      return;
    }
    operation.setElement(element1);
    performActionOnElement(operation);
  }

  private boolean elementIsSemicolon(PsiElement element) {
    ASTNode node = null == element ? null : element.getNode();
    IElementType type = null == node ? null : node.getElementType();
    return HaxeTokenTypes.OSEMI.equals(type);
  }

  protected boolean checkIntroduceContext(PsiFile file, Editor editor, PsiElement element) {
    if (!isValidIntroduceContext(element)) {
      showCannotPerformError(file.getProject(), editor);
      return false;
    }
    return true;
  }

  protected void showCannotPerformError(Project project, Editor editor) {
    CommonRefactoringUtil.showErrorHint(
      project,
      editor,
      HaxeBundle.message("refactoring.introduce.selection.error"),
      myDialogTitle,
      "refactoring.extractMethod"
    );
  }

  protected boolean isValidIntroduceContext(PsiElement element) {
    if (PsiTreeUtil.getParentOfType(element, HaxeParameterList.class) != null) return false;
    if (element instanceof HaxeMapInitializerExpression) return false;

    return true;
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
      if (elementAtCaret instanceof HaxeExpression && isValidForExtraction(elementAtCaret)) {
        expressions.add((HaxeExpression)elementAtCaret);
      }
      elementAtCaret = elementAtCaret.getParent();
      if (elementAtCaret instanceof RegExpFile) {
        elementAtCaret = elementAtCaret.getContext();
      }
    }
    if (expressions.size() == 0) {
      return false;
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

  private boolean isValidForExtraction(PsiElement element) {
    if (null == element) {
      return false;
    }

    // Fat arrow expressions are not allowed outside of a map, so make no sense to turn into
    // separate variables.
    if (element instanceof HaxeMapInitializerExpression) {
      return false;
    }

    // A reference expression is normally a target, but if that reference is for a type declaration
    // (for instance, the type tag on a var declaration or in a TypeCheckExpression), then that element
    // would not make a valid expression.  (e.g. `int i = Float;`)
    PsiElement parent = element.getParent();
    if (parent instanceof HaxeType) {
      return false;
    }

    return true;
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
        operation.suggestName();
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
    while (type != null && notFunctionMethodClass(type));  // XXX-EBatTiVo: Probably should not stop if type == null.
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
    return HaxeNameSuggesterUtil.getSuggestedNames(expression, false);
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
    final HaxeComponent target = statement instanceof HaxeComponent ? (HaxeComponent)statement : PsiTreeUtil.findChildOfType(statement, HaxeComponent.class);
    if (target == null) {
      return;
    }

    final LinkedHashSet<String> names = new LinkedHashSet<>();
    if (operation.getName() != null && !operation.isNameSuggested()) {
      names.add(operation.getName());
    } else {
      names.addAll(operation.getSuggestedNames());
    }

    final List<PsiElement> occurrences = operation.getOccurrences();
    final PsiElement occurrence = HaxeRefactoringUtil.findOccurrenceUnderCaret(occurrences, operation.getEditor());
    final PsiElement elementForCaret = occurrence != null ? occurrence : target;

    // The editor can get swapped if the initial element that we are moving was in an embedded file
    // (for instance, when the caret is in a regular expression, thus a dummy REGEXP_FILE).  In this
    // case, when we move the parent element, the operation's file and editor are the parent, not the
    // embedded file. If we continue the renaming process in the original embedded editor, then elements
    // are not found in it, and the process aborts.
    if (elementForCaret.getContainingFile() != operation.getFile() && operation.getEditor() instanceof EditorWindow) {
      EditorWindow editorWindow = ((EditorWindow)operation.getEditor());
      Editor parentEditor = editorWindow.getDelegate();

      operation.updateEditor(parentEditor);
      operation.updateFile(elementForCaret.getContainingFile());
    }

    operation.getEditor().getCaretModel().moveToOffset(elementForCaret.getTextRange().getStartOffset());
    final InplaceVariableIntroducer<PsiElement> introducer =
      new HaxeInplaceVariableIntroducer(target.getComponentName(), operation, occurrences);
    introducer.performInplaceRefactoring(names);
  }

  @Nullable
  protected PsiElement performRefactoring(HaxeIntroduceOperation operation) {
    PsiElement declaration = createDeclaration(operation);
    if (declaration == null || declaration instanceof PsiErrorElement) {
      showCannotPerformError(operation.getProject(), operation.getEditor());
      return null;
    }

    declaration = performReplace(declaration, operation);
    if (null != declaration) {
      declaration = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(declaration);
    }
    return declaration;
  }

  @Nullable
  public PsiElement createDeclaration(HaxeIntroduceOperation operation) {
    HaxeExpression initializer = operation.getInitializer();
    String typeText = "";
    if (initializer instanceof HaxeTypeCheckExpr) {
      typeText = toTypeText(((HaxeTypeCheckExpr)initializer).getTypeOrAnonymous());
      initializer = ((HaxeTypeCheckExpr)initializer).getExpression();
    }
    InitializerTextBuilder builder = new InitializerTextBuilder();
    initializer.accept(builder);
    String suffix = builder.result() != null && builder.result().endsWith(";") ? "" : ";";
    String assignmentText = "var " + operation.getName() + typeText + " = " + builder.result() + suffix;
    PsiElement anchor = operation.isReplaceAll()
                        ? findAnchor(operation.getOccurrences())
                        : findAnchor(initializer);
    return createDeclaration(operation.getProject(), assignmentText, anchor);
  }

  @NotNull
  private String toTypeText(HaxeTypeOrAnonymous toa) {
    String ret = null;
    if (null != toa.getAnonymousType()) {
      ret = toa.getAnonymousType().toString();
    } else if (null != toa.getType()) {
      ret = toa.getType().getText();
    }
    return ret == null || ret.isEmpty() ? "" : ":" + ret;
  }

  @Nullable
  protected PsiElement createDeclaration(Project project, String text, PsiElement anchor) {
    return HaxeElementGenerator.createStatementFromText(project, text);
  }

  private PsiElement performReplace(@NotNull final PsiElement declaration, final HaxeIntroduceOperation operation) {
    final HaxeExpression expression = operation.getInitializer();
    final Project project = operation.getProject();

    PsiElement result = WriteCommandAction.writeCommandAction(project, expression.getContainingFile()).compute(() -> {

      final PsiElement createdDeclaration = addDeclaration(operation, declaration);

      if (createdDeclaration == null) return null;

      modifyDeclaration(createdDeclaration);

      PsiElement newExpression = createExpression(project, operation);
      if (null == newExpression) {
        Logger.getInstance(this.getClass()).warn("Could not create replaceable expression for '" + operation.getName() + "'.");
        return createdDeclaration;
      }

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
      return createdDeclaration;
    });

    // if we failed to extract return null;
    if (result == null) {
      return null;
    }

    // We have added the new declaration, but the new element gets invalidated by
    // the reformatting that is triggered at the end of the write action (the execute above).
    // The element that was added has been orphaned and no longer contains any file information.
    // Therefore, we will have to resolve to finding the new declaration -- and the best way to
    // do that is to resolve().  (Another way to do it would be to look up the element by
    // offset within the file -- to do so, we would have to capture the position immediately
    // after the declaration was added.)
    if (operation.getOccurrences().size() > 0) {
      PsiElement added = operation.getOccurrences().get(0);
      if (added != null) {
        PsiElement resolved = added.getReference().resolve();
        return resolved;
      }
    }
    return null;
  }

  protected void modifyDeclaration(@NotNull PsiElement declaration) {
    final PsiElement newLineNode = PsiParserFacade.getInstance(declaration.getProject()).createWhiteSpaceFromText("\n");
    final PsiElement parent = declaration.getParent();
    parent.addAfter(newLineNode, declaration);
  }

  @Nullable
  protected HaxeReference createExpression(Project project, @NotNull HaxeIntroduceOperation operation) {
    return HaxeElementGenerator.createReferenceFromText(project, operation.getName());
  }

  @Nullable
  protected PsiElement replaceExpression(PsiElement expression, PsiElement newExpression, HaxeIntroduceOperation operation) {

    PsiElement nextToken = findNextToken(expression, operation);
    String nextText = null != nextToken ? nextToken.getText() : "";

    PsiElement insertedElement = expression.replace(newExpression);

    // If the element we are replacing ended with a semi-colon or was a block statement, the new expression should end with a colon.
    String elementText = operation.getElement().getText();
    char lastChar = elementText.charAt(elementText.length()-1);
    if (lastChar == ';' || (lastChar == '}' && !";".equals(nextText)) ) {
      insertedElement.addAfter(HaxeElementGenerator.createEmptyStatement(expression.getProject()), expression);
    }

    return insertedElement;
  }

  // Lifted from CE platform/lang-api/src/com/intellij/codeInsight/completion/util/ParenthesesInsertHandler.java
  private PsiElement findNextToken(PsiElement expression, HaxeIntroduceOperation operation) {
    final PsiFile file = operation.getFile();
    PsiElement element = file.findElementAt(expression.getTextRange().getEndOffset());

    if (element instanceof PsiWhiteSpace) {
      if (element.textContains('\n')) {
        return null;
      }
      element = file.findElementAt(element.getTextRange().getEndOffset());
    }
    return element;
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


  public static class HaxeInplaceVariableIntroducer extends InplaceVariableIntroducer<PsiElement> {
    private final HaxeComponentName myTarget;

    public HaxeInplaceVariableIntroducer(HaxeComponentName target,
                                         HaxeIntroduceOperation operation,
                                         List<PsiElement> occurrences) {
      super(target, operation.getEditor(), operation.getProject(), "Introduce Variable",
            occurrences.toArray(new PsiElement[0]), null);
      myTarget = target;
    }
    public HaxeInplaceVariableIntroducer(HaxeComponentName target,
                                         Editor editor,
                                         List<PsiElement> occurrences) {
      super(target, editor, editor.getProject(), "Introduce Variable",
            occurrences.toArray(new PsiElement[0]), null);
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
