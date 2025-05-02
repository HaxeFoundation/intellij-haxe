package com.intellij.plugins.haxe.ide.actions;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInsight.navigation.PsiTargetNavigator;
import com.intellij.codeInspection.HintAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class HaxeStaticMemberAddImportIntentionAction implements HintAction, QuestionAction, LocalQuickFix {
  private final List<HaxeMemberModel> candidates;
  private final PsiElement myReference;
  private Editor myEditor;

  public HaxeStaticMemberAddImportIntentionAction(@NotNull PsiElement reference, @NotNull List<HaxeMemberModel> members) {
    myReference = reference;
    candidates = members;
  }

  @Override
  public boolean showHint(@NotNull Editor editor) {
    myEditor = editor;
    TextRange range = InjectedLanguageManager.getInstance(myReference.getProject()).injectedToHost(myReference, myReference.getTextRange());
    HintManager.getInstance().showQuestionHint(editor, getText(), range.getStartOffset(), range.getEndOffset(), this);
    return true;
  }

  @NotNull
  @Override
  public String getText() {
    if (candidates.size() > 1) {
      final HaxeMemberModel model = candidates.getFirst();
      return HaxeBundle.message("add.import.multiple.candidates", model.getQualifiedInfo().toShortendImportReferenceString());
    }
    else if (candidates.size() == 1) {
      final HaxeMemberModel model = candidates.getFirst();
      return model.getQualifiedInfo().toShortendImportReferenceString() + " ?";
    }
    return "";
  }

  @NotNull
  @Override
  public String getName() {
    return getText();
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    invoke(project, myEditor, descriptor.getPsiElement().getContainingFile());
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return myReference.isValid();
  }

  @Override
  public void invoke(@NotNull final Project project, final Editor editor, PsiFile file) throws IncorrectOperationException {
    if (candidates.size() > 1) {
      PsiElement[] psiElements = candidates.stream().map(haxeMemberModel -> haxeMemberModel.getNamedComponentPsi()).toArray(PsiElement[]::new);
      PsiElementProcessor<PsiElement> processor = element -> {
        CommandProcessor.getInstance().executeCommand(project, () -> doImport(element), getClass().getName(), this);
        return true;
      };
      ApplicationManager.getApplication().invokeLater(() -> {
      new PsiTargetNavigator<>(psiElements)
        .createPopup(project, HaxeBundle.message("choose.class.to.import.title"), processor)
        .showInBestPositionFor(editor);
      });
    }
    else if (!candidates.isEmpty())  {
      doImport(candidates.getFirst().getMemberPsi());
    }
  }

  private void doImport(final PsiElement component) {
    PsiFile file = myReference.getContainingFile();

    WriteCommandAction.writeCommandAction(myReference.getProject(), file)
      .run(() -> {
        if(component instanceof HaxeModelTarget target) {
          HaxeModel model = target.getModel();
          String qname = model.getQualifiedInfo().toShortendImportReferenceString();
          HaxeAddImportHelper.addImport(qname, file);
          PsiUtilCore.ensureValid(file);
        }
      });
  }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile original) {
    PsiFile file = (PsiFile)original.copy();

    FullyQualifiedInfo qualifiedInfo = candidates.getFirst().getQualifiedInfo();
    if (qualifiedInfo != null) {
      String next = qualifiedInfo.toShortendImportReferenceString();
      HaxeAddImportHelper.addImport(next, file);
      return new IntentionPreviewInfo.CustomDiff(HaxeFileType.INSTANCE, null, original.getText(), file.getText(), true);
    }
    return HintAction.super.generatePreview(project, editor, file);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @Override
  public boolean execute() {
    final PsiFile containingFile = myReference.getContainingFile();
    invoke(containingFile.getProject(), myEditor, containingFile);
    return true;
  }
}
