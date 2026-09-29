package com.intellij.plugins.haxe.runner.debugger.browser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.jdom.Element;

@DisplayName("Debugger: browser run configuration persistence")
public class BrowserRunConfigurationPersistenceTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("settings survive a round trip")
  public void testSettingsSurviveARoundTrip() throws Exception {
    BrowserRunConfiguration saved = newConfiguration();
    saved.setServeContent(false);
    saved.setUrl("http://localhost:9000/app.html");
    saved.setContentRoot("bin");
    saved.setBrowserId("chrome");
    saved.setNodePath("/usr/bin/node");

    Element element = new Element("configuration");
    saved.writeExternal(element);

    BrowserRunConfiguration loaded = newConfiguration();
    loaded.readExternal(element);

    assertFalse(loaded.isServeContent());
    assertEquals("http://localhost:9000/app.html", loaded.getUrl());
    assertEquals("bin", loaded.getContentRoot());
    assertEquals("chrome", loaded.getBrowserId());
    assertEquals("/usr/bin/node", loaded.getNodePath());
  }

  private BrowserRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new BrowserRunConfiguration("browser", getProject(), new BrowserConfigurationFactory(type));
  }
}
