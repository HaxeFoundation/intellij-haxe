package com.intellij.plugins.haxe.profiler.bridge.hints;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import org.jetbrains.annotations.NotNull;

/**
 * The editor-gutter popup entry that shows or hides the Haxe gutter time
 * chips — the counterpart of the Java line profiler's "Hide Performance
 * Hints". Appears only on files the active capture has times for; the
 * choice persists across captures and sessions.
 */
public class HaxeToggleProfilerHintsAction extends AnAction implements DumbAware {

  public HaxeToggleProfilerHintsAction() {
    super(() -> HaxeProfilerBundle.message("haxe.profiler.hints.hide"));
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
    HaxeIuPerformanceHints hints = HaxeIuPerformanceHints.getInstance();
    boolean applicable = project != null && file != null && hints != null && hints.hasHintsFor(project, file);
    event.getPresentation().setEnabledAndVisible(applicable);
    if (applicable) {
      String key = HaxeIuPerformanceHints.hintsVisible() ? "haxe.profiler.hints.hide" : "haxe.profiler.hints.show";
      event.getPresentation().setText(HaxeProfilerBundle.message(key));
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    HaxeIuPerformanceHints hints = HaxeIuPerformanceHints.getInstance();
    if (hints == null) return;
    HaxeIuPerformanceHints.setHintsVisible(!HaxeIuPerformanceHints.hintsVisible());
    hints.applyHintsVisibility();
  }
}
