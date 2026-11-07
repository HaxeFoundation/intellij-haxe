package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

  @Test
  @DisplayName("a string literal is not asked")
  public void testAStringLiteralIsNotAsked() {
    PsiFile file = myFixture.configureByText("Main.hx", "@:native(\"foo.Bar\") class Main { var field:Config; }");
    PsiElement inString = file.findElementAt(file.getText().indexOf("foo"));
    PsiElement inCode = file.findElementAt(file.getText().indexOf("Config"));

    assertNull(HaxeCompilerGotoDeclarationHandler.referenceToAsk(inString), "the string-link references place a name in a string");
    assertNotNull(HaxeCompilerGotoDeclarationHandler.referenceToAsk(inCode), "a reference in code is the compiler's to place");
  }

  @Test
  @DisplayName("a rejected name has no generated preview")
  public void testARejectedNameHasNoGeneratedPreview() {
    PsiFile file = myFixture.configureByText("Tests.hx", "class Tests { macro function test() { var value:TypeTools; return null; } }");
    HaxeReference reference = referenceAt(file, "TypeTools");

    PsiElement[] targets = HaxeCompilerGotoDeclarationHandler.targetsOf(getProject(), null, reference, context());

    assertNull(targets, "a name the compiler rejects does not compile, so no macro generated it");
  }

  @Test
  @DisplayName("an unplaced name opens the generated preview")
  public void testAnUnplacedNameOpensTheGeneratedPreview() {
    PsiFile file = myFixture.configureByText("Tests.hx", "class Tests { static function run() { trace(it); } }");
    HaxeReference reference = referenceAt(file, "it)");

    PsiElement[] targets = HaxeCompilerGotoDeclarationHandler.targetsOf(getProject(), List.of(), reference, context());

    assertNotNull(targets, "a name the server answered for without a place is one a macro generated");
    assertInstanceOf(PomTargetPsiElement.class, targets[0]);
  }

  private static HaxeReference referenceAt(PsiFile file, String text) {
    HaxeReference reference = HaxeCompilerGotoDeclarationHandler.referenceToAsk(file.findElementAt(file.getText().indexOf(text)));
    assertNotNull(reference, "no reference at " + text);
    return reference;
  }

  private static HaxeCompilerDisplayService.DisplayContext context() {
    var overrides = HaxeDisplayConfiguration.OverrideArguments.EMPTY;
    return new HaxeCompilerDisplayService.DisplayContext(List.of(), null, null, overrides, "container");
  }
}
