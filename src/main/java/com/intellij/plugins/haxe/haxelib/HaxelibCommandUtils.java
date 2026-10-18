/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.haxelib;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.util.HaxeProcessUtil;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Utilities to run the haxelib command and capture its output.
 */
@CustomLog
public class HaxelibCommandUtils {

  /** Long enough for a tool run such as {@code lime display}; the compiler's help probes finish in well under a second. */
  private static final int PROBE_TIMEOUT_MS = 60_000;

  /**
   * Find the path to the 'haxelib' executable, using the module paths.
   *
   * @param module to look up haxelib for.
   * @return the configured haxelib for the module (or project, if the module
   * uses the project SDK); "haxelib" if not specified.
   */
  @NotNull
  public static String getHaxelibPath(@NotNull Module module) {

    // ModuleRootManager.getInstance returns either a ModuleJdkOrderEntryImpl
    // or an InheritedJdgOrderEntryImpl, as appropriate.
    Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
    return sdk == null ? "haxelib" : getHaxelibPath(sdk);
  }

  /**
   * Find the path to the 'haxelib' executable, using a specific SDK.
   *
   * @param sdk - SDK to look up haxelib for.
   * @return the configured haxelib for the SDK; "haxelib" if not specified.
   */
  @NotNull
  public static String getHaxelibPath(@NotNull Sdk sdk) {

    String haxelibPath = "haxelib";
    if (sdk != null) {
      SdkAdditionalData data = sdk.getSdkAdditionalData();

      if (data instanceof HaxeSdkData) {
        HaxeSdkData sdkData = (HaxeSdkData)data;
        String path = sdkData.getHaxelibPath();
        if (!path.isEmpty()) {
          haxelibPath = path;
        }
      }
    }

    return haxelibPath;
  }

  /**
   * Issue a 'haxelib' command to the OS, capturing its output.
   *
   * @param args arguments to be provided to the haxelib command.
   * @return a set of Strings, possibly empty, one per line of command output.
   */

  @NotNull
  public static List<String> issueHaxelibCommand(@NotNull Sdk sdk, @Nullable VirtualFile workDir, String... args) {
    Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      List<String> result = new ArrayList<>();
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> result.addAll(_issueHaxelibCommand(sdk,workDir, args)),
                                                                        "Haxelib Command", false, null);
      return result;
    }
    else if (application.isReadAccessAllowed()) {
      try {
        // TODO: this is a hacky way to get around the `checkEdtAndReadAction` check,
        //  while we move the logic to another thread we block the current one using get(), preferred
        //  method would be some kind of callback
        return application.executeOnPooledThread(()-> _issueHaxelibCommand(sdk, workDir, args)).get();
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      return _issueHaxelibCommand(sdk, workDir, args);
    }

  }

  @NotNull
  private static List<String> _issueHaxelibCommand(@NotNull Sdk sdk, @Nullable VirtualFile workDir, String... args) {

    // TODO: Wrap the process with a timer?

    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add(getHaxelibPath(sdk));
    commandLineArguments.addAll(Arrays.asList(args));

    HaxeSdkAdditionalDataBase sdkData = HaxeSdkUtilBase.getSdkData(sdk);
    String haxelibPath = null != sdkData ? sdkData.getHaxelibPath() : HaxeSdkUtil.suggestHomePath();
    if (null == haxelibPath) {
      log.warn("Could not find 'haxelib' executable to run using " + sdk.getName());
      return Collections.EMPTY_LIST;
    }

    // TODO mlo: try to clean up code so it only uses either dir or file
    File haxelibCmd = new File(haxelibPath);
    if(workDir == null) {
      workDir = haxelibCmd.isFile()
                        ? LocalFileSystem.getInstance().findFileByPath(haxelibCmd.getParent())
                        : LocalFileSystem.getInstance().findFileByPath(haxelibPath);
    }
    if (workDir == null) {
      log.error("unable to execute haxelib command, haxelib path is null");
      return List.of();
    }
    List<String> stdout = new ArrayList<String>();
    int exitvalue = HaxeProcessUtil.runProcess(commandLineArguments, true, workDir, sdkData,
                                               stdout, null, null, false);

    if (0 != exitvalue) {
      // At least throw a warning into idea.log so we have some clue as to what is going on.
      log.warn("Error " + Integer.toString(exitvalue) + " returned from " + commandLineArguments.toString());
    }

    return stdout;
  }


  /*
  public static void startProcess(ArrayList<String> commandLineArguments, @Nullable File dir) {
    ProcessBuilder builder = new ProcessBuilder(commandLineArguments);
    if (dir != null) {
      builder = builder.directory(dir);
    }
    try {
      Process process = builder.start();
      BaseOSProcessHandler handler = new BaseOSProcessHandler(process, null, null);
      handler.addProcessListener(new CapturingProcessAdapter()
      {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          super.onTextAvailable(event, outputType);
          String text = event.getText();
          String text2 = event.getText();
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          super.processTerminated(event);
        }
      });
    }
    catch (IOException e) {
      e.printStackTrace();
    }

  }
  */


  /**
   * Runs a tool and returns its complete output, stdout and stderr merged,
   * one line per entry. A tool that outlives the timeout is killed and the
   * output it produced so far is returned.
   */
  @NotNull
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments,
                                              @Nullable File dir,
                                              @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    GeneralCommandLine commandLine = new GeneralCommandLine(commandLineArguments)
      .withRedirectErrorStream(true)
      .withCharset(StandardCharsets.UTF_8);
    if (dir != null) {
      commandLine.withWorkingDirectory(dir.toPath());
    }
    if (haxeSdkData != null) {
      // patched over the effective environment: the SDK folders go in front of the real PATH instead of replacing it
      Map<String, String> environment = new HashMap<>(commandLine.getEffectiveEnvironment());
      commandLine.withEnvironment(HaxeSdkUtilBase.patchEnvironment(environment, haxeSdkData));
    }
    try {
      return new CapturingProcessHandler(commandLine).runProcess(PROBE_TIMEOUT_MS).getStdoutLines();
    }
    catch (com.intellij.execution.ExecutionException e) {
      log.info("cannot run " + commandLineArguments.getFirst() + ": " + e.getMessage());
      return List.of();
    }
  }

  /** {@link #getProcessStdout(ArrayList, File, HaxeSdkAdditionalDataBase)} in the IDE's current directory. */
  @NotNull
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments,
                                              @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    return getProcessStdout(commandLineArguments, null, haxeSdkData);
  }
}
