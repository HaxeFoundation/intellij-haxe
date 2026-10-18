package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: environment store")
public class HaxeEnvironmentStoreTest {

  private static final String MODULE = "app";

  @Test
  @DisplayName("defaults are empty")
  public void defaultsAreEmpty() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    assertNull(store.getSdkName(MODULE));
    assertFalse(store.hasUserOverrides(MODULE));
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

    store.setCompileCommand("mod", new HaxeEnvironmentStore.CompileCommand("build.hxml", null, ""));
    assertTrue(store.hasUserOverrides("mod"));
    store.setCompileCommand("mod", null);
    assertFalse(store.hasUserOverrides("mod"));

    store.skipBuild("mod");
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
  @DisplayName("skipping the build clears the command until one is set")
  public void skippingTheBuildClearsTheCommandUntilOneIsSet() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.setCompileCommand(MODULE, new HaxeEnvironmentStore.CompileCommand("/p/build.hxml", null, "-debug"));

    store.skipBuild(MODULE);
    assertTrue(store.isBuildSkipped(MODULE));
    assertNull(store.getCompileCommand(MODULE), "a skipped container has no command");

    store.setCompileCommand(MODULE, null);
    assertFalse(store.isBuildSkipped(MODULE), "returning to the default builds again");
  }

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.setSdkName(MODULE, "Haxe 4.3.7");
    store.setCompileCommand(MODULE, new HaxeEnvironmentStore.CompileCommand("/p/build.hxml", "compile", "-debug"));

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeEnvironmentStore.State deserialized = XmlSerializer.deserialize(serialized, HaxeEnvironmentStore.State.class);

    HaxeEnvironmentStore reloaded = new HaxeEnvironmentStore();
    reloaded.loadState(deserialized);

    assertEquals("Haxe 4.3.7", reloaded.getSdkName(MODULE));
    assertEquals(new HaxeEnvironmentStore.CompileCommand("/p/build.hxml", "compile", "-debug"), reloaded.getCompileCommand(MODULE));
  }

  /** The layout before version 1: define entries and the custom target were per container. */
  private static final String UNVERSIONED_STATE_XML = """
    <State>
      <option name="environments">
        <list>
          <ContainerEnvironment>
            <option name="containerId" value="app" />
            <option name="sdkName" value="Haxe 4.3.7" />
            <option name="defines">
              <list>
                <DefineState>
                  <option name="name" value="debug" />
                </DefineState>
              </list>
            </option>
            <option name="customTarget" value="go" />
          </ContainerEnvironment>
        </list>
      </option>
    </State>
    """;

  @Test
  @DisplayName("an unversioned layout loads and drops the moved options on save")
  public void testAnUnversionedLayoutLoadsAndDropsTheMovedOptionsOnSave() throws IOException, JDOMException {
    Element stored = JDOMUtil.load(UNVERSIONED_STATE_XML);
    HaxeEnvironmentStore.State loaded = XmlSerializer.deserialize(stored, HaxeEnvironmentStore.State.class);
    int loadedVersion = loaded.version;
    HaxeEnvironmentStore store = new HaxeEnvironmentStore();
    store.loadState(loaded);

    String saved = JDOMUtil.writeElement(XmlSerializer.serialize(store.getState()));

    assertEquals(0, loadedVersion, "a layout without the field reads as version 0");
    assertEquals("Haxe 4.3.7", store.getSdkName(MODULE));
    assertFalse(saved.contains("defines") || saved.contains("customTarget"), "the moved options are not written back, got: " + saved);
    assertTrue(saved.contains("<option name=\"version\" value=\"" + HaxeEnvironmentStore.STATE_VERSION + "\""),
               "the saved layout carries its version, got: " + saved);
  }
}
