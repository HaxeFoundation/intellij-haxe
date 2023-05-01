/*
 * Copyright 2017-2019 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.util.containers.ContainerUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CustomLog
public class HaxeProcessUtil {
  static {
    log.setLevel(LogLevel.INFO);
  }  // Set to INFO when finished debugging.

  /**
   * Records errors so that we only show them once.
   */
  private static final Set<String> REPORTED_EXECUTIONS = ContainerUtil.newConcurrentSet();

  public static class Result {
    public List<String> stderr;
    public List<String> stdout;
    public int exitCode;

    public Result() {
      stderr = new ArrayList<>();
      stdout = new ArrayList<>();
      exitCode = -1;
    }

    public Result(@Nullable List<String> stdout, @Nullable List<String> stderr, @Nullable HaxeDebugTimeLog timeLog) {
      this.stderr = null == stderr ? new ArrayList<>() : stderr;
      this.stdout = null == stdout ? new ArrayList<>() : stdout;
      exitCode = -1;
    }

    /**
     * Gets the output from the command.  This may contain both standard and
     * error output if mixed output was requested.
     *
     * @return List of strings for the output, one line per entry.
     */
    public List<String> getOutput() {
      return stdout;
    }

    /**
     * Gets the error output from the command.
     *
     * @return
     */
    public List<String> getErrorOutput() {
      return stderr;
    }

    public int getExitCode() {
      return exitCode;
    }
  }


  private HaxeProcessUtil() {
  } // No instantiation.

  /**
   * Run an interruptible process (without environment updates for the SDK).
   * Checks whether the process/thread has been cancelled (from within IDEA).
   *
   * @param command - Command to run
   * @param args    - arguments to add to the command.
   * @return the output gathered during the run -- even in error conditions.
   */
  @NotNull
  public static Result runProcess(@NotNull String command, @Nullable String... args) {
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add(command);
    for (String arg : args) {
      if (null != arg && !arg.isEmpty()) {
        cmd.add(arg);
      }
    }
    Result result = new Result();

    result.exitCode = runProcess(cmd, false, null, result.stdout, result.stderr, null, true);
    return result;
  }

  /**
   * Run a possibly interruptible process (without environment updates for the SDK).
   * Checks whether the process/thread has been canceled (from within IDEA).
   *
   * @param command       - Command and parameters.
   * @param mixedOutput   - include stderr in stdout output.
   * @param dir           - directory to run the command in.
   * @param stdout        - List to append output to.  Will not be cleared on start.
   * @param stderr        - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   * @return The exit status of the command.
   */
  public static int runSynchronousProcessOnBackgroundThread(List<String> command,
                                                            boolean mixedOutput,
                                                            VirtualFile dir,
    /*modifies*/ List<String> stdout,
    /*modifies*/ List<String> stderr,
                                                            HaxeDebugTimeLog timeLog,
                                                            boolean interruptible) {
    return runSynchronousProcessOnBackgroundThread(command, mixedOutput, dir,
                                                   null, stdout, stderr, timeLog, interruptible);
  }

  /**
   * Run a possibly interruptible process (without environment updates for the SDK).
   * Checks whether the process/thread has been canceled (from within IDEA).
   *
   * @param command       - Command and parameters.
   * @param mixedOutput   - include stderr in stdout output.
   * @param dir           - directory to run the command in.
   * @param stdout        - List to append output to.  Will not be cleared on start.
   * @param stderr        - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   * @return The exit status of the command.
   */
  public static int runProcess(List<String> command,
                               boolean mixedOutput,
                               VirtualFile dir,
    /*modifies*/ List<String> stdout,
    /*modifies*/ List<String> stderr,
                               HaxeDebugTimeLog timeLog,
                               boolean interruptible) {
    return runProcess(command, mixedOutput, dir, null, stdout, stderr, timeLog, interruptible);
  }

  /**
   * Run a possibly interruptible process with the Haxe SDK environment (if given).
   * Checks whether the process/thread has been canceled (from within IDEA).
   *
   * @param command       - Command and parameters.
   * @param mixedOutput   - include stderr in stdout output.
   * @param dir           - directory to run the command in.
   * @param sdkData       - sdk to use to set the command environment.
   * @param stdout        - List to append output to.  Will not be cleared on start.
   * @param stderr        - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   * @return The exit status of the command.
   */
  public static int runSynchronousProcessOnBackgroundThread(List<String> command,
                                                            boolean mixedOutput,
                                                            VirtualFile dir,
                                                            HaxeSdkAdditionalDataBase sdkData,
    /*modifies*/ List<String> stdout,
    /*modifies*/ List<String> stderr,
                                                            HaxeDebugTimeLog timeLog,
                                                            boolean interruptible) {

    if (null == command || command.isEmpty()) return -1;

    return ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> runProcess(command, mixedOutput, dir, sdkData, stdout, stderr, timeLog, interruptible),
      command.get(0),
      interruptible,
      null);
  }

  /**
   * Run a possibly interruptible process with the Haxe SDK environment (if given).
   * Checks whether the process/thread has been canceled (from within IDEA).
   *
   * @param command       - Command and parameters.
   * @param mixedOutput   - include stderr in stdout output.
   * @param dir           - directory to run the command in.
   * @param sdkData       - sdk to use to set the command environment.
   * @param stdout        - List to append output to.  Will not be cleared on start.
   * @param stderr        - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   * @return The exit status of the command.
   */
  public static int runProcess(List<String> command,
                               boolean mixedOutput,
                               VirtualFile dir,
                               HaxeSdkAdditionalDataBase sdkData,
                  /*modifies*/ List<String> stdout,
                  /*modifies*/ List<String> stderr,
                               HaxeDebugTimeLog timeLog,
                               boolean interruptible) {


    // TODO mlo:  Test all kind of exec and clean up unused params

    GeneralCommandLine line = new GeneralCommandLine();
    line.addParameters(command.subList(1, command.size()));
    line.setExePath(command.get(0));
    line.setWorkDirectory(dir.getPath());


    if (null != sdkData) {
      HaxeSdkUtilBase.patchEnvironment(line.getEnvironment(), sdkData);
    }


    boolean weAllocatedTimeLog = false;
    if (null == timeLog && log.isDebugEnabled()) {
      timeLog = HaxeDebugTimeLog.startNew("runProcess", HaxeDebugTimeLog.Since.StartAndPrevious);
      weAllocatedTimeLog = true;
    }

    try {
      if (null != timeLog) timeLog.stamp("Executing " + command);
      log.info("Starting external process: " + command);
      ProcessOutput output = ExecUtil.execAndGetOutput(line);
      if (stderr != null) stderr.addAll(output.getStderrLines());
      if (stdout != null) stdout.addAll(output.getStdoutLines());

      if (weAllocatedTimeLog) timeLog.print();

      return output.getExitCode();
    }
    catch (ExecutionException e) {
      String message = "I/O exception running command " + command.get(0);
      log.info(message);
      //throw new RuntimeException(e);
      return 255;
    }
  }
}