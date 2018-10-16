/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
 * Copyright 2018 Ilya Malanin
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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeProcessUtil;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Utilities to run the haxelib command and capture its output.
 */
public class HaxelibCommandUtils {
  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();

  /**
   * Find the path to the 'haxelib' executable, using the module paths.
   *
   * @param module to look up haxelib for.
   * @return the configured haxelib for the module (or project, if the module
   *         uses the project SDK); "haxelib" if not specified.
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
  public static List<String> issueHaxelibCommand(@NotNull Sdk sdk, String ... args) {

    // TODO: Wrap the process with a timer?

    ArrayList<String> commandLineArguments = new ArrayList<>();
    commandLineArguments.add(getHaxelibPath(sdk));
    commandLineArguments.addAll(Arrays.asList(args));

    HaxeSdkAdditionalDataBase sdkData = HaxeSdkUtilBase.getSdkData(sdk);
    String haxelibPath = null != sdkData ? sdkData.getHaxelibPath() : HaxeSdkUtil.suggestHomePath();
    if (null == haxelibPath) {
      LOG.warn("Could not find 'haxelib' executable to run using " + sdk.getName());
      return Collections.emptyList();
    }

    File haxelibCmd = new File(haxelibPath);
    VirtualFile dir = haxelibCmd.isFile() ? LocalFileSystem.getInstance().findFileByPath(haxelibCmd.getParent()) : null;

    List<String> stdout = new ArrayList<>();
    int exitvalue = HaxeProcessUtil.runProcess(commandLineArguments, true, dir, sdkData,
                                      stdout, null, null, false);

    if (0 != exitvalue) {
      // At least throw a warning into idea.log so we have some clue as to what is going on.
      LOG.warn("Error " + Integer.toString(exitvalue) + " returned from " + commandLineArguments.toString());
    }

    return stdout;
  }

  /**
   * Run a shell command, capturing its standard output.
   *
   * @param commandLineArguments a command and its arguments, as a list of strings.
   * @param dir directory in which to run the command.
   * @return the output of the command, as a list of strings, one line per string.
   */
  @NotNull
  public static List<String> getProcessOutput(@NotNull List<String> commandLineArguments, @Nullable File dir, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    List<String> lines = new ArrayList<>();

    final GeneralCommandLine command = new GeneralCommandLine(commandLineArguments);
    command.setWorkDirectory(dir);

    try {
      final ProcessOutput output = new CapturingProcessHandler(
        command.createProcess(),
        Charset.defaultCharset(),
        command.getCommandLineString()).runProcess();

      lines.addAll(output.getStdoutLines());
      lines.addAll(output.getStderrLines());
    }
    catch (ExecutionException e) {
      e.printStackTrace();
    }

    return lines;
  }

  /**
   * Run a shell command in the (IDEA's) current directory, capturing its standard output.
   *
   * @param commandLineArguments a command and its arguments, as a list of strings.
   * @return the output of the command, as a list of strings, one line per string.
   */
  @NotNull
  public static List<String> getProcessOutput(@NotNull List<String> commandLineArguments, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    return getProcessOutput(commandLineArguments, null, haxeSdkData);
  }
}
