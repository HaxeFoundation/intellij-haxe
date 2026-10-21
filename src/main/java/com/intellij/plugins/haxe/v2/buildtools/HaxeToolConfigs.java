package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
   * upward discovery the tools perform per source file. The IDE formatter's own
   * lookup ({@code HxformatConfigs.findConfig}) differs on purpose: it keeps
   * walking above the project for library sources and falls back to the
   * fallback config the user chose, which the tools cannot read (they take the
   * config beside their working directory).
   */
  @Nullable
  public static VirtualFile findConfigDirectory(@NotNull Project project, @NotNull VirtualFile file, @NotNull String configName) {
    VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
    VirtualFile directory = file.isDirectory() ? file : file.getParent();
    while (directory != null) {
      VirtualFile config = directory.findChild(configName);
      if (config != null && !config.isDirectory()) return directory;
      if (directory.equals(projectDir)) return null;
      directory = directory.getParent();
    }
    return null;
  }

  /** {@code haxelib run <tool> [modes] -s <source>...}: mode flags ({@code --check}, {@code --stdin}) go before the source list. */
  @NotNull
  public static List<String> toolCommand(@NotNull String haxelib, @NotNull String tool,
                                         @NotNull List<String> modes, @NotNull List<String> sources) {
    List<String> command = new ArrayList<>(List.of(haxelib, "run", tool));
    command.addAll(modes);
    for (String source : sources) {
      command.addAll(List.of("-s", source));
    }
    return command;
  }
}
