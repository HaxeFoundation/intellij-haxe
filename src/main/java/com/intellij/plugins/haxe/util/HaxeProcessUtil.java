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
import lombok.CustomLog;

import java.util.HashMap;
import java.util.List;

@CustomLog
public class HaxeProcessUtil {
  static {
    log.setLevel(LogLevel.INFO);
  }  // Set to INFO when finished debugging.

  private HaxeProcessUtil() {
  } // No instantiation.

  /**
   * Run a possibly interruptible process (without environment updates for the SDK)
   * behind a modal progress, which the user can cancel when the process is interruptible.
   *
   * @param command       - Command and parameters.
   * @param dir           - directory to run the command in.
   * @param stdout        - List to append output to.  Will not be cleared on start.
   * @param stderr        - List to append error output to. Will not be cleared on start.
   * @param interruptible - Let the user cancel the progress.
   * @return The exit status of the command.
   */
  public static int runSynchronousProcessOnBackgroundThread(List<String> command,
                                                            VirtualFile dir,
    /*modifies*/ List<String> stdout,
    /*modifies*/ List<String> stderr,
                                                            HaxeDebugTimeLog timeLog,
                                                            boolean interruptible) {
    return runSynchronousProcessOnBackgroundThread(command, dir, null, stdout, stderr, timeLog, interruptible);
  }

  /**
   * Run a process (without environment updates for the SDK).
   *
   * @param command - Command and parameters.
   * @param dir     - directory to run the command in.
   * @param stdout  - List to append output to.  Will not be cleared on start.
   * @param stderr  - List to append error output to. Will not be cleared on start.
   * @return The exit status of the command.
   */
  public static int runProcess(List<String> command,
                               VirtualFile dir,
    /*modifies*/ List<String> stdout,
    /*modifies*/ List<String> stderr,
                               HaxeDebugTimeLog timeLog) {
    return runProcess(command, dir, null, stdout, stderr, timeLog);
  }

  /**
   * Run a possibly interruptible process with the Haxe SDK environment (if given)
   * behind a modal progress, which the user can cancel when the process is interruptible.
   *
   * @param command       - Command and parameters.
   * @param dir           - directory to run the command in.
   * @param sdkData       - sdk to use to set the command environment.
   * @param stdout        - List to append output to.  Will not be cleared on start.
   * @param stderr        - List to append error output to. Will not be cleared on start.
   * @param interruptible - Let the user cancel the progress.
   * @return The exit status of the command.
   */
  public static int runSynchronousProcessOnBackgroundThread(List<String> command,
                                                            VirtualFile dir,
                                                            HaxeSdkAdditionalDataBase sdkData,
    /*modifies*/ List<String> stdout,
    /*modifies*/ List<String> stderr,
                                                            HaxeDebugTimeLog timeLog,
                                                            boolean interruptible) {

    if (null == command || command.isEmpty()) return -1;

    return ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> runProcess(command, dir, sdkData, stdout, stderr, timeLog),
      command.get(0),
      interruptible,
      null);
  }

  /**
   * Run a process with the Haxe SDK environment (if given).
   *
   * @param command - Command and parameters.
   * @param dir     - directory to run the command in.
   * @param sdkData - sdk to use to set the command environment.
   * @param stdout  - List to append output to.  Will not be cleared on start.
   * @param stderr  - List to append error output to. Will not be cleared on start.
   * @return The exit status of the command.
   */
  public static int runProcess(List<String> command,
                               VirtualFile dir,
                               HaxeSdkAdditionalDataBase sdkData,
                  /*modifies*/ List<String> stdout,
                  /*modifies*/ List<String> stderr,
                               HaxeDebugTimeLog timeLog) {

    GeneralCommandLine line = new GeneralCommandLine();
    line.addParameters(command.subList(1, command.size()));
    line.setExePath(command.get(0));
    line.setWorkDirectory(dir.getPath());


    if (null != sdkData) {
      // patched over the effective environment: the SDK folders go in front of the real PATH instead of replacing it
      line.withEnvironment(HaxeSdkUtilBase.patchEnvironment(new HashMap<>(line.getEffectiveEnvironment()), sdkData));
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
