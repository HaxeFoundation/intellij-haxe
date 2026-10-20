package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("Compiler services: display context cache")
public class HaxeDisplayContextCacheTest extends HaxeCodeInsightFixtureTestCase {

  private static final String WEB_HXML_SOURCE = """
    -cp src
    -main WebMain
    -js bin/web.js
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a display context is reused until a build setting changes")
  public void testADisplayContextIsReusedUntilABuildSettingChanges() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile webMain = myFixture.addFileToProject("src/WebMain.hx", "class WebMain {}").getVirtualFile();
    HaxeCompilerDisplayService service = HaxeCompilerDisplayService.getInstance(getProject());

    DisplayContext first = HaxeReadActions.compute(() -> service.contextFor(webMain));
    assertNotNull(first, "web.hxml gives WebMain a display context");
    assertSame(first, HaxeReadActions.compute(() -> service.contextFor(webMain)), "nothing changed: the cached context answers");

    HaxeIdeOverridesStore.getInstance(getProject()).putDefine(web.getPath(), "foo", "");
    DisplayContext changed = HaxeReadActions.compute(() -> service.contextFor(webMain));

    assertNotNull(changed);
    assertEquals(List.of("-D", "foo"), changed.overrides().defineArgs(), "the new IDE define reaches the recomputed context");
  }
}
