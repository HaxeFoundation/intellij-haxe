package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Config files of the HaxeCheckstyle tool family. A directory holding one marks
 * where its tool is meant to run: the tools resolve their config and relative
 * excludes against the working directory (haxelib forwards it via the
 * HAXELIB_RUN cwd convention).
 */
public final class HaxeToolConfigs {

  public static final String CHECKSTYLE_CONFIG_NAME = "checkstyle.json";
  public static final String FORMATTER_CONFIG_NAME = "hxformat.json";
  /** The haxelibs {@code haxelib run} starts for these configs. */
  public static final String CHECKSTYLE_HAXELIB = "checkstyle";
  public static final String FORMATTER_HAXELIB = "formatter";

  private HaxeToolConfigs() {
  }

  /**
   * The nearest directory holding the config, walking up from the file to the
   * project base directory (inclusive), or null when none holds one — the same
   * upward discovery the formatter itself performs per source file.
   */
  @Nullable
  public static VirtualFile findConfigDirectory(@NotNull Project project, @NotNull VirtualFile file, @NotNull String configName) {
    VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
    VirtualFile directory = file.isDirectory() ? file : file.getParent();
    while (directory != null) {
      if (directory.findChild(configName) != null) return directory;
      if (directory.equals(projectDir)) return null;
      directory = directory.getParent();
    }
    return null;
  }
}
