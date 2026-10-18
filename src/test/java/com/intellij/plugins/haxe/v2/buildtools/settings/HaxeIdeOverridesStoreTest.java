package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: IDE overrides store")
public class HaxeIdeOverridesStoreTest {

  private static final String BUILD_FILE = "/p/build.hxml";
  private static final String OTHER_BUILD_FILE = "/p/test.hxml";

  @Test
  @DisplayName("defaults are empty")
  public void testDefaultsAreEmpty() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();

    assertTrue(store.getDefines(BUILD_FILE).isEmpty());
    assertNull(store.getCustomTarget(BUILD_FILE));
    assertFalse(store.hasOverrides(BUILD_FILE));
  }

  @Test
  @DisplayName("put define adds and updates set entries")
  public void testPutDefineAddsAndUpdatesSetEntries() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();

    store.putDefine(BUILD_FILE, "debug", "");
    store.putDefine(BUILD_FILE, "level", "1");
    store.putDefine(BUILD_FILE, "level", "2");

    assertEquals(List.of(new DefineOverride("debug", "", DefineEffect.SET),
                         new DefineOverride("level", "2", DefineEffect.SET)),
                 store.getDefines(BUILD_FILE));
  }

  @Test
  @DisplayName("put undefine replaces an entry of the same name")
  public void testPutUndefineReplacesAnEntryOfTheSameName() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();
    store.putDefine(BUILD_FILE, "sys", "1");

    store.putUndefine(BUILD_FILE, "sys");
    assertEquals(List.of(new DefineOverride("sys", "", DefineEffect.REMOVE)), store.getDefines(BUILD_FILE));

    store.putDefine(BUILD_FILE, "sys", "");
    assertEquals(List.of(new DefineOverride("sys", "", DefineEffect.SET)), store.getDefines(BUILD_FILE));
  }

  @Test
  @DisplayName("set defines replaces the whole list and drops blank names")
  public void testSetDefinesReplacesTheWholeListAndDropsBlankNames() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();
    store.putDefine(BUILD_FILE, "old", "");

    store.setDefines(BUILD_FILE, List.of(new DefineOverride("html5", "", DefineEffect.SET),
                                         new DefineOverride("  ", "ignored", DefineEffect.SET),
                                         new DefineOverride("no-traces", "", DefineEffect.REMOVE)));

    assertEquals(List.of(new DefineOverride("html5", "", DefineEffect.SET),
                         new DefineOverride("no-traces", "", DefineEffect.REMOVE)),
                 store.getDefines(BUILD_FILE));
  }

  @Test
  @DisplayName("each build file keeps its own overrides")
  public void testEachBuildFileKeepsItsOwnOverrides() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();

    store.putDefine(BUILD_FILE, "html5", "");
    store.putDefine(OTHER_BUILD_FILE, "testOnlyCode", "");
    store.setCustomTarget(OTHER_BUILD_FILE, "go");

    assertEquals(List.of(new DefineOverride("html5", "", DefineEffect.SET)), store.getDefines(BUILD_FILE));
    assertNull(store.getCustomTarget(BUILD_FILE));
    assertEquals(List.of(new DefineOverride("testOnlyCode", "", DefineEffect.SET)), store.getDefines(OTHER_BUILD_FILE));
    assertEquals("go", store.getCustomTarget(OTHER_BUILD_FILE));
  }

  @Test
  @DisplayName("custom target can be set trimmed and cleared")
  public void testCustomTargetCanBeSetTrimmedAndCleared() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();

    store.setCustomTarget(BUILD_FILE, " go ");
    assertEquals("go", store.getCustomTarget(BUILD_FILE));

    store.setCustomTarget(BUILD_FILE, "   ");
    assertNull(store.getCustomTarget(BUILD_FILE));
  }

  @Test
  @DisplayName("a build file left without overrides leaves no entry")
  public void testABuildFileLeftWithoutOverridesLeavesNoEntry() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();
    store.putDefine(BUILD_FILE, "debug", "");
    store.setCustomTarget(BUILD_FILE, "go");
    assertTrue(store.hasOverrides(BUILD_FILE));

    store.removeDefine(BUILD_FILE, "debug");
    store.setCustomTarget(BUILD_FILE, null);

    assertFalse(store.hasOverrides(BUILD_FILE));
    assertTrue(store.getState().buildFiles.isEmpty());
  }

  @Test
  @DisplayName("overrides follow a moved build file")
  public void testOverridesFollowAMovedBuildFile() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();
    store.putDefine(BUILD_FILE, "debug", "");

    assertTrue(store.moveFile("/p", "/q"));

    assertEquals(List.of(new DefineOverride("debug", "", DefineEffect.SET)), store.getDefines("/q/build.hxml"));
    assertTrue(store.getDefines(BUILD_FILE).isEmpty());
    assertFalse(store.moveFile("/elsewhere", "/r"));
  }

  @Test
  @DisplayName("state survives xml serialization round trip with its version")
  public void testStateSurvivesXmlSerializationRoundTripWithItsVersion() {
    HaxeIdeOverridesStore store = new HaxeIdeOverridesStore();
    store.setDefines(BUILD_FILE, List.of(new DefineOverride("analyzer-optimize", "", DefineEffect.SET),
                                         new DefineOverride("no-traces", "", DefineEffect.REMOVE)));
    store.setCustomTarget(BUILD_FILE, "go");

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeIdeOverridesStore.State deserialized = XmlSerializer.deserialize(serialized, HaxeIdeOverridesStore.State.class);
    HaxeIdeOverridesStore reloaded = new HaxeIdeOverridesStore();
    reloaded.loadState(deserialized);

    assertEquals(HaxeIdeOverridesStore.STATE_VERSION, deserialized.version);
    assertEquals(List.of(new DefineOverride("analyzer-optimize", "", DefineEffect.SET),
                         new DefineOverride("no-traces", "", DefineEffect.REMOVE)),
                 reloaded.getDefines(BUILD_FILE));
    assertEquals("go", reloaded.getCustomTarget(BUILD_FILE));
  }
}
