package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionPhase;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

/**
 * More or less a copy of CompletionAutoPopupHandler but allows "$" (macros) and "#" (preprocessor).
 */
@CustomLog
public class HaxeCompletionAutoPopupHandler extends CompletionAutoPopupHandler {
  @NotNull
  @Override
  public Result checkAutoPopup(char charTyped, @NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
    LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(editor);

    CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();

    if (log.isDebugEnabled()) {
      log.debug("checkAutoPopup: character=" + charTyped + ";");
      log.debug("phase=" + phase);
      log.debug("lookup=" + lookup);
      log.debug("currentCompletion=" + CompletionServiceImpl.getCompletionService().getCurrentCompletion());
    }

    if (lookup != null) {
      if (editor.getSelectionModel().hasSelection()) {
        lookup.performGuardedChange(() -> EditorModificationUtil.deleteSelectedText(editor));
      }
      return Result.STOP;
    }

    if (Character.isLetterOrDigit(charTyped) || charTyped == '_'|| charTyped == '$' || charTyped == '#') {
      if (phase instanceof CompletionPhase.EmptyAutoPopup && ((CompletionPhase.EmptyAutoPopup)phase).allowsSkippingNewAutoPopup(editor, charTyped)) {
        return Result.CONTINUE;
      }

      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return Result.STOP;
    }

    return Result.CONTINUE;
  }

}
