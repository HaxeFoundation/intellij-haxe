package com.intellij.plugins.haxe.ide.conditional.settings;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Settings | Languages & Frameworks | Haxe | Conditional Compilation:
 * display of inactive branches.
 */
public class HaxeConditionalCompilationSettingsConfigurable implements Configurable {

  private final JBIntSpinner dimIntensity =
    new JBIntSpinner(new HaxeConditionalCompilationSettings.State().dimIntensityPercent, 0, 100);

  @Override
  public @Nls String getDisplayName() {
    return HaxeBundle.message("haxe.settings.cc.name");
  }

  @Override
  public @Nullable JComponent createComponent() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeBundle.message("haxe.settings.cc.dim.intensity"), dimIntensity)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();
  }

  @Override
  public boolean isModified() {
    return HaxeConditionalCompilationSettings.getInstance().getState().dimIntensityPercent != dimIntensity.getNumber();
  }

  @Override
  public void apply() {
    HaxeConditionalCompilationSettings.getInstance().getState().dimIntensityPercent = dimIntensity.getNumber();
    rehighlightOpenProjects();
  }

  /** Dim colors are computed at annotation time - re-analyze to repaint. */
  private static void rehighlightOpenProjects() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      DaemonCodeAnalyzer.getInstance(project).restart("haxe: conditional compilation settings changed");
    }
  }

  @Override
  public void reset() {
    dimIntensity.setNumber(HaxeConditionalCompilationSettings.getInstance().getState().dimIntensityPercent);
  }
}
