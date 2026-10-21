package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
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

@DisplayName("Navigation: generated preview spot")
public class HaxeGeneratedPreviewSpotTest extends HaxeLightFixtureTestCase {

  private static final String LEXER_HX_NAME = "L.hx";
  /** A rule set as hxparse writes it: the build macro binds {@code lexer}, the source never declares it. */
  private static final String LEXER_HX_SOURCE = """
    package a.b;
    class L {
    	static var string2 = ["x" => lexer.curPos()];
    	static function run() {}
    }
    class Helper {}
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a reference inside a member spots its module type member and local")
  public void testAReferenceInsideAMemberSpotsItsModuleTypeMemberAndLocal() {
    HaxeFile file = (HaxeFile)myFixture.configureByText(LEXER_HX_NAME, LEXER_HX_SOURCE);
    HaxeReferenceExpression lexer = referenceNamed(file, "lexer");

    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.around(lexer, "lexer");

    assertEquals(new HaxeGeneratedPreviewSpot("a.b.L", "a.b.L", "string2", "lexer"), spot);
    assertEquals("lexer", spot.title());
  }

  @Test
  @DisplayName("a secondary type keeps the module of its file")
  public void testASecondaryTypeKeepsTheModuleOfItsFile() {
    HaxeFile file = (HaxeFile)myFixture.configureByText(LEXER_HX_NAME, LEXER_HX_SOURCE);
    HaxeClass helper = file.getClassList().get(1);

    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.of(helper, null, null);

    assertEquals(new HaxeGeneratedPreviewSpot("a.b.L", "a.b.Helper", null, null), spot);
    assertEquals("a.b.Helper", spot.title());
  }

  @Test
  @DisplayName("the enclosing member is the class body child")
  public void testTheEnclosingMemberIsTheClassBodyChild() {
    HaxeFile file = (HaxeFile)myFixture.configureByText(LEXER_HX_NAME, LEXER_HX_SOURCE);
    HaxeReferenceExpression lexer = referenceNamed(file, "lexer");
    HaxeClass lexerClass = file.getClassList().get(0);

    HaxeNamedComponent member = HaxeGeneratedPreviewSpot.enclosingMember(lexer);

    assertNotNull(member);
    assertEquals("string2", member.getName());
    assertNull(HaxeGeneratedPreviewSpot.enclosingMember(lexerClass), "the class itself sits in no member");
  }

  @Test
  @DisplayName("an unplaceable name targets the preview of its member")
  public void testAnUnplaceableNameTargetsThePreviewOfItsMember() {
    HaxeFile file = (HaxeFile)myFixture.configureByText(LEXER_HX_NAME, LEXER_HX_SOURCE);
    HaxeReferenceExpression lexer = referenceNamed(file, "lexer");
    var overrides = HaxeDisplayConfiguration.OverrideArguments.EMPTY;
    var context = new HaxeCompilerDisplayService.DisplayContext(List.of(), null, null, overrides, "container");

    PsiElement target = HaxeCompilerGotoDeclarationHandler.generatedDeclarationOf(lexer, context);

    PomTargetPsiElement element = assertInstanceOf(PomTargetPsiElement.class, target);
    HaxeGeneratedPreviewTarget previewTarget = assertInstanceOf(HaxeGeneratedPreviewTarget.class, element.getTarget());
    assertEquals(new HaxeGeneratedPreviewSpot("a.b.L", "a.b.L", "string2", "lexer"), previewTarget.spot());
  }

  @Test
  @DisplayName("a name the resolver places gets no preview target")
  public void testANameTheResolverPlacesGetsNoPreviewTarget() {
    String source = LEXER_HX_SOURCE.replace("lexer.curPos()", "run()");
    PsiFile file = myFixture.configureByText(LEXER_HX_NAME, source);
    HaxeReferenceExpression run = referenceNamed(file, "run");
    var overrides = HaxeDisplayConfiguration.OverrideArguments.EMPTY;
    var context = new HaxeCompilerDisplayService.DisplayContext(List.of(), null, null, overrides, "container");

    assertNull(HaxeCompilerGotoDeclarationHandler.generatedDeclarationOf(run, context));
  }

  private static HaxeReferenceExpression referenceNamed(PsiFile file, String name) {
    for (HaxeReferenceExpression reference : PsiTreeUtil.findChildrenOfType(file, HaxeReferenceExpression.class)) {
      if (name.equals(reference.getText())) return reference;
    }
    throw new AssertionError("no reference " + name);
  }
}
