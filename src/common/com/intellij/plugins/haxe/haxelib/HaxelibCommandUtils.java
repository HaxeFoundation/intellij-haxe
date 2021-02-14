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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

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

    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add(getHaxelibPath(sdk));
    for (String arg : args) {
      commandLineArguments.add(arg);
    }

    HaxeSdkAdditionalDataBase sdkData = HaxeSdkUtilBase.getSdkData(sdk);
    String haxelibPath = null != sdkData ? sdkData.getHaxelibPath() : HaxeSdkUtil.suggestHomePath();
    if (null == haxelibPath) {
      LOG.warn("Could not find 'haxelib' executable to run using " + sdk.getName());
      return Collections.EMPTY_LIST;
    }

    File haxelibCmd = new File(haxelibPath);
    VirtualFile dir = haxelibCmd.isFile() ? LocalFileSystem.getInstance().findFileByPath(haxelibCmd.getParent()) : null;

    List<String> stdout = new ArrayList<String>();
    int exitvalue = HaxeProcessUtil.runProcess(commandLineArguments, true, dir, sdkData,
                                      stdout, null, null, false);

    if (0 != exitvalue) {
      // At least throw a warning into idea.log so we have some clue as to what is going on.
      LOG.warn("Error " + Integer.toString(exitvalue) + " returned from " + commandLineArguments.toString());
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
   * Run a shell command, capturing its standard output.
   *
   * @param commandLineArguments a command and its arguments, as a list of strings.
   * @param dir directory in which to run the command.
   * @return the output of the command, as a list of strings, one line per string.
   */
  @NotNull
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments, @Nullable File dir, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    List<String> strings = new ArrayList<String>();

    try {
      ProcessBuilder builder = HaxeSdkUtilBase.createProcessBuilder(commandLineArguments, dir, haxeSdkData);
      builder.redirectErrorStream(true);
      if (dir != null) {
        builder = builder.directory(dir);
      }
      Process process = builder.start();
      InputStreamReader reader = new InputStreamReader(process.getInputStream());
      Scanner scanner = new Scanner(reader);
      process.waitFor(100, TimeUnit.MILLISECONDS);

      if(reader.ready()) {
        while (scanner.hasNextLine()) {
          String nextLine = scanner.nextLine();
          strings.add(nextLine);
        }
      }

      if(process.isAlive()) {
        if(!process.waitFor(10, TimeUnit.MILLISECONDS)) {
          process.destroy();
        }
      }


    }
    catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    return strings;
  }


  //private static Logger log = Logger.getInstance(HaxelibCommandUtils.class);

  public static List<String> getProcessStderr(ArrayList<String> commandLineArguments, File dir, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    List<String> strings = new ArrayList<String>();

    try {
      ProcessBuilder builder = HaxeSdkUtilBase.createProcessBuilder(commandLineArguments, dir, haxeSdkData);
      Process process = builder.start();
      InputStreamReader reader = new InputStreamReader(process.getErrorStream());
      Scanner scanner = new Scanner(reader);


      if(reader.ready()) {
        while (scanner.hasNextLine()) {
          String nextLine = scanner.nextLine();
          strings.add(nextLine);
        }
      }

      if(process.isAlive()) {
        if(!process.waitFor(10, TimeUnit.MILLISECONDS)) {
          process.destroy();
        }
      }


    }
    catch (IOException | InterruptedException e) {
      e.printStackTrace();
      //log.error(StringUtil.getMessage(e));
    }

    return strings;
  }


  /**
   * Run a shell command in the (IDEA's) current directory, capturing its standard output.
   *
   * @param commandLineArguments a command and its arguments, as a list of strings.
   * @return the output of the command, as a list of strings, one line per string.
   */
  @NotNull
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    return getProcessStdout(commandLineArguments, null, haxeSdkData);
  }

  public static List<String> getProcessStderr(ArrayList<String> commandLineArguments, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    return getProcessStderr(commandLineArguments, null, haxeSdkData);
  }

}
