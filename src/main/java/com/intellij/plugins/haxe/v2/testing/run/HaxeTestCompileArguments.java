package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.testing.*;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The compile arguments a unit-test run adds to its tests build: the
 * framework's reporting set (and filter) in plain hxml spelling, respelled
 * into lime's or nme's forwarding forms for packaged builds, plus a single
 * run's narrowing. Call inside a read action.
 */
final class HaxeTestCompileArguments {

  /** The run's root-suite label plus whether an IDE-provided host (adl, a served browser page) runs the tests. */
  private record SuiteContext(@Nullable String suiteName, boolean hostedTests) {
  }

  /** A build tool's forwarding forms for the three plain-hxml flags; each flag's VALUE is appended to its form. */
  private record FlagSpellings(@NotNull String defineForm, @NotNull String classpathForm, @NotNull String macroForm) {
  }

  /**
   * lime's forwarding forms (lime 8.3.2): ATTACHED defines ({@code -Dname=value}:
   * the two-word spelling trips a lime bug duplicating the value),
   * {@code --source=} for classpaths and {@code --haxeflag=} for macros.
   */
  private static final FlagSpellings LIME_SPELLINGS = new FlagSpellings("-D", "--source=", "--haxeflag=--macro ");

  /**
   * nme's forwarding forms: the tool forwards ATTACHED defines and any
   * double-dash token verbatim into its generated build.hxml (single-dash
   * haxe flags like {@code -cp} are swallowed - the classpath rides the
   * {@code --class-path} spelling; nme 7.0.64).
   */
  private static final FlagSpellings NME_SPELLINGS = new FlagSpellings("-D", "--class-path ", "--macro ");

  /** lime's app-field override form for the entry point ({@code --app-main=Class}, lime 8.3.2). */
  private static final String LIME_APP_MAIN_FORM = "--app-main=";

  private HaxeTestCompileArguments() {
  }

  /**
   * The framework's compile arguments (reporting + optional filter) as an
   * extra-arguments string, in the spelling the build tool takes: plain haxe
   * flags for hxml builds, the tool's forwarding forms for lime-family and nme
   * builds (see {@link #LIME_SPELLINGS}, {@link #NME_SPELLINGS}).
   */
  @NotNull
  static String compileArguments(@NotNull Project project,
                                 @NotNull String buildFilePath,
                                 @Nullable String filterPattern) {
    // only a lime single run can fail to produce arguments, and this form asks for none
    return Objects.requireNonNull(compileArguments(project, buildFilePath, filterPattern, null));
  }

  /**
   * As {@link #compileArguments(Project, String, String)}; a lime-family
   * single run additionally overrides the app's main with the generated
   * template main ({@code --app-main=} plus its {@code --source=}), so the
   * tool packages the selection exactly like the whole build - the runtime
   * (native library, assets, application bootstrap) is what the tests expect.
   * Null only for such a run whose main could not be generated: the
   * arguments without it would build and run the WHOLE app under the
   * selection's name, so the launch must fail instead.
   */
  @Nullable
  static String compileArguments(@NotNull Project project,
                                 @NotNull String buildFilePath,
                                 @Nullable String filterPattern,
                                 @Nullable HaxeTestSingleRuns.SingleRun singleRun) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    HaxeBuildFileType type = buildFile == null ? null : buildFile.type();
    if (LimeProjects.isLimeFamily(type)) {
      return singleRun != null
             ? limeSingleRunCompileArguments(project, buildFile.file(), type, singleRun)
             : limeCompileArguments(project, buildFile.file(), type, filterPattern);
    }
    if (type == HaxeBuildFileType.NMML) {
      return nmeCompileArguments(project, buildFile.file(), filterPattern);
    }

