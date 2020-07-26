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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.util.containers.ContainerUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HaxeProcessUtil {
  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  static { LOG.setLevel(Level.INFO); }  // Set to INFO when finished debugging.

  /** Records errors so that we only show them once. */
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
     * @return
     */
    public List<String> getErrorOutput() {
      return stderr;
    }

    public int getExitCode() {
      return exitCode;
    }
  }


  private HaxeProcessUtil() {} // No instantiation.

  /**
   * Run an interruptible process (without environment updates for the SDK).
   * Checks whether the process/thread has been cancelled (from within IDEA).
   *
   * @param command - Command to run
   * @param args - arguments to add to the command.
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
   * @param command - Command and parameters.
   * @param mixedOutput - include stderr in stdout output.
   * @param dir - directory to run the command in.
   * @param stdout - List to append output to.  Will not be cleared on start.
   * @param stderr - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   *
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
   * @param command - Command and parameters.
   * @param mixedOutput - include stderr in stdout output.
   * @param dir - directory to run the command in.
   * @param stdout - List to append output to.  Will not be cleared on start.
   * @param stderr - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   *
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
   * @param command - Command and parameters.
   * @param mixedOutput - include stderr in stdout output.
   * @param dir - directory to run the command in.
   * @param sdkData - sdk to use to set the command environment.
   * @param stdout - List to append output to.  Will not be cleared on start.
   * @param stderr - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   *
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
      ()->runProcess(command, mixedOutput, dir, sdkData, stdout, stderr, timeLog, interruptible),
      command.get(0),
      interruptible,
      null);
  }

  /**
   * Run a possibly interruptible process with the Haxe SDK environment (if given).
   * Checks whether the process/thread has been canceled (from within IDEA).
   *
   * @param command - Command and parameters.
   * @param mixedOutput - include stderr in stdout output.
   * @param dir - directory to run the command in.
   * @param sdkData - sdk to use to set the command environment.
   * @param stdout - List to append output to.  Will not be cleared on start.
   * @param stderr - List to append error output to. Will not be cleared on start.
   * @param interruptible - Check for user cancellation while running.
   *
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
    // Seems like a good idea to add this check, which was added to IDEA for 2019.2.
    // Unless our users run with "-Didea.is.internal=true", they
    // won't see any output from this check.
    checkEdtAndReadAction(command.toString());

    int ret = 255;
    Process process = null;
    boolean weAllocatedTimeLog = false;
    if (null == timeLog && LOG.isDebugEnabled()) {
      timeLog = HaxeDebugTimeLog.startNew("runProcess", HaxeDebugTimeLog.Since.StartAndPrevious);
      weAllocatedTimeLog = true;
    }

    try {
      LOG.info("Starting external process: " + command.toString());

      if (interruptible) {
        ProgressManager.checkCanceled();
      }

      File fdir = null != dir ? new File(dir.getPath()) : null;
      LOG.debug("Working directory is " + (null == dir ? "<null>" : dir.getPath()));
      ProcessBuilder builder = createProcessBuilder(command, fdir, sdkData);
      if (mixedOutput)
        builder.redirectErrorStream(true);

      if (null != timeLog) {
        timeLog.stamp("Executing " + command.toString());
      }
      process = builder.start();
      LOG.debug("External process has started.");
      InputStreamReader stdoutReader = new InputStreamReader(process.getInputStream());
      BufferedReader bufferedStdout = new BufferedReader(stdoutReader);

      InputStreamReader stderrReader = mixedOutput ? null : new InputStreamReader(process.getErrorStream());
      BufferedReader bufferedStderr = mixedOutput ? null : new BufferedReader(stderrReader);

      do {
        try {
          // Let our forked process get a little work done.
          Thread.sleep(2);
        } catch (InterruptedException e) {
          ; // Swallow it.
        }
        GatherOutput(mixedOutput, stdout, stderr, bufferedStdout, bufferedStderr, interruptible);
      }
      while (process.isAlive());
      ret = process.exitValue();

      // Pick up any uncollected output.
      GatherOutput(mixedOutput, stdout, stderr, bufferedStdout, bufferedStderr, interruptible);

      String message = "Process exited cleanly: Return value = " + Integer.toString(ret);
      LOG.debug(message);
      if (null != timeLog) {
        timeLog.stamp(message);
      }
    }
    catch (IOException e) {
      String message = "I/O exception running command " + command.get(0);
      LOG.info(message);
      ret = 255;
      if (null != timeLog) {
        timeLog.stamp(message);
      }
    } catch (ProcessCanceledException e) {
      String message = "Process canceled.";
      LOG.debug(message);
      ret = 255;
      if (null!= timeLog) {
        timeLog.stamp(message);
      }
    }finally {
      if (null != process && process.isAlive()) {
        process.destroyForcibly();
      }
      if (weAllocatedTimeLog) {
        timeLog.print();
      }
    }
    return ret;
  }


  private static void GatherOutput(boolean mixedOutput,
                                   List<String> stdout,
                                   List<String> stderr,
                                   BufferedReader bufferedStdout,
                                   BufferedReader bufferedStderr,
                                   boolean interruptible) throws IOException {
    while (bufferedStdout.ready()) {
      if (null != stdout) {
        stdout.add(bufferedStdout.readLine());
      }
      else {
        // Goes to the bit bucket.
        bufferedStdout.readLine();
      }
    }
    if (!mixedOutput) {
      while (bufferedStderr.ready()) {
        if (null != stderr) {
          stderr.add(bufferedStderr.readLine());
        }
        else {
          // bit bucket.
          bufferedStderr.readLine();
        }
      }
    }

    if (interruptible) {
      ProgressManager.checkCanceled();
    }
  }


  @NotNull
  public static ProcessBuilder createProcessBuilder(List<String> commandLine,
                                                    @Nullable File workingDirectory,
                                                    @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    final ProcessBuilder processBuilder = new ProcessBuilder(commandLine);

    if(workingDirectory != null) {
      processBuilder.directory(workingDirectory);
    }

    if (null != haxeSdkData) {
      HaxeSdkUtilBase.patchEnvironment(processBuilder.environment(), haxeSdkData);
    }

    return processBuilder;
  }


  // Modified version of OSProcessHandler.checkEdtAndReadAction().
  /**
   * Checks if we are going to wait for {@code processHandler} to finish on EDT or under ReadAction. Logs error if we do so.
   * <br/><br/>
   * HOW-TO fix an error from this method:
   * <ul>
   * <li>You are on the pooled thread under {@link com.intellij.openapi.application.ReadAction ReadAction}:
   * <ul>
   *     <li>Synchronous (you need to return execution result or derived information to the caller) - get rid the ReadAction or synchronicity.
   *    *     Move execution part out of the code executed under ReadAction, or make your execution asynchronous - execute on
   *    *     {@link Task.Backgroundable other thread} and invoke a callback.</li>
   *     <li>Non-synchronous (you don't need to return something) - execute on other thread. E.g. using {@link Task.Backgroundable}</li>
   * </ul>
   * </li>
   *
   * <li>You are on EDT:
   * <ul>
   *
   * <li>Outside of {@link com.intellij.openapi.application.WriteAction WriteAction}:
   *   <ul>
   *     <li>Synchronous (you need to return execution result or derived information to the caller) - execute under
   *       {@link ProgressManager#runProcessWithProgressSynchronously(java.lang.Runnable, java.lang.String, boolean, com.intellij.openapi.project.Project) modal progress}.</li>
   *     <li>Non-synchronous (you don't need to return something) - execute on the pooled thread. E.g. using {@link Task.Backgroundable}</li>
   *   </ul>
   * </li>
   *
   * <li>Under {@link com.intellij.openapi.application.WriteAction WriteAction}
   *   <ul>
   *     <li>Synchronous (you need to return execution result or derived information to the caller) - get rid the WriteAction or synchronicity.
   *       Move execution part out of the code executed under WriteAction, or make your execution asynchronous - execute on
   *      {@link Task.Backgroundable other thread} and invoke a callback.</li>
   *     <li>Non-synchronous (you don't need to return something) - execute on the pooled thread. E.g. using {@link Task.Backgroundable}</li>
   *   </ul>
   * </li>
   * </ul></li></ul>
   *
   * @apiNote works only in internal mode with UI. Reports once per running session per stacktrace per cause.
   *
   * Why this matters:
   *   Running on the EDT -- or in a WriteAction (which, by definition is on the EDT -- see JetBrains' threading
   *                         model docs -- means that the process blocks the UI and *any other thread*
   *                         waiting to start a WriteAction or ReadAction.
   *   Running in a ReadAction  -- While multiple ReadActions can run simultaneously, any running ReadAction
   *                               still blocks waiting WriteActions.
   */
  public static void checkEdtAndReadAction(@NotNull String processName) {
    Application application = ApplicationManager.getApplication();
    if (application == null || !application.isInternal() || application.isHeadlessEnvironment()) {
      return;
    }
    String message = null;
    if (application.isDispatchThread()) {
      message = "Synchronous execution on EDT: ";
    }
    else if (application.isReadAccessAllowed()) {
      message = "Synchronous execution under ReadAction: ";
    }
    String callers = HaxeDebugUtil.printCallers(7);
    if (message != null && REPORTED_EXECUTIONS.add(callers)) {
      LOG.error(message +'\n' + callers + "...while running process: " + processName);
    }
  }
}
