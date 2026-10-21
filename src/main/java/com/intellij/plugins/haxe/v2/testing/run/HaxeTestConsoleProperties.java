package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.Executor;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import org.jetbrains.annotations.NotNull;

/**
 * Console properties for Haxe unit-test runs: the {@link HaxeTestLocator} for
 * click-through ({@code haxe:test}, {@code haxe:tink} and {@code haxe:buddy}
 * hints), and the converter injecting location hints into utest's hint-less
 * TeamCity stream.
 */
public final class HaxeTestConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {

  public HaxeTestConsoleProperties(@NotNull HaxeTestRunConfiguration configuration, @NotNull Executor executor) {
    super(configuration, HaxeTestRunConfiguration.TEST_FRAMEWORK_NAME, executor);
  }

  @Override
  public SMTestLocator getTestLocator() {
    return HaxeTestLocator.INSTANCE;
  }

  @Override
  public OutputToGeneralTestEventsConverter createTestEventsConverter(@NotNull String testFrameworkName,
                                                                      @NotNull TestConsoleProperties consoleProperties) {
    return new HaxeTestEventsConverter(testFrameworkName, consoleProperties);
  }
}
