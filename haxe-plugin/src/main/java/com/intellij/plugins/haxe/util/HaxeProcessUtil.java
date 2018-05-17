/*
 * Copyright 2017 Eric Bishton
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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import org.apache.log4j.Level;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class HaxeProcessUtil {
  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  static { LOG.setLevel(Level.DEBUG); }  // Remove when finished debugging.

  private HaxeProcessUtil() {} // No instantiation.


  /**
   * Run a possibly interruptible process with the Haxe SDK environment.  Checks
   * whether the process/thread has been canceled (from within IDEA).
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
    int ret = 255;
    Process process = null;
    boolean weAllocatedTimeLog = false;
    if (null == timeLog && LOG.isDebugEnabled()) {
      timeLog = HaxeDebugTimeLog.startNew("runProcess", HaxeDebugTimeLog.Since.StartAndPrevious);
      weAllocatedTimeLog = true;
    }

    try {
      LOG.debug("Starting runProcess for " + command.toString());

      if (interruptible) {
        ProgressManager.checkCanceled();
      }

      File fdir = null != dir ? new File(dir.getPath()) : null;
      LOG.debug("dir is " + (null == dir ? "<null>" : dir.getPath()));
      ProcessBuilder builder = HaxeSdkUtilBase.createProcessBuilder(command, fdir, sdkData);
      if (mixedOutput)
        builder.redirectErrorStream(true);

      if (null != timeLog) {
        timeLog.stamp("Executing " + command.toString());
      }
      process = builder.start();
      LOG.debug("Compiler process has started.");
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
      while (processIsAlive(process));
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
      if (null != process && processIsAlive(process)) {
        process.destroy();
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

  /**
   * Check whether the process is still alive the hard way,
   * Java8 gives us process.isAlive(), but Java6 doesn't.
   * @param process
   */
  private static boolean processIsAlive(Process process) {
    try {
      int exitStatus = process.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      ; // swallow it.  The thread hasn't exited yet.
    }
    return true;
  }


}
