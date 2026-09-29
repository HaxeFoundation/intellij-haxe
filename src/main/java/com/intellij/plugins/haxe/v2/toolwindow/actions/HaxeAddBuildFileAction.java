package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildGroupNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Tree context menu on the Build group (or a build file row): registers a build
 * file by hand - subfolders and project xml files with non-standard names are not
 * auto-detected.
 */
public final class HaxeAddBuildFileAction extends DumbAwareAction {

  private static final Set<String> BUILD_FILE_EXTENSIONS = Set.of("hxml", "xml", "nmml", "hxp");

  private final HaxeToolWindowPanel panel;

  public HaxeAddBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.add.build.file"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    String containerId = selectedContainerId();
    if (project == null || containerId == null) return;

    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleFile()
      .withTitle(HaxeBundle.message("haxe.toolwindow.add.build.file.chooser.title"))
      .withFileFilter(file -> BUILD_FILE_EXTENSIONS.contains(StringUtil.toLowerCase(StringUtil.notNullize(file.getExtension()))));

    VirtualFile chosen = FileChooser.chooseFile(descriptor, project, ProjectUtil.guessProjectDir(project));
    if (chosen == null) return;

    if (HaxeBuildFileScanner.detectType(project, chosen) == null) {
      Messages.showErrorDialog(project,
                               HaxeBundle.message("haxe.toolwindow.add.build.file.invalid", chosen.getName()),
                               HaxeBundle.message("haxe.toolwindow.add.build.file.chooser.title"));
      return;
    }

    HaxeKnownBuildFiles.registerBuildFile(project, containerId, chosen, panel::refreshTree);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(selectedContainerId() != null);
  }

  @Nullable
  private String selectedContainerId() {
    return switch (panel.getSelectedUserObject()) {
      case BuildGroupNode buildGroup -> buildGroup.containerId();
      case BuildFileRow row -> row.containerId();
      case null, default -> null;
    };
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
