package com.intellij.plugins.haxe.v2.buildtools.projectmodel;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * After a build file is registered (or marked as a tests build), offers to
 * derive folders from it when any of its classpath directories is not yet
 * covered by a source root: resolvable classpath directories become source
 * roots, output locations become excluded. A module whose MAIN sources are
 * configured still gets the offer for a tests build's separate tree —
 * breakpoint resolution needs it marked. Same derivation the silent
 * first-open pass uses ({@link HaxeSourceRootsInitializer}); here the user
 * decides. Call on the EDT.
 */
public final class HaxeSourceRootsOffer {

  private HaxeSourceRootsOffer() {
  }

  public static void offerFor(@NotNull Project project, @NotNull String containerId, @NotNull VirtualFile file) {
    HaxeBuildFileType type = ReadAction.computeBlocking(() -> HaxeBuildFileScanner.detectType(project, file));
    if (type == null) return;
    HaxeBuildFile buildFile = new HaxeBuildFile(file, type);
    List<String> uncovered = ReadAction.computeBlocking(
      () -> HaxeSourceRootsInitializer.INSTANCE.uncoveredSourceDirs(project, containerId, buildFile));
    if (uncovered.isEmpty()) return;

    boolean accepted = MessageDialogBuilder
      .yesNo(HaxeBundle.message("haxe.roots.offer.title"), HaxeBundle.message("haxe.roots.offer.message", file.getName()))
      .icon(Messages.getQuestionIcon())
      .ask(project);
    if (!accepted) return;

    HaxeSourceRootsApplier.getInstance(project).applyFromBuildFileAsync(containerId, buildFile);
  }
}
