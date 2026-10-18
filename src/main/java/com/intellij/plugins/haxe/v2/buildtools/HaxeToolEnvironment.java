package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The environment a tool process runs with: the SDK's folders (compiler,
 * haxelib, neko) in front of the effective PATH, which is extended, never
 * replaced. The compiler resolves {@code -lib} through haxelib and the lime
 * and hxcpp tools start neko and haxelib by bare name, so a container whose
 * SDK is not on PATH would otherwise pair that SDK's compiler with whichever
 * haxelib and neko PATH lists first.
 */
public final class HaxeToolEnvironment {

  private static final String PATH_VARIABLE = "PATH";

  private HaxeToolEnvironment() {
  }

  /** The command line with the SDK's folders in front of its effective PATH; unchanged without an SDK or without folders. */
  @NotNull
  public static GeneralCommandLine withSdkOnPath(@NotNull GeneralCommandLine commandLine, @Nullable Sdk sdk) {
    List<String> folders = sdkFolders(sdk);
    if (folders.isEmpty()) return commandLine;
    Map<String, String> effective = commandLine.getEffectiveEnvironment();
    String path = effective.getOrDefault(PATH_VARIABLE, "");
    String prefix = String.join(File.pathSeparator, folders);
    return commandLine.withEnvironment(PATH_VARIABLE, path.isEmpty() ? prefix : prefix + File.pathSeparator + path);
  }

  /** The folders holding the SDK's compiler, its haxelib and its neko, each once and only when the executable exists. */
  @NotNull
  static List<String> sdkFolders(@Nullable Sdk sdk) {
    if (sdk == null || sdk.getHomePath() == null) return List.of();
    Set<String> folders = new LinkedHashSet<>();
    addFolderOf(folders, HaxeSdkUtilBase.getCompilerPathByFolderPath(sdk.getHomePath()));
    addFolderOf(folders, HaxeRuntimeTool.HAXELIB.fromSdk(sdk));
    addFolderOf(folders, HaxeRuntimeTool.NEKO.fromSdk(sdk));
    return List.copyOf(folders);
  }

  private static void addFolderOf(@NotNull Set<String> folders, @Nullable String executablePath) {
    if (executablePath == null) return;
    Path parent = Path.of(executablePath).getParent();
    if (parent != null) folders.add(parent.toString());
  }
}
