package com.intellij.plugins.haxe.resolve;

import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeEvaluationTaint;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.EnumValueMatchUtil;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeTestDefines;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each link of a chained extension call ({@code s.replace(..).replace(..)},
 * {@code replace} from {@code using StringTools}) resolves by evaluating its
 * receiver, the call before it. Preparing that receiver's evaluation must not
 * resolve the callee of the ENCLOSING call: that resolve is the one in
 * progress, and re-entering it answers empty and taints the chain's
 * evaluation, which is then never cached and repeats on every use.
 */
@DisplayName("Resolve: chained extension call")
public class HaxeChainedExtensionCallResolveTest extends HaxeToolkitLightFixtureTestCase {

  private static final String CHAIN_SOURCE = """
    using StringTools;
    class Main {
      static function escape(s:String):String {
        return s.replace("a", "b").replace("c", "d").rep<caret>lace("e", "f");
      }
    }
    """;

  private static final String MATCH_SOURCE = """
    enum Verdict {
      Pass(value:Int);
      Fail(message:String);
    }
    class Main {
      static function passed(verdict:Verdict):Bool {
        return verdict.ma<caret>tch(Pass(_));
      }
    }
    """;

  /** The shape of haxe.macro.Printer's escapeString: the chain sits between `+` operands and ends in a conditional link. */
  private static final String CONDITIONAL_CHAIN_SOURCE = """
    using StringTools;
    class Main {
      static function escape(s:String, delim:String) {
        return delim
          + s.replace("a", "b")
            .replace("c", "d")
            #if sys .replace("e", "f") #end + delim;
      }
    }
    """;

  private static final String LONG_CHAIN_SOURCE = """
    using StringTools;
    class Main {
      static function escape(s:String):String {
        return s.replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .replace("a", "b")
          .rep<caret>lace("a", "b");
      }
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      HaxeTestDefines.set(getProject(), null);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("the last link resolves to the extension method")
  public void testTheLastLinkResolvesToTheExtensionMethod() {
    myFixture.configureByText("Main.hx", CHAIN_SOURCE);

    PsiElement resolved = caretReference().resolve();

    HaxeMethodDeclaration method = assertInstanceOf(HaxeMethodDeclaration.class, resolved);
    assertEquals("replace", method.getName());
  }

  @Test
  @DisplayName("evaluating the chain is certain")
  public void testEvaluatingTheChainIsCertain() {
    myFixture.configureByText("Main.hx", CHAIN_SOURCE);
    HaxeCallExpression chain = caretCall();

    long mark = HaxeEvaluationTaint.mark();
    ResultHolder result = HaxeExpressionEvaluator.evaluate(chain, new HaxeExpressionEvaluatorContext(chain), null).result;

    assertEquals("String", result.toStringWithoutConstant());
    assertFalse(HaxeEvaluationTaint.taintedSince(mark), "the chain's evaluation re-entered a resolve in progress");
  }

  @Test
  @DisplayName("the return type survives an inactive conditional link in the chain")
  public void testTheReturnTypeSurvivesAnInactiveConditionalLinkInTheChain() {
    myFixture.configureByText("Main.hx", CONDITIONAL_CHAIN_SOURCE);

    assertEquals("String", inferredReturnTypeOfFirstMethod().toStringWithoutConstant());
  }

  @Test
  @DisplayName("the return type survives an active conditional link in the chain")
  public void testTheReturnTypeSurvivesAnActiveConditionalLinkInTheChain() {
    HaxeTestDefines.set(getProject(), "sys");
    myFixture.configureByText("Main.hx", CONDITIONAL_CHAIN_SOURCE);

    assertEquals("String", inferredReturnTypeOfFirstMethod().toStringWithoutConstant());
  }

  @Test
  @DisplayName("a long chain resolves and evaluates cleanly")
  public void testALongChainResolvesAndEvaluatesCleanly() {
    myFixture.configureByText("Main.hx", LONG_CHAIN_SOURCE);
    HaxeCallExpression chain = caretCall();

    long mark = HaxeEvaluationTaint.mark();
    PsiElement resolved = caretReference().resolve();
    ResultHolder result = HaxeExpressionEvaluator.evaluate(chain, new HaxeExpressionEvaluatorContext(chain), null).result;

    assertInstanceOf(HaxeMethodDeclaration.class, resolved);
    assertEquals("String", result.toStringWithoutConstant());
    assertFalse(HaxeEvaluationTaint.taintedSince(mark), "a 20-link chain tripped a recursion guard or budget");
  }

  @Test
  @DisplayName("an enclosing callee not named match is never resolved")
  public void testAnEnclosingCalleeNotNamedMatchIsNeverResolved() {
    myFixture.configureByText("Main.hx", CHAIN_SOURCE);
    HaxeResolver resolver = HaxeResolver.getInstance(getProject());
    HaxeCallExpression chain = caretCall();

    int before = resolver.pipelineRuns();
    boolean matchCall = EnumValueMatchUtil.isPatternMatcherCall(chain);
    int runs = resolver.pipelineRuns() - before;

    assertFalse(matchCall);
    assertEquals(0, runs, "resolves spent on a callee that cannot be EnumValue.match");
  }

  @Test
  @DisplayName("an enum value match call is recognized")
  public void testAnEnumValueMatchCallIsRecognized() {
    myFixture.configureByText("Main.hx", MATCH_SOURCE);

    assertTrue(EnumValueMatchUtil.isPatternMatcherCall(caretCall()));
  }

  /** The return type as the inlay provider infers it: no type tag, the method's own generic resolver. */
  private ResultHolder inferredReturnTypeOfFirstMethod() {
    HaxeMethodDeclaration method = PsiTreeUtil.findChildOfType(myFixture.getFile(), HaxeMethodDeclaration.class);
    assertNotNull(method, "method in the fixture");
    HaxeMethodModel model = method.getModel();
    HaxeGenericResolver resolver = model.getGenericResolver(null).withTypeParametersAsType(model.getGenericParams());
    return model.getReturnType(resolver);
  }

  private PsiReference caretReference() {
    PsiReference reference = myFixture.getReferenceAtCaretPosition();
    assertNotNull(reference, "reference at caret");
    return reference;
  }

  /** The call whose callee is the reference at the caret. */
  private HaxeCallExpression caretCall() {
    return assertInstanceOf(HaxeCallExpression.class, caretReference().getElement().getParent());
  }
}
