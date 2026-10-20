package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSections;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A build file's info as the build sees it, the one reading every consumer
 * shares (the tool window, the define context, the library sync):
 * <ul>
 *   <li>an hxml: its selected section;</li>
 *   <li>an nmml: the {@code nme prepare} evaluation;</li>
 *   <li>a lime-family file: the lime tool's evaluation of the selected target,
 *   whose library list is the full resolved set, transitive libraries included.
 *   The declared libraries stay when it lists none (the {@code lime display}
 *   fallback lists none).</li>
 * </ul>
 * Until an evaluation lands, or without one (untrusted project, failed
 * tool), the raw parse serves with the selected target's platform defines
 * and its conditions unevaluated. A landing is announced as
 * {@link HaxeBuildEventListener#evaluationLanded}. Call in a read action.
 */
public final class HaxeEffectiveBuildFileInfo {
  private HaxeEffectiveBuildFileInfo() {
  }

  @NotNull
  public static HaxeBuildFileInfo of(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return of(project, buildFile, HaxeProjectInfoCache.NO_CALLBACK);
  }

  /**
   * {@link #of(Project, HaxeBuildFile)} for a caller that must react to the
   * evaluation this call starts: {@code onEvaluated} runs on the EDT when it
   * lands. Kept by identity, so pass one long-lived instance.
   */
  @NotNull
  public static HaxeBuildFileInfo of(@NotNull Project project,
                                     @NotNull HaxeBuildFile buildFile,
                                     @NotNull Runnable onEvaluated) {
    HaxeBuildFileInfo raw = HaxeBuildSections.inspectSelected(project, buildFile);
    HaxeBuildFileType type = buildFile.type();
    String containerId = HaxeContainers.containerIdFor(project, buildFile.file());
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    if (type == HaxeBuildFileType.NMML) {
      return HaxeNmeProjectInfoService.getInstance(project).effectiveInfo(buildFile, raw, environmentSdk, onEvaluated);
    }
    if (!LimeProjects.isLimeFamily(type)) return raw;

    String targetFlag = LimeProjects.selectedTargetFlag(project, type, buildFile.file());
    HaxeBuildFileInfo evaluated = HaxeLimeProjectInfoService.getInstance(project)
      .getCachedOrSchedule(buildFile, targetFlag, environmentSdk, onEvaluated);
    if (evaluated == null) return raw.withTarget(LimeProjects.targetFor(targetFlag), null);
    List<HaxeLibDependency> libraries = !evaluated.libraries().isEmpty() ? evaluated.libraries() : raw.libraries();
    return evaluated.withLibraries(libraries);
  }
}
