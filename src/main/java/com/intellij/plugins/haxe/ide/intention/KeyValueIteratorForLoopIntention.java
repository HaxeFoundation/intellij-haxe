package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.refactoring.introduce.HaxeIntroduceHandler;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class KeyValueIteratorForLoopIntention extends BaseIntentionAction {



  public KeyValueIteratorForLoopIntention() {
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
    return HaxeBundle.message("haxe.quickfix.create.loop.key.value");
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
      HaxeForStatement forLoop = HaxeElementGenerator.createForInLoop(project, "key", "value", reference.getText());
      forLoop = (HaxeForStatement)reference.replace(forLoop.copy());

      if (!editor.isViewer()) {
        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(forLoop);
        HaxeKeyValueIterator keyValueIterator = forLoop.getKeyValueIterator();

        introduceKeyValueIterators(editor, keyValueIterator);
      }
    }
  }




  private static void introduceKeyValueIterators(Editor editor, HaxeKeyValueIterator keyValueIterator) {
    HaxeIteratorkey keyItr = keyValueIterator.getIteratorkey();
    HaxeIteratorValue valueItr = keyValueIterator.getIteratorValue();

    HaxeComponentName keyNamed = keyItr.getComponentName();
    HaxeComponentName valueNamed = valueItr.getComponentName();

    final var introducer = new HaxeIntroduceHandler.HaxeInplaceVariableIntroducer(keyNamed, editor, List.of(), Map.of( valueNamed, "value"));
    introducer.setElementToRename(keyNamed);
    TextRange range = keyNamed.getTextRange();
    editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
    editor.getCaretModel().moveToOffset(range.getEndOffset());
    introducer.performInplaceRefactoring(new LinkedHashSet<>(List.of()));
  }

  private HaxeReference attemptToFindIterableExpression(Editor editor, PsiFile file) {
    PsiElement psiElement = file.findElementAt(editor.getCaretModel().getOffset());
    if (psiElement instanceof PsiWhiteSpace) {
      psiElement = UsefulPsiTreeUtil.getPrevSiblingSkippingCondition(psiElement, element -> !(element instanceof HaxePsiCompositeElement), true);
    }
    if (psiElement == null) return null;

    return psiElement instanceof HaxeReference reference ? reference : PsiTreeUtil.getParentOfType(psiElement, HaxeReference.class);
  }

  private boolean hasIterator(HaxeReference reference) {
    if (reference == null) return false;

    ResultHolder holder = HaxeExpressionEvaluator.evaluate(reference, null).result;
    SpecificHaxeClassReference resolvedType = holder.getClassType();
    if (resolvedType == null) return false;

    return resolvedType.isLiteralArray() || hasKeyValueIterator(resolvedType);
  }


  private boolean hasKeyValueIterator(SpecificHaxeClassReference type) {
    if (type.getHaxeClassModel() == null) return false;
    return type.getHaxeClassModel().getMember("keyValueIterator", null) != null;
  }
}