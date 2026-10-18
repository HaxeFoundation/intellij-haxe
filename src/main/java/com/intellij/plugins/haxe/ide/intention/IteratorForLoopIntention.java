package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.refactoring.introduceVariable.HaxeIntroduceHandler;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeIterationProtocol;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;

public class IteratorForLoopIntention extends BaseIntentionAction {


  public IteratorForLoopIntention() {
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.create.loop");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getLanguage() != HaxeLanguage.INSTANCE) return false;
    HaxeReference reference = attemptToFindIterableExpression(editor, file);
    return hasIterator(reference);
  }


  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxeReference reference = attemptToFindIterableExpression(editor, file);
    if (reference != null) {
      HaxeForStatement itr = HaxeElementGenerator.createForInLoop(project, "itr", reference.getText());
      itr = (HaxeForStatement)reference.replace(itr.copy());

      if (!editor.isViewer()) {
        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(itr);
        HaxeValueIterator valueIterator = itr.getValueIterator();
        if (valueIterator != null ) {
          HaxeComponentName name = valueIterator.getComponentName();
          final var introducer = new HaxeIntroduceHandler.HaxeInplaceVariableIntroducer(name, editor, List.of(), "Introduce loop iterator");
          introducer.setElementToRename(name);
          TextRange range = name.getTextRange();
          editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
          editor.getCaretModel().moveToOffset(range.getEndOffset());
          introducer.performInplaceRefactoring(new LinkedHashSet<>(List.of()));
        }
      }
    }
  }


  private HaxeReference attemptToFindIterableExpression(Editor editor, PsiFile file) {
    PsiElement psiElement = file.findElementAt(editor.getCaretModel().getOffset());
    if (psiElement instanceof PsiWhiteSpace) {
      psiElement = UsefulPsiTreeUtil.getPrevSiblingSkippingCondition(psiElement, element -> !(element instanceof HaxePsiCompositeElement), true);
    }
    if (psiElement == null) return null;

     return psiElement instanceof HaxeReference reference ? reference : PsiTreeUtil.getParentOfType(psiElement, HaxeReference.class);
  }

  private static boolean hasIterator(@Nullable HaxeReference reference) {
    if (reference == null) return false;
    ResultHolder value = HaxeExpressionEvaluator.evaluate(reference, null).result;
    return HaxeIterationProtocol.iterates(value, false, reference);
  }

}