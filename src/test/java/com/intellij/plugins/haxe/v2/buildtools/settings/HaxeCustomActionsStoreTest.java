package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.RunBeforeRefusal;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: custom actions store")
public class HaxeCustomActionsStoreTest {

  private static final String FILE = "/p/build.hxml";
  private static final String OTHER_FILE = "/p/lib/build.hxml";

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
  @DisplayName("launching actions survive the xml round trip, created and existing alike")
  public void aLaunchingActionSurvivesTheXmlRoundTripWithItsKindParametersAndDebugFlag() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    Launch launch = new Launch.Create("HASHLINK", Map.of("hlFile", "dist/final.hl"), false);
    store.addAction(FILE, new CustomAction("final", "haxe final.hxml", "", launch));
    store.addAction(FILE, new CustomAction("deploy", "", "", new Launch.Existing("Deploy (Gradle)", true)));
    store.addAction(FILE, new CustomAction("docs", "haxe doc.hxml", ""));

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeCustomActionsStore reloaded = new HaxeCustomActionsStore();
    reloaded.loadState(XmlSerializer.deserialize(serialized, HaxeCustomActionsStore.State.class));

    assertEquals(List.of(new CustomAction("final", "haxe final.hxml", "", launch),
                         new CustomAction("deploy", "", "", new Launch.Existing("Deploy (Gradle)", true)),
                         new CustomAction("docs", "haxe doc.hxml", "", null)),
                 reloaded.getActions(FILE));
  }

  @Test
  @DisplayName("run before chains expand depth first and run every action once")
  public void runBeforeChainsExpandDepthFirstAndRunEveryActionOnce() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("prepare", "cmd-prepare", "", null, List.of(own("Clean"))));
    store.addAction(FILE, new CustomAction("release", "cmd-release", "", null, List.of(own("prepare"), own("Clean"))));

    assertEquals(List.of(in(FILE, "Clean"), in(FILE, "prepare")), store.runBeforeChain(FILE, "release"));
    assertEquals(List.of(in(FILE, "Clean")), store.runBeforeChain(FILE, "prepare"));
    assertEquals(List.of(), store.runBeforeChain(FILE, "Build"), "a built-in runs nothing first");
  }

  @Test
  @DisplayName("a chain crosses build files and each runs its own references")
  public void aChainCrossesBuildFilesAndEachRunsItsOwnReferences() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(OTHER_FILE, new CustomAction("prepare", "cmd-prepare", "", null, List.of(own("Clean"))));
    store.addAction(FILE, new CustomAction("release", "cmd-release", "", null, List.of(in(OTHER_FILE, "prepare"))));

    assertEquals(List.of(in(OTHER_FILE, "Clean"), in(OTHER_FILE, "prepare")), store.runBeforeChain(FILE, "release"),
                 "the other file's own reference means its own Clean");
  }

  @Test
  @DisplayName("a loop in the run before references has no chain, across build files too")
  public void aLoopInTheRunBeforeReferencesHasNoChainAcrossBuildFilesToo() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("a", "cmd-a", "", null, List.of(in(OTHER_FILE, "b"))));
    store.addAction(OTHER_FILE, new CustomAction("b", "cmd-b", "", null, List.of(in(FILE, "a"))));

    assertNull(store.runBeforeChain(FILE, "a"));
    assertEquals(RunBeforeRefusal.LOOPS, store.refusal(in(FILE, "a"), FILE, "anything"), "a looping action is refused whatever it would run before");
  }

  @Test
  @DisplayName("references follow a rename and go with a removal, from any build file")
  public void referencesFollowARenameAndGoWithARemovalFromAnyBuildFile() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(OTHER_FILE, new CustomAction("prepare", "cmd-prepare", ""));
    store.addAction(FILE, new CustomAction("release", "cmd-release", "", null, List.of(own("Clean"), in(OTHER_FILE, "prepare"))));

    store.renameReferences(OTHER_FILE, "prepare", "setup");
    assertEquals(List.of(in(FILE, "Clean"), in(OTHER_FILE, "setup")), store.runBeforeChain(FILE, "release"));
    assertEquals(List.of(in(FILE, "release")), store.dependents(OTHER_FILE, "setup"));

    store.dropReferences(OTHER_FILE, "setup");
    assertEquals(List.of(in(FILE, "Clean")), store.runBeforeChain(FILE, "release"));
    assertTrue(store.dependents(OTHER_FILE, "setup").isEmpty());
  }

  @Test
  @DisplayName("moving a build file or its folder keeps its actions and the references to them")
  public void movingABuildFileOrItsFolderKeepsItsActionsAndTheReferencesToThem() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(OTHER_FILE, new CustomAction("prepare", "cmd-prepare", "", null, List.of(own("Clean"))));
    store.addAction(FILE, new CustomAction("release", "cmd-release", "", null, List.of(in(OTHER_FILE, "prepare"))));

    store.moveFile("/p/lib", "/p/libs/core");
    String moved = "/p/libs/core/build.hxml";
    assertEquals(List.of("prepare"), store.getActions(moved).stream().map(CustomAction::name).toList());
    assertEquals(List.of(in(moved, "Clean"), in(moved, "prepare")), store.runBeforeChain(FILE, "release"));

    store.moveFile(moved, "/p/libs/core/main.hxml");
    assertEquals(List.of("prepare"), store.getActions("/p/libs/core/main.hxml").stream().map(CustomAction::name).toList());
  }

  @Test
  @DisplayName("run before survives the xml round trip")
  public void runBeforeSurvivesTheXmlRoundTrip() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    CustomAction release = new CustomAction("release", "cmd-release", "", null, List.of(own("Clean"), in(OTHER_FILE, "prepare")));
    store.addAction(FILE, release);

    Element serialized = XmlSerializer.serialize(store.getState());
    HaxeCustomActionsStore reloaded = new HaxeCustomActionsStore();
    reloaded.loadState(XmlSerializer.deserialize(serialized, HaxeCustomActionsStore.State.class));

    assertEquals(List.of(release), reloaded.getActions(FILE));
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

  private static ActionReference own(String action) {
    return new ActionReference(null, action);
  }

  private static ActionReference in(String buildFile, String action) {
    return new ActionReference(buildFile, action);
  }
}
