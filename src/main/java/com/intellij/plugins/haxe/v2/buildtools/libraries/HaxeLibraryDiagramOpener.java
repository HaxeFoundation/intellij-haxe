package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the library dependency diagram of a build file. The one
 * implementation is registered by the optional descriptor that needs the
 * Diagrams plugin, so an absent extension means the diagram cannot be shown
 * and the entries offering it stay hidden.
 */
public interface HaxeLibraryDiagramOpener {

  ExtensionPointName<HaxeLibraryDiagramOpener> EP_NAME = ExtensionPointName.create("com.intellij.plugins.haxe.libraryDiagramOpener");

  /** The opener when the Diagrams plugin is present, else null. */
  @Nullable
  static HaxeLibraryDiagramOpener available() {
    return EP_NAME.getExtensionList().stream().findFirst().orElse(null);
  }

  void open(@NotNull Project project, @NotNull HaxeBuildFile buildFile);
}
