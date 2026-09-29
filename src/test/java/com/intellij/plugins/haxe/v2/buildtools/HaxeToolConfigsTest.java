package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Build tools: tool config discovery")
public class HaxeToolConfigsTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    // fixtures are built from text; no test-data directory
    return "";
  }

  @Test
  @DisplayName("finds the nearest ancestor holding the config")
  public void testFindsTheNearestAncestorHoldingTheConfig() {
    myFixture.addFileToProject("hxformat.json", "{}");
    myFixture.addFileToProject("sub/hxformat.json", "{}");
    PsiFile deep = myFixture.addFileToProject("sub/pack/Deep.hx", "class Deep {}");
    PsiFile top = myFixture.addFileToProject("Top.hx", "class Top {}");

    VirtualFile nearest = HaxeToolConfigs.findConfigDirectory(getProject(), deep.getVirtualFile(), HaxeToolConfigs.FORMATTER_CONFIG_NAME);
    assertNotNull(nearest);
    assertEquals("sub", nearest.getName());

    VirtualFile root = HaxeToolConfigs.findConfigDirectory(getProject(), top.getVirtualFile(), HaxeToolConfigs.FORMATTER_CONFIG_NAME);
    assertNotNull(root);
    assertEquals(top.getVirtualFile().getParent(), root);
  }

  @Test
  @DisplayName("no config anywhere yields null")
  public void testNoConfigAnywhereYieldsNull() {
    PsiFile file = myFixture.addFileToProject("pack/Lone.hx", "class Lone {}");

    VirtualFile found = HaxeToolConfigs.findConfigDirectory(getProject(), file.getVirtualFile(), HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME);
    assertNull(found);
  }
}
