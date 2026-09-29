package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tool window: custom tools store")
public class HaxeCustomToolsStoreTest {

  @Test
  @DisplayName("defaults are empty")
  public void testDefaultsAreEmpty() {
    HaxeCustomToolsStore store = new HaxeCustomToolsStore();
    assertTrue(store.getTools("mod").isEmpty());
  }

  @Test
  @DisplayName("add update and remove keep creation order")
  public void testAddUpdateAndRemoveKeepCreationOrder() {
    HaxeCustomToolsStore store = new HaxeCustomToolsStore();
    store.addTool("mod", new HaxeCustomToolsStore.CustomTool("lint", "haxelib run checkstyle -s src", ""));
    store.addTool("mod", new HaxeCustomToolsStore.CustomTool("docs", "haxelib run dox", ""));
    assertEquals(List.of("lint", "docs"), store.getTools("mod").stream().map(HaxeCustomToolsStore.CustomTool::name).toList());

    store.updateTool("mod", "lint", new HaxeCustomToolsStore.CustomTool("lint all", "haxelib run checkstyle -s src -s test", ""));
    assertEquals(List.of("lint all", "docs"), store.getTools("mod").stream().map(HaxeCustomToolsStore.CustomTool::name).toList());
    assertEquals("haxelib run checkstyle -s src -s test", store.getTools("mod").getFirst().command());

    store.removeTool("mod", "docs");
    assertEquals(List.of("lint all"), store.getTools("mod").stream().map(HaxeCustomToolsStore.CustomTool::name).toList());
  }

  @Test
  @DisplayName("tools survive xml serialization round trip")
  public void testToolsSurviveXmlSerializationRoundTrip() {
    HaxeCustomToolsStore store = new HaxeCustomToolsStore();
    store.addTool("mod", new HaxeCustomToolsStore.CustomTool("lint", "haxelib run checkstyle -s src", "${projectRoot}"));

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeCustomToolsStore.State deserialized = XmlSerializer.deserialize(serialized, HaxeCustomToolsStore.State.class);

    HaxeCustomToolsStore reloaded = new HaxeCustomToolsStore();
    reloaded.loadState(deserialized);
    assertEquals(List.of(new HaxeCustomToolsStore.CustomTool("lint", "haxelib run checkstyle -s src", "${projectRoot}")),
                 reloaded.getTools("mod"));
  }
}
