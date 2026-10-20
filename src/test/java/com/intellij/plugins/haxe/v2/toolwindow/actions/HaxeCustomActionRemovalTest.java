package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.plugins.haxe.v2.toolwindow.actions.HaxeCustomActionRemoval.Decisions;
import com.intellij.plugins.haxe.v2.toolwindow.actions.HaxeCustomActionRemoval.References;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Tool window: custom action removal")
public class HaxeCustomActionRemovalTest {

  private static final String FILE = "/p/build.hxml";

  /** (second decision, prepare still present afterwards, release still runs it first afterwards). */
  static final List<Arguments> REFERENCE_DECISIONS = List.of(
    arguments(References.REMOVE, false, false),
    // No keeps the reference: release shows prepare as missing
    arguments(References.KEEP, false, true),
    arguments(References.CANCEL, true, true));

  @ParameterizedTest(name = "{0}")
  @FieldSource("REFERENCE_DECISIONS")
  @DisplayName("the second decision rules the references of the actions that run it first")
  public void testTheSecondDecisionRulesTheReferencesOfTheActionsThatRunItFirst(References decision, boolean kept, boolean referenced) {
    HaxeCustomActionsStore store = storeWithPrepareAndRelease();

    boolean removed = HaxeCustomActionRemoval.remove(store, FILE, "prepare", new ScriptedDecisions(true, decision));

    assertEquals(!kept, removed);
    assertEquals(kept, hasAction(store, "prepare"));
    assertEquals(referenced, releaseRunsPrepareFirst(store));
  }

  @Test
  @DisplayName("a declined confirmation changes nothing")
  public void testADeclinedConfirmationChangesNothing() {
    HaxeCustomActionsStore store = storeWithPrepareAndRelease();

    boolean removed = HaxeCustomActionRemoval.remove(store, FILE, "prepare", new ScriptedDecisions(false, References.REMOVE));

    assertFalse(removed);
    assertTrue(hasAction(store, "prepare"));
    assertTrue(releaseRunsPrepareFirst(store));
  }

  @Test
  @DisplayName("an action nobody runs first is removed without the second question")
  public void testAnActionNobodyRunsFirstIsRemovedWithoutTheSecondQuestion() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("docs", "cmd-docs", ""));

    boolean removed = HaxeCustomActionRemoval.remove(store, FILE, "docs", new ScriptedDecisions(true, null));

    assertTrue(removed);
    assertFalse(hasAction(store, "docs"));
  }

  private static HaxeCustomActionsStore storeWithPrepareAndRelease() {
    HaxeCustomActionsStore store = new HaxeCustomActionsStore();
    store.addAction(FILE, new CustomAction("prepare", "cmd-prepare", ""));
    store.addAction(FILE, new CustomAction("release", "cmd-release", "", null, List.of(new ActionReference(null, "prepare"))));
    return store;
  }

  private static boolean hasAction(HaxeCustomActionsStore store, String name) {
    return store.getActions(FILE).stream().anyMatch(action -> action.name().equals(name));
  }

  private static boolean releaseRunsPrepareFirst(HaxeCustomActionsStore store) {
    return !store.dependents(FILE, "prepare").isEmpty();
  }

  /** Answers fixed in advance; a null second answer means the second question must not be asked. */
  private record ScriptedDecisions(boolean confirm, @Nullable References decision) implements Decisions {

    @Override
    public boolean confirmRemoval(@NotNull String name) {
      return confirm;
    }

    @Override
    public @NotNull References references(@NotNull String name, @NotNull List<ActionReference> dependents) {
      assertNotNull(decision, "the references question must not be asked");
      return decision;
    }
  }
}
