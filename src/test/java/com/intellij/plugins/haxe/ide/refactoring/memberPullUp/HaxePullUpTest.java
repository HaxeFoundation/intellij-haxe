package com.intellij.plugins.haxe.ide.refactoring.memberPullUp;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Refactoring: pull up")
public class HaxePullUpTest extends HaxeLightFixtureTestCase {

  private static final String MAIN_HX_NAME = "Main.hx";
  private static final String OVERRIDING_METHOD_HX_SOURCE = """
    interface Drawable {
    }
    class Base {
    	public function draw():Void {}
    }
    class Shape extends Base implements Drawable {
    	override public function draw():Void {}
    	public function name() return "shape";
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a method pulled up into an interface becomes a prototype without override")
  public void testAMethodPulledUpIntoAnInterfaceBecomesAPrototypeWithoutOverride() {
    myFixture.configureByText(MAIN_HX_NAME, OVERRIDING_METHOD_HX_SOURCE);
    HaxeClass shape = classNamed("Shape");
    HaxeClass drawable = classNamed("Drawable");
    MemberInfo[] members = {new MemberInfo(methodOf(shape, "draw")), new MemberInfo(methodOf(shape, "name"))};

    new PullUpProcessor(shape, drawable, members, new DocCommentPolicy(DocCommentPolicy.ASIS)).run();

    String interfaceText = drawable.getText();
    assertTrue(interfaceText.contains("public function draw():Void;"), interfaceText);
    assertTrue(interfaceText.contains("public function name();"), interfaceText);
    assertFalse(interfaceText.contains("override"), interfaceText);
    assertFalse(interfaceText.contains("return"), interfaceText);
    String classText = classNamed("Shape").getText();
    assertFalse(classText.contains("draw"), classText);
    assertFalse(classText.contains("name"), classText);
  }

  private static PsiMethod methodOf(HaxeClass owner, String name) {
    PsiMethod[] methods = owner.findMethodsByName(name, false);
    assertTrue(methods.length == 1, owner.getName() + " declares " + name + " once");
    return methods[0];
  }

  private HaxeClass classNamed(String name) {
    for (HaxeClass haxeClass : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), HaxeClass.class)) {
      if (name.equals(haxeClass.getName())) return haxeClass;
    }
    throw new AssertionError("no class " + name);
  }
}
