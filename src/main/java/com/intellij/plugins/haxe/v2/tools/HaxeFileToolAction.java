package com.intellij.plugins.haxe.v2.tools;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeConsoleCommandRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of the editor's per-file tool actions: visible only for a Haxe file
 * whose ancestor directories (up to the project base) hold the tool's config.
 * The config's directory becomes the working directory, so the tool resolves
 * its config and relative excludes the same way a terminal run there would.
 * The update answers from {@link HaxeToolConfigDetector}'s memo so the menu
 * never waits on a search; the action's run searches afresh.
 */
abstract class HaxeFileToolAction extends DumbAwareAction {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(knownConfigDirectory(e) != null);
  }

  /** Which config file gates and anchors this action. */
  @NotNull
  abstract String configName();

  /** The haxelib the action runs. */
  @NotNull
  abstract String toolHaxelib();

  @Nullable
  static VirtualFile haxeFile(@NotNull AnActionEvent e) {
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (e.getProject() == null || file == null || file.isDirectory()) return null;
    return "hx".equalsIgnoreCase(file.getExtension()) ? file : null;
  }

  /** The config directory as already detected; null while unknown or when none exists. */
  @Nullable
  VirtualFile knownConfigDirectory(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = haxeFile(e);
    if (project == null || file == null) return null;
    return HaxeToolConfigDetector.getInstance(project).knownConfigDirectory(file, configName());
  }

  /** The config directory searched now, for the action's run. */
  @Nullable
  VirtualFile configDirectory(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = haxeFile(e);
    if (project == null || file == null) return null;
    return HaxeToolConfigs.findConfigDirectory(project, file, configName());
  }

  /** The haxelib executable for the file's container, honoring its environment SDK. */
  @NotNull
  static String haxelibFor(@NotNull Project project, @NotNull VirtualFile file) {
    String containerId = HaxeContainers.containerIdFor(project, file);
    String sdkName = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    return HaxeToolPathResolver.resolveHaxelibExecutable(project, sdkName);
  }

  /** {@code haxelib run <tool> <flags> -s <file>} in the config directory, shown in the run console. */
  void runFileTool(@NotNull AnActionEvent e, @NotNull List<String> flags, @Nullable Runnable onTerminated) {
    Project project = e.getProject();
    VirtualFile file = haxeFile(e);
    VirtualFile configDirectory = configDirectory(e);
    if (project == null || file == null || configDirectory == null) return;

    String relative = VfsUtilCore.getRelativePath(file, configDirectory);
    String source = relative != null ? relative : file.getPath();
    String tool = toolHaxelib();
    List<String> command = new ArrayList<>(List.of(haxelibFor(project, file), "run", tool));
    command.addAll(flags);
    command.addAll(List.of("-s", source));

    String presentableName = tool + " " + file.getName();
    HaxeConsoleCommandRunner.run(project, presentableName, command, configDirectory.getPath(), onTerminated);
  }
}
