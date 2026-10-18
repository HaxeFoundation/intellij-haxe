package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A build file with the hxml files it includes, each with what its OWN
 * lines declare: the structure the compiler flattens away, kept for the
 * project diagram. References resolve next to the root file, as haxe reads
 * them, so one root serves every nesting level; a file reached twice is read
 * once, so an include loop ends where it closes; a reference that does not
 * resolve is left out. The whole file counts, {@code --next} sections
 * included.
 */
public record HxmlIncludeTree(@NotNull VirtualFile file, @NotNull HaxeBuildFileInfo own, @NotNull List<HxmlIncludeTree> includes) {

  /** The tree under an hxml root. */
  @NotNull
  public static HxmlIncludeTree of(@NotNull VirtualFile root) {
    Set<String> visited = new HashSet<>();
    visited.add(root.getPath());
    return read(root, root, visited);
  }

  /** A build file shown without includes (a project.xml, an unreadable file): one entry with the given declarations. */
  @NotNull
  public static HxmlIncludeTree leaf(@NotNull VirtualFile file, @NotNull HaxeBuildFileInfo own) {
    return new HxmlIncludeTree(file, own, List.of());
  }

  @NotNull
  private static HxmlIncludeTree read(@NotNull VirtualFile root, @NotNull VirtualFile file, @NotNull Set<String> visited) {
    String content = HaxeBuildFileInspector.loadText(file);
    if (content == null) return leaf(file, HaxeBuildFileInfo.EMPTY);
    List<String> ownLines = new ArrayList<>();
    List<HxmlIncludeTree> includes = new ArrayList<>();
    for (String rawLine : content.lines().toList()) {
      String line = rawLine.trim();
      if (!HxmlFileParser.isHxmlReference(line)) {
        ownLines.add(rawLine);
        continue;
      }
      VirtualFile included = resolveNextToRoot(root, line);
      if (included != null && visited.add(included.getPath())) {
        includes.add(read(root, included, visited));
      }
    }
    return new HxmlIncludeTree(file, HxmlFileParser.parse(String.join("\n", ownLines)), List.copyOf(includes));
  }

  @Nullable
  private static VirtualFile resolveNextToRoot(@NotNull VirtualFile root, @NotNull String reference) {
    VirtualFile parent = root.getParent();
    return parent == null ? null : parent.findFileByRelativePath(reference);
  }

  /** Every file of the tree, this one first, depth first. */
  @NotNull
  public List<HxmlIncludeTree> files() {
    List<HxmlIncludeTree> files = new ArrayList<>();
    collect(files);
    return files;
  }

  private void collect(@NotNull List<HxmlIncludeTree> into) {
    into.add(this);
    for (HxmlIncludeTree include : includes) {
      include.collect(into);
    }
  }

  /** Every library any file of the tree declares, in tree order, the first declaration of a name winning. */
  @NotNull
  public List<HaxeLibDependency> declaredLibraries() {
    Map<String, HaxeLibDependency> byName = new LinkedHashMap<>();
    for (HxmlIncludeTree entry : files()) {
      for (HaxeLibDependency library : entry.own().libraries()) {
        byName.putIfAbsent(library.key(), library);
      }
    }
    return List.copyOf(byName.values());
  }
}
