package com.intellij.plugins.haxe.resolve;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.psi.PsiReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A class whose supertypes do not resolve (a missing haxelib, a wrong
 * classpath) must stay cheap to resolve members in: each missing supertype is
 * looked up once per resolve, never once per permutation of the list.
 */
@DisplayName("Resolve: unresolvable supertypes")
public class HaxeUnresolvableSupertypesTest extends HaxeLightFixtureTestCase {

  /** Generous next to the linear cost (one run per supertype plus the reference itself); the permutation blow-up runs thousands. */
  private static final int LINEAR_RUNS_BOUND = 40;

  private static final String SIX_MISSING_SUPERTYPES_SOURCE = """
    class Main extends MissingBase implements MissingOne implements MissingTwo
        implements MissingThree implements MissingFour implements MissingFive {
      function run() {
        unkn<caret>own();
      }
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a member lookup walks each missing supertype once")
  public void testAMemberLookupWalksEachMissingSupertypeOnce() {
    myFixture.configureByText("Main.hx", SIX_MISSING_SUPERTYPES_SOURCE);
    HaxeResolver resolver = HaxeResolver.getInstance(getProject());
    PsiReference reference = myFixture.getReferenceAtCaretPosition();
    assertNotNull(reference);

    int before = resolver.pipelineRuns();
    assertNull(reference.resolve());
    int runs = resolver.pipelineRuns() - before;

    assertTrue(runs <= LINEAR_RUNS_BOUND, "pipeline runs for one unresolvable member: " + runs);
  }
}
