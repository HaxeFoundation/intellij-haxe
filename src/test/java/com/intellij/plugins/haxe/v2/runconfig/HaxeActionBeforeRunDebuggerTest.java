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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The before-run step derives the debugger whose additions a Debug compile gets from the launched configuration's class. */
@DisplayName("Run configurations: action before run debugger")
public class HaxeActionBeforeRunDebuggerTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("hxcpp configurations name their own server")
  public void testHxcppConfigurationsNameTheirOwnServer() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();

    assertEquals(Debugger.DEFAULT, debuggerOf(new HxcppIntellijConfigurationFactory(type)));
    assertEquals(Debugger.HXCPP_VSHAXE, debuggerOf(new HxcppVshaxeConfigurationFactory(type)));
    assertEquals(Debugger.HXCPP_LEGACY, debuggerOf(new LegacyHxcppConfigurationFactory(type)));
  }

  @Test
  @DisplayName("only the flash player configuration tags the swf")
  public void testOnlyTheFlashPlayerConfigurationTagsTheSwf() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();

    assertEquals(Debugger.FLASH_PLAYER, debuggerOf(new FlashConfigurationFactory(type)));
    assertEquals(Debugger.DEFAULT, debuggerOf(new AirConfigurationFactory(type)));
  }

  private Debugger debuggerOf(ConfigurationFactory factory) {
    RunConfiguration configuration = factory.createTemplateConfiguration(getProject());
    return HaxeActionBeforeRunTaskProvider.debuggerOf(configuration);
  }
}
