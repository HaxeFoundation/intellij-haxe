package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tool window: ui state")
public class HaxeToolWindowUiStateTest {

  @Test
  @DisplayName("defaults are empty")
  public void defaultsAreEmpty() {
    HaxeToolWindowUiState uiState = new HaxeToolWindowUiState();
    assertTrue(uiState.getExpandedKeys().isEmpty());
  }

  @Test
  @DisplayName("hide empty modules flag survives xml serialization round trip")
  public void hideEmptyModulesFlagSurvivesXmlSerializationRoundTrip() {
    HaxeToolWindowUiState uiState = new HaxeToolWindowUiState();
    assertFalse(uiState.isHideEmptyModules());
    uiState.setHideEmptyModules(true);

    Element serialized = XmlSerializer.serialize(uiState.getState());
    HaxeToolWindowUiState.State deserialized = XmlSerializer.deserialize(serialized, HaxeToolWindowUiState.State.class);

    HaxeToolWindowUiState reloaded = new HaxeToolWindowUiState();
    reloaded.loadState(deserialized);
    assertTrue(reloaded.isHideEmptyModules());
  }

  @Test
  @DisplayName("expanded keys survive xml serialization round trip")
  public void expandedKeysSurviveXmlSerializationRoundTrip() {
    HaxeToolWindowUiState uiState = new HaxeToolWindowUiState();
    uiState.setExpandedKeys(Set.of("|module:app|env", "|module:app|env|envdefines", "|module:app|build"));

    Element serialized = XmlSerializer.serialize(uiState.getState());
    HaxeToolWindowUiState.State deserialized = XmlSerializer.deserialize(serialized, HaxeToolWindowUiState.State.class);

    HaxeToolWindowUiState reloaded = new HaxeToolWindowUiState();
    reloaded.loadState(deserialized);
    assertEquals(Set.of("|module:app|env", "|module:app|env|envdefines", "|module:app|build"), reloaded.getExpandedKeys());
    assertEquals(List.of("|module:app|build", "|module:app|env", "|module:app|env|envdefines"),
                 reloaded.getState().expandedKeys);
  }
}
