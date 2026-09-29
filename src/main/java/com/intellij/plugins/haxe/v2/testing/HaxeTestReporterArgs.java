package com.intellij.plugins.haxe.v2.testing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The argument shapes the shipped live reporters share (see the READMEs under
 * {@code resources/testing/}): every reporter reads the run's root-suite label
 * from the same define, and the macro-injected ones ride the same
 * classpath-plus-entry-point tail.
 */
final class HaxeTestReporterArgs {

  /** The define every shipped reporter reads the run's root-suite label from at compile/macro time. */
  static final String SUITE_NAME_DEFINE = "teamcity_suite_name";

  private HaxeTestReporterArgs() {
  }

  /** The root-suite-name define, or nothing without a name. */
  @NotNull
  static List<String> suiteNameDefine(@Nullable String suiteName) {
    return suiteName == null ? List.of() : List.of("-D", SUITE_NAME_DEFINE + "=" + suiteName);
  }

  /**
   * A macro-injected reporter's arguments: the root-suite-name define, the
   * extracted reporter's classpath and its {@code --macro} entry point. Empty
   * without an extracted classpath — there is then nothing to inject and the
   * run keeps the framework's console output only.
   */
  @NotNull
  static List<String> macroReporterArgs(@Nullable String suiteName,
                                        @Nullable String reporterClasspath,
                                        @NotNull String macroInit) {
    if (reporterClasspath == null) return List.of();
    List<String> arguments = new ArrayList<>(suiteNameDefine(suiteName));
    arguments.add("-cp");
    arguments.add(reporterClasspath);
    arguments.add("--macro");
    arguments.add(macroInit);
    return arguments;
  }
}
