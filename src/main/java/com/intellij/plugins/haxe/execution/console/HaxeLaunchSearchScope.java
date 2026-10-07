package com.intellij.plugins.haxe.execution.console;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The console scope of a launch that knows its build: the whole project, with
 * the build's source directories marked as the place name-based lookups try
 * first. Two sibling projects in one IDE project may each hold a
 * {@code Main.hx}; a trace line names only the classpath-relative path, and
 * the build that produced the output is the one that disambiguates it.
 */
public final class HaxeLaunchSearchScope extends DelegatingGlobalSearchScope {

  private final List<VirtualFile> preferredDirectories;

  public HaxeLaunchSearchScope(@NotNull Project project, @NotNull List<VirtualFile> preferredDirectories) {
    super(GlobalSearchScope.allScope(project));
    this.preferredDirectories = List.copyOf(preferredDirectories);
  }

  /**
   * The directories a lookup through {@code scope} tries first; empty for a
   * scope with no build behind it. A console receives the run profile's scope
   * wrapped in a lazy {@link DelegatingGlobalSearchScope}, so the wrappers are
   * unwrapped until this class appears.
   */
  @NotNull
  public static List<VirtualFile> preferredDirectoriesOf(@NotNull GlobalSearchScope scope) {
    GlobalSearchScope current = scope;
    while (!(current instanceof HaxeLaunchSearchScope) && current instanceof DelegatingGlobalSearchScope delegating) {
      current = delegating.getDelegate();
    }
    return current instanceof HaxeLaunchSearchScope launch ? launch.preferredDirectories : List.of();
  }
}
