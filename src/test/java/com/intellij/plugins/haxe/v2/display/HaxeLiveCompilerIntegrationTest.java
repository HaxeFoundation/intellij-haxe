package com.intellij.plugins.haxe.v2.display;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.fqn.HaxeFullyQualifiedClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerTypeCatalogService.GeneratedType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.intellij.util.ui.UIUtil;

import java.util.Collection;
import java.util.List;

/**
 * Live IDE integration, opt-in with {@code -PliveCompilerTests=true}. Covers
 * the full chain from a real {@code haxe --wait} compilation server to what
 * the user sees: the type catalog discovering a {@code Context.defineType}
 * type, the unified indexes resolving it, and completion offering it.
 * Fixtures live in {@code testData/liveCompiler/}. Skips itself when haxe is
 * not on the PATH.
 *
 * The catalog fill and blueprint hydration run through the synchronous
 * {@code @TestOnly} entry points, because production scheduling is disabled
 * in unit-test mode. That also keeps a stepping debugger session
 * deterministic.
 */
@DisplayName("Compiler services: compiler integration (live)")
public class HaxeLiveCompilerIntegrationTest extends HaxeCodeInsightFixtureTestCase {

  private static final String GENERATED_FQN = "gen.GeneratedThing";

  @Override
  protected String getBasePath() {
    return "/liveCompiler/";
  }

  @BeforeEach
  void wireCompilerProject() {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping live compiler integration test");

    // real files on disk: the compilation server compiles what the fixture copied
    VirtualFile buildFile = myFixture.copyFileToProject("build.hxml");
    myFixture.copyFileToProject("Main.hx");
    myFixture.copyFileToProject("GenMacro.hx");

    // the module's Build command supplies the display context (same wiring the
    // tool window's Compile command row writes)
    String containerId = myFixture.getModule().getName();
    HaxeEnvironmentStore.CompileCommand command =
      new HaxeEnvironmentStore.CompileCommand(buildFile.getPath(), null, "");
    HaxeEnvironmentStore.getInstance(getProject()).setCompileCommand(containerId, command);
  }

  @AfterEach
  void stopCompilationServer() {
    HaxeCompilationServerManager.getInstance(getProject()).stop();
  }

  @Test
  @Timeout(180)
  @DisplayName("catalog discovers the generated type and blueprints materialize its members")
  public void testCatalogDiscoversTheGeneratedTypeAndBlueprintsMaterializeItsMembers() {
    fillCatalogStably();
    HaxeCompilerTypeCatalogService catalog = HaxeCompilerTypeCatalogService.getInstance(getProject());

    List<GeneratedType> entries = catalog.byName("GeneratedThing");
    assertFalse(entries.isEmpty(), "the catalog must discover the defineType type through the dependency sweep");
    assertEquals(GENERATED_FQN, entries.get(0).fqn());

    HaxeClassModel model = catalog.materializeNowForTests(entries.get(0));
    assertNotNull(model, "the blueprint must render a class");
    assertNotNull(model.getMember("tag", null), "instance members come from the blueprint");
    assertNotNull(model.getMember("make", null), "static members come from the blueprint");
  }

  @Test
  @Timeout(180)
  @DisplayName("unified indexes resolve the generated type by fqn and by name")
  public void testUnifiedIndexesResolveTheGeneratedTypeByFqnAndByName() {
    fillCatalogStably();
    HaxeCompilerTypeCatalogService catalog = HaxeCompilerTypeCatalogService.getInstance(getProject());
    for (GeneratedType entry : catalog.byFqn(GENERATED_FQN)) {
      catalog.materializeNowForTests(entry);
    }

    List<HaxeClass> byFqn = HaxeFullyQualifiedClassNameUnifiedIndex.getByFqn(GENERATED_FQN, getProject(), null);
    assertFalse(byFqn.isEmpty(), "the compiler leg must resolve the generated FQN");
    assertEquals("GeneratedThing", byFqn.get(0).getName());

    Collection<HaxeClass> byName = HaxeClassNameUnifiedIndex.getByNameFiltered("GeneratedThing", getProject(), null);
    assertFalse(byName.isEmpty(), "import-candidate lookups must see the generated type");
  }

  @Test
  @Timeout(180)
  @DisplayName("completion offers the generated type")
  public void testCompletionOffersTheGeneratedType() {
    fillCatalogStably();

    myFixture.configureByFile("Completion.hx");
    myFixture.completeBasic();

    List<String> lookups = myFixture.getLookupElementStrings();
    if (lookups == null) {
      // a single match auto-inserts instead of showing a lookup
      String text = myFixture.getEditor().getDocument().getText();
      assertTrue(text.contains("v:GeneratedThing"),
                 "the sole completion match must have been inserted, document:\n" + text);
    } else {
      assertTrue(lookups.contains("GeneratedThing"),
                 "type completion must offer the generated type, got: " + lookups);
    }
  }

  /**
   * The FIRST fill spawns the compilation server, whose start event queues a
   * cache invalidation on the EDT (correct in production: caches die with
   * server state, background fills re-run). Dispatch that event, then fill
   * again against the now-running server so the catalog stays stable for the
   * test's assertions.
   */
  private void fillCatalogStably() {
    HaxeCompilerTypeCatalogService catalog = HaxeCompilerTypeCatalogService.getInstance(getProject());
    catalog.fillNowForTests();
    UIUtil.dispatchAllInvocationEvents();
    catalog.fillNowForTests();
  }
}
