package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibCacheManager;
import com.intellij.plugins.haxe.haxelib.HaxelibUtil;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeLimeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeNmeProjectInfoService;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerCaches;
import org.jetbrains.annotations.NotNull;

/**
 * Tool window button that clears every cached value: the haxelib metadata
 * caches, the lime project evaluation cache and all compiler-derived caches
 * (with the PSI drop + daemon restart that makes editors recompute). The
 * tree refresh afterwards rebuilds the tool window from the emptied caches.
 */
public final class HaxePurgeCachesAction extends DumbAwareAction {

  private final Runnable afterPurge;

  public HaxePurgeCachesAction(@NotNull Runnable afterPurge) {
    super(HaxeBundle.message("haxe.toolwindow.purge.caches.text"),
          HaxeBundle.message("haxe.toolwindow.purge.caches.description"),
          AllIcons.Actions.GC);
    this.afterPurge = afterPurge;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    HaxelibUtil.clearCache();
    HaxelibCacheManager.getAllInstances().forEach(HaxelibCacheManager::reload);
    HaxeLimeProjectInfoService.getInstance(project).clearCache();
    HaxeNmeProjectInfoService.getInstance(project).clearCache();
    HaxeCompilerCaches.clearAndRehighlight(project, "haxe: caches purged");
    afterPurge.run();
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
