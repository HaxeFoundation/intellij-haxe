package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.execution.console.HaxeLaunchSearchScope;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildClasspaths;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** The search scope a launch's console and test navigation resolve file names in. */
public final class HaxeLaunchScopes {

  private HaxeLaunchScopes() {
  }

  /**
   * The scope of a launch with a Haxe build step: that build's directories
   * first, the project after; the project alone without a build step.
   */
  @NotNull
  public static GlobalSearchScope forConfiguration(@NotNull RunConfiguration configuration) {
    Project project = configuration.getProject();
    String buildFilePath = HaxeActionBeforeRunTaskProvider.buildStepFilePath(configuration);
    return buildFilePath == null ? GlobalSearchScope.allScope(project) : forBuildFile(project, buildFilePath);
  }

  /**
   * The build's work directory and source directories first, the project
   * after. The compiler prints positions relative to the directory it runs in
   * ({@code src/Main.hx:21:} for a lime build run from the project folder),
   * so that directory is a lookup root as much as the classpaths are.
   */
  @NotNull
  public static GlobalSearchScope forBuildFile(@NotNull Project project, @NotNull String buildFilePath) {
    List<String> directories = new ArrayList<>();
    VirtualFile buildFile = LocalFileSystem.getInstance().findFileByPath(buildFilePath);
    if (buildFile != null) directories.add(HaxeBuildWorkDirectories.workDirectory(project, buildFile));
    directories.addAll(HaxeReadActions.compute(() -> HaxeBuildClasspaths.sourceDirectories(project, buildFilePath)));
    return forDirectories(project, directories);
  }

  /** The given directories first, the project after; the project alone when none of them exists. */
  @NotNull
  public static GlobalSearchScope forDirectories(@NotNull Project project, @NotNull List<String> directories) {
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    List<VirtualFile> preferred = directories.stream()
      .distinct()
      .map(fileSystem::findFileByPath)
      .filter(Objects::nonNull)
      .toList();
    return preferred.isEmpty() ? GlobalSearchScope.allScope(project) : new HaxeLaunchSearchScope(project, preferred);
  }
}
