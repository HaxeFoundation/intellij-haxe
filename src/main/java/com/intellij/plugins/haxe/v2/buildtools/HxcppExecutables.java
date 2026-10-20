package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// hxcpp's rules for what a direct (hxml) C++ build links and how it names the
/// result, as its toolchain states them. The linker follows the link and
/// platform defines, the last matching rule winning: an executable by default,
/// a static library with `static_link`, a shared one on `android`, an
/// executable again with `exe_link`, a shared library with `dll_import` or
/// `dll_link`. The output is `HAXE_OUTPUT_FILE`, by default
/// `HAXE_OUTPUT_PART` (the main class's simple name, `output` without one)
/// plus `-debug` (or `DEBUGSUFFIX`) for a debug build, plus the platform's
/// executable extension.
public final class HxcppExecutables {

  /** A run configuration's executable field holding this resolves to the executable its build step writes; see {@link #executable}. */
  public static final String EXECUTABLE_VARIABLE = "${executable}";

  /** Toolchains building for another platform: whatever they link does not run here. */
  private static final Set<String> CROSS_TOOLCHAIN_DEFINES = Set.of(
    "android", "iphoneos", "iphonesim", "appletvos", "appletvsim", "watchos", "watchsimulator",
    "emscripten", "tizen", "blackberry", "webos", "gph", "gcw0", "winrt");
  private static final Set<String> SHARED_LIBRARY_DEFINES = Set.of("dll_import", "dll_link");
  private static final String DEFAULT_OUTPUT_NAME = "output";
  private static final String DEFAULT_DEBUG_SUFFIX = "-debug";

  private HxcppExecutables() {
  }

  /** Whether a build with these defines links an executable this machine runs. */
  public static boolean buildsHostExecutable(@NotNull List<HaxeDefine> defines) {
    Set<String> names = byName(defines).keySet();
    if (names.stream().anyMatch(CROSS_TOOLCHAIN_DEFINES::contains)) return false;
    if (names.stream().anyMatch(SHARED_LIBRARY_DEFINES::contains)) return false;
    return names.contains("exe_link") || !names.contains("static_link");
  }

  /** The executable's file name for a build declaring {@code mainClass} (null: none) and {@code defines}. */
  @NotNull
  public static String fileName(@Nullable String mainClass, @NotNull List<HaxeDefine> defines) {
    Map<String, String> byName = byName(defines);
    String outputFile = byName.get("HAXE_OUTPUT_FILE");
    if (outputFile == null) {
      String outputPart = byName.getOrDefault("HAXE_OUTPUT_PART", simpleName(mainClass));
      outputFile = outputPart + debugSuffix(byName);
    }
    return HaxeSdkUtilBase.getExecutableName(outputFile);
  }

  /** The defines a compile's command line adds: {@code -debug}, and {@code -D name[=value]} or its {@code --define} spelling. */
  @NotNull
  public static List<HaxeDefine> commandLineDefines(@NotNull List<String> arguments) {
    List<HaxeDefine> defines = new ArrayList<>();
    for (int i = 0; i < arguments.size(); i++) {
      String argument = arguments.get(i);
      if (HxmlFileParser.DEBUG_FLAGS.contains(argument)) {
        defines.add(new HaxeDefine(HxmlFileParser.DEBUG_DEFINE_NAME, null));
      }
      else if (HxmlFileParser.DEFINE_FLAGS.contains(argument) && i + 1 < arguments.size()) {
        defines.add(define(arguments.get(++i)));
      }
    }
    return defines;
  }

  /**
   * The executable a direct C++ build of the hxml's selected section writes,
   * when it links one this machine runs: the section's {@code -cpp} folder
   * plus {@link #fileName}, the debug flags and defines in
   * {@code extraArguments} (what a launch adds to the compile) counted in.
   * Null for any other build.
   */
  @Nullable
  public static Path executable(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull List<String> extraArguments) {
    if (buildFile.type() != HaxeBuildFileType.HXML) return null;
    String content = HaxeReadActions.compute(() -> HaxeBuildSections.selectedSectionContent(project, buildFile));
    if (content == null) return null;
    HaxeBuildFileInfo info = HxmlFileParser.parse(content);
    if (info.target() != HaxeTarget.CPP || info.targetOutput() == null) return null;

    List<HaxeDefine> defines = new ArrayList<>(info.defines());
    defines.addAll(commandLineDefines(extraArguments));
    if (!buildsHostExecutable(defines)) return null;
    String fileName = fileName(HxmlFileParser.mainClass(content), defines);
    return HaxeBuildWorkDirectories.resolve(project, buildFile.file(), info.targetOutput()).resolve(fileName);
  }

  /** Defines by the name hxcpp sees, a later one winning; a bare define's value is empty. */
  @NotNull
  private static Map<String, String> byName(@NotNull List<HaxeDefine> defines) {
    Map<String, String> byName = new HashMap<>();
    for (HaxeDefine define : defines) {
      byName.put(HaxeDefine.compilerName(define.name()), define.value() != null ? define.value() : "");
    }
    return byName;
  }

  @NotNull
  private static HaxeDefine define(@NotNull String nameAndValue) {
    int equals = nameAndValue.indexOf('=');
    return equals < 0
           ? new HaxeDefine(nameAndValue, null)
           : new HaxeDefine(nameAndValue.substring(0, equals), nameAndValue.substring(equals + 1));
  }

  @NotNull
  private static String simpleName(@Nullable String mainClass) {
    if (mainClass == null) return DEFAULT_OUTPUT_NAME;
    return mainClass.substring(mainClass.lastIndexOf('.') + 1);
  }

  @NotNull
  private static String debugSuffix(@NotNull Map<String, String> defines) {
    if (!defines.containsKey("debug")) return "";
    return defines.getOrDefault("DEBUGSUFFIX", DEFAULT_DEBUG_SUFFIX);
  }
}
