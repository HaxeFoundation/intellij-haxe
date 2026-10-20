package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Build tools: project settings")
public class HaxeBuildToolProjectSettingsTest {

  @Test
  @DisplayName("defaults are empty")
  public void defaultsAreEmpty() {
    HaxeBuildToolProjectSettings settings = new HaxeBuildToolProjectSettings();
    assertNull(settings.getSdkName());
    assertEquals("", settings.getNekoPath());
    assertEquals("", settings.getHashlinkPath());
    assertEquals("", settings.getHaxelibPath());
  }

  @Test
  @DisplayName("blank sdk name is treated as unset")
  public void blankSdkNameIsTreatedAsUnset() {
    HaxeBuildToolProjectSettings settings = new HaxeBuildToolProjectSettings();
    settings.setSdkName("");
    assertNull(settings.getSdkName());
  }

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeBuildToolProjectSettings settings = new HaxeBuildToolProjectSettings();
    settings.setSdkName("Haxe 4.3.7");
    settings.setNekoPath("C:/HaxeToolkit/neko/neko.exe");
    settings.setHashlinkPath("C:/HaxeToolkit/hl/hl.exe");
    settings.setHaxelibPath("C:/HaxeToolkit/haxe/haxelib.exe");

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeBuildToolProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeBuildToolProjectSettings.State.class);

    HaxeBuildToolProjectSettings reloaded = new HaxeBuildToolProjectSettings();
    reloaded.loadState(deserialized);

    assertEquals("Haxe 4.3.7", reloaded.getSdkName());
    assertEquals("C:/HaxeToolkit/neko/neko.exe", reloaded.getNekoPath());
    assertEquals("C:/HaxeToolkit/hl/hl.exe", reloaded.getHashlinkPath());
    assertEquals("C:/HaxeToolkit/haxe/haxelib.exe", reloaded.getHaxelibPath());
  }
}
