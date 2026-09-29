package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeSourceRootsOffer;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTestsBuildFileStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: marks the selected build file as one of its container's
 * tests build files, or unmarks it. A test run compiles and launches its tests
 * build file, which supplies the libs, defines and target of that run.
 */
public final class HaxeMarkTestsBuildFileAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeMarkTestsBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.mark.tests.build.file"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null && panel.getSelectedUserObject() instanceof BuildFileRow row) {
      HaxeTestsBuildFileStore store = HaxeTestsBuildFileStore.getInstance(project);
      String path = row.buildFile().file().getPath();
      // each file's (Tests) tag is an independent toggle: unmark records an
      // exclusion, so a conventionally-named file stays out too
      if (row.testsFile()) {
        store.unmarkTestsFile(row.containerId(), path);
      }
      else {
        store.markTestsFile(row.containerId(), path);
        // a tests build often has its own source tree (tests/src) the module
        // never marked - unmarked roots leave breakpoints unresolvable
        HaxeSourceRootsOffer.offerFor(project, row.containerId(), row.buildFile().file());
      }
      // the tests build's libraries feed the module's resolve scope - re-sync
      HaxeLibrarySync.sync(project, panel::refreshTree);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (panel.getSelectedUserObject() instanceof BuildFileRow row) {
      String key = row.testsFile() ? "haxe.toolwindow.unmark.tests.build.file"
                                   : "haxe.toolwindow.mark.tests.build.file";
      e.getPresentation().setText(HaxeBundle.message(key));
      // marking is only offered for builds declaring a test framework lib -
      // a marked file without one still shows unmark, so a stale mark (the
      // lib was removed) can be cleaned up
      e.getPresentation().setEnabledAndVisible(row.frameworkDetected() || row.testsFile());
    }
    else {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
