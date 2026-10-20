package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The compilation server instance a container's builds and compiler requests
 * go to. A server's cache belongs to one haxe binary, so the instance is the
 * SDK's haxe executable, shared by every container of that SDK - unless the
 * container runs a dedicated one ({@link HaxeEnvironmentStore#isUsingDedicatedServer}):
 * then the id carries the container and the cache is the container's alone.
 * A dedicated container's display process is dedicated the same way.
 */
public record HaxeServerAssignment(@NotNull String id,
                                   @NotNull String executablePath,
                                   @Nullable String sdkName,
                                   @NotNull String displayName) {

  /** Separates the container from the executable in a dedicated instance's id. */
  static final String DEDICATED_SEPARATOR = "#";

  /** The instance serving the container: its effective SDK's shared server, or its own when it asks for one. */
  @NotNull
  public static HaxeServerAssignment forContainer(@NotNull Project project, @NotNull String containerId) {
    String sdkName = HaxeToolPathResolver.effectiveSdkName(project, containerId);
    HaxeServerAssignment shared = shared(project, sdkName);
    if (!HaxeEnvironmentStore.getInstance(project).isUsingDedicatedServer(containerId)) {
      return shared;
    }
    String containerName = HaxeContainers.displayName(project, containerId);
    String displayName = HaxeBundle.message("haxe.server.console.dedicated.name", shared.displayName(), containerName);
    String id = shared.id() + DEDICATED_SEPARATOR + containerId;
    return new HaxeServerAssignment(id, shared.executablePath(), sdkName, displayName);
  }

  /** The instance every container of the SDK shares; null names the project default SDK. */
  @NotNull
  public static HaxeServerAssignment shared(@NotNull Project project, @Nullable String sdkName) {
    String executablePath = HaxeToolPathResolver.resolveHaxeExecutable(project, sdkName);
    String displayName = HaxeCompilationServerManager.displayNameFor(project, sdkName, executablePath);
    return new HaxeServerAssignment(executablePath, executablePath, sdkName, displayName);
  }
}
