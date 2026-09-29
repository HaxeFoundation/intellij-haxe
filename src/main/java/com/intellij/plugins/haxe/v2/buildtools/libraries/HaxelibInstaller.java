package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Installs haxelib libraries without touching haxelib's selected version.
 * Runs external processes - call on a background thread.
 */
@CustomLog
public final class HaxelibInstaller {

  private static final int INSTALL_TIMEOUT_MS = 600_000;

  private HaxelibInstaller() {
  }

  /**
   * Runs one {@code haxelib install} (restoring the previously selected version
   * when the install would hijack it); null on success, else the failure detail.
   * {@code version} is the declared/pinned version, {@code resolvedVersion} the
   * version haxelib currently selects. A {@code git:URL[#ref]} pin installs
   * through {@code haxelib git} instead - {@code haxelib install} would fetch
   * the latest haxelib.org release and drop the pinned ref - and keeps the
   * git selection that install makes current (it is what the pin asks for).
   */
  @Nullable
  public static String install(@NotNull Project project, @NotNull String name,
                               @Nullable String version, @Nullable String resolvedVersion) {
    HaxelibGitSpec gitSpec = HaxelibGitSpec.parse(version);
    if (gitSpec != null) {
      return installGit(project, name, gitSpec.url(), gitSpec.ref());
    }
    String failure = run(project, installParameters(name, version));
    if (failure == null) {
      restoreSelectedVersion(project, name, version, resolvedVersion);
    }
    return failure;
  }

  /**
   * Runs {@code haxelib git <name> <url> [ref]} — clones the repository and
   * checks out the branch/tag/commit; null on success, else the failure
   * detail. Registers the library when unknown, and haxelib selects the git
   * version as current.
   */
  @Nullable
  public static String installGit(@NotNull Project project, @NotNull String name,
                                  @NotNull String url, @Nullable String ref) {
    var parameters = new ArrayList<>(List.of("git", name, url));
    if (ref != null && !ref.isBlank()) {
      parameters.add(ref);
    }
    parameters.add("--always");
    return run(project, parameters);
  }

  /** Whether haxelib resolves the library (installed, dev or git) — runs {@code haxelib path}. */
  public static boolean isInstalled(@NotNull Project project, @NotNull String name) {
    return run(project, List.of("path", name)) == null;
  }

  /** Runs {@code haxelib remove <name> [version]}; null on success, else the failure detail. */
  @Nullable
  public static String remove(@NotNull Project project, @NotNull String name, @Nullable String version) {
    var parameters = new ArrayList<>(List.of("remove", name));
    if (version != null && !version.isBlank()) {
      parameters.add(version);
    }
    return run(project, parameters);
  }

  /** Runs {@code haxelib set <name> <version> --always}; null on success, else the failure detail. */
  @Nullable
  public static String setCurrent(@NotNull Project project, @NotNull String name, @NotNull String version) {
    return run(project, List.of("set", name, version, "--always"));
  }

  /**
   * Registers the library's dev pointer ({@code haxelib dev <name> <directory>});
   * null on success, else the failure detail. Works for libraries with no
   * installed release too - dev registration creates the repository entry.
   */
  @Nullable
  public static String setDev(@NotNull Project project, @NotNull String name, @NotNull String directory) {
    return run(project, List.of("dev", name, directory));
  }

  /**
   * Clears the library's dev pointer ({@code haxelib dev <name>} with no
   * directory); null on success, else the failure detail. A dev pointer
   * overrides the selected version, so switching a dev-pinned library to a
   * release needs this on top of {@link #setCurrent}.
   */
  @Nullable
  public static String clearDev(@NotNull Project project, @NotNull String name) {
    return run(project, List.of("dev", name));
  }

  @Nullable
  private static String run(@NotNull Project project, @NotNull List<String> parameters) {
    GeneralCommandLine commandLine = haxelibCommand(project, parameters);
    try {
      ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(INSTALL_TIMEOUT_MS);
      if (output.getExitCode() != 0 || output.isTimeout()) {
        String detail = StringUtil.trimTrailing(output.getStdout() + "\n" + output.getStderr());
        String commandText = commandLine.getCommandLineString();
        log.warn("haxelib command failed (exit " + output.getExitCode() + "): " + commandText + "\n" + detail);
        return detail;
      }
      return null;
    }
    catch (ExecutionException e) {
      log.warn("haxelib command failed to start: " + commandLine.getCommandLineString(), e);
      return StringUtil.notNullize(e.getMessage());
    }
  }

  @NotNull
  private static GeneralCommandLine haxelibCommand(@NotNull Project project, @NotNull List<String> parameters) {
    return new GeneralCommandLine()
      .withExePath(HaxeToolPathResolver.resolveHaxelibExecutable(project))
      .withParameters(parameters)
      .withWorkDirectory(project.getBasePath());
  }

  // "haxelib install name [version] --always"; git/path pseudo-versions cannot be passed to install
  @NotNull
  private static List<String> installParameters(@NotNull String name, @Nullable String version) {
    var parameters = new ArrayList<>(List.of("install", name));
    if (HaxelibSemVer.isReleaseVersion(version)) {
      parameters.add(version);
    }
    parameters.add("--always");
    return parameters;
  }

  /**
   * Installing a pinned version makes it haxelib's SELECTED version as a side
   * effect, silently switching every unpinned project. Restore the previous
   * selection ("dev"/"git" pseudo-versions cannot be re-set and stay put).
   */
  private static void restoreSelectedVersion(@NotNull Project project, @NotNull String name,
                                             @Nullable String version, @Nullable String previous) {
    boolean selectionHijacked = HaxelibSemVer.isReleaseVersion(version)
                                && HaxelibSemVer.isReleaseVersion(previous)
                                && !previous.equals(version);
    if (!selectionHijacked) return;

    // the install itself succeeded; a failed restore only leaves the new version selected
    setCurrent(project, name, previous);
  }
}
