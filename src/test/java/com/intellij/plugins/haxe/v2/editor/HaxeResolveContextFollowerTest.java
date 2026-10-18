package com.intellij.plugins.haxe.v2.editor;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContext;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContexts;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.testFramework.PlatformTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The editor events of a navigation reach {@link HaxeResolveContexts#follow} through the real editor manager. */
@DisplayName("Resolve context: follower")
public class HaxeResolveContextFollowerTest extends HaxeCodeInsightFixtureTestCase {
  private static final String WEB_HXML_SOURCE = """
    -cp src
    -main WebMain
    -js bin/web.js
    """;
  private static final String NODE_HXML_SOURCE = """
    -cp src
    -main NodeMain
    -js bin/node.js
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a file opened from an editor takes that editors context")
  public void testAFileOpenedFromAnEditorTakesThatEditorsContext() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile node = myFixture.addFileToProject("node.hxml", NODE_HXML_SOURCE).getVirtualFile();
    VirtualFile nodeMain = myFixture.addFileToProject("src/NodeMain.hx", "class NodeMain {}").getVirtualFile();
    VirtualFile shared = myFixture.addFileToProject("src/Shared.hx", "class Shared {}").getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    HaxeResolveContexts contexts = HaxeResolveContexts.getInstance(getProject());
    assertEquals(web.getPath(), contextPath(contexts, shared), "the active build before any navigation");

    myFixture.openFileInEditor(nodeMain);
    myFixture.openFileInEditor(shared);

    PlatformTestUtil.waitWithEventsDispatching("the opened file must follow the editor it was opened from",
                                               () -> contexts.isFollowed(shared), 10);
    assertEquals(node.getPath(), contextPath(contexts, shared));
    assertFalse(contexts.isPinned(shared));
  }

  private static String contextPath(HaxeResolveContexts contexts, VirtualFile file) {
    HaxeResolveContext context = contexts.contextFor(file);
    assertNotNull(context);
    return context.path();
  }
}
