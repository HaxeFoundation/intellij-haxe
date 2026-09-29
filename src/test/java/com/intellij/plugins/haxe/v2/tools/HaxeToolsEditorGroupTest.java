package com.intellij.plugins.haxe.v2.tools;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolConfigDetector;
import com.intellij.testFramework.TestActionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Build tools: tools editor group")
public class HaxeToolsEditorGroupTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    // fixtures are built from text; no test-data directory
    return "";
  }

  @Test
  @DisplayName("hidden until a tool config is detected, then shown")
  public void testHiddenUntilAToolConfigIsDetectedThenShown() throws Exception {
    myFixture.addFileToProject("hxformat.json", "{}");
    VirtualFile file = myFixture.addFileToProject("pack/Main.hx", "class Main {}").getVirtualFile();
    HaxeToolsEditorGroup group = new HaxeToolsEditorGroup();

    AnActionEvent firstOpening = TestActionEvent.createTestEvent(group, editorContextOn(file));
    group.update(firstOpening);
    assertFalse(firstOpening.getPresentation().isVisible(), "the first opening must not wait on the search");

    HaxeToolConfigDetector.getInstance(getProject()).awaitSearchesForTests();
    AnActionEvent nextOpening = TestActionEvent.createTestEvent(group, editorContextOn(file));
    group.update(nextOpening);
    assertTrue(nextOpening.getPresentation().isVisible(), "the detected config shows the tools from the next opening on");
  }

  @Test
  @DisplayName("stays hidden without any tool config")
  public void testStaysHiddenWithoutAnyToolConfig() throws Exception {
    VirtualFile file = myFixture.addFileToProject("pack/Main.hx", "class Main {}").getVirtualFile();
    HaxeToolsEditorGroup group = new HaxeToolsEditorGroup();
    group.update(TestActionEvent.createTestEvent(group, editorContextOn(file)));
    HaxeToolConfigDetector.getInstance(getProject()).awaitSearchesForTests();

    AnActionEvent nextOpening = TestActionEvent.createTestEvent(group, editorContextOn(file));
    group.update(nextOpening);
    assertFalse(nextOpening.getPresentation().isVisible());
  }

  private DataContext editorContextOn(VirtualFile file) {
    return SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, getProject())
      .add(CommonDataKeys.VIRTUAL_FILE, file)
      .build();
  }
}
