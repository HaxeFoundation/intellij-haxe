package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectFileDetectionUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds Haxe build/project files (hxml, OpenFL/Lime xml, nmml, hxp). Only the top
 * level of each content root is scanned: generated build files in subfolders (for
 * example OpenFL's Export directory) must not be picked up - files in subfolders
 * are registered manually via Add Build File. Must run in a read action.
 */
public final class HaxeBuildFileScanner {

  private HaxeBuildFileScanner() {
  }

  @NotNull
  public static List<HaxeBuildFile> scan(@NotNull Module module) {
    List<HaxeBuildFile> result = new ArrayList<>();
    for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
      scanDirectory(module.getProject(), contentRoot, result);
    }
    sortByName(result);
    return result;
  }

  /**
   * Build files directly in the project base dir that belong to no module (such files
   * are outside every content root, so the module scan cannot see them).
   */
  @NotNull
  public static List<HaxeBuildFile> scanProjectRoot(@NotNull Project project) {
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    if (baseDir == null) return List.of();

    List<HaxeBuildFile> result = new ArrayList<>();
    for (VirtualFile child : baseDir.getChildren()) {
      if (child.isDirectory() || ModuleUtilCore.findModuleForFile(child, project) != null) continue;
      addIfBuildFile(project, child, result);
    }
    sortByName(result);
    return result;
  }

  private static void scanDirectory(@NotNull Project project, @NotNull VirtualFile directory, @NotNull List<HaxeBuildFile> result) {
    for (VirtualFile child : directory.getChildren()) {
      if (!child.isDirectory()) {
        addIfBuildFile(project, child, result);
      }
    }
  }

  private static void addIfBuildFile(@NotNull Project project, @NotNull VirtualFile file, @NotNull List<HaxeBuildFile> result) {
    HaxeBuildFileType type = detectType(project, file);
    if (type != null) {
      result.add(new HaxeBuildFile(file, type));
    }
  }

  private record DetectedType(long modificationStamp, @Nullable HaxeBuildFileType type) {
  }

  // Detection only CLASSIFIES the file (a streaming XML parse for xml/nmml,
  // a Haxe PSI check for hxp) - reading what a build file actually configures
  // is the lime tooling's job and happens elsewhere. Classification is
  // queried from hot paths (the define context feeds conditional
  // compilation); build files are few, so a cache keyed by path +
  // modification stamp stays tiny
  private static final Map<String, DetectedType> detectionCache = new ConcurrentHashMap<>();

  /** The typed build file at the path, or null when the path names no valid build file. */
  @Nullable
  public static HaxeBuildFile findBuildFile(@NotNull Project project, @NotNull String path) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    if (file == null || !file.isValid()) return null;
    HaxeBuildFileType type = detectType(project, file);
    return type == null ? null : new HaxeBuildFile(file, type);
  }

  /** Detects the build file type of an arbitrary file (any xml name is accepted). */
  @Nullable
  public static HaxeBuildFileType detectType(@NotNull Project project, @NotNull VirtualFile file) {
    String path = file.getPath();
    long stamp = file.getModificationStamp();
    DetectedType cached = detectionCache.get(path);
    if (cached != null && cached.modificationStamp() == stamp) {
      return cached.type();
    }
    HaxeBuildFileType type = doDetectType(project, file);
    detectionCache.put(path, new DetectedType(stamp, type));
    return type;
  }

  private static HaxeBuildFileType doDetectType(@NotNull Project project, @NotNull VirtualFile file) {
    String extension = file.getExtension();
    if (extension == null) return null;
    return switch (extension.toLowerCase(Locale.ROOT)) {
      case "hxml" -> HaxeBuildFileType.HXML;
      case "hxp" -> isLimeHxpProject(project, file) ? HaxeBuildFileType.HXP_PROJECT : HaxeBuildFileType.HXP_SCRIPT;
      case "nmml" -> HaxeProjectFileDetectionUtil.isNMMLProject(file) ? HaxeBuildFileType.NMML : null;
      case "xml" -> {
        if (HaxeProjectFileDetectionUtil.isOpenFLProject(file)) yield HaxeBuildFileType.OPENFL;
        if (HaxeProjectFileDetectionUtil.isLimeProject(file)) yield HaxeBuildFileType.LIME;
        yield null;
      }
      default -> null;
    };
  }

  /**
   * Whether the .hxp is a lime/openfl PROJECT script: it declares a class
   * extending HXProject, optionally qualified (lime.tools.HXProject) - the
   * shape lime's HXProject.fromFile requires. Checked on the Haxe PSI
   * (.hxp IS Haxe source); the extends name is compared as text because
   * lime's classes may not be resolvable (lime not installed), and lime
   * itself only requires the declared shape. Anything else is a plain hxp
   * build script: arbitrary Haxe that lime cannot load - offering lime
   * actions or targets for it would run `lime build` against a non-project.
   */
  private static boolean isLimeHxpProject(@NotNull Project project, @NotNull VirtualFile file) {
    return HaxeReadActions.compute(() -> {
      if (!(PsiManager.getInstance(project).findFile(file) instanceof HaxeFile haxeFile)) return false;
      for (HaxeClass haxeClass : haxeFile.getClassList()) {
        for (HaxeType extendsType : haxeClass.getHaxeExtendsList()) {
          String name = extendsType.getReferenceExpression().getText();
          if (name.equals("HXProject") || name.endsWith(".HXProject")) return true;
        }
      }
      return false;
    });
  }

  private static void sortByName(@NotNull List<HaxeBuildFile> files) {
    files.sort(Comparator.comparing(buildFile -> buildFile.file().getName(), String.CASE_INSENSITIVE_ORDER));
  }
}
