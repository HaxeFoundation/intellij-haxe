package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetOptions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Facts about NME projects (project.nmml): the nme tool's commands. The
 * lime-family counterpart is {@link LimeProjects}.
 */
public final class NmeProjects {

  /**
   * The nme tool's core project actions (it accepts many more - installer,
   * trace, update... - which stay available as custom actions). "test" is
   * update + build + run, mirroring lime's.
   */
  public static final List<String> DEFAULT_ACTIONS = List.of("test", "run", "build", "clean");

  /** The default action an nmml file compiles with. */
  public static final String BUILD_ACTION = "build";

  private NmeProjects() {
  }

  /** The file's currently selected NME target as the tool's target argument (e.g. "html5", "windows"). */
  @NotNull
  public static String selectedTargetFlag(@NotNull Project project, @NotNull VirtualFile file) {
    String targetId = HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(file);
    return HaxeTargetOptions.targetFlagFor(HaxeBuildFileType.NMML, targetId);
  }

  /** The selected target's full flag list — the target word plus configured extras (e.g. "-64"). */
  @NotNull
  public static List<String> selectedTargetFlags(@NotNull Project project, @NotNull VirtualFile file) {
    String targetId = HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(file);
    return HaxeTargetOptions.targetFlagsFor(HaxeBuildFileType.NMML, targetId);
  }

  /// Whether the command is a `haxelib run nme …` invocation. These can compile through the
  /// server, but only as a single `"--connect <port>"` token - the tool's
  /// two-token forwarding is broken (see
  /// [HaxeCompileCommands#connectIfEnabled]).
  public static boolean isToolCommand(@NotNull List<String> command) {
    return command.size() >= 3 && "run".equals(command.get(1)) && "nme".equals(command.get(2));
  }

  /**
   * One of the tool's actions as a full command line, using the file's selected
   * target (all its flags). The tool recognizes the target token anywhere in
   * the argument list and treats the remaining first word as the project file,
   * so the action-file-target order matches the lime commands.
   */
  @NotNull
  public static List<String> actionCommand(@NotNull Project project,
                                           @Nullable String environmentSdk,
                                           @NotNull VirtualFile file,
                                           @NotNull String actionName) {
    String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk);
    List<String> command = new ArrayList<>(List.of(haxelib, "run", "nme", actionName, file.getName()));
    command.addAll(selectedTargetFlags(project, file));
    return command;
  }

  /** The haxe target compiled and the artifact packaged for a selected nme target. */
  public record TargetArtifact(@NotNull HaxeTarget target, @NotNull String relativeOutput) {
  }

  /// The raw nmml info completed with the selected target's haxe target and
  /// artifact path - the xml declares neither. Unchanged when the target's
  /// artifact is not mapped or the nmml has no `<app file>`.
  /// Call in a read action.
  @NotNull
  public static HaxeBuildFileInfo withTargetArtifact(@NotNull Project project,
                                                     @NotNull VirtualFile file,
                                                     @NotNull HaxeBuildFileInfo raw) {
    String content = HaxeBuildFileInspector.loadText(file);
    String appFile = content == null ? null : ProjectXmlParser.parseAppFile(content);
    if (appFile == null) return raw;

    // the nmml's <app path> overrides the tool's default "bin" output root
    String appPath = ProjectXmlParser.parseAppPath(content);
    String outputRoot = appPath != null ? appPath : "bin";
    TargetArtifact artifact = targetArtifact(selectedTargetFlag(project, file), appFile, outputRoot);
    if (artifact == null) return raw;
    return raw.withTarget(artifact.target(), artifact.relativeOutput());
  }

  /** The haxe compilation target behind an nme CLI target id ("cpp" is nme's host-desktop word), or null for an unknown id. */
  @Nullable
  public static HaxeTarget targetFor(@NotNull String targetFlag) {
    return switch (targetFlag) {
      case "neko" -> HaxeTarget.NEKO;
      case "cpp", "windows", "mac", "linux", "android", "ios" -> HaxeTarget.CPP;
      case "flash", "air" -> HaxeTarget.FLASH;
      case "html5", "js", "jsprime" -> HaxeTarget.JAVA_SCRIPT;
      default -> null;
    };
  }

  /// Where the nme tool packages a target's runnable artifact, relative to the
  /// project file: `<output root>/<platform dir>/<app file>/...` (mac
  /// wraps an .app bundle instead of a plain directory). The output root is the
  /// nmml's `<app path>`, defaulting to `bin`. Platform dirs follow
  /// the tool's naming, which suffixes "64" for the 64-bit desktop builds every
  /// modern mac/linux host produces; a neko build lands in a host-suffixed
  /// `-neko` dir wrapping the bytecode in a launcher executable. "cpp" builds
  /// for the host desktop. Target flags without a launchable artifact mapping
  /// (android, ios, user-configured console targets...) return null.
  @Nullable
  public static TargetArtifact targetArtifact(@NotNull String targetFlag, @NotNull String appFile, @NotNull String outputRoot) {
    return switch (targetFlag) {
      case "cpp" -> hostDesktopArtifact(appFile, outputRoot);
      case "windows" -> windowsArtifact(appFile, outputRoot);
      case "linux" -> linuxArtifact(appFile, outputRoot);
      case "mac" -> macArtifact(appFile, outputRoot);
      case "neko" -> nekoArtifact(appFile, outputRoot);
      case "flash" -> new TargetArtifact(HaxeTarget.FLASH, outputRoot + "/flash/" + appFile + "/" + appFile + ".swf");
      default -> null;
    };
  }

  @NotNull
  private static TargetArtifact nekoArtifact(@NotNull String appFile, @NotNull String outputRoot) {
    String platformDir = nekoPlatformDirectory();
    String launcher = SystemInfo.isWindows ? appFile + ".exe" : appFile;
    return new TargetArtifact(HaxeTarget.NEKO, outputRoot + "/" + platformDir + "/" + appFile + "/" + launcher);
  }

  private static String nekoPlatformDirectory() {
    if (SystemInfo.isWindows) return "windows-neko";
    if (SystemInfo.isMac) return "mac64-neko";
    return "linux64-neko";
  }

  @NotNull
  private static TargetArtifact hostDesktopArtifact(@NotNull String appFile, @NotNull String outputRoot) {
    if (SystemInfo.isWindows) return windowsArtifact(appFile, outputRoot);
    if (SystemInfo.isMac) return macArtifact(appFile, outputRoot);
    return linuxArtifact(appFile, outputRoot);
  }

  @NotNull
  private static TargetArtifact windowsArtifact(@NotNull String appFile, @NotNull String outputRoot) {
    return new TargetArtifact(HaxeTarget.CPP, outputRoot + "/windows/" + appFile + "/" + appFile + ".exe");
  }

  @NotNull
  private static TargetArtifact linuxArtifact(@NotNull String appFile, @NotNull String outputRoot) {
    return new TargetArtifact(HaxeTarget.CPP, outputRoot + "/linux64/" + appFile + "/" + appFile);
  }

  @NotNull
  private static TargetArtifact macArtifact(@NotNull String appFile, @NotNull String outputRoot) {
    return new TargetArtifact(HaxeTarget.CPP, outputRoot + "/mac64/" + appFile + ".app/Contents/MacOS/" + appFile);
  }
}
