package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.jdom.Element;

public class BrowserRunConfigurationPersistenceTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  private BrowserRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new BrowserRunConfiguration("browser", getProject(), new BrowserConfigurationFactory(type));
  }

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

}
