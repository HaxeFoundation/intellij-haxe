/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.display.client.HaxeVersionProbe;
import com.intellij.plugins.haxe.display.protocol.InitializeResult.SemVer;
import com.intellij.plugins.haxe.util.HaxeEnvironmentVariables;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.HaxeProcessUtil;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeRuntimeTool;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.util.HaxeEnvironmentVariables.*;

@CustomLog
public class HaxeSdkUtil extends HaxeSdkUtilBase {
  private static final String UNKNOWN_VERSION = "NA";

  @Nullable
  public static HaxeSdkData testHaxeSdk(String path) {
    final String exePath = getCompilerPathByFolderPath(path);

    if (exePath == null) {
      return null;
    }

    // -version is the one spelling every supported compiler knows
    List<String> command = List.of(exePath, "-version");

    try {
      VirtualFile dir = VirtualFileManager.getInstance().findFileByUrl(HaxeFileUtil.fixUrl(path));
      List<String> stdout = new ArrayList<>();
      List<String> stderr = new ArrayList<>();
      int exitCode;
      if (ApplicationManager.getApplication().isDispatchThread()) {
        exitCode = HaxeProcessUtil.runSynchronousProcessOnBackgroundThread(command, dir, stdout, stderr, null, true);
      }
      else {
        exitCode = HaxeProcessUtil.runProcess(command, dir, stdout, stderr, null);
      }
      if (exitCode != 0) {
        log.error("Haxe compiler exited with invalid exit code: " + exitCode);
        return null;
      }

      final HaxeSdkData haxeSdkData = new HaxeSdkData(path, versionOf(stdout, stderr));
      haxeSdkData.setHaxelibPath(getHaxelibPathByFolderPath(path));
      haxeSdkData.setNekoBinPath(suggestNekoBinPath());
      return haxeSdkData;
    }
    catch (Exception e) {
      log.info("Exception while executing the process:", e);
      return null;
    }
  }

  /** The version a {@code -version} answer names, {@code NA} when none: haxe 3 prints it to stderr, haxe 4 and 5 to stdout. */
  @NotNull
  private static String versionOf(List<String> stdout, List<String> stderr) {
    String answer = String.join("\n", stdout) + "\n" + String.join("\n", stderr);
    SemVer version = HaxeVersionProbe.parse(answer);
    return version != null ? version.toString() : UNKNOWN_VERSION;
  }

  public static void setupSdkPaths(@Nullable VirtualFile sdkRoot, SdkModificator modificator) {
    if (sdkRoot == null) {
      return;
    }
    for (VirtualFile stdRoot : findStdRoots(sdkRoot)) {
      modificator.addRoot(stdRoot, OrderRootType.SOURCES);
      modificator.addRoot(stdRoot, OrderRootType.CLASSES);
    }
  }

  /** The std folders the SDK's compiler searches that exist, in its order; a missing folder cannot be a root. */
  @NotNull
  private static Set<VirtualFile> findStdRoots(@NotNull VirtualFile sdkRoot) {
    Set<VirtualFile> stdRoots = new LinkedHashSet<>();
    for (String candidate : stdFolderCandidates(sdkRoot)) {
      VirtualFile stdRoot = findDirectory(candidate);
      if (stdRoot != null) stdRoots.add(stdRoot);
    }
    return stdRoots;
  }

  @NotNull
  private static Set<String> stdFolderCandidates(@NotNull VirtualFile sdkRoot) {
    String compilerPath = getCompilerPathByFolderPath(sdkRoot.getPath());
    if (compilerPath == null) return Set.of();

    Path compilerDirectory = HaxeStdClassPaths.compilerDirectory(Path.of(compilerPath));
    String stdPathValue = HaxeEnvironmentVariables.value(HAXE_STD_PATH);
    return HaxeStdClassPaths.candidates(stdPathValue, compilerDirectory, !SystemInfo.isWindows);
  }

  @Nullable
  private static VirtualFile findDirectory(@NotNull String path) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    return file != null && file.isDirectory() ? file : null;
  }

  /** The neko a new SDK starts with: the one Neko's own variables name, else the PATH's. */
  @Nullable
  private static String suggestNekoBinPath() {
    return HaxeRuntimeTool.NEKO.fromEnvironmentOrPath(HaxeRuntimeTool.PROCESS_ENVIRONMENT);
  }

  @Nullable
  public static String suggestHomePath() {
    //HAXEPATH is created by windows installer. Used by haxelib.
    String haxePath = HaxeEnvironmentVariables.value(HAXEPATH);
    if(haxePath != null) {
      return haxePath;
    }

    //Specifies the path to `std` directory in SDK. Used by Haxe compiler.
    List<String> stdPaths = HaxeEnvironmentVariables.pathList(HAXE_STD_PATH);
    if(!stdPaths.isEmpty()) {
      return new File(stdPaths.getFirst()).getParent();
    }

    //Try to locate SDK path relative to the compiler executable.
    String compilerPath = canonicalCompilerOnPath();
    if(compilerPath != null) {
      return new File(compilerPath).getParent();
    }

    return null;
  }

  /** The compiler a PATH lookup finds, canonical (symlinks resolved) so the SDK home derived from it is the real install; null when absent. */
  @Nullable
  private static String canonicalCompilerOnPath() {
    String found = HaxeToolPathResolver.pathDetectedExecutable("haxe");
    if (found == null) return null;
    try {
      return new File(found).getCanonicalPath();
    }
    catch (IOException e) {
      return null;
    }
  }
}
