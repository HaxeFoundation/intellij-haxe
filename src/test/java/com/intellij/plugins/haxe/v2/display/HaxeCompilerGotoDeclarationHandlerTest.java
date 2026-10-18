package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Navigation: compiler goto declaration handler")
public class HaxeCompilerGotoDeclarationHandlerTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    // the fixtures are built from text; no test-data directory
    return "";
  }

  @Test
  @DisplayName("blueprint member opens the generated preview")
  public void testBlueprintMemberOpensTheGeneratedPreview() {
    String blueprintText = "extern class Config { public function setWidth(value:Int):Void; }";
    HaxeFile blueprint = HaxeElementGenerator.createFile(getProject(), "Config", blueprintText);
    blueprint.putUserData(HaxeCompilerResolveService.BLUEPRINT_DOT_PATH, "Config");
    HaxeMethod setter = PsiTreeUtil.findChildOfType(blueprint, HaxeMethod.class);

    PsiElement element = HaxeCompilerGotoDeclarationHandler.blueprintMemberPreviewOf(setter, "setWidth", context());

    PomTargetPsiElement wrapper = assertInstanceOf(PomTargetPsiElement.class, element);
    HaxeGeneratedPreviewTarget target = assertInstanceOf(HaxeGeneratedPreviewTarget.class, wrapper.getTarget());
    assertEquals(HaxeGeneratedPreviewSpot.ofType("Config", "setWidth"), target.spot());
  }

  @Test
  @DisplayName("source member has no generated preview")
  public void testSourceMemberHasNoGeneratedPreview() {
    PsiFile file = myFixture.configureByText("Config.hx", "class Config { public function setWidth(value:Int):Void {} }");
    HaxeMethod setter = PsiTreeUtil.findChildOfType(file, HaxeMethod.class);

    PsiElement element = HaxeCompilerGotoDeclarationHandler.blueprintMemberPreviewOf(setter, "setWidth", context());

    assertNull(element, "a member with a source declaration navigates to that declaration");
  }

  private static HaxeCompilerDisplayService.DisplayContext context() {
    var overrides = HaxeDisplayConfiguration.OverrideArguments.EMPTY;
    return new HaxeCompilerDisplayService.DisplayContext(List.of(), null, null, overrides, "container");
  }
}
