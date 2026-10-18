package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
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
public final class HaxeMarkTestsBuildFileAction extends HaxeSelectedNodeAction<BuildFileRow> {

  public HaxeMarkTestsBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, BuildFileRow.class, HaxeBundle.message("haxe.toolwindow.mark.tests.build.file"));
  }

  /** Marking is offered for builds declaring a test framework lib; unmarking also for a stale mark (the lib was removed). */
  @Override
  protected boolean applies(@NotNull BuildFileRow row) {
    return row.frameworkDetected() || isUnmark(row);
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull BuildFileRow row) {
    HaxeTestsBuildFileStore store = HaxeTestsBuildFileStore.getInstance(project);
    String path = row.buildFile().file().getPath();
    // each file's (Tests) tag is an independent toggle: unmark records an
    // exclusion, so a conventionally-named file stays out too
    if (isUnmark(row)) {
      store.unmarkTestsFile(row.containerId(), path);
    }
    else {
      store.markTestsFile(row.containerId(), path);
      // a tests build often has its own source tree (tests/src) the module
      // never marked - unmarked roots leave breakpoints unresolvable
      HaxeSourceRootsOffer.offerFor(project, row.containerId(), row.buildFile().file());
    }
    // the tests build's libraries feed the module's resolve scope - re-sync
    HaxeLibrarySync.sync(project, null);
  }

  /** "Mark" or "Unmark", after the row's current state. */
  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    BuildFileRow row = selectedNode();
    if (row != null) {
      String key = isUnmark(row) ? "haxe.toolwindow.unmark.tests.build.file" : "haxe.toolwindow.mark.tests.build.file";
      e.getPresentation().setText(HaxeBundle.message(key));
    }
  }

  /** Whether the row toggles OFF: an effective tests build, or a stale mark whose framework lib is gone. */
  private static boolean isUnmark(@NotNull BuildFileRow row) {
    return row.testsFile() || row.marked();
  }
}
