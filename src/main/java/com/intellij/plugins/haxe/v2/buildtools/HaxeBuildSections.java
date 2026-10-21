package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeSectionSelectionStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The developer's view onto an hxml {@code --next} chain: which compilation
 * section the tool window, library sync, define context and test runs follow.
 * Selections are stored by section identity, so a removed section falls back
 * to the first one — the section the compiler's own display mode uses.
 * Single-section files (and non-hxml builds) are unaffected.
 */
public final class HaxeBuildSections {

  private HaxeBuildSections() {
  }

  /** The index the stored selection resolves to among the given section contents; 0 without a (surviving) selection. */
  public static int selectedIndex(@NotNull Project project, @NotNull VirtualFile buildFile, @NotNull List<String> sections) {
    List<String> ids = HxmlFileParser.sectionIds(buildFile.getName(), sections);
    return HaxeSectionSelectionStore.getInstance(project).getSelectedSection(buildFile, ids);
  }

  /** The build file's info for the SELECTED section (first by default). */
  @NotNull
  public static HaxeBuildFileInfo inspectSelected(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    if (buildFile.type() != HaxeBuildFileType.HXML) {
      return HaxeBuildFileInspector.inspect(project, buildFile);
    }
    String selected = selectedSectionContent(project, buildFile);
    return selected == null ? HaxeBuildFileInfo.EMPTY : HxmlFileParser.parse(selected);
  }

  /** The SELECTED section's effective hxml content, or the whole effective content for single-section files. Null when unreadable. */
  @Nullable
  public static String selectedSectionContent(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    if (buildFile.type() != HaxeBuildFileType.HXML) {
      return HaxeBuildFileInspector.effectiveContent(buildFile.file());
    }
    List<String> sections = HaxeBuildFileInspector.sectionContents(project, buildFile.file());
    if (sections.isEmpty()) return null;
    if (sections.size() == 1) return sections.getFirst();
    return sections.get(selectedIndex(project, buildFile.file(), sections));
  }

  /**
   * The SELECTED {@code --next} section's lines as compiler arguments, or
   * null for a single-section file — callers keep their whole-file shape
   * then. The one expansion the section-scoped compile and the display
   * service's server context share. Call in a read action.
   */
  @Nullable
  public static List<String> selectedSectionArguments(@NotNull Project project, @NotNull VirtualFile file) {
    List<String> sections = HaxeBuildFileInspector.sectionContents(project, file);
    if (sections.size() < 2) return null;
    int index = selectedIndex(project, file, sections);
    return HxmlArguments.parseLines(sections.get(index).lines().toList());
  }
}
