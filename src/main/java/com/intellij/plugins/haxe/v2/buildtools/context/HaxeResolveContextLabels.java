package com.intellij.plugins.haxe.v2.buildtools.context;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInspector;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSections;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetOptions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * How a resolve context reads in the switcher: the build file's name, with
 * the target or section it is currently set to when the file has several -
 * {@code project.xml [HTML5]}, {@code build.hxml [hl]}. Call in a read action.
 */
public final class HaxeResolveContextLabels {

  private HaxeResolveContextLabels() {
  }

  @NotNull
  public static String label(@NotNull Project project, @NotNull HaxeResolveContext context) {
    HaxeBuildFile buildFile = context.buildFile();
    String name = buildFile.file().getName();
    String variant = variant(project, buildFile);
    return variant == null ? name : HaxeBundle.message("haxe.resolve.context.label.variant", name, variant);
  }

  /** The selected target of a lime-family file, the selected section of a multi-section hxml; null otherwise. */
  @Nullable
  private static String variant(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    HaxeBuildFileType type = buildFile.type();
    VirtualFile file = buildFile.file();
    if (HaxeTargetOptions.isTargetSelectable(type)) {
      return HaxeTargetOptions.displayNameFor(type, HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(file));
    }
    if (type != HaxeBuildFileType.HXML) return null;
    List<String> sections = HaxeBuildFileInspector.sectionContents(project, file);
    if (sections.size() < 2) return null;
    int selected = HaxeBuildSections.selectedIndex(project, file, sections);
    return HxmlFileParser.sectionDescriptors(sections).get(selected);
  }
}
