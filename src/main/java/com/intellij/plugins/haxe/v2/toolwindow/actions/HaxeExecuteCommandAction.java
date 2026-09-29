package com.intellij.plugins.haxe.v2.toolwindow.actions;

import icons.HaxeIcons;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeConsoleCommandRunner;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool window "Execute Haxe Command" button (the Gradle/Maven "execute" equivalent).
 * Prompts for compiler arguments and runs haxe with them in a console.
 */
public final class HaxeExecuteCommandAction extends DumbAwareAction {

  private static final String LAST_COMMAND_KEY = "haxe.v2.toolwindow.last.command";

  public HaxeExecuteCommandAction() {
    super(HaxeBundle.message("haxe.toolwindow.execute.text"),
          HaxeBundle.message("haxe.toolwindow.execute.description"),
          AllIcons.Actions.Execute);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    PropertiesComponent properties = PropertiesComponent.getInstance(project);
    String arguments = Messages.showInputDialog(project,
                                                HaxeBundle.message("haxe.toolwindow.execute.prompt"),
                                                HaxeBundle.message("haxe.toolwindow.execute.title"),
                                                HaxeIcons.HAXE_LOGO,
                                                properties.getValue(LAST_COMMAND_KEY, "--version"),
                                                null);
    if (StringUtil.isEmptyOrSpaces(arguments)) return;
    properties.setValue(LAST_COMMAND_KEY, arguments);

    List<String> command = new ArrayList<>();
    command.add(HaxeToolPathResolver.resolveHaxeExecutable(project));
    command.addAll(ParametersListUtil.parse(arguments.trim()));
    HaxeConsoleCommandRunner.run(project, "haxe " + arguments.trim(), command, project.getBasePath());
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
