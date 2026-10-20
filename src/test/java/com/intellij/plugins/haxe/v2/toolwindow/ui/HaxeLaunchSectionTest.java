package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind.Parameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Tool window: launch section")
public class HaxeLaunchSectionTest extends HaxeLightFixtureTestCase {

  private static final Map<Parameter, String> HASHLINK_DEFAULTS = Map.of(Parameter.HL_FILE, "/work/out/game.hl");

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("without a kind the section yields no launch")
  public void testWithoutAKindTheSectionYieldsNoLaunch() {
    HaxeLaunchSection section = new HaxeLaunchSection(getProject(), kind -> HASHLINK_DEFAULTS, null);

    assertNull(section.launch());
  }

  @Test
  @DisplayName("a stored launch comes back as edited and blank fields stay defaults")
  public void testAStoredLaunchComesBackAsEditedAndBlankFieldsStayDefaults() {
    Launch stored = new Launch.Create("HASHLINK", Map.of("hlFile", "dist/final.hl"), false);
    HaxeLaunchSection section = new HaxeLaunchSection(getProject(), kind -> HASHLINK_DEFAULTS, stored);

    Launch edited = section.launch();

    assertEquals(stored, edited, "the typed hl file is kept, the untouched runtime is not stored");
  }

  @Test
  @DisplayName("an existing configuration launch keeps its name even when no such configuration is registered")
  public void testAnExistingConfigurationLaunchKeepsItsNameEvenWhenNoSuchConfigurationIsRegistered() {
    Launch stored = new Launch.Existing("Deploy (Gradle)", false);
    HaxeLaunchSection section = new HaxeLaunchSection(getProject(), kind -> Map.of(), stored);

    assertEquals(stored, section.launch());
  }

  @Test
  @DisplayName("a key no parameter has is dropped on the way in")
  public void testAKeyNoParameterHasIsDroppedOnTheWayIn() {
    Launch stored = new Launch.Create("NEKO", Map.of("executable", "", "futureKey", "x"), true);
    HaxeLaunchSection section = new HaxeLaunchSection(getProject(), kind -> Map.of(), stored);

    assertEquals(new Launch.Create("NEKO", Map.of(), true), section.launch(), "blank and unknown values are not stored");
  }
}
