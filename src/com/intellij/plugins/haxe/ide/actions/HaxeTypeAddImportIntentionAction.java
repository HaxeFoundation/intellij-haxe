package com.intellij.plugins.haxe.ide.actions;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.codeInspection.HintAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTypeAddImportIntentionAction implements HintAction, QuestionAction, LocalQuickFix {
  private final List<HaxeComponent> candidates;
  private final PsiElement myType;
  private Editor myEditor;

  public HaxeTypeAddImportIntentionAction(@NotNull PsiElement type, @NotNull List<HaxeComponent> components) {
    myType = type;
    candidates = components;
  }

  @Override
  public boolean showHint(@NotNull Editor editor) {
    myEditor = editor;
    TextRange range = InjectedLanguageManager.getInstance(myType.getProject()).injectedToHost(myType, myType.getTextRange());
    HintManager.getInstance().showQuestionHint(editor, getText(), range.getStartOffset(), range.getEndOffset(), this);
    return true;
  }

  @NotNull
  @Override
  public String getText() {
    if (candidates.size() > 1) {
      final HaxeClass haxeClass = (HaxeClass)candidates.iterator().next();
      return HaxeBundle.message("add.import.multiple.candidates", haxeClass.getQualifiedName());
    }
    else if (candidates.size() == 1) {
      final HaxeClass haxeClass = (HaxeClass)candidates.iterator().next();
      return haxeClass.getQualifiedName() + " ?";
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
    return myType.isValid();
  }

  @Override
  public void invoke(@NotNull final Project project, final Editor editor, PsiFile file) throws IncorrectOperationException {
    if (candidates.size() > 1) {
      NavigationUtil.getPsiElementPopup(
        candidates.toArray(new PsiElement[candidates.size()]),
        new DefaultPsiElementCellRenderer(),
        HaxeBundle.message("choose.class.to.import.title"),
        new PsiElementProcessor<PsiElement>() {
          public boolean execute(@NotNull final PsiElement element) {
            CommandProcessor.getInstance().executeCommand(
              project,
              new Runnable() {
                public void run() {
                  doImport(editor, element);
                }
              },
              getClass().getName(),
              this
            );
            return false;
          }
        }
      ).showInBestPositionFor(editor);
    }
    else {
      doImport(editor, candidates.iterator().next());
    }
  }

  private void doImport(final Editor editor, final PsiElement component) {
    new WriteCommandAction(myType.getProject(), myType.getContainingFile()) {
      @Override
      protected void run(Result result) throws Throwable {
        HaxeAddImportHelper.addImport(((HaxeClass)component).getQualifiedName(), myType.getContainingFile());
      }
    }.execute();
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @Override
  public boolean execute() {
    final PsiFile containingFile = myType.getContainingFile();
    invoke(containingFile.getProject(), myEditor, containingFile);
    return true;
  }
}
