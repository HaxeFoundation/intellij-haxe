package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.usageView.UsageInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Compiler services: locations")
public class HaxeCompilerLocationsTest extends HaxeLightFixtureTestCase {

  private static final String MAIN_HX_SOURCE = """
    class Main {
    	static var total = 0.0;
    	static function main() {
    		helper();
    		total += weight(1);
    		Main.total *= 8.0;
    	}
    	static function helper() {}
    	static function weight(n:Int):Float return n * 0.5;
    }
    """;
  /** The compiler's 0-based positions of the call and the declaration of helper in the source above. */
  private static final Range HELPER_CALL = rangeOf(3, 2, 8);
  private static final Range HELPER_DECLARATION = rangeOf(7, 17, 23);
  /** Where the compiler puts the left-hand field of a compound assignment: the last five characters of the whole expression. */
  private static final Range TOTAL_COMPOUND_ASSIGNMENT = rangeOf(4, 15, 20);
  private static final Range QUALIFIED_TOTAL_COMPOUND_ASSIGNMENT = rangeOf(5, 14, 19);
  private static final Range PAST_THE_END = rangeOf(40, 0, 3);

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a usage location becomes the reference there")
  public void testAUsageLocationBecomesTheReferenceThere() {
    PsiFile file = myFixture.configureByText("Main.hx", MAIN_HX_SOURCE);

    UsageInfo usage = HaxeCompilerLocations.usageIn(file, HELPER_CALL, "helper");

    assertNotNull(usage);
    assertNotNull(usage.getReference(), "the call is a reference the platform knows");
    PsiElement resolved = usage.getReference().resolve();
    assertInstanceOf(HaxeMethod.class, resolved);
    assertEquals("helper", ((HaxeMethod)resolved).getName());
  }

  @Test
  @DisplayName("a compound assignment location becomes its left hand reference")
  public void testACompoundAssignmentLocationBecomesItsLeftHandReference() {
    PsiFile file = myFixture.configureByText("Main.hx", MAIN_HX_SOURCE);

    UsageInfo plain = HaxeCompilerLocations.usageIn(file, TOTAL_COMPOUND_ASSIGNMENT, "total");
    UsageInfo qualified = HaxeCompilerLocations.usageIn(file, QUALIFIED_TOTAL_COMPOUND_ASSIGNMENT, "total");

    assertNotNull(plain);
    assertNotNull(plain.getReference());
    assertEquals("total", plain.getReference().getElement().getText());
    assertNotNull(qualified);
    assertNotNull(qualified.getReference());
    assertEquals("Main.total", qualified.getReference().getElement().getText());
  }

  @Test
  @DisplayName("a declaration location becomes the named component")
  public void testADeclarationLocationBecomesTheNamedComponent() {
    PsiFile file = myFixture.configureByText("Main.hx", MAIN_HX_SOURCE);

    PsiElement declaration = HaxeCompilerLocations.declarationIn(file, HELPER_DECLARATION);

    assertInstanceOf(HaxeNamedComponent.class, declaration);
    assertEquals("helper", ((HaxeNamedComponent)declaration).getName());
  }

  @Test
  @DisplayName("a location past the document is dropped")
  public void testALocationPastTheDocumentIsDropped() {
    PsiFile file = myFixture.configureByText("Main.hx", MAIN_HX_SOURCE);

    assertNull(HaxeCompilerLocations.usageIn(file, PAST_THE_END, "helper"));
    assertNull(HaxeCompilerLocations.declarationIn(file, PAST_THE_END));
  }

  private static Range rangeOf(int line, int startCharacter, int endCharacter) {
    return new Range(new Position(line, startCharacter), new Position(line, endCharacter));
  }
}
