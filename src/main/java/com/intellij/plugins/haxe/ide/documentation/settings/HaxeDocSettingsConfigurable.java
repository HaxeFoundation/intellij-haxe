package com.intellij.plugins.haxe.ide.documentation.settings;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.psi.PsiManager;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Settings | Languages & Frameworks | Haxe | Documentation: the doc comment
 * editor behaviors, each an independent opt-out.
 */
public class HaxeDocSettingsConfigurable implements Configurable {

  private final JBCheckBox injectCodeFences =
    new JBCheckBox(HaxeBundle.message("haxe.settings.docs.inject.code.fences"));
  private final JBCheckBox highlightDocMarkup =
    new JBCheckBox(HaxeBundle.message("haxe.settings.docs.highlight.markup"));
  private final JBCheckBox completeDocTags =
    new JBCheckBox(HaxeBundle.message("haxe.settings.docs.complete.tags"));
  private final JBCheckBox enterKeepsIndentation =
    new JBCheckBox(HaxeBundle.message("haxe.settings.docs.enter.keeps.indentation"));

  @Override
  public @Nls String getDisplayName() {
    return HaxeBundle.message("haxe.settings.docs.name");
  }

  @Override
  public @Nullable JComponent createComponent() {
    return FormBuilder.createFormBuilder()
      .addComponent(injectCodeFences)
      .addComponent(highlightDocMarkup)
      .addComponent(completeDocTags)
      .addComponent(enterKeepsIndentation)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();
  }

  @Override
  public boolean isModified() {
    HaxeDocSettings.State state = HaxeDocSettings.getInstance().getState();
    return state.injectCodeFences != injectCodeFences.isSelected()
           || state.highlightDocMarkup != highlightDocMarkup.isSelected()
           || state.completeDocTags != completeDocTags.isSelected()
           || state.enterKeepsIndentation != enterKeepsIndentation.isSelected();
  }

  @Override
  public void apply() {
    HaxeDocSettings.State state = HaxeDocSettings.getInstance().getState();
    boolean visualsChanged = state.injectCodeFences != injectCodeFences.isSelected()
                             || state.highlightDocMarkup != highlightDocMarkup.isSelected();
    state.injectCodeFences = injectCodeFences.isSelected();
    state.highlightDocMarkup = highlightDocMarkup.isSelected();
    state.completeDocTags = completeDocTags.isSelected();
    state.enterKeepsIndentation = enterKeepsIndentation.isSelected();
    if (visualsChanged) {
      rehighlightOpenProjects();
    }
  }

  /** Cached injections and highlighting survive a settings change - drop and re-analyze. */
  private static void rehighlightOpenProjects() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      ApplicationManager.getApplication().runWriteAction(() -> PsiManager.getInstance(project).dropPsiCaches());
      DaemonCodeAnalyzer.getInstance(project).restart("haxe: doc settings changed");
    }
  }

  @Override
  public void reset() {
    HaxeDocSettings.State state = HaxeDocSettings.getInstance().getState();
    injectCodeFences.setSelected(state.injectCodeFences);
    highlightDocMarkup.setSelected(state.highlightDocMarkup);
    completeDocTags.setSelected(state.completeDocTags);
    enterKeepsIndentation.setSelected(state.enterKeepsIndentation);
  }
}
