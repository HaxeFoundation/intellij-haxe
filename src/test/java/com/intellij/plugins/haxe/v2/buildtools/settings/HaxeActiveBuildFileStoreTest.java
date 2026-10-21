package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: active build file store")
public class HaxeActiveBuildFileStoreTest {

  private static final List<String> TWO_FILES = List.of("/p/build.hxml", "/p/project.xml");

  @Test
  @DisplayName("stored choice wins when still present")
  public void storedChoiceWinsWhenStillPresent() {
    HaxeActiveBuildFileStore store = new HaxeActiveBuildFileStore();
    store.setActiveFile("/p/project.xml");
    assertEquals("/p/project.xml", store.resolveActivePath(TWO_FILES));
  }

  @Test
  @DisplayName("single candidate is active without explicit selection")
  public void singleCandidateIsActiveWithoutExplicitSelection() {
    HaxeActiveBuildFileStore store = new HaxeActiveBuildFileStore();
    assertEquals("/p/build.hxml", store.resolveActivePath(List.of("/p/build.hxml")));
  }

  @Test
  @DisplayName("stale stored path falls back")
  public void staleStoredPathFallsBack() {
    HaxeActiveBuildFileStore store = new HaxeActiveBuildFileStore();
    store.setActiveFile("/p/deleted.hxml");
    assertNull(store.resolveActivePath(TWO_FILES));
    assertEquals("/p/build.hxml", store.resolveActivePath(List.of("/p/build.hxml")));
  }

  @Test
  @DisplayName("no selection and multiple candidates yields none")
  public void noSelectionAndMultipleCandidatesYieldsNone() {
    HaxeActiveBuildFileStore store = new HaxeActiveBuildFileStore();
    assertNull(store.resolveActivePath(TWO_FILES));
  }

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeActiveBuildFileStore store = new HaxeActiveBuildFileStore();
    store.setActiveFile("/p/build.hxml");

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeActiveBuildFileStore.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeActiveBuildFileStore.State.class);

    HaxeActiveBuildFileStore reloaded = new HaxeActiveBuildFileStore();
    reloaded.loadState(deserialized);
    assertEquals("/p/build.hxml", reloaded.resolveActivePath(TWO_FILES));
  }
}
