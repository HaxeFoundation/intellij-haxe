package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The library defines the compiler receives per {@code -lib}: {@code haxelib
 * path} prints {@code -D name=version} for the library and each transitive
 * dependency, and the compiler defines all of them. The library sync records
 * that output per container and dependency; the define context overlays the
 * entries of the active build file's libraries. In-memory only: a fresh
 * session has no library defines until the first sync lands.
 */
@Service(Service.Level.PROJECT)
public final class HaxeLibraryDefinesStore {

  private final Project project;
  /** container id -> dependency name -> (define name -> value: the version, or {@code 1} when haxelib printed none). */
  private volatile Map<String, Map<String, Map<String, String>>> byContainer = Map.of();

  public HaxeLibraryDefinesStore(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeLibraryDefinesStore getInstance(@NotNull Project project) {
    return project.getService(HaxeLibraryDefinesStore.class);
  }

  /**
   * The container's recorded defines per dependency name (each entry: the
   * dependency itself plus its transitives); empty until synced. Immutable -
   * cheap to hold as a cache key.
   */
  @NotNull
  public Map<String, Map<String, String>> definesByDependency(@NotNull String containerId) {
    return byContainer.getOrDefault(containerId, Map.of());
  }

  /** Replaces every container's recorded defines with a sync's result; a changed set publishes a build-settings change so the define context refreshes. */
  public void replaceAll(@NotNull Map<String, Map<String, Map<String, String>>> resolvedByContainer) {
    if (byContainer.equals(resolvedByContainer)) return;
    byContainer = Map.copyOf(resolvedByContainer);
    HaxeBuildSettingsListener.publish(project);
  }
}
