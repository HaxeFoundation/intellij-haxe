package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;

/**
 * Jump to Source resolution from a Variables-view access path, pinning down
 * WHICH mechanism each case needs:
 *
 * <ul>
 *   <li>members of the DECLARED type resolve through the ordinary chain
 *       resolution — no fallback involved;</li>
 *   <li>members that exist only on the RUNTIME type (the debugger reports
 *       concrete types: {@code var s:Shape = new Circle()} shows Circle's
 *       members) do NOT resolve through the chain, which is why the
 *       runtime-type fallback exists at all;</li>
 *   <li>the fallback must match classes by their RUNTIME name (package + bare
 *       name): for an ancillary (secondary) class in a module, PSI's
 *       getQualifiedName() inserts the module segment, which the debugger's
 *       type strings never contain.</li>
 * </ul>
 */
public class HaxeVariableSourceNavigatorTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /** Shape/Circle in their own files (both PRIMARY classes), stopped in Main with `var s:Shape = new Circle()`. */
  private void shapesProject() {
    myFixture.addFileToProject("shapes/Shape.hx",
                               "package shapes;\nclass Shape { public var base:Int = 0; }");
    myFixture.addFileToProject("shapes/Circle.hx",
                               "package shapes;\nclass Circle extends Shape { public var radius:Float = 1; }");
    myFixture.configureByText("Main.hx",
                              "import shapes.Shape;\nimport shapes.Circle;\n"
                              + "class Main { static function main() { var s:Shape = new Circle(); trace<caret>(s); } }");
  }

  private XSourcePosition framePosition() {
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull("context element at the breakpoint", context);
    return XDebuggerUtil.getInstance().createPositionByElement(context);
  }

  private @Nullable XSourcePosition resolve(String path, @Nullable String containerType, @Nullable String member) {
    return HaxeVariableSourceNavigator.resolvePosition(getProject(), framePosition(), path, containerType, member);
  }

  // --- which cases need which mechanism ---

  public void testDeclaredMemberResolvesThroughTheChainAlone() {
    shapesProject();
    assertNotNull("a member of the DECLARED type needs no fallback",
                  resolve("s.base", null, null));
  }

  public void testSubtypeOnlyMemberDoesNotResolveThroughDeclaredTypes() {
    shapesProject();
    assertNull("the declared type Shape knows no `radius`: chain resolution fails even for PRIMARY classes"
               + " — this is the case the runtime-type fallback exists for",
               resolve("s.radius", null, null));
  }

  public void testSubtypeOnlyMemberResolvesViaRuntimeTypeFallback() {
    shapesProject();
    assertNotNull("the runtime type reported by the debugger knows `radius`",
                  resolve("s.radius", "shapes.Circle", "radius"));
  }

  public void testInheritedMemberFoundThroughRuntimeSubtype() {
    shapesProject();
    // an unresolvable path forces the fallback; `base` is declared on Shape,
    // looked up through runtime type Circle
    assertNotNull("member lookup on the runtime type includes inherited members",
                  resolve("ghost.base", "shapes.Circle", "base"));
  }

  // --- the ancillary-class naming trap ---

  public void testAncillaryClassQualifiedNameIncludesTheModule() {
    myFixture.addFileToProject("pack/Module.hx",
                               "package pack;\nclass Module {}\nclass Secondary { public var marker:Int = 1; }");
    myFixture.configureByText("Main.hx", "class Main { static function main() { trace<caret>(0); } }");
    Collection<HaxeClass> candidates = HaxeClassNameUnifiedIndex.getByNameFiltered(
      "Secondary", getProject(), GlobalSearchScope.allScope(getProject()));
    assertEquals("the ancillary class is indexed under its short name", 1, candidates.size());
    HaxeClass secondary = candidates.iterator().next();
    // the premise of the runtime-name comparison: PSI's qualified name is NOT
    // what the debugger reports (pack.Secondary)
    assertEquals("pack.Module.Secondary", secondary.getQualifiedName());
    assertEquals("pack.Secondary", HaxeDebuggerSupportUtils.runtimeClassName(secondary));
  }

  public void testAncillaryRuntimeTypeResolvesByRuntimeName() {
    myFixture.addFileToProject("pack/Module.hx",
                               "package pack;\nclass Module {}\nclass Secondary { public var marker:Int = 1; }");
    myFixture.configureByText("Main.hx",
                              "class Main { static function main() { var o = null; trace<caret>(o); } }");
    assertNotNull("the debugger-reported name pack.Secondary matches the ancillary class",
                  resolve("o.marker", "pack.Secondary", "marker"));
  }

  // --- this-rooted paths (the resolver's fragment-context fallback) ---

  /** Instance frame: stopped inside Widget.update(), where `this` is a Widget extends Base. */
  private void instanceFrameProject() {
    myFixture.addFileToProject("Base.hx", "class Base { public var inherited:Int = 2; }");
    myFixture.configureByText("Widget.hx",
                              "class Widget extends Base { var count:Int = 1;\n"
                              + "  function update() { trace<caret>(count); }\n"
                              + "  static function main() { new Widget().update(); } }");
  }

  public void testThisMemberResolvesThroughTheFragment() {
    instanceFrameProject();
    assertNotNull("this.count resolves through the fragment: the resolver falls back to the"
                  + " fragment's creation context when the enclosing-class parent walk dead-ends",
                  resolve("this.count", null, null));
  }

  public void testThisInheritedMemberResolvesThroughTheFragment() {
    instanceFrameProject();
    assertNotNull("this.inherited resolves via the super-class walk from the context class",
                  resolve("this.inherited", null, null));
  }

  public void testBareThisNavigatesToTheEnclosingClass() {
    instanceFrameProject();
    assertNotNull("bare this names no member; it navigates to the enclosing class",
                  resolve("this", null, null));
  }

  /**
   * Same as instanceFrameProject, but stopped INSIDE a callback defined in an
   * object literal. Object literals are HaxeClass in the PSI but don't rebind
   * {@code this} in Haxe, so both fragment-context fallbacks must walk past
   * them — the literal is the first HaxeClass the context walk finds.
   */
  private void objectLiteralFrameProject() {
    myFixture.addFileToProject("Base.hx", "class Base { public var inherited:Int = 2; }");
    myFixture.configureByText("Widget.hx",
                              "class Widget extends Base { var count:Int = 1;\n"
                              + "  function update() { var o = { cb: function() { trace<caret>(0); } }; }\n"
                              + "  static function main() { new Widget().update(); } }");
  }

  public void testThisOwnMemberFromAFrameInsideAnObjectLiteral() {
    objectLiteralFrameProject();
    assertNotNull("this.count needs HaxeReferenceImpl's fallback to skip the literal",
                  resolve("this.count", null, null));
  }

  public void testThisInheritedMemberFromAFrameInsideAnObjectLiteral() {
    objectLiteralFrameProject();
    assertNotNull("this.inherited needs HaxeResolver's fallback to skip the literal",
                  resolve("this.inherited", null, null));
  }

  /** The parity baseline the two tests above must match: real-file resolution skips literals. */
  public void testRealFileThisMemberInsideAnObjectLiteralResolves() {
    myFixture.configureByText("Widget.hx",
                              "class Widget { var count:Int = 1;\n"
                              + "  function update() { var o = { cb: function() { trace(this.cou<caret>nt); } }; } }");
    assertNotNull("this.count written in a REAL file inside an object-literal closure",
                  myFixture.getFile().findReferenceAt(myFixture.getCaretOffset()).resolve());
  }

  // --- graceful misses ---

  public void testUnknownRuntimeTypeFallsThroughToNoNavigation() {
    shapesProject();
    assertNull("a VM-internal type name misses the index without blowing up",
               resolve("ghost.x", "vm.Internal.Thing", "x"));
  }

  public void testNonIdentifierMemberSkipsTheFallback() {
    shapesProject();
    assertNull("array-index children have no member to look up",
               resolve("ghost.x", "shapes.Circle", "[0]"));
  }
}
