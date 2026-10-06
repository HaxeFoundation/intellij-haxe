package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.OSAgnosticPathUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The configuration files a build file imports: files whose edits change the
 * build although the build file itself stays untouched.
 * <p>
 * For an hxml these are its hxml references at every nesting level, each
 * resolved against the build file's work directory as the compiler does. For a
 * lime/openfl/nme project file they are its {@code <include path|name>} project
 * files, each resolved against the INCLUDING file's folder; a folder stands for
 * its include.lime, include.nmml or include.xml, whichever exists first.
 * <p>
 * Conditions are not evaluated, since a file behind {@code if}/{@code unless}
 * applies to some target. Assets, templates and {@code <include haxelib>} (a
 * library's own include file, outside the project) are not reported. Only files
 * under a content root are reported, because the build-file watcher sees
 * nothing else.
 */
public final class HaxeBuildFileIncludes {

  /** A folder include's project file candidates, in lime's probing order. */
  private static final List<String> FOLDER_INCLUDE_NAMES = List.of("include.lime", "include.nmml", "include.xml");

  private HaxeBuildFileIncludes() {
  }

  /** Absolute paths of the EXISTING configuration files the build file imports, recursively; cycles are cut. */
  @NotNull
  public static Set<String> configurationIncludes(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return HaxeReadActions.compute(() -> {
      Set<String> includes = new LinkedHashSet<>();
      ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
      switch (buildFile.type()) {
        case HXML -> collectHxmlIncludes(project, buildFile.file(), fileIndex, includes);
        case OPENFL, LIME, NMML -> collectXmlIncludes(buildFile.file(), fileIndex, new HashSet<>(), includes);
        case HXP_PROJECT, HXP_SCRIPT -> { }
      }
      return includes;
    });
  }

  private static void collectHxmlIncludes(@NotNull Project project,
                                          @NotNull VirtualFile hxml,
                                          @NotNull ProjectFileIndex fileIndex,
                                          @NotNull Set<String> includes) {
    String content = HaxeBuildFileInspector.loadText(hxml);
    VirtualFile workDirectory = HaxeBuildWorkDirectories.anchor(project, hxml);
    if (content == null || workDirectory == null) return;
    HxmlFileParser.flatten(content, new HxmlIncludeCollector(workDirectory, fileIndex, includes));
  }

  /**
   * The reader {@link HxmlFileParser#flatten} reads each reference through;
   * it records every reference that resolves under a content root. Files
   * outside are still read, so the references THEY hold are followed.
   */
  private record HxmlIncludeCollector(@NotNull VirtualFile workDirectory,
                                      @NotNull ProjectFileIndex fileIndex,
                                      @NotNull Set<String> includes) implements HxmlFileParser.IncludeReader {

    @Override
    public @Nullable String read(@NotNull String reference) {
      VirtualFile included = workDirectory.findFileByRelativePath(reference);
      if (included == null || included.isDirectory()) return null;
      if (fileIndex.isInContent(included)) {
        includes.add(included.getPath());
      }
      return HaxeBuildFileInspector.loadText(included);
    }
  }

  private static void collectXmlIncludes(@NotNull VirtualFile projectFile,
                                         @NotNull ProjectFileIndex fileIndex,
                                         @NotNull Set<String> visited,
                                         @NotNull Set<String> includes) {
    visited.add(projectFile.getPath());
    String content = HaxeBuildFileInspector.loadText(projectFile);
    VirtualFile folder = projectFile.getParent();
    if (content == null || folder == null) return;
    for (String path : ProjectXmlParser.parseIncludePaths(content)) {
      // TODO: a ${...} path needs lime's variable substitution (haxelib paths, defines); such includes are not watched
      if (path.contains("${")) continue;
      VirtualFile included = resolveIncludeFile(folder, path);
      if (included == null || !fileIndex.isInContent(included)) continue;
      if (visited.add(included.getPath())) {
        includes.add(included.getPath());
        collectXmlIncludes(included, fileIndex, visited, includes);
      }
    }
  }

  /** The include's project file: the path itself, or a folder's first existing include.lime / include.nmml / include.xml. */
  @Nullable
  private static VirtualFile resolveIncludeFile(@NotNull VirtualFile folder, @NotNull String path) {
    VirtualFile target = OSAgnosticPathUtil.isAbsolute(path)
                         ? folder.getFileSystem().findFileByPath(path)
                         : folder.findFileByRelativePath(path);
    if (target == null || !target.isDirectory()) return target;
    for (String name : FOLDER_INCLUDE_NAMES) {
      VirtualFile candidate = target.findChild(name);
      if (candidate != null && !candidate.isDirectory()) return candidate;
    }
    return null;
  }
}
