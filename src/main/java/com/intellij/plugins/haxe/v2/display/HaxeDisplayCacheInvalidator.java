package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Clears every compiler-derived cache when the compilation server's state
 * changes. A restarted server has an EMPTY module cache, so blueprints, usage
 * verdicts, the record of compiled contexts and the metadata registry all
 * describe a process that no longer exists. Dropping the PSI caches also
 * discards resolve results computed while the old server, or a broken
 * compile, made members look unresolved.
 */
public class HaxeDisplayCacheInvalidator implements HaxeCompilationServerListener {

  private final Project project;

  public HaxeDisplayCacheInvalidator(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void serverStateChanged() {
    // delivered on the EDT (see the topic contract), as the clear requires
    HaxeCompilerCaches.clearAndRehighlight(project, "haxe: compilation server state changed");
  }
}
