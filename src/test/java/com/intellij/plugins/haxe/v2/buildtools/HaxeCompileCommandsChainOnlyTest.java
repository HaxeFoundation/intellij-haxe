package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands.NamedAction;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands.Resolved;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands.RunBefore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A custom action without a command is a chain of other actions: it resolves, contributes nothing of its own to a chain, and an unknown action still resolves to nothing. */
@DisplayName("Build tools: compile commands chain only")
public class HaxeCompileCommandsChainOnlyTest extends HaxeCodeInsightFixtureTestCase {
  private static final String BUILD_HXML_SOURCE = """
    -cp src
    -main Main
    --interp
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("an action without a command resolves chain-only and runs nothing itself")
  public void testAnActionWithoutACommandResolvesChainOnlyAndRunsNothingItself() {
    VirtualFile build = myFixture.addFileToProject("build.hxml", BUILD_HXML_SOURCE).getVirtualFile();
    String buildPath = build.getPath();
    HaxeCustomActionsStore store = HaxeCustomActionsStore.getInstance(getProject());
    store.addAction(buildPath, new CustomAction("prepare", "echo prepare", ""));
    store.addAction(buildPath, new CustomAction("all", "", "", null, List.of(new ActionReference(null, "prepare"))));
    store.addAction(buildPath, new CustomAction("ship", "", "", null, List.of(new ActionReference(null, "all"))));

    Resolved all = resolveAction(buildPath, "all");
    Resolved unknown = resolveAction(buildPath, "nope");
    RunBefore allRunsFirst = resolveRunBefore(buildPath, "all");
    RunBefore shipRunsFirst = resolveRunBefore(buildPath, "ship");

    assertNotNull(all, "a known action resolves although it has no command");
    assertTrue(all.chainOnly());
    assertNull(unknown, "an unknown action still resolves to nothing");
    assertNull(allRunsFirst.problem());
    assertEquals(List.of("prepare"), names(allRunsFirst));
    assertNull(shipRunsFirst.problem());
    assertEquals(List.of("prepare"), names(shipRunsFirst), "the chain-only 'all' contributes its actions, not itself");
  }

  private Resolved resolveAction(String buildPath, String action) {
    return HaxeReadActions.compute(() -> HaxeCompileCommands.resolveAction(getProject(), buildPath, action, ""));
  }

  private RunBefore resolveRunBefore(String buildPath, String action) {
    return HaxeReadActions.compute(() -> HaxeCompileCommands.resolveRunBefore(getProject(), buildPath, action));
  }

  private static List<String> names(RunBefore runBefore) {
    return runBefore.actions().stream()
      .map(NamedAction::name)
      .toList();
  }
}
