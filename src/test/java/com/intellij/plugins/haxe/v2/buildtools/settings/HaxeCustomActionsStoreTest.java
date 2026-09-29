package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: custom actions store")
public class HaxeCustomActionsStoreTest {

  private static final String FILE = "/p/build.hxml";

  @Test
  @DisplayName("defaults are empty")
  public void defaultsAreEmpty() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    assertTrue(store.getActions(FILE).isEmpty());
  }

  @Test
  @DisplayName("actions keep creation order and same name replaces")
  public void actionsKeepCreationOrderAndSameNameReplaces() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("docs", "haxe doc.hxml", ""));
    store.addAction(FILE, new CustomAction("serve", "nekotools server", ""));
    store.addAction(FILE, new CustomAction("docs", "haxe docs2.hxml", ""));

    assertEquals(List.of(new CustomAction("serve", "nekotools server", ""),
                         new CustomAction("docs", "haxe docs2.hxml", "")),
                 store.getActions(FILE));
  }

  @Test
  @DisplayName("update keeps position and rename works")
  public void updateKeepsPositionAndRenameWorks() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("a", "cmd-a", ""));
    store.addAction(FILE, new CustomAction("b", "cmd-b", ""));

    store.updateAction(FILE, "a", new CustomAction("a2", "cmd-a2", ""));

    assertEquals(List.of(new CustomAction("a2", "cmd-a2", ""), new CustomAction("b", "cmd-b", "")),
                 store.getActions(FILE));
  }

  @Test
  @DisplayName("remove deletes by name")
  public void removeDeletesByName() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("a", "cmd-a", ""));
    store.removeAction(FILE, "a");
    assertTrue(store.getActions(FILE).isEmpty());
  }

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("docs", "haxe doc.hxml", "${moduleRoot}/docs"));

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeCustomActionsStore.State deserialized = XmlSerializer.deserialize(serialized, HaxeCustomActionsStore.State.class);

    HaxeCustomActionsStore reloaded = new HaxeCustomActionsStore();
    reloaded.loadState(deserialized);
    assertEquals(List.of(new CustomAction("docs", "haxe doc.hxml", "${moduleRoot}/docs")), reloaded.getActions(FILE));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  @DisplayName("foreign entries from format changes are dropped")
  public void foreignEntriesFromFormatChangesAreDropped() {
    HaxeCustomActionsStore.ContainerActions container = new HaxeCustomActionsStore.ContainerActions();
    container.ownerId = FILE;
    ((List)container.actions).add("legacy-entry");
    HaxeCustomActionsStore.State state = new HaxeCustomActionsStore.State();
    state.containers.add(container);

    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.loadState(state);
    assertTrue(store.getActions(FILE).isEmpty());
  }
}
