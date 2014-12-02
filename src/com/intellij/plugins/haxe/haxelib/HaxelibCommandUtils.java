/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Utilities to run the haxelib command and capture its output.
 */
public class HaxelibCommandUtils {

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
    // TODO: Wrap this invocation with a timer??

    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add(getHaxelibPath(sdk));
    for (String arg : args) {
      commandLineArguments.add(arg);
    }

    List<String> strings = getProcessStdout(commandLineArguments);
    return strings;
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
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments, @Nullable File dir) {
    List<String> strings = new ArrayList<String>();

    try {
      ProcessBuilder builder = new ProcessBuilder(commandLineArguments);
      if (dir != null) {
        builder = builder.directory(dir);
      }
      Process process = builder.start();
      InputStreamReader reader = new InputStreamReader(process.getInputStream());
      Scanner scanner = new Scanner(reader);
      process.waitFor();

      while (scanner.hasNextLine()) {
        String nextLine = scanner.nextLine();
        strings.add(nextLine);
      }

      /*
      try {
        Thread.sleep(250);
        try {
          process.exitValue();
        }
        catch (IllegalThreadStateException e) {
          process.destroy();
        }
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      */
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    return strings;
  }

  public static List<String> getProcessStderr(ArrayList<String> commandLineArguments, File dir) {
    List<String> strings = new ArrayList<String>();

    try {
      ProcessBuilder builder = new ProcessBuilder(commandLineArguments);
      if (dir != null) {
        builder = builder.directory(dir);
      }
      Process process = builder.start();
      InputStreamReader reader = new InputStreamReader(process.getErrorStream());
      Scanner scanner = new Scanner(reader);
      process.waitFor();

      while (scanner.hasNextLine()) {
        String nextLine = scanner.nextLine();
        strings.add(nextLine);
      }
      /*
      try {
        Thread.sleep(250);
        try {
          process.exitValue();
        }
        catch (IllegalThreadStateException e) {
          process.destroy();
        }
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      */
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
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
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments) {
    return getProcessStdout(commandLineArguments, null);
  }

  public static List<String> getProcessStderr(ArrayList<String> commandLineArguments) {
    return getProcessStderr(commandLineArguments, null);
  }

}
