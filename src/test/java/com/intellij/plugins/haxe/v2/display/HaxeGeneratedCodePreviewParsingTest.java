package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Compiler services: generated code preview parsing")
public class HaxeGeneratedCodePreviewParsingTest extends HaxeLightFixtureTestCase {

  private static final String TEMPORARIES_HX_NAME = "Temporaries.hx";
  /** The compiler's temporaries as -D dump=pretty prints them: a map literal's map and a catch-all's exception. */
  private static final String TEMPORARIES_HX_SOURCE = """
    class Temporaries {
      static function main() {
        var ` = new haxe.ds.StringMap();
        `.set("a", 1);
        try {
          throw "x";
        } catch (`:haxe.ValueException) {
          `trace("caught", null);
        }
      }
    }
    """;
  private static final String CATCH_LIST_HX_NAME = "CatchList.hx";
  /** The printer's catch list: a comma between the clauses, which no Haxe parser accepts. */
  private static final String CATCH_LIST_HX_SOURCE = """
    class CatchList {
      static function main() {
        try {
          throw "x";
        } catch (e:String) {
        },  catch (`:haxe.ValueException) {
        }
      }
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("compiler temporaries parse as names")
  public void testCompilerTemporariesParseAsNames() {
    PsiFile file = myFixture.configureByText(TEMPORARIES_HX_NAME, TEMPORARIES_HX_SOURCE);

    PsiErrorElement error = PsiTreeUtil.findChildOfType(file, PsiErrorElement.class);
    assertNull(error, () -> "unexpected parse error: " + error.getErrorDescription());
  }

  @Test
  @DisplayName("temporary uses resolve to the declaration")
  public void testTemporaryUsesResolveToTheDeclaration() {
    PsiFile file = myFixture.configureByText(TEMPORARIES_HX_NAME, TEMPORARIES_HX_SOURCE);
    HaxeReferenceExpression use = referenceNamed(file, "`");

    PsiElement resolved = use.resolve();

    HaxeLocalVarDeclaration declaration = PsiTreeUtil.getParentOfType(resolved, HaxeLocalVarDeclaration.class, false);
    assertNotNull(declaration, "the map literal's temporary resolves to its var");
    assertEquals("`", declaration.getName());
  }

  @Test
  @DisplayName("a preview hides its parse errors and a source file keeps them")
  public void testAPreviewHidesItsParseErrorsAndASourceFileKeepsThem() {
    PsiErrorElement inSource = firstError(myFixture.configureByText(CATCH_LIST_HX_NAME, CATCH_LIST_HX_SOURCE));
    PsiErrorElement inPreview = firstError(previewFile(CATCH_LIST_HX_SOURCE));

    HaxeGeneratedPreviewErrorFilter filter = new HaxeGeneratedPreviewErrorFilter();
    assertTrue(filter.shouldHighlightErrorElement(inSource));
    assertFalse(filter.shouldHighlightErrorElement(inPreview));
  }

  private static HaxeReferenceExpression referenceNamed(PsiFile file, String name) {
    for (HaxeReferenceExpression reference : PsiTreeUtil.findChildrenOfType(file, HaxeReferenceExpression.class)) {
      if (reference.getText().equals(name)) return reference;
    }
    throw new AssertionError("no reference named " + name);
  }

  private PsiFile previewFile(String text) {
    LightVirtualFile file = new LightVirtualFile("CatchList.generated.hx", HaxeLanguage.INSTANCE, text);
    file.putUserData(HaxeGeneratedCodePreview.PREVIEW_KEY, "CatchList");
    return PsiManager.getInstance(getProject()).findFile(file);
  }

  private static PsiErrorElement firstError(PsiFile file) {
    PsiErrorElement error = PsiTreeUtil.findChildOfType(file, PsiErrorElement.class);
    assertNotNull(error, "the printer's catch list does not parse");
    return error;
  }
}
