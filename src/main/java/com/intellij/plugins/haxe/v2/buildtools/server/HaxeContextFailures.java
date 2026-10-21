package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.plugins.haxe.v2.buildtools.HaxeLastFailures;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * The last failed compiler request per container. A build context that fails
 * to compile, for example because a define override breaks a library,
 * silently disables every compiler-backed feature. This store makes such a
 * failure visible: the display service records the outcome of each request
 * and of the context warm-up compile, and the tool window shows the failure
 * in the Compilation server row.
 */
@Service(Service.Level.PROJECT)
public final class HaxeContextFailures extends HaxeLastFailures {
  private final Project project;

  public HaxeContextFailures(@NotNull Project project) {
    super(project);
    this.project = project;
  }

  @NotNull
  public static HaxeContextFailures getInstance(@NotNull Project project) {
    return project.getService(HaxeContextFailures.class);
  }

  /** Drops the failures of every container the given server serves. They describe a process that no longer runs. */
  public void clearForServer(@NotNull String serverId) {
    for (String containerId : failingKeys()) {
      if (serverId.equals(HaxeCompilationServerManager.serverIdFor(project, containerId))) {
        record(containerId, null);
      }
    }
  }
}
