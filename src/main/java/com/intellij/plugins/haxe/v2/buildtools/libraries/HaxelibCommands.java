package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolEnvironment;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs haxelib for a container: the executable the Build Tools settings and
 * the container's SDK resolve to, that SDK's folders on PATH, the given
 * working directory (haxelib finds a local {@code .haxelib} repository from
 * it). Library management goes through here (installs, the installed-library
 * list, the sync's {@code haxelib path}), so all three hit the same haxelib
 * and repository. Runs a process: never on the EDT or under the read lock.
 */
@CustomLog
public final class HaxelibCommands {

  private static final int COMMAND_TIMEOUT_MS = 600_000;

  private HaxelibCommands() {
  }

  /** The command line of one haxelib invocation; the caller runs it (captured output, its own timeout). */
  @NotNull
  public static GeneralCommandLine commandLine(@NotNull Project project,
                                               @NotNull String containerId,
                                               @Nullable Path workDirectory,
                                               @NotNull List<String> arguments) {
    String sdkName = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    GeneralCommandLine commandLine = new GeneralCommandLine(HaxeToolPathResolver.resolveHaxelibExecutable(project, sdkName))
      .withParameters(arguments)
      .withWorkingDirectory(workDirectory)
      .withCharset(StandardCharsets.UTF_8);
    return HaxeToolEnvironment.withSdkOnPath(commandLine, HaxeToolPathResolver.resolveSdk(project, containerId));
  }

  /** The lines haxelib printed, stdout and stderr merged; empty when it could not start (logged), whatever it produced when it failed. */
  @NotNull
  public static List<String> output(@NotNull Project project,
                                    @NotNull String containerId,
                                    @Nullable Path workDirectory,
                                    @NotNull List<String> arguments) {
    GeneralCommandLine commandLine = commandLine(project, containerId, workDirectory, arguments).withRedirectErrorStream(true);
    try {
      ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(COMMAND_TIMEOUT_MS);
      if (output.getExitCode() != 0 || output.isTimeout()) {
        String outcome = output.isTimeout() ? "timed out" : "returned " + output.getExitCode();
        log.warn("haxelib " + outcome + ": " + commandLine.getCommandLineString());
      }
      return output.getStdoutLines();
    }
    catch (ExecutionException e) {
      log.warn("haxelib failed to start: " + commandLine.getCommandLineString(), e);
      return List.of();
    }
  }
}
