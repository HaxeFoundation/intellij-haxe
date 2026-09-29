package com.intellij.plugins.haxe.runner.debugger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
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
@DisplayName("Debugger: variable source navigator")
public class HaxeVariableSourceNavigatorTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  private XSourcePosition framePosition() {
    return XDebuggerUtil.getInstance().createPositionByElement(contextAtCaret());
  }

  private @Nullable XSourcePosition resolve(String path, @Nullable String containerType, @Nullable String member) {
    return HaxeVariableSourceNavigator.resolvePosition(getProject(), framePosition(), path, containerType, member);
  }

  // --- which cases need which mechanism ---

  @Test
  @DisplayName("declared member resolves through the chain alone")
  public void testDeclaredMemberResolvesThroughTheChainAlone() {
    HaxeDebuggerTestFixtures.shapesProject(myFixture);
    assertNotNull(resolve("s.base", null, null), "a member of the DECLARED type needs no fallback");
  }

  @Test
  @DisplayName("subtype only member does not resolve through declared types")
  public void testSubtypeOnlyMemberDoesNotResolveThroughDeclaredTypes() {
    HaxeDebuggerTestFixtures.shapesProject(myFixture);
    assertNull(resolve("s.radius", null, null), "the declared type Shape knows no `radius`: chain resolution fails even for PRIMARY classes"
               + " — this is the case the runtime-type fallback exists for");
  }

  @Test
  @DisplayName("subtype only member resolves via runtime type fallback")
  public void testSubtypeOnlyMemberResolvesViaRuntimeTypeFallback() {
    HaxeDebuggerTestFixtures.shapesProject(myFixture);
    assertNotNull(resolve("s.radius", "shapes.Circle", "radius"), "the runtime type reported by the debugger knows `radius`");
  }

  @Test
  @DisplayName("inherited member found through runtime subtype")
  public void testInheritedMemberFoundThroughRuntimeSubtype() {
    HaxeDebuggerTestFixtures.shapesProject(myFixture);
    // an unresolvable path forces the fallback; `base` is declared on Shape,
    // looked up through runtime type Circle
    assertNotNull(resolve("ghost.base", "shapes.Circle", "base"), "member lookup on the runtime type includes inherited members");
  }

  // --- the ancillary-class naming trap ---

  @Test
  @DisplayName("ancillary class qualified name includes the module")
  public void testAncillaryClassQualifiedNameIncludesTheModule() {
    HaxeDebuggerTestFixtures.ancillaryProject(myFixture);

    Collection<HaxeClass> candidates = HaxeClassNameUnifiedIndex.getByNameFiltered(
      "Secondary", getProject(), GlobalSearchScope.allScope(getProject()));
    assertEquals(1, candidates.size(), "the ancillary class is indexed under its short name");
    HaxeClass secondary = candidates.iterator().next();

    // the premise of the runtime-name comparison: PSI's qualified name is NOT
    // what the debugger reports (pack.Secondary)
    assertEquals("pack.Module.Secondary", secondary.getFullyQualifiedName());
    assertEquals("pack.Secondary", HaxeDebuggerSupportUtils.runtimeClassName(secondary));
  }

  @Test
  @DisplayName("ancillary runtime type resolves by runtime name")
  public void testAncillaryRuntimeTypeResolvesByRuntimeName() {
    HaxeDebuggerTestFixtures.ancillaryProject(myFixture);
    assertNotNull(resolve("o.marker", "pack.Secondary", "marker"), "the debugger-reported name pack.Secondary matches the ancillary class");
  }

  // --- this-rooted paths (the resolver's fragment-context fallback) ---

  @Test
  @DisplayName("this member resolves through the fragment")
  public void testThisMemberResolvesThroughTheFragment() {
    HaxeDebuggerTestFixtures.instanceFrameProject(myFixture);
    assertNotNull(resolve("this.count", null, null), "this.count resolves through the fragment: the resolver falls back to the"
                  + " fragment's creation context when the enclosing-class parent walk dead-ends");
  }

  @Test
  @DisplayName("this inherited member resolves through the fragment")
  public void testThisInheritedMemberResolvesThroughTheFragment() {
    HaxeDebuggerTestFixtures.instanceFrameProject(myFixture);
    assertNotNull(resolve("this.inherited", null, null), "this.inherited resolves via the super-class walk from the context class");
  }

  @Test
  @DisplayName("bare this navigates to the enclosing class")
  public void testBareThisNavigatesToTheEnclosingClass() {
    HaxeDebuggerTestFixtures.instanceFrameProject(myFixture);
    assertNotNull(resolve("this", null, null), "bare this names no member; it navigates to the enclosing class");
  }

  @Test
  @DisplayName("this own member from a frame inside an object literal")
  public void testThisOwnMemberFromAFrameInsideAnObjectLiteral() {
    HaxeDebuggerTestFixtures.objectLiteralFrameProject(myFixture);
    assertNotNull(resolve("this.count", null, null), "this.count needs HaxeReferenceImpl's fallback to skip the literal");
  }

  @Test
  @DisplayName("this inherited member from a frame inside an object literal")
  public void testThisInheritedMemberFromAFrameInsideAnObjectLiteral() {
    HaxeDebuggerTestFixtures.objectLiteralFrameProject(myFixture);
    assertNotNull(resolve("this.inherited", null, null), "this.inherited needs HaxeResolver's fallback to skip the literal");
  }

  /** The parity baseline the two tests above must match: real-file resolution skips literals. */
  @Test
  @DisplayName("real file this member inside an object literal resolves")
  public void testRealFileThisMemberInsideAnObjectLiteralResolves() {
    myFixture.configureByText("Widget.hx", """
      class Widget { var count:Int = 1;
        function update() { var o = { cb: function() { trace(this.cou<caret>nt); } }; } }""");
    assertNotNull(myFixture.getFile().findReferenceAt(myFixture.getCaretOffset()).resolve(), "this.count written in a REAL file inside an object-literal closure");
  }

  // --- graceful misses ---

  @Test
  @DisplayName("unknown runtime type falls through to no navigation")
  public void testUnknownRuntimeTypeFallsThroughToNoNavigation() {
    HaxeDebuggerTestFixtures.shapesProject(myFixture);
    assertNull(resolve("ghost.x", "vm.Internal.Thing", "x"), "a VM-internal type name misses the index without blowing up");
  }

  @Test
  @DisplayName("non identifier member skips the fallback")
  public void testNonIdentifierMemberSkipsTheFallback() {
    HaxeDebuggerTestFixtures.shapesProject(myFixture);
    assertNull(resolve("ghost.x", "shapes.Circle", "[0]"), "array-index children have no member to look up");
  }
}
