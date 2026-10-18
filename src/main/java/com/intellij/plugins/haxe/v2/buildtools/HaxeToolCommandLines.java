package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command lines for tool runs. Console runs go under a
 * pseudo-terminal: a tool that prompts through the console device (hxcpp's
 * {@code Sys.getChar}) only sees typed answers that way, a stdin pipe never
 * reaches it. Console mode keeps stderr separate and leaves the echo of
 * typed text to the console view. Console and captured runs alike carry
 * the lime tool's environment mirror ({@link LimeProjects#commandEnvironment}),
 * so every run packages air builds the same way, and put the container's
 * SDK folders first on PATH ({@link HaxeToolEnvironment}), so the tools
 * reach that SDK's haxelib and neko.
 */
public final class HaxeToolCommandLines {

  private HaxeToolCommandLines() {
  }

  @NotNull
  public static GeneralCommandLine interactive(@NotNull Project project,
                                               @NotNull String containerId,
                                               @NotNull List<String> command,
                                               @Nullable String workDirectory) {
    Path workingDirectory = workDirectory == null ? null : Path.of(workDirectory);
    // the pty hard-wraps output at its column count, and the console view is no terminal
    GeneralCommandLine commandLine = new PtyCommandLine(withResolvedExecutable(command))
      .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
      .withWorkingDirectory(workingDirectory)
      .withEnvironment(LimeProjects.commandEnvironment(command));
    return withContainerSdkOnPath(project, containerId, commandLine);
  }

  /**
   * A command line whose output the caller reads itself: the same executable
   * resolution and tool environment as {@link #interactive}, as a plain pipe
   * so stdin can be fed and stdout captured.
   */
  @NotNull
  public static GeneralCommandLine captured(@NotNull Project project,
                                            @NotNull String containerId,
                                            @NotNull List<String> command,
                                            @Nullable String workDirectory) {
    Path workingDirectory = workDirectory == null ? null : Path.of(workDirectory);
    GeneralCommandLine commandLine = new GeneralCommandLine(withResolvedExecutable(command))
      .withWorkingDirectory(workingDirectory)
      .withEnvironment(LimeProjects.commandEnvironment(command))
      .withCharset(StandardCharsets.UTF_8);
    return withContainerSdkOnPath(project, containerId, commandLine);
  }

  @NotNull
  private static GeneralCommandLine withContainerSdkOnPath(@NotNull Project project,
                                                           @NotNull String containerId,
                                                           @NotNull GeneralCommandLine commandLine) {
    return HaxeToolEnvironment.withSdkOnPath(commandLine, HaxeToolPathResolver.resolveSdk(project, containerId));
  }

  /**
   * On Windows a bare program name is resolved through PATH honoring PATHEXT:
   * a terminal launches "nme" (nme.bat) fine, but CreateProcess never tries
   * extensions and fails with error=2. Resolved here, at launch, so building
   * the tool window's model does not stat every PATH entry per command.
   */
  @NotNull
  private static List<String> withResolvedExecutable(@NotNull List<String> command) {
    if (command.isEmpty()) return command;
    List<String> resolved = new ArrayList<>(command);
    resolved.set(0, PathEnvironmentVariableUtil.findExecutableInWindowsPath(command.getFirst()));
    return resolved;
  }
}
