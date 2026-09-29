package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.ui.HaxeBuildToolsConfigurable;
import com.intellij.plugins.haxe.v2.compiler.settings.ui.HaxeCompilerConfigurable;
import org.jetbrains.annotations.NotNull;

/**
 * Tool window gear button (like Gradle's): quick navigation to the Haxe settings pages.
 */
public final class HaxeSettingsActionGroup extends DefaultActionGroup {

  public HaxeSettingsActionGroup() {
    super(HaxeBundle.message("haxe.toolwindow.settings.group"), true);
    getTemplatePresentation().setIcon(AllIcons.General.GearPlain);
    add(new OpenSettingsAction(HaxeBundle.message("haxe.toolwindow.settings.build.tools"), HaxeBuildToolsConfigurable.class));
    add(new OpenSettingsAction(HaxeBundle.message("haxe.toolwindow.settings.compiler"), HaxeCompilerConfigurable.class));
  }

  private static final class OpenSettingsAction extends DumbAwareAction {
    private final Class<? extends Configurable> configurableClass;

    OpenSettingsAction(@NotNull String text, @NotNull Class<? extends Configurable> configurableClass) {
      super(text);
      this.configurableClass = configurableClass;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project != null) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableClass);
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }
  }
}
