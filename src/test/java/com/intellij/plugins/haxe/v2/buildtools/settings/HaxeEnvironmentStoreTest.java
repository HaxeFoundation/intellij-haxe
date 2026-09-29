package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: environment store")
public class HaxeEnvironmentStoreTest {

  private static final String MODULE = "app";

  @Test
  @DisplayName("defaults are empty")
  public void defaultsAreEmpty() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    assertNull(store.getSdkName(MODULE));
    assertTrue(store.getDefines(MODULE).isEmpty());
  }

  @Test
  @DisplayName("has user overrides reflects every stored override kind")
  public void testHasUserOverridesReflectsEveryStoredOverrideKind() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    assertFalse(store.hasUserOverrides("mod"));

    store.setSdkName("mod", "haxe 4.3.7");
    assertTrue(store.hasUserOverrides("mod"));
    store.setSdkName("mod", null);
    assertFalse(store.hasUserOverrides("mod"));

    store.putDefine("mod", "debug", "");
    assertTrue(store.hasUserOverrides("mod"));
    store.removeDefine("mod", "debug");
    assertFalse(store.hasUserOverrides("mod"));

    store.setCustomTarget("mod", "hl");
    assertTrue(store.hasUserOverrides("mod"));
    store.setCustomTarget("mod", null);
    assertFalse(store.hasUserOverrides("mod"));

    store.setCompileCommand("mod", new HaxeEnvironmentStore.CompileCommand("build.hxml", null, ""));
    assertTrue(store.hasUserOverrides("mod"));
    store.setCompileCommand("mod", null);
    assertFalse(store.hasUserOverrides("mod"));

    store.setUsingCompilationServer("mod", false);
    assertTrue(store.hasUserOverrides("mod"));
    store.setUsingCompilationServer("mod", true);
    assertFalse(store.hasUserOverrides("mod"));
  }

  @Test
  @DisplayName("sdk can be set and cleared")
  public void sdkCanBeSetAndCleared() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.setSdkName(MODULE, "Haxe 4.3.7");
    assertEquals("Haxe 4.3.7", store.getSdkName(MODULE));

    store.setSdkName(MODULE, null);
    assertNull(store.getSdkName(MODULE));
  }

  @Test
  @DisplayName("put define adds and updates set entries")
  public void putDefineAddsAndUpdatesSetEntries() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.putDefine(MODULE, "debug", "");
    store.putDefine(MODULE, "level", "1");
    store.putDefine(MODULE, "level", "2");

    assertEquals(List.of(new EnvironmentDefine("debug", "", DefineEffect.SET),
                         new EnvironmentDefine("level", "2", DefineEffect.SET)),
                 store.getDefines(MODULE));
  }

  @Test
  @DisplayName("remove define drops the entry")
  public void removeDefineDropsTheEntry() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.putDefine(MODULE, "debug", "");
    store.putDefine(MODULE, "level", "2");

    store.removeDefine(MODULE, "debug");
    assertEquals(List.of(new EnvironmentDefine("level", "2", DefineEffect.SET)), store.getDefines(MODULE));
  }

  @Test
  @DisplayName("set defines replaces the whole list and drops blank names")
  public void setDefinesReplacesTheWholeListAndDropsBlankNames() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.putDefine(MODULE, "old", "");

    store.setDefines(MODULE, List.of(new EnvironmentDefine("html5", "", DefineEffect.SET),
                                     new EnvironmentDefine("  ", "ignored", DefineEffect.SET),
                                     new EnvironmentDefine("no-traces", "", DefineEffect.REMOVE)));

    assertEquals(List.of(new EnvironmentDefine("html5", "", DefineEffect.SET),
                         new EnvironmentDefine("no-traces", "", DefineEffect.REMOVE)),
                 store.getDefines(MODULE));
  }

  @Test
  @DisplayName("containers are independent")
  public void containersAreIndependent() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.putDefine("app", "html5", "");
    store.putDefine("lib", "hl", "");

    assertEquals(List.of(new EnvironmentDefine("html5", "", DefineEffect.SET)), store.getDefines("app"));
    assertEquals(List.of(new EnvironmentDefine("hl", "", DefineEffect.SET)), store.getDefines("lib"));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  @DisplayName("legacy format entries are dropped instead of exploding later")
  public void legacyFormatEntriesAreDroppedInsteadOfExplodingLater() {
    // the old storage format (defines as a map) deserializes into the list as Strings
    HaxeEnvironmentStore.ContainerEnvironment environment = new HaxeEnvironmentStore.ContainerEnvironment();
    environment.containerId = MODULE;
    ((List)environment.defines).add("legacy-map-entry");
    HaxeEnvironmentStore.State state = new HaxeEnvironmentStore.State();
    state.environments.add(environment);

    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.loadState(state);

    assertTrue(store.getDefines(MODULE).isEmpty());
    store.putDefine(MODULE, "debug", "");
    assertEquals(List.of(new EnvironmentDefine("debug", "", DefineEffect.SET)), store.getDefines(MODULE));
  }

  @Test
  @DisplayName("custom target can be set trimmed and cleared")
  public void customTargetCanBeSetTrimmedAndCleared() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    assertNull(store.getCustomTarget(MODULE));

    store.setCustomTarget(MODULE, " go ");
    assertEquals("go", store.getCustomTarget(MODULE));

    store.setCustomTarget(MODULE, "   ");
    assertNull(store.getCustomTarget(MODULE));
  }

  @Test
  @DisplayName("compilation server participation defaults on and can be toggled")
  public void compilationServerParticipationDefaultsOnAndCanBeToggled() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    assertTrue(store.isUsingCompilationServer(MODULE));

    store.setUsingCompilationServer(MODULE, false);
    assertFalse(store.isUsingCompilationServer(MODULE));

    store.setUsingCompilationServer(MODULE, true);
    assertTrue(store.isUsingCompilationServer(MODULE));
  }

  @Test
  @DisplayName("compile command can be set and cleared")
  public void compileCommandCanBeSetAndCleared() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    assertNull(store.getCompileCommand(MODULE));

    store.setCompileCommand(MODULE, new HaxeEnvironmentStore.CompileCommand("/p/project.xml", null, "-clean -debug"));
    assertEquals(new HaxeEnvironmentStore.CompileCommand("/p/project.xml", null, "-clean -debug"),
                 store.getCompileCommand(MODULE));

    store.setCompileCommand(MODULE, null);
    assertNull(store.getCompileCommand(MODULE));
  }

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.setSdkName(MODULE, "Haxe 4.3.7");
    store.setDefines(MODULE, List.of(new EnvironmentDefine("analyzer-optimize", "", DefineEffect.SET),
                                     new EnvironmentDefine("no-traces", "", DefineEffect.REMOVE)));
    store.setCustomTarget(MODULE, "go");
    store.setCompileCommand(MODULE, new HaxeEnvironmentStore.CompileCommand("/p/build.hxml", "compile", "-debug"));

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeEnvironmentStore.State deserialized = XmlSerializer.deserialize(serialized, HaxeEnvironmentStore.State.class);

    HaxeEnvironmentStore reloaded = new HaxeEnvironmentStore();
    reloaded.loadState(deserialized);

    assertEquals("Haxe 4.3.7", reloaded.getSdkName(MODULE));
    assertEquals(List.of(new EnvironmentDefine("analyzer-optimize", "", DefineEffect.SET),
                         new EnvironmentDefine("no-traces", "", DefineEffect.REMOVE)),
                 reloaded.getDefines(MODULE));
    assertEquals("go", reloaded.getCustomTarget(MODULE));
    assertEquals(new HaxeEnvironmentStore.CompileCommand("/p/build.hxml", "compile", "-debug"), reloaded.getCompileCommand(MODULE));
  }
}
