package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeLastFailures;
import org.jetbrains.annotations.NotNull;

/**
 * The last failed build-tool evaluation per build file path. An
 * {@code nme prepare} or {@code lime display} that exits with an error leaves
 * the file at its raw parse (defines, libraries and class paths without the
 * tool's view), which silently degrades analysis. The tool window marks the
 * build file row with the failure.
 */
@Service(Service.Level.PROJECT)
public final class HaxeBuildToolFailures extends HaxeLastFailures {

  public HaxeBuildToolFailures(@NotNull Project project) {
    super(project);
  }

  @NotNull
  public static HaxeBuildToolFailures getInstance(@NotNull Project project) {
    return project.getService(HaxeBuildToolFailures.class);
  }

  /** The failure text for a tool run that timed out or exited with an error: the tool's first stderr line. */
  @NotNull
  static String failureOf(@NotNull String tool, @NotNull ProcessOutput output) {
    String detail = output.isTimeout()
                    ? HaxeBundle.message("haxe.toolwindow.build.file.tool.timeout")
                    : HaxeProjectInfoCache.firstErrorLine(output);
    return HaxeBundle.message("haxe.toolwindow.build.file.tool.failed", tool, detail);
  }

  /** The failure text for a tool that could not be started. */
  @NotNull
  static String failureOf(@NotNull String tool, @NotNull Exception e) {
    return HaxeBundle.message("haxe.toolwindow.build.file.tool.failed", tool, e.getMessage());
  }
}