    return ParametersListUtil.join(
      frameworkArguments(project, buildFilePath, hxmlSuiteContext(project, buildFilePath), filterPattern));
  }

  /** Whether the hxml tests build compiles for flash - the adl-hosted lane needs the injected reporter (exit + stdout). */
  private static boolean isFlashHxml(@NotNull Project project, @NotNull String buildFilePath) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFilePath);
    if (file == null || !file.isValid()) return false;
    HaxeBuildFileInfo info = HaxeBuildSections.inspectSelected(project, new HaxeBuildFile(file, HaxeBuildFileType.HXML));
    return info.target() == HaxeTarget.FLASH;
  }

  @NotNull
  private static String limeCompileArguments(@NotNull Project project,
                                             @NotNull VirtualFile file,
                                             @NotNull HaxeBuildFileType type,
                                             @Nullable String filterPattern) {
    String targetFlag = LimeProjects.selectedTargetFlag(project, type, file);
    List<String> plain = frameworkArguments(project, file.getPath(), limeSuiteContext(targetFlag), filterPattern);
    return ParametersListUtil.join(limeSpelled(project, targetFlag, plain));
  }

  /** Plain hxml arguments in lime's forwarding spelling, plus the air swf-version flag where the target needs it. */
  @NotNull
  private static List<String> limeSpelled(@NotNull Project project, @NotNull String targetFlag, @NotNull List<String> plain) {
    List<String> spelled = new ArrayList<>(respell(plain, LIME_SPELLINGS));
    if ("air".equals(targetFlag)) {
      spelled.addAll(airSwfVersionFlag(project));
    }
    return spelled;
  }

  /**
   * The lime-family single-run arguments: the reporting set (the method
   * narrowing for a single test) plus the generated main overriding the
   * app's; null when the main could not be written (see
   * {@link #compileArguments(Project, String, String, HaxeTestSingleRuns.SingleRun)}).
   */
  @Nullable
  private static String limeSingleRunCompileArguments(@NotNull Project project,
                                                      @NotNull VirtualFile file,
                                                      @NotNull HaxeBuildFileType type,
                                                      @NotNull HaxeTestSingleRuns.SingleRun singleRun) {
    HaxeTestFramework framework = HaxeTestFrameworks.forBuildFile(project, file.getPath());
    String targetFlag = LimeProjects.selectedTargetFlag(project, type, file);
    List<String> plain = singleRunArguments(project, framework, limeSuiteContext(targetFlag), singleRun);
    List<String> spelled = limeSpelled(project, targetFlag, plain);
    Path generated = HaxeTestSingleRuns.generatedDirectory(file.getPath(), framework, singleRun);
    if (generated == null) return null;
    spelled.add(LIME_SPELLINGS.classpathForm() + generated);
    spelled.add(LIME_APP_MAIN_FORM + HaxeTestSingleRuns.MAIN_CLASS);
    return ParametersListUtil.join(spelled);
  }

  /**
   * lime's air builds default to {@code -swf-version 17}, whose openfl AIR
   * extern overrides trip VerifyError #1053 under a modern AIR runtime. The
   * tests swf targets the hosting SDK's own version instead (a trailing CLI
   * flag overrides the default). No flag when no AIR SDK resolves - the run
   * would already stop at the missing adl.
   */
  @NotNull
  private static List<String> airSwfVersionFlag(@NotNull Project project) {
    String adl = HaxeToolPathResolver.resolveAdlExecutable(project);
    if (adl == null) return List.of();
    // namespaceVersion is "major.minor" (e.g. 31.0); -swf-version takes the major
    String major = AirTestHost.namespaceVersion(Path.of(adl)).split("\\.")[0];
    return List.of("--haxeflag=-swf-version " + major);
  }

  @NotNull
  private static SuiteContext limeSuiteContext(@NotNull String targetFlag) {
    return new SuiteContext(suiteLabel(limeTarget(targetFlag)), LimeProjects.isHostedTarget(targetFlag));
  }

  @NotNull
  private static SuiteContext hxmlSuiteContext(@NotNull Project project, @NotNull String buildFilePath) {
    return new SuiteContext(rootSuiteName(project, buildFilePath), isFlashHxml(project, buildFilePath));
  }

  /**
   * The tests build's framework arguments in plain hxml spelling — the
   * reporting set plus the filter. The tool-specific paths respell them (see
   * {@link #respell}).
   *
   * buddy's packaged (lime/nme) tests only report when the app's OWN main
   * honors the injected {@code -D reporter} define — a lime/nme main is the
   * Sprite, not buddy's generated main, so buddy's built-in handling of that
   * define never runs. The conditional to copy into such a TestMain lives in
   * the testProjects buddy samples; without it the run stays console-only.
   */
  @NotNull
  private static List<String> frameworkArguments(@NotNull Project project,
                                                 @NotNull String buildFilePath,
                                                 @NotNull SuiteContext suite,
                                                 @Nullable String filterPattern) {
    HaxeTestFramework framework = HaxeTestFrameworks.forBuildFile(project, buildFilePath);
    List<String> arguments = reportingArguments(project, framework, suite);
    arguments.addAll(framework.filterArgs(StringUtil.nullize(filterPattern, true)));
    return arguments;
  }

  /**
   * The framework's reporting arguments over its extracted shipped reporter.
   * The hosted lanes depend on the injected reporter: adl-hosted flash for its
   * stdout output AND the exit call, browser-hosted html5 for the console
   * transport and the completion sentinel - the live-reporting toggle cannot
   * opt a hosted build out of it.
   */
  @NotNull
  private static List<String> reportingArguments(@NotNull Project project,
                                                 @NotNull HaxeTestFramework framework,
                                                 @NotNull SuiteContext suite) {
    boolean liveReporting = suite.hostedTests() || HaxeBuildToolSettings.getInstance(project).isLiveTestReporting();
    return new ArrayList<>(framework.reportingArgs(suite.suiteName(), framework.reporterClasspath(), liveReporting));
  }

  /** Respells plain hxml arguments into a build tool's forwarding forms. */
  @NotNull
  private static List<String> respell(@NotNull List<String> plainArguments, @NotNull FlagSpellings forms) {
    List<String> spelled = new ArrayList<>();
    for (int i = 0; i < plainArguments.size(); i++) {
      String argument = plainArguments.get(i);
      switch (argument) {
        case "-D" -> spelled.add(forms.defineForm() + plainArguments.get(++i));
        case "-cp" -> spelled.add(forms.classpathForm() + plainArguments.get(++i));
        case "--macro" -> spelled.add(forms.macroForm() + plainArguments.get(++i));
        default -> spelled.add(argument);
      }
    }
    return spelled;
  }

  /** The plan-level target a lime target flag compiles through; unknown ids fall to hxcpp, the desktop default. */
  @NotNull
  static HaxeTarget limeTarget(@NotNull String targetFlag) {
    HaxeTarget target = LimeProjects.targetFor(targetFlag);
    return target != null ? target : HaxeTarget.CPP;
  }

  @NotNull
  private static String nmeCompileArguments(@NotNull Project project,
                                            @NotNull VirtualFile file,
                                            @Nullable String filterPattern) {
    String targetFlag = NmeProjects.selectedTargetFlag(project, file);
    // unknown target ids fall to hxcpp, nme's host-desktop default
    HaxeTarget mapped = NmeProjects.targetFor(targetFlag);
    HaxeTarget target = mapped != null ? mapped : HaxeTarget.CPP;
    SuiteContext suite = new SuiteContext(suiteLabel(target), target == HaxeTarget.FLASH);
    List<String> plain = frameworkArguments(project, file.getPath(), suite, filterPattern);
    return ParametersListUtil.join(respell(plain, NME_SPELLINGS));
  }

  /** The tree's root-suite label for a target ({@code Target: Neko}). */
  @NotNull
  private static String suiteLabel(@NotNull HaxeTarget target) {
    return "Target: " + target;
  }

  /** The tree's root suite label, from the tests build's target. Null when the file cannot be inspected. */
  @Nullable
  private static String rootSuiteName(@NotNull Project project, @NotNull String buildFilePath) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    if (buildFile == null || buildFile.type() != HaxeBuildFileType.HXML) return null;
    HaxeTarget target = HaxeBuildSections.inspectSelected(project, buildFile).target();
    return suiteLabel(Objects.requireNonNullElse(target, HaxeTarget.INTERP));
  }

  /** The hxml single-run compile arguments: the reporting set plus the method narrowing. */
  @NotNull
  static String singleRunCompileArguments(@NotNull Project project,
                                                  @NotNull String buildFilePath,
                                                  @NotNull HaxeTestFramework framework,
                                                  @NotNull HaxeTestSingleRuns.SingleRun singleRun) {
    SuiteContext suite = hxmlSuiteContext(project, buildFilePath);
    return ParametersListUtil.join(singleRunArguments(project, framework, suite, singleRun));
  }

  /** The reporting set, narrowed to the one method for a single-test run, in plain hxml spelling. */
  @NotNull
  private static List<String> singleRunArguments(@NotNull Project project,
                                                 @NotNull HaxeTestFramework framework,
                                                 @NotNull SuiteContext suite,
                                                 @NotNull HaxeTestSingleRuns.SingleRun singleRun) {
    List<String> arguments = reportingArguments(project, framework, suite);
    if (singleRun.singleTest()) {
      arguments.addAll(framework.singleRunFilterArgs(singleRun.testMethod()));
    }
    return arguments;
  }
}
