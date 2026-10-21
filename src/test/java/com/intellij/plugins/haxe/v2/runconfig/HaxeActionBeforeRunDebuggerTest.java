package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.runner.debugger.flash.AirConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.flash.FlashConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy.LegacyHxcppConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.HxcppVshaxeConfigurationFactory;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions.Debugger;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The before-run step derives the debugger whose additions a Debug compile gets from the launched configuration's class. */
@DisplayName("Run configurations: action before run debugger")
public class HaxeActionBeforeRunDebuggerTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /** (configuration factory, the debugger its template configuration maps to). */
  static final List<Arguments> FACTORY_DEBUGGERS = List.of(
    // hxcpp configurations name their own server
    arguments(HxcppIntellijConfigurationFactory.class, Debugger.DEFAULT),
    arguments(HxcppVshaxeConfigurationFactory.class, Debugger.HXCPP_VSHAXE),
    arguments(LegacyHxcppConfigurationFactory.class, Debugger.HXCPP_LEGACY),
    // only the flash player configuration tags the swf
    arguments(FlashConfigurationFactory.class, Debugger.FLASH_PLAYER),
    arguments(AirConfigurationFactory.class, Debugger.DEFAULT));

  @ParameterizedTest(name = "{0}")
  @FieldSource("FACTORY_DEBUGGERS")
  @DisplayName("configuration maps to its debugger")
  public void testConfigurationMapsToItsDebugger(Class<? extends ConfigurationFactory> factoryClass, Debugger expected) {
    ConfigurationFactory factory = HaxeRunConfigurationType.getInstance().getFactory(factoryClass);
    RunConfiguration configuration = factory.createTemplateConfiguration(getProject());

    assertEquals(expected, HaxeActionBeforeRunTaskProvider.debuggerOf(configuration));
  }
}
