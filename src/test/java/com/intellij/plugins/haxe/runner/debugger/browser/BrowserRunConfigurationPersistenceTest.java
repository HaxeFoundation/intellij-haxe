package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import org.jdom.Element;

/**
 * Round-trips the browser run configuration through its XML form. The
 * settings survive a save/load, and a stored element that predates a field
 * (or omits it) leaves that field at its default rather than at whatever
 * parsing an empty string yields — serve mode defaults to ON, and
 * Boolean.parseBoolean("") is false.
 */
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
    saved.setBrowserFamily(BrowserFamily.CHROMIUM);
    saved.setServeContent(false);
    saved.setUrl("http://localhost:9000/app.html");
    saved.setContentRoot("bin");
    saved.setBrowserExecutablePath("/opt/chrome/chrome");
    saved.setNodePath("/usr/bin/node");

    Element element = new Element("configuration");
    saved.writeExternal(element);

    BrowserRunConfiguration loaded = newConfiguration();
    loaded.readExternal(element);

    assertEquals(BrowserFamily.CHROMIUM, loaded.getBrowserFamily());
    assertFalse(loaded.isServeContent());
    assertEquals("http://localhost:9000/app.html", loaded.getUrl());
    assertEquals("bin", loaded.getContentRoot());
    assertEquals("/opt/chrome/chrome", loaded.getBrowserExecutablePath());
    assertEquals("/usr/bin/node", loaded.getNodePath());
  }

  /** Serve mode stays on: an absent field must not read as false. */
  public void testAbsentServeContentKeepsTheDefault() throws Exception {
    BrowserRunConfiguration loaded = newConfiguration();
    assertTrue("precondition: serve mode is the default", loaded.isServeContent());

    loaded.readExternal(new Element("configuration"));

    assertTrue("an element without the field leaves serve mode on", loaded.isServeContent());
  }

  /** An unreadable family name falls back rather than failing the load. */
  public void testUnknownFamilyFallsBackToFirefox() throws Exception {
    BrowserRunConfiguration saved = newConfiguration();
    saved.setBrowserFamily(BrowserFamily.CHROMIUM);
    Element element = new Element("configuration");
    saved.writeExternal(element);

    BrowserRunConfiguration loaded = newConfiguration();
    loaded.readExternal(new Element("configuration"));
    assertEquals(BrowserFamily.FIREFOX, loaded.getBrowserFamily());
  }
}
