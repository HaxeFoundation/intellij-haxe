package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildConfigListener;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last failed compiler request per container. A build context that fails
 * to compile, for example because a define override breaks a library,
 * silently disables every compiler-backed feature. This store makes such a
 * failure visible: the display service records the outcome of each request,
 * and the tool window shows the failure in the Compilation server row.
 */
@Service(Service.Level.PROJECT)
public final class HaxeContextFailures {

  private final Project project;
  private final Map<String, String> failures = new ConcurrentHashMap<>();

  public HaxeContextFailures(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeContextFailures getInstance(@NotNull Project project) {
    return project.getService(HaxeContextFailures.class);
  }

  /** The container's last failed compiler request, or null while requests succeed. */
  @Nullable
  public String lastFailure(@NotNull String containerId) {
    return failures.get(containerId);
  }

  /** Current failures per container (a copy). */
  @NotNull
  public Map<String, String> snapshot() {
    return Map.copyOf(failures);
  }

  /** Drops the failures of every container the given server serves. They describe a process that no longer runs. */
  public void clearForServer(@NotNull String serverId) {
    for (String containerId : List.copyOf(failures.keySet())) {
      if (serverId.equals(HaxeCompilationServerManager.serverIdFor(project, containerId))) {
        record(containerId, null);
      }
    }
  }

  /**
   * Records the outcome of a request; a null failure means success. When the
   * container's failure changes, the tool window repaints.
   */
  public void record(@NotNull String containerId, @Nullable String failure) {
    boolean changed = failure != null
                      ? !failure.equals(failures.put(containerId, failure))
                      : failures.remove(containerId) != null;
    if (changed && !project.isDisposed()) {
      project.getMessageBus().syncPublisher(HaxeBuildConfigListener.TOPIC).buildConfigurationChanged();
    }
  }
}
