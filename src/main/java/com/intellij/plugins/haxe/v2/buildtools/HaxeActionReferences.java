package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// How a run-before reference reads: an action of the referring action's own
/// build file by its name alone, any other by its module and the build file's
/// path inside that module, so two modules' `build.hxml` stay apart -
/// `moduleB › build.hxml › Build`.
public final class HaxeActionReferences {

  /**
   * Where another file's action lives: its module (the project for the
   * project root), the file's path inside it, and the file's kind when known.
   */
  public record Location(@NotNull String module, @NotNull String path, @Nullable HaxeBuildFileType type) {
  }

  private HaxeActionReferences() {
  }

  /** The reference as a row or a list shows it, read from an action of {@code ownerId}. */
  @NotNull
  public static String label(@NotNull Project project, @NotNull String ownerId, @NotNull ActionReference reference) {
    String fileLabel = fileLabel(project, ownerId, reference);
    return fileLabel == null ? reference.action() : HaxeBundle.message("haxe.action.reference", fileLabel, reference.action());
  }

  /** The build file part of the label - module and path inside it - or null for {@code ownerId}'s own file. */
  @Nullable
  private static String fileLabel(@NotNull Project project, @NotNull String ownerId, @NotNull ActionReference reference) {
    String buildFile = reference.buildFile();
    if (buildFile == null || buildFile.equals(ownerId)) return null;
    Location location = location(project, buildFile);
    // a deleted file: its path is all there is to show
    if (location == null) return buildFile;
    return HaxeBundle.message("haxe.action.reference.file", location.module(), location.path());
  }

  /** Whether the reference names an existing action: its build file exists and offers the action (built in for its kind, or custom). */
  public static boolean exists(@NotNull Project project, @NotNull String ownerId, @NotNull ActionReference reference) {
    String buildFile = reference.buildFile() == null ? ownerId : reference.buildFile();
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFile);
    if (file == null) return false;
    return HaxeReadActions.compute(() -> HaxeCompileCommands.availableActionNames(project, file)).contains(reference.action());
  }

  /** The reference's label, marked when it names nothing (a removed action, a vanished built-in, a deleted file). */
  @NotNull
  public static String labelOrMissing(@NotNull Project project, @NotNull String ownerId, @NotNull ActionReference reference) {
    String label = label(project, ownerId, reference);
    return exists(project, ownerId, reference) ? label : HaxeBundle.message("haxe.action.reference.missing", label);
  }

  /** The module and path of the build file at {@code buildFile}, or null when no such file exists. */
  @Nullable
  public static Location location(@NotNull Project project, @NotNull String buildFile) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFile);
    if (file == null) return null;
    return HaxeReadActions.compute(() -> locationOf(project, file));
  }

  @NotNull
  private static Location locationOf(@NotNull Project project, @NotNull VirtualFile file) {
    String containerId = HaxeContainers.containerIdFor(project, file);
    String module = HaxeContainers.isProjectRoot(containerId) ? project.getName() : containerId;
    return new Location(module, pathInside(project, containerId, file), HaxeBuildFileScanner.detectType(project, file));
  }

  /** The file's path relative to its container's root, or its name when it lies elsewhere. */
  @NotNull
  private static String pathInside(@NotNull Project project, @NotNull String containerId, @NotNull VirtualFile file) {
    String root = HaxeContainers.containerRootPath(project, containerId);
    String path = file.getPath();
    if (root == null || !path.startsWith(root + "/")) return file.getName();
    return path.substring(root.length() + 1);
  }
}
