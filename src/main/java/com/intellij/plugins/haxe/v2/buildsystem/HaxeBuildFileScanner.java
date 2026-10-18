package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLFileType;
import com.intellij.plugins.haxe.buildsystem.hxp.HxpFileType;
import com.intellij.plugins.haxe.buildsystem.lime.LimeProjectShape;
import com.intellij.plugins.haxe.buildsystem.nmml.NMMLFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds Haxe build/project files (hxml, hxp, nmml, Lime/OpenFL .xml or .lime). Only the top
 * level of each content root is scanned: generated build files in subfolders (for
 * example OpenFL's Export directory) must not be picked up - files in subfolders
 * are registered manually via Add Build File. Must run in a read action.
 */
public final class HaxeBuildFileScanner {

  /** Every extension {@link #detectType} classifies - the Add Build File chooser's filter. */
  public static final Set<String> BUILD_FILE_EXTENSIONS = buildFileExtensions();

  private HaxeBuildFileScanner() {
  }

  private static Set<String> buildFileExtensions() {
    Set<String> extensions = new HashSet<>(LimeProjectShape.FILE_EXTENSIONS);
    extensions.add(HXMLFileType.DEFAULT_EXTENSION);
    extensions.add(HxpFileType.DEFAULT_EXTENSION);
    extensions.add(NMMLFileType.DEFAULT_EXTENSION);
    return Set.copyOf(extensions);
  }

  /** Whether the path's extension is one a build file can carry; the content decides the rest ({@link #detectType}). */
  public static boolean hasBuildFileExtension(@NotNull String path) {
    return BUILD_FILE_EXTENSIONS.contains(extensionOf(path));
  }

  private static String extensionOf(@NotNull String path) {
    return FileUtilRt.getExtension(path).toLowerCase(Locale.ROOT);
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
    String extension = extensionOf(file.getName());
    return switch (extension) {
      case HXMLFileType.DEFAULT_EXTENSION -> HaxeBuildFileType.HXML;
      case HxpFileType.DEFAULT_EXTENSION -> hxpType(project, file);
      case NMMLFileType.DEFAULT_EXTENSION -> nmmlType(file);
      default -> LimeProjectShape.FILE_EXTENSIONS.contains(extension) ? limeFamilyType(file) : null;
    };
  }

  /** A lime project script or a plain hxp build script; lime's include.hxp is a library's include file and no build of its own. */
  @Nullable
  private static HaxeBuildFileType hxpType(@NotNull Project project, @NotNull VirtualFile file) {
    if (ProjectXmlVocabulary.isLibraryIncludeFileName(file.getName())) return null;
    return isLimeHxpProject(project, file) ? HaxeBuildFileType.HXP_PROJECT : HaxeBuildFileType.HXP_SCRIPT;
  }

  /**
   * Any file under NME's extension with a {@code <project>} root. NME needs no
   * {@code <haxelib>} or {@code <source>} (it adds itself and uses the project
   * folder), and a project declaring the openfl lib is still built by NME.
   */
  @Nullable
  private static HaxeBuildFileType nmmlType(@NotNull VirtualFile file) {
    return shapeOf(file).projectRoot() ? HaxeBuildFileType.NMML : null;
  }

  /**
   * Under the Lime extensions: a {@code <project>} declaring a library or a
   * source path is a Lime project, an OpenFL one when a declared library is
   * openfl; anything else (a maven pom, a library's include file) is no build file.
   */
  @Nullable
  private static HaxeBuildFileType limeFamilyType(@NotNull VirtualFile file) {
    // lime merges a library's include file into the build naming the library and skips it when looking for a project
    if (ProjectXmlVocabulary.isLibraryIncludeFileName(file.getName())) return null;
    LimeProjectShape shape = shapeOf(file);
    if (shape.isOpenfl()) return HaxeBuildFileType.OPENFL;
    return shape.isLime() ? HaxeBuildFileType.LIME : null;
  }

  /** The file's project-file shape as on disk, which the detection cache's VFS stamp tracks; {@link LimeProjectShape#NONE} when it cannot be read. */
  @NotNull
  private static LimeProjectShape shapeOf(@NotNull VirtualFile file) {
    try {
      return ProjectXmlParser.parseShape(VfsUtilCore.loadText(file));
    }
    catch (IOException e) {
      return LimeProjectShape.NONE;
    }
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
