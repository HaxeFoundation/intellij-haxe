package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildInputs;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles;
import com.intellij.plugins.haxe.v2.buildtools.HaxeMainClasses;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeEffectiveBuildFileInfo;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Build &amp; run launches that start from a Haxe class: every kind that
 * launches the output of each build file whose main class it is
 * ({@link HaxeMainClasses}). Tests builds are left out: their main runs a
 * test runner, which the test gutter covers.
 */
public final class HaxeMainClassLaunches {

  /** A build file launching its output through one kind. */
  public record MainLaunch(@NotNull HaxeBuildFile buildFile, @NotNull HaxeLaunchKind kind, @NotNull String targetOutput) {
  }

  /** A main class: the file declaring it and its name. */
  private record MainClass(@NotNull VirtualFile file, @NotNull String name) {
  }

  private static final Key<CachedValue<Map<MainClass, List<MainLaunch>>>> LAUNCHES_KEY = Key.create("haxe.main.class.launches");

  private HaxeMainClassLaunches() {
  }

  /**
   * The launches starting from the class. The project's map is rebuilt when
   * a build input or the file tree changes, never on a Haxe source edit.
   * Read action.
   */
  @NotNull
  public static List<MainLaunch> launchesOf(@NotNull HaxeClass haxeClass) {
    VirtualFile file = PsiUtilCore.getVirtualFile(haxeClass);
    String name = haxeClass.getName();
    if (file == null || name == null) return List.of();
    Map<MainClass, List<MainLaunch>> launches = launchesByMainClass(haxeClass.getProject());
    return launches.getOrDefault(new MainClass(file, name), List.of());
  }

  @NotNull
  private static Map<MainClass, List<MainLaunch>> launchesByMainClass(@NotNull Project project) {
    CachedValuesManager cachedValues = CachedValuesManager.getManager(project);
    return cachedValues.getCachedValue(project, LAUNCHES_KEY, () -> launchesResult(project), false);
  }

  @NotNull
  private static CachedValueProvider.Result<Map<MainClass, List<MainLaunch>>> launchesResult(@NotNull Project project) {
    List<Object> dependencies = new ArrayList<>(List.of(HaxeBuildInputs.getInstance(project).dependencies()));
    // a main class file created, deleted or moved changes what a build's main resolves to
    dependencies.add(VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS);
    return CachedValueProvider.Result.create(collectLaunches(project), dependencies.toArray());
  }

  @NotNull
  private static Map<MainClass, List<MainLaunch>> collectLaunches(@NotNull Project project) {
    Map<MainClass, List<MainLaunch>> launches = new HashMap<>();
    Set<VirtualFile> seen = new HashSet<>();
    for (String containerId : HaxeContainers.containerIds(project)) {
      List<HaxeBuildFile> buildFiles = HaxeKnownBuildFiles.forContainer(project, containerId);
      List<String> testsPaths = HaxeTestFrameworks.testsBuildPathsAmong(project, containerId, buildFiles);
      for (HaxeBuildFile buildFile : buildFiles) {
        boolean testsBuild = testsPaths.contains(buildFile.file().getPath());
        if (testsBuild || !seen.add(buildFile.file())) continue;
        addLaunches(project, buildFile, launches);
      }
    }
    return launches;
  }

  private static void addLaunches(@NotNull Project project,
                                  @NotNull HaxeBuildFile buildFile,
                                  @NotNull Map<MainClass, List<MainLaunch>> launches) {
    HaxeClass mainClass = HaxeMainClasses.mainClassOf(project, buildFile);
    VirtualFile mainFile = mainClass == null ? null : PsiUtilCore.getVirtualFile(mainClass);
    if (mainFile == null || mainClass.getName() == null) return;

    HaxeBuildFileInfo info = HaxeEffectiveBuildFileInfo.of(project, buildFile);
    String targetOutput = HaxeProgramLaunches.launchOutput(info);
    List<HaxeLaunchKind> kinds = HaxeProgramLaunches.launchKinds(project, info, buildFile);
    if (targetOutput == null || kinds.isEmpty()) return;

    MainClass key = new MainClass(mainFile, mainClass.getName());
    List<MainLaunch> classLaunches = launches.computeIfAbsent(key, ignored -> new ArrayList<>());
    for (HaxeLaunchKind kind : kinds) {
      classLaunches.add(new MainLaunch(buildFile, kind, targetOutput));
    }
  }
}
