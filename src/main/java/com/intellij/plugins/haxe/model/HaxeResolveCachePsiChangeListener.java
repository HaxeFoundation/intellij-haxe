package com.intellij.plugins.haxe.model;

import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;

/** Every PSI change drops the plugin's resolution caches ({@link HaxeResolveCaches#clear}). */
public final class HaxeResolveCachePsiChangeListener implements PsiModificationTracker.Listener {

  private final Project project;

  public HaxeResolveCachePsiChangeListener(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void modificationCountChanged() {
    HaxeResolveCaches.clear(project);
  }
}
