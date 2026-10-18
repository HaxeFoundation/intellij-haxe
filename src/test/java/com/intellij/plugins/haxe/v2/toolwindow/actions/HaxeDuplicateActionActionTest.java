package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Tool window: duplicate action")
public class HaxeDuplicateActionActionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("copy names number up past the names the file uses")
  public void testCopyNamesNumberUpPastTheNamesTheFileUses() {
    assertEquals("Build (copy)", HaxeDuplicateActionAction.copyName("Build", Set.of("Build")));
    assertEquals("Build (copy 2)", HaxeDuplicateActionAction.copyName("Build", Set.of("Build", "Build (copy)")));
    assertEquals("Build (copy 3)", HaxeDuplicateActionAction.copyName("Build", Set.of("Build (copy)", "Build (copy 2)")));
  }
}
