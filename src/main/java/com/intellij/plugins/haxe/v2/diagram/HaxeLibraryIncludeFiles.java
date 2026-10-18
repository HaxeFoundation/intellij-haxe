package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.buildsystem.hxp.HxpFileType;
import com.intellij.plugins.haxe.haxelib.HaxelibRepositoryLayout;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildsystem.ProjectXmlParser;
import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The files the build's libraries add to it, as the project diagram shows
 * them: each library's extraParams.hxml, whose lines haxelib adds to every
 * build using the library, and - when the build tool merges them (lime, nme) -
 * its include file. Each comes with the libraries it brings into the build
 * and the compiler defines it sets. An include file is mostly
 * platform-conditional and its conditions are not evaluated here, so only what
 * the build's own evaluation uses counts; an extraParams.hxml has no
 * conditions. A define a {@code --macro} sets is the compiler's to know and
 * not shown.
 */
final class HaxeLibraryIncludeFiles {

  /** One file a library adds to the build, the libraries it brings into the build and the defines the build takes from it. */
  record LibraryInclude(@NotNull String library,
                        @NotNull VirtualFile file,
                        @NotNull List<HaxeLibDependency> declares,
                        @NotNull List<HaxeDefine> defines) {
  }

  /** The library and define names a build's evaluation uses. */
  private record BuildUse(@NotNull Set<String> libraries, @NotNull Set<String> defines) {

    @NotNull
    static BuildUse of(@NotNull HaxeBuildFileInfo build) {
      Set<String> libraries = build.libraries().stream()
        .map(HaxeLibDependency::key)
        .collect(Collectors.toSet());
      Set<String> defines = build.defines().stream()
        .map(HaxeDefine::name)
        .collect(Collectors.toSet());
      return new BuildUse(libraries, defines);
    }
  }

  private HaxeLibraryIncludeFiles() {
  }

  /**
   * The files of the libraries and of every library one of them brings in, in
   * walk order. {@code rootOf} gives a library's folder, null when unknown;
   * {@code mergingBuild} is what a build whose tool merges include files uses,
   * null for any other build. Read action.
   */
  @NotNull
  static List<LibraryInclude> walk(@NotNull Collection<String> libraries,
                                   @NotNull Function<String, VirtualFile> rootOf,
                                   @Nullable HaxeBuildFileInfo mergingBuild) {
    BuildUse use = mergingBuild == null ? null : BuildUse.of(mergingBuild);
    List<LibraryInclude> includes = new ArrayList<>();
    Deque<String> pending = new ArrayDeque<>(libraries);
    Set<String> visited = new HashSet<>();
    while (!pending.isEmpty()) {
      String library = pending.removeFirst();
      if (!visited.add(HaxeLibDependency.key(library))) continue;
      VirtualFile root = rootOf.apply(library);
      if (root == null) continue;
      for (LibraryInclude include : includesOf(library, root, use)) {
        includes.add(include);
        include.declares().forEach(declared -> pending.addLast(declared.name()));
      }
    }
    return includes;
  }

  /**
   * A library file's title: the library folder it ships in, above a haxelib
   * version folder, and its name ({@code openfl/include.xml}); null for any
   * other file.
   */
  @Nullable
  static String titleOf(@NotNull String path) {
    List<String> segments = StringUtil.split(FileUtil.toSystemIndependentName(path), "/");
    if (segments.size() < 2 || !isLibraryFileName(segments.getLast())) return null;
    int folder = segments.size() - 2;
    if (folder > 0 && HaxelibRepositoryLayout.versionOf(segments.get(folder)) != null) folder--;
    return segments.get(folder) + "/" + segments.getLast();
  }

  private static boolean isLibraryFileName(@NotNull String fileName) {
    return ProjectXmlVocabulary.isLibraryIncludeFileName(fileName) || HaxelibRepositoryLayout.EXTRA_PARAMS_FILE_NAME.equals(fileName);
  }

  /** The library's extraParams.hxml, then the include file a merging build takes from it; whichever exist. */
  @NotNull
  private static List<LibraryInclude> includesOf(@NotNull String library, @NotNull VirtualFile root, @Nullable BuildUse use) {
    List<LibraryInclude> includes = new ArrayList<>(2);
    VirtualFile extraParams = root.findChild(HaxelibRepositoryLayout.EXTRA_PARAMS_FILE_NAME);
    if (extraParams != null && !extraParams.isDirectory()) {
      includes.add(extraParamsInclude(library, extraParams));
    }
    VirtualFile toolInclude = use == null ? null : LimeProjects.libraryIncludeFile(root);
    if (toolInclude != null) {
      includes.add(toolInclude(library, toolInclude, use));
    }
    return includes;
  }

  // haxelib adds every line whenever the library is used: nothing to filter
  @NotNull
  private static LibraryInclude extraParamsInclude(@NotNull String library, @NotNull VirtualFile file) {
    HaxeBuildFileInfo declared = HxmlFileParser.parse(textOf(file));
    List<HaxeLibDependency> declares = declared.libraries().stream().distinct().toList();
    List<HaxeDefine> defines = declared.defines().stream().distinct().toList();
    return new LibraryInclude(library, file, declares, defines);
  }

  @NotNull
  private static LibraryInclude toolInclude(@NotNull String library, @NotNull VirtualFile file, @NotNull BuildUse use) {
    HaxeBuildFileInfo declared = parseToolInclude(file);
    List<HaxeLibDependency> declares = declared.libraries().stream()
      .filter(dependency -> use.libraries().contains(dependency.key()))
      .map(HaxeLibraryIncludeFiles::withoutVariableVersion)
      .distinct()
      .toList();
    List<HaxeDefine> defines = declared.defines().stream()
      .filter(define -> use.defines().contains(define.name()))
      .distinct()
      .toList();
    return new LibraryInclude(library, file, declares, defines);
  }

  // a version spelled with a tool variable (${lime-version}) is only known to the build's evaluation
  @NotNull
  private static HaxeLibDependency withoutVariableVersion(@NotNull HaxeLibDependency dependency) {
    String version = dependency.version();
    return version != null && version.contains("${") ? new HaxeLibDependency(dependency.name(), null) : dependency;
  }

  @NotNull
  private static HaxeBuildFileInfo parseToolInclude(@NotNull VirtualFile file) {
    // TODO: read the libraries an include.hxp declares; they are Haxe code, the box lists none
    if (FileUtilRt.extensionEquals(file.getName(), HxpFileType.DEFAULT_EXTENSION)) return HaxeBuildFileInfo.EMPTY;
    return ProjectXmlParser.parse(textOf(file));
  }

  /** The file's text; empty when it cannot be read. */
  @NotNull
  private static String textOf(@NotNull VirtualFile file) {
    try {
      return VfsUtilCore.loadText(file);
    }
    catch (IOException e) {
      return "";
    }
  }
}
