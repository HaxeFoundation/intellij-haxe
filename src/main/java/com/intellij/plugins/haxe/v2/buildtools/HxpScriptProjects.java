package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// Facts about plain hxp build scripts (arbitrary Haxe run via
/// `haxelib run hxp`, no lime semantics): the single build action and its
/// command. Counterparts: [HxmlProjects], [LimeProjects], [NmeProjects].
public final class HxpScriptProjects {

  /** A plain hxp script's single default action: `haxelib run hxp <file>`. */
  public static final String BUILD_ACTION = "build";

  private HxpScriptProjects() {
  }

  /** The hxp tool runs the script itself - no lime, no target flag. */
  @NotNull
  public static List<String> buildCommand(@NotNull Project project,
                                          @Nullable String environmentSdk,
                                          @NotNull VirtualFile file) {
    return List.of(HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk), "run", "hxp", file.getName());
  }
}
