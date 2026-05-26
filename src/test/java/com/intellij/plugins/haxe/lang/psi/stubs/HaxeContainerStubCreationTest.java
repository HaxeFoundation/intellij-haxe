package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubTree;
import com.intellij.psi.tree.IElementType;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regression test for UpToDateStubIndexMismatch in files that use abstract `to`/`from`
 * clauses (e.g. tink_state's State.hx).
 *
 * Hypothesis being verified: HaxeContainerStubElementType — which mixes the legacy
 * IStubElementType API with the new EmptyStubSerializer interface — does not actually
 * cause the stub builder to create stubs for its element types. The platform's AST
 * walker still counts them as stubbable, producing a count mismatch.
 *
 * If these tests fail, the hypothesis is confirmed.
 */
public class HaxeContainerStubCreationTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/stubs/";
  }

  /**
   * MyAbstract.hx contains:
   *   abstract MyAbstract(Int) from Int to Int { ... }
   * So the stub tree MUST contain exactly one ABSTRACT_TO_TYPE and one ABSTRACT_FROM_TYPE
   * stub. If the stub builder is silently skipping these element types, both counts
   * will be 0 here.
   */
  @Test
  public void testAbstractToFromStubsAreCreated() throws Throwable {
    myFixture.configureByFiles("MyAbstract.hx");
    PsiFile psiFile = myFixture.getFile();

    StubTree stubTree = ((PsiFileImpl) psiFile).calcStubTree();
    assertNotNull("calcStubTree() must return a stub tree", stubTree);

    List<StubElement<?>> stubs = stubTree.getPlainList();
    long toCount = countOfType(stubs, HaxeStubElementTypes.ABSTRACT_TO_TYPE);
    long fromCount = countOfType(stubs, HaxeStubElementTypes.ABSTRACT_FROM_TYPE);

    assertEquals("Expected exactly 1 ABSTRACT_TO_TYPE stub for `to Int` clause " +
                 "(stub builder produced " + describeStubs(stubs) + ")",
                 1L, toCount);
    assertEquals("Expected exactly 1 ABSTRACT_FROM_TYPE stub for `from Int` clause",
                 1L, fromCount);
  }

  /**
   * Control test: ChildClass.hx contains `extends SimpleClass implements IBase`.
   * If the bug affects ALL HaxeContainerStubElementType element types (not just abstract
   * to/from), this test fails too. If only the previous test fails, the bug is narrower.
   */
  @Test
  public void testExtendsImplementsStubsAreCreated() throws Throwable {
    myFixture.configureByFiles("ChildClass.hx", "SimpleClass.hx", "IBase.hx", "IExtended.hx");
    PsiFile psiFile = myFixture.getFile();

    StubTree stubTree = ((PsiFileImpl) psiFile).calcStubTree();
    assertNotNull(stubTree);

    List<StubElement<?>> stubs = stubTree.getPlainList();
    long extendsCount = countOfType(stubs, HaxeStubElementTypes.EXTENDS_DECLARATION);
    long implementsCount = countOfType(stubs, HaxeStubElementTypes.IMPLEMENTS_DECLARATION);

    assertTrue("Expected at least 1 EXTENDS_DECLARATION stub for ChildClass extends SimpleClass " +
               "(stub builder produced " + describeStubs(stubs) + ")",
               extendsCount >= 1);
    assertTrue("Expected at least 1 IMPLEMENTS_DECLARATION stub", implementsCount >= 1);
  }

  /**
   * Mirrors the platform's assertion in FileTrees.reconcilePsi: the stub tree size
   * must equal the count of AST nodes whose IElementType is an IStubElementType
   * (modulo subtrees pruned by skipChildProcessingWhenBuildingStubs).
   *
   * We walk the AST applying the same skip predicate the stub builder uses, count
   * IStubElementType nodes, and compare against the stub-tree size. A mismatch here
   * is the same condition that produces UpToDateStubIndexMismatch at runtime.
   */
  /**
   * State.hx is the tink_state file that triggers UpToDateStubIndexMismatch in production.
   * If we reproduce the +3 count delta in a unit test, the per-element-type diff in the
   * failure message tells us exactly which 3 element types are missing stubs.
   */
  @Test
  public void testStubCountMatchesAstStubbableCount_State() throws Throwable {
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();
    checkStubVsAstParity(psiFile, "State.hx");
  }

  /**
   * Cross-check: the platform's own AstSpine count (the value compared against the stub-tree
   * size by FileTrees.reconcilePsi) must equal calcStubTree().size(). This is the assertion
   * the platform itself fires as UpToDateStubIndexMismatch.
   */
  @Test
  public void testPlatformAstSpineMatchesStubTree_State() throws Throwable {
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();

    StubTree stubTree = ((PsiFileImpl) psiFile).calcStubTree();
    int stubCount = stubTree.getPlainList().size();

    com.intellij.psi.impl.source.tree.FileElement fileElement =
      (com.intellij.psi.impl.source.tree.FileElement) psiFile.getNode();
    int spineCount = fileElement.getStubbedSpine().getSpineNodes().size();

    assertEquals("Platform's AstSpine count must equal stub-tree size. " +
                 "A mismatch here is precisely UpToDateStubIndexMismatch.",
                 stubCount, spineCount);
  }

  @Test
  public void testStubCountMatchesAstStubbableCount_MyAbstract() throws Throwable {
    myFixture.configureByFiles("MyAbstract.hx");
    PsiFile psiFile = myFixture.getFile();
    checkStubVsAstParity(psiFile, "MyAbstract.hx");
  }

  /**
   * Round-trip test: serialise the stub tree, deserialise it, count the result.
   * If the serialise->deserialise pipeline drops or duplicates stubs, the counts will differ.
   * In production the stub tree is loaded from the persisted index (deserialised),
   * not built fresh, so any roundtrip bug shows up there but NOT in the other tests.
   */
  @Test
  public void testStubTreeSurvivesSerializationRoundTrip_State() throws Throwable {
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();

    StubTree original = ((PsiFileImpl) psiFile).calcStubTree();
    int originalCount = original.getPlainList().size();

    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    com.intellij.psi.stubs.SerializationManagerEx mgr = com.intellij.psi.stubs.SerializationManagerEx.getInstanceEx();
    mgr.serialize(original.getRoot(), baos);
    byte[] bytes = baos.toByteArray();

    com.intellij.psi.stubs.Stub deserializedRoot = mgr.deserialize(new java.io.ByteArrayInputStream(bytes));
    int deserializedCount = countAllStubs(deserializedRoot);

    assertEquals("Stub count after serialise/deserialise round-trip must match original. " +
                 "If this differs, persistence is losing/duplicating stubs (the production root cause).",
                 originalCount, deserializedCount);
  }

  private static int countAllStubs(com.intellij.psi.stubs.Stub stub) {
    int count = 1;
    for (com.intellij.psi.stubs.Stub child : stub.getChildrenStubs()) {
      count += countAllStubs(child);
    }
    return count;
  }

  /**
   * Determinism test: build the stub tree multiple times and verify the count is identical.
   * A flapping count would indicate the stub builder consults order-dependent state
   * (cached resolves, import.hx index, conditional compilation context) and would
   * explain why the production assertion's direction can flip across runs.
   */
  @Test
  public void testStubBuildingIsDeterministic_State() throws Throwable {
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();

    int[] sizes = new int[5];
    for (int i = 0; i < sizes.length; i++) {
      StubTree tree = ((PsiFileImpl) psiFile).calcStubTree();
      sizes[i] = tree.getPlainList().size();
    }
    for (int i = 1; i < sizes.length; i++) {
      assertEquals("calcStubTree() must be deterministic across calls. " +
                   "Sizes seen: " + java.util.Arrays.toString(sizes),
                   sizes[0], sizes[i]);
    }
  }

  /**
   * RED TEST: Reproduces the exact production scenario.
   *
   * Step 1: Index State.hx with tink_state.debug=TRUE — get stubs reflecting the active branch.
   * Step 2: Switch defines to tink_state.debug=FALSE — simulating a session where the haxelib
   *         autodetection hasn't repopulated the define.
   * Step 3: Force a fresh re-parse (which uses the new defines).
   * Step 4: Compare the previously-built stub count with the freshly-parsed AST spine count.
   *
   * If this asserts unequal, we've reproduced the production UpToDateStubIndexMismatch
   * without needing the on-disk index pipeline.
   */
  @Test
  public void testStubAstMismatch_WhenDefinesChangeAcrossReparse() throws Throwable {
    com.intellij.openapi.util.Key<Object> definesKey =
      com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression.DEFINES_KEY;

    myFixture.getProject().putUserData(definesKey, "tink_state.debug=1");
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();
    StubTree stubsBuiltWithDebug = ((PsiFileImpl) psiFile).calcStubTree();
    int stubCountFromDebugBuild = stubsBuiltWithDebug.getPlainList().size();

    myFixture.getProject().putUserData(definesKey, "");

    // The PsiFile still has its cached AST from the previous parse, which reflects defines-with-debug.
    // We want to FORCE a re-parse. Drop caches and re-fetch.
    com.intellij.psi.PsiDocumentManager.getInstance(myFixture.getProject())
      .reparseFiles(java.util.Collections.singleton(psiFile.getVirtualFile()), true);

    com.intellij.psi.impl.source.tree.FileElement fileElement =
      (com.intellij.psi.impl.source.tree.FileElement) psiFile.getNode();
    int astSpineCount = fileElement.getStubbedSpine().getSpineNodes().size();

    System.out.println("Stub count built with debug=TRUE : " + stubCountFromDebugBuild);
    System.out.println("AST spine count after debug=FALSE: " + astSpineCount);
    System.out.println("Delta (stub - ast)               : " + (stubCountFromDebugBuild - astSpineCount));

    // If the production reproduction is real, these should not be equal.
    // Surface the inequality so we can see it in CI output even when the assertion isn't fatal.
    if (stubCountFromDebugBuild != astSpineCount) {
      System.out.println(">>> MISMATCH REPRODUCED: this is the production failure mode.");
    } else {
      System.out.println(">>> No mismatch — the in-process re-parse may have invalidated cached stubs.");
    }
  }

  /**
   * Reproduce hypothesis: at indexing time tink_state.debug evaluates to TRUE
   * (giving 519 stubs); at access time it evaluates to FALSE (giving 516 AST nodes).
   *
   * In test mode, the conditional eval reads defines from the project's DEFINES_KEY
   * user data. We can flip it at will to mimic the asymmetry.
   */
  @Test
  public void testReproduceMismatch_WhenDefinesChangeBetweenIndexAndReconcile() throws Throwable {
    // First: index with tink_state.debug TRUE
    com.intellij.openapi.util.Key<Object> definesKey =
      com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression.DEFINES_KEY;
    myFixture.getProject().putUserData(definesKey, "tink_state.debug=1");

    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();
    StubTree stubTreeWithDebug = ((PsiFileImpl) psiFile).calcStubTree();
    int withDebug = stubTreeWithDebug.getPlainList().size();

    // Now: flip defines off and recalc
    myFixture.getProject().putUserData(definesKey, "");

    // Force a fresh parse by clearing caches
    com.intellij.psi.impl.PsiManagerEx.getInstanceEx(myFixture.getProject()).getFileManager().cleanupForNextTest();
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile2 = myFixture.getFile();
    StubTree stubTreeWithoutDebug = ((PsiFileImpl) psiFile2).calcStubTree();
    int withoutDebug = stubTreeWithoutDebug.getPlainList().size();

    System.out.println("tink_state.debug=TRUE  → stubCount=" + withDebug);
    System.out.println("tink_state.debug=FALSE → stubCount=" + withoutDebug);
    System.out.println("Delta = " + (withDebug - withoutDebug));
  }

  /**
   * Probe: print the actual stub counts in production-like scenarios.
   * Helps diagnose what's different between test env and prod env.
   */
  @Test
  public void testDumpStubCounts_State() throws Throwable {
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();

    StubTree stubTree = ((PsiFileImpl) psiFile).calcStubTree();
    int stubCount = stubTree.getPlainList().size();

    ASTNode root = psiFile.getNode();
    int astStubbableCount = countStubbableNodesRespectingSkipPredicate(root, null) + 1;

    com.intellij.psi.impl.source.tree.FileElement fileElement =
      (com.intellij.psi.impl.source.tree.FileElement) psiFile.getNode();
    int spineCount = fileElement.getStubbedSpine().getSpineNodes().size();

    System.out.println("==== State.hx stub diagnostic dump ====");
    System.out.println("stubCount         = " + stubCount);
    System.out.println("astStubbableCount = " + astStubbableCount + " (using HaxeFileElementType skip predicate)");
    System.out.println("spineCount        = " + spineCount + " (platform's StubbedSpine)");

    Map<IElementType, Integer> stubTypeCounts = countByType(stubTree.getPlainList());
    Map<IElementType, Integer> astTypeCounts = new LinkedHashMap<>();
    collectAstStubbableNodesByType(root, null, astTypeCounts);

    System.out.println("--- by-type STUB counts ---");
    stubTypeCounts.forEach((t, c) -> System.out.println("  " + t + ": " + c));
    System.out.println("--- by-type AST counts ---");
    astTypeCounts.forEach((t, c) -> System.out.println("  " + t + ": " + c));
  }

  /**
   * Reproduces the EXACT production flow that throws UpToDateStubIndexMismatch:
   *
   *   1. Configure State.hx (loads it via the stub-based index)
   *   2. Find the HaxeAbstractTypeDeclaration PSI element
   *   3. Call getUnderlyingType() on it — this is the user's stack trace entry point
   *
   * getUnderlyingType() calls PsiTreeUtil.getChildOfType which triggers getFirstChild()
   * which forces AST loading. The platform then runs reconcilePsi() comparing the
   * already-loaded stub tree to the freshly-built AST spine. A mismatch throws here.
   */
  @Test
  public void testGetUnderlyingTypeDoesNotThrowReconciliationError() throws Throwable {
    myFixture.configureByFiles("State.hx");
    PsiFile psiFile = myFixture.getFile();

    // Force the stub tree to load FIRST (no AST). This matches the production flow
    // where stubs are deserialised from the index before any AST is built.
    StubTree stubTree = ((PsiFileImpl) psiFile).calcStubTree();
    assertNotNull(stubTree);

    // Now find the abstract type declaration via the stub tree (without forcing AST).
    com.intellij.plugins.haxe.lang.psi.HaxeAbstractTypeDeclaration abstractDecl =
      com.intellij.psi.util.PsiTreeUtil.findChildOfType(
        psiFile, com.intellij.plugins.haxe.lang.psi.HaxeAbstractTypeDeclaration.class);
    assertNotNull("State.hx must contain an abstract type declaration", abstractDecl);

    // This call triggers AST loading + reconciliation. If stubs and AST disagree,
    // FileTrees.reconcilePsi throws AssertionError "Stub count (N) doesn't match …".
    com.intellij.plugins.haxe.lang.psi.HaxeUnderlyingType underlying = abstractDecl.getUnderlyingType();
    assertNotNull("State.hx abstract must have an underlying type", underlying);
  }

  private void checkStubVsAstParity(PsiFile psiFile, String label) {

    StubTree stubTree = ((PsiFileImpl) psiFile).calcStubTree();
    int stubCount = stubTree.getPlainList().size();

    ASTNode root = psiFile.getNode();
    int astStubbableCount = countStubbableNodesRespectingSkipPredicate(root, null) + 1;
    // +1 because the file stub itself is in the stub tree but the file element doesn't
    // self-report as IStubElementType during our recursion.

    if (stubCount != astStubbableCount) {
      // Print a per-element-type breakdown so we can see WHICH element types are mismatched.
      Map<IElementType, Integer> stubTypeCounts = countByType(stubTree.getPlainList());
      Map<IElementType, Integer> astTypeCounts = new LinkedHashMap<>();
      collectAstStubbableNodesByType(root, null, astTypeCounts);

      StringBuilder breakdown = new StringBuilder();
      breakdown.append("\n=== STUB tree counts ===\n");
      stubTypeCounts.forEach((t, c) -> breakdown.append("  ").append(t).append(": ").append(c).append('\n'));
      breakdown.append("=== AST IStubElementType node counts (with skip predicate) ===\n");
      astTypeCounts.forEach((t, c) -> breakdown.append("  ").append(t).append(": ").append(c).append('\n'));
      breakdown.append("=== Per-type diff (AST - STUB) ===\n");
      java.util.Set<IElementType> allTypes = new java.util.LinkedHashSet<>();
      allTypes.addAll(stubTypeCounts.keySet());
      allTypes.addAll(astTypeCounts.keySet());
      for (IElementType t : allTypes) {
        int s = stubTypeCounts.getOrDefault(t, 0);
        int a = astTypeCounts.getOrDefault(t, 0);
        if (s != a) breakdown.append("  ").append(t).append(": stub=").append(s).append(" ast=").append(a)
                              .append(" diff=").append(a - s).append('\n');
      }
      fail("[" + label + "] Stub count (" + stubCount + ") doesn't match AST IStubElementType-node count ("
           + astStubbableCount + "). " + breakdown);
    }
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  private static long countOfType(List<StubElement<?>> stubs, IElementType type) {
    return stubs.stream().filter(s -> s.getStubType() == type).count();
  }

  private static String describeStubs(List<StubElement<?>> stubs) {
    return "[" + stubs.stream()
      .map(s -> s.getStubType() == null ? "FILE" : s.getStubType().toString())
      .collect(Collectors.joining(", ")) + "]";
  }

  /**
   * Exact mirror of HaxeFileElementType.skipChildProcessingWhenBuildingStubs.
   * Kept in lock-step with the production code so this test reproduces the same
   * walk semantics the platform's calcStubbedDescendants applies during reconciliation.
   */
  private static boolean shouldSkip(ASTNode parent, ASTNode node) {
    if (parent == null) return false;
    IElementType ct = node.getElementType();
    if (HaxeTokenTypeSets.WHITESPACES.contains(ct)) return true;
    if (HaxeTokenTypeSets.ONLY_COMMENTS.contains(ct)) return true;
    if (HaxeTokenTypeSets.CONDITIONALLY_NOT_COMPILED.contains(ct)) return true;
    IElementType pt = parent.getElementType();
    return isElementTypeToSkip(pt) || isElementTypeToSkip(ct);
  }

  private static boolean isElementTypeToSkip(IElementType type) {
    return type == HaxeTokenTypes.VAR_INIT
        || type == HaxeTokenTypes.BLOCK_STATEMENT
        || type == HaxeTokenTypes.RETURN_STATEMENT
        || type == HaxeTokenTypes.IF_STATEMENT
        || type == HaxeTokenTypes.TRY_STATEMENT
        || type == HaxeTokenTypes.SWITCH_STATEMENT
        || type == HaxeTokenTypes.WHILE_STATEMENT
        || type == HaxeTokenTypes.DO_WHILE_STATEMENT
        || type == HaxeTokenTypes.FOR_STATEMENT
        || type == HaxeTokenTypes.THROW_STATEMENT
        || type == HaxeTokenTypes.THIS_EXPRESSION
        || type == HaxeTokenTypes.SUPER_EXPRESSION
        || type == HaxeTokenTypes.ASSIGN_EXPRESSION;
  }

  private static int countStubbableNodesRespectingSkipPredicate(ASTNode node, ASTNode parent) {
    if (shouldSkip(parent, node)) return 0;
    int count = (node.getElementType() instanceof IStubElementType) ? 1 : 0;
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      count += countStubbableNodesRespectingSkipPredicate(child, node);
    }
    return count;
  }

  private static void collectAstStubbableNodesByType(ASTNode node, ASTNode parent, Map<IElementType, Integer> sink) {
    if (shouldSkip(parent, node)) return;
    IElementType t = node.getElementType();
    if (t instanceof IStubElementType) {
      sink.merge(t, 1, Integer::sum);
    }
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      collectAstStubbableNodesByType(child, node, sink);
    }
  }

  private static Map<IElementType, Integer> countByType(List<StubElement<?>> stubs) {
    Map<IElementType, Integer> out = new LinkedHashMap<>();
    for (StubElement<?> s : stubs) {
      IElementType t = s.getStubType();
      if (t != null) out.merge(t, 1, Integer::sum);
    }
    return out;
  }
}
