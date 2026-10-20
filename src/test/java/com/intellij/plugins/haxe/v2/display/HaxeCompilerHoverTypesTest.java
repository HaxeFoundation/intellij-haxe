package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.editor.Document;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.display.protocol.HoverInfo;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerHoverTypes.Batch;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerHoverTypes.Scope;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerHoverTypes.Target;
import com.intellij.plugins.haxe.v2.display.HaxeDisplayConfiguration.OverrideArguments;
import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A batch asks about every covered declaration of a file that has no type
 * tag, at its name, and expects the compiler's answer to report exactly that
 * name's range; the local scope leaves the class members out. A compiler
 * error costs only its own declaration, unless the batch's first requests
 * all fail.
 */
@DisplayName("Compiler types: hover targets")
public class HaxeCompilerHoverTypesTest extends HaxeLightFixtureTestCase {

  private static final String MAIN_HX_SOURCE = """
    enum Opt {
      Some(v:Int);
      None;
    }
    class Main {
      static var count = 1;
      static function run(size, total:Int) {
        var sum = size + total;
        switch (Some(sum)) {
          case Some(v): trace(v);
          case None:
        }
      }
      public function new() {}
    }
    """;

  private static final String WILDCARD_HX_SOURCE = """
    enum Op {
      Bin(op:Int, left:Int, right:Int);
    }
    class Wildcard {
      static function run(op:Op) {
        switch (op) {
          case Bin(_): var late = 1;
        }
      }
    }
    """;

  private static final String LOCALS_HX_SOURCE = """
    class Locals {
      static function run() {
        var first = 1;
        var second = 2;
        var third = 3;
        var fourth = 4;
      }
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The answers and suspensions live in a project service the light project shares between tests. */
  @AfterEach
  void clearHoverTypes() {
    HaxeCompilerHoverTypes.getInstance(getProject()).clearCaches();
  }

  @Test
  @DisplayName("every untyped covered declaration becomes a target at its name")
  public void testEveryUntypedCoveredDeclarationBecomesATargetAtItsName() {
    PsiFile main = myFixture.configureByText("Main.hx", MAIN_HX_SOURCE);
    Document document = myFixture.getEditor().getDocument();

    List<Target> targets = HaxeCompilerHoverTypes.targetsOf(main, document, Scope.ALL);

    assertEquals(List.of("count", "run", "size", "sum", "v"), names(targets, document),
                 "total and the enum constructor's v carry type tags; the constructor has no type to show");
    Target count = targets.getFirst();
    assertEquals(new Range(new Position(5, 13), new Position(5, 18)), count.expectedRange(), "0-based line and character of the name");
    assertEquals(5, count.line());
  }

  @Test
  @DisplayName("the local scope leaves the class members out")
  public void testTheLocalScopeLeavesTheClassMembersOut() {
    PsiFile main = myFixture.configureByText("Main.hx", MAIN_HX_SOURCE);
    Document document = myFixture.getEditor().getDocument();

    List<Target> targets = HaxeCompilerHoverTypes.targetsOf(main, document, Scope.LOCALS);

    assertEquals(List.of("sum", "v"), names(targets, document), "the field, the method and its parameter are members");
  }

  @Test
  @DisplayName("a wildcard is no target")
  public void testAWildcardIsNoTarget() {
    PsiFile file = myFixture.configureByText("Wildcard.hx", WILDCARD_HX_SOURCE);
    Document document = myFixture.getEditor().getDocument();

    List<Target> targets = HaxeCompilerHoverTypes.targetsOf(file, document, Scope.LOCALS);

    assertEquals(List.of("late"), names(targets, document), "a wildcard names nothing, and the compiler fails a hover on one in a constructor pattern");
  }

  @Test
  @DisplayName("a failing hover answers only its own declaration")
  public void testAFailingHoverAnswersOnlyItsOwnDeclaration() {
    Batch batch = localsBatch();
    HaxeCompilerHoverTypes hoverTypes = HaxeCompilerHoverTypes.getInstance(getProject());

    hoverTypes.hoverEach(batch, target -> nameOf(target, batch.document()).equals("second") ? failWithCompilerError() : answerFor(target));

    assertEquals(List.of("first", "third", "fourth"), typedNames(hoverTypes, batch));
    assertFalse(hoverTypes.isSuspended(batch.file(), batch.documentStamp()));
  }

  @Test
  @DisplayName("a batch whose first hovers all fail suspends the file")
  public void testABatchWhoseFirstHoversAllFailSuspendsTheFile() {
    Batch batch = localsBatch();
    HaxeCompilerHoverTypes hoverTypes = HaxeCompilerHoverTypes.getInstance(getProject());
    List<Target> asked = new ArrayList<>();

    hoverTypes.hoverEach(batch, target -> failAfterRecording(asked, target));

    assertEquals(List.of("first", "second", "third"), names(asked, batch.document()), "the fourth declaration is not asked about");
    assertTrue(hoverTypes.isSuspended(batch.file(), batch.documentStamp()));
  }

  private Batch localsBatch() {
    PsiFile file = myFixture.configureByText("Locals.hx", LOCALS_HX_SOURCE);
    Document document = myFixture.getEditor().getDocument();
    List<Target> targets = HaxeCompilerHoverTypes.targetsOf(file, document, Scope.LOCALS);
    DisplayContext context = new DisplayContext(List.of(), null, null, OverrideArguments.EMPTY, "test");
    return new Batch(file.getVirtualFile(), document, context, null, document.getModificationStamp(), false, targets);
  }

  private static List<String> typedNames(HaxeCompilerHoverTypes hoverTypes, Batch batch) {
    return batch.targets().stream()
      .filter(target -> hoverTypes.wireTypeOf(target.element(), Scope.LOCALS) != null)
      .map(target -> nameOf(target, batch.document()))
      .toList();
  }

  private static HoverInfo answerFor(Target target) {
    return new HoverInfo(target.expectedRange(), "Local", new JsonTypeRef.NamedType("TAbstract", null));
  }

  private static HoverInfo failWithCompilerError() throws DisplayRequestException {
    throw new DisplayRequestException("Not enough arguments", true);
  }

  private static HoverInfo failAfterRecording(List<Target> asked, Target target) throws DisplayRequestException {
    asked.add(target);
    return failWithCompilerError();
  }

  private static List<String> names(List<Target> targets, Document document) {
    return targets.stream().map(target -> nameOf(target, document)).toList();
  }

  private static String nameOf(Target target, Document document) {
    return document.getText().substring(target.offset(), target.offset() + nameLength(target));
  }

  private static int nameLength(Target target) {
    return target.expectedRange().end().character() - target.expectedRange().start().character();
  }
}
