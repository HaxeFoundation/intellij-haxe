package com.intellij.plugins.haxe.runner.customtarget;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Run configuration: custom target")
public class CustomTargetRunConfigurationTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("settings survive a round trip")
  public void testSettingsSurviveARoundTrip() throws Exception {
    CustomTargetRunConfiguration saved = newConfiguration();
    saved.setExecutablePath("tools/haxe.exe");
    saved.setWorkingDirectory("libs");
    saved.setProgramArguments("build.hxml -D release");

    Element element = new Element("configuration");
    saved.writeExternal(element);

    CustomTargetRunConfiguration loaded = newConfiguration();
    loaded.readExternal(element);

    assertEquals("tools/haxe.exe", loaded.getExecutablePath());
    assertEquals("libs", loaded.getWorkingDirectory());
    assertEquals("build.hxml -D release", loaded.getProgramArguments());
  }

  private CustomTargetRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new CustomTargetRunConfiguration("executable", getProject(), new CustomTargetConfigurationFactory(type));
  }
}
