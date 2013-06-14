package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.util.HaxeUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDefineIntention implements IntentionAction {
  private final String myWord;
  private final boolean isDefined;

  public HaxeDefineIntention(@Nls String word, boolean contains) {
    myWord = word;
    isDefined = contains;
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message(isDefined ? "haxe.intention.undefine" : "haxe.intention.define", myWord);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return HaxeBundle.message("quick.fixes.family");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final HaxeProjectSettings projectSettings = HaxeProjectSettings.getInstance(file.getProject());
    final Set<String> definitions = projectSettings.getUserCompilerDefinitionsAsSet();
    projectSettings.setUserCompilerDefinitions(changeDefinitions(definitions));
    HaxeUtil.reparseProjectFiles(project);
  }

  private String[] changeDefinitions(Set<String> definitions) {
    if (isDefined) {
      definitions.remove(myWord);
    }
    else {
      definitions.add(myWord);
    }
    return ArrayUtil.toStringArray(definitions);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
