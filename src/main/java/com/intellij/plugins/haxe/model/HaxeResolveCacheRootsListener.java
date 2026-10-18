package com.intellij.plugins.haxe.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import org.jetbrains.annotations.NotNull;

/**
 * A module's roots or libraries changed (the library sync drops and adds
 * haxelib libraries this way): the plugin's own caches must not answer
 * from the old set. The platform drops its resolve caches and restarts
 * highlighting on a roots change itself; this covers the caches it cannot
 * see.
 */
public final class HaxeResolveCacheRootsListener implements ModuleRootListener {

  private final Project project;

  public HaxeResolveCacheRootsListener(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void rootsChanged(@NotNull ModuleRootEvent event) {
    HaxeResolveCaches.clear(project);
  }
}
