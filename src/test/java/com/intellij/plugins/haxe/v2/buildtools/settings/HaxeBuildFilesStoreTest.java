package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: build files store")
public class HaxeBuildFilesStoreTest {

  private static final String MODULE = "app";
  private static final String PATH = "/p/sub/build.hxml";

  @Test
  @DisplayName("defaults are empty")
  public void defaultsAreEmpty() {
    HaxeBuildFilesStore store = new HaxeBuildFilesStore();
    assertTrue(store.getAddedPaths(MODULE).isEmpty());
    assertTrue(store.getHiddenPaths(MODULE).isEmpty());
  }

  @Test
  @DisplayName("added files can be removed again")
  public void addedFilesCanBeRemovedAgain() {
    HaxeBuildFilesStore store = new HaxeBuildFilesStore();
    store.addFile(MODULE, PATH);
    assertEquals(List.of(PATH), store.getAddedPaths(MODULE));

    store.removeFile(MODULE, PATH);
    assertTrue(store.getAddedPaths(MODULE).isEmpty());
    assertTrue(store.getHiddenPaths(MODULE).isEmpty());
  }

  @Test
  @DisplayName("removing an auto detected file hides it")
  public void removingAnAutoDetectedFileHidesIt() {
    HaxeBuildFilesStore store = new HaxeBuildFilesStore();
    store.removeFile(MODULE, PATH);
    assertEquals(List.of(PATH), store.getHiddenPaths(MODULE));
  }

  @Test
  @DisplayName("adding a hidden file unhides it")
  public void addingAHiddenFileUnhidesIt() {
    HaxeBuildFilesStore store = new HaxeBuildFilesStore();
    store.removeFile(MODULE, PATH);
    store.addFile(MODULE, PATH);

    assertTrue(store.getHiddenPaths(MODULE).isEmpty());
    assertEquals(List.of(PATH), store.getAddedPaths(MODULE));
  }

  @Test
  @DisplayName("adding twice keeps one entry")
  public void addingTwiceKeepsOneEntry() {
    HaxeBuildFilesStore store = new HaxeBuildFilesStore();
    store.addFile(MODULE, PATH);
    store.addFile(MODULE, PATH);
    assertEquals(List.of(PATH), store.getAddedPaths(MODULE));
  }

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeBuildFilesStore store = new HaxeBuildFilesStore();
    store.addFile(MODULE, PATH);
    store.removeFile(MODULE, "/p/tests.hxml");

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeBuildFilesStore.State deserialized = XmlSerializer.deserialize(serialized, HaxeBuildFilesStore.State.class);

    HaxeBuildFilesStore reloaded = new HaxeBuildFilesStore();
    reloaded.loadState(deserialized);

    assertEquals(List.of(PATH), reloaded.getAddedPaths(MODULE));
    assertEquals(List.of("/p/tests.hxml"), reloaded.getHiddenPaths(MODULE));
  }
}
