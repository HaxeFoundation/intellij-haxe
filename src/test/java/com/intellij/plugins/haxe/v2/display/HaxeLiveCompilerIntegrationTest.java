package com.intellij.plugins.haxe.v2.display;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.fqn.HaxeFullyQualifiedClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeServerAssignment;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerTypeCatalogService.GeneratedType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeDisplayConnectionMode;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeServerGeneration;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.Connected;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
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
 * type, the unified indexes resolving it, completion offering it, and
 * go-to-declaration on a member a build macro generated.
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

  private VirtualFile mainFile;

  @Override
  protected String getBasePath() {
    return "/liveCompiler/";
  }

  @BeforeEach
  void wireCompilerProject() {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping live compiler integration test");

    // real files on disk: the compilation server compiles what the fixture copied
    VirtualFile buildFile = myFixture.copyFileToProject("build.hxml");
    mainFile = myFixture.copyFileToProject("Main.hx");
    myFixture.copyFileToProject("GenMacro.hx");
    myFixture.copyFileToProject("Settings.hx");
    myFixture.copyFileToProject("AccessorMacro.hx");

    // the module's Build command as the tool window's row writes it; the
    // compiler features work without one too (see the test that clears it)
    String containerId = myFixture.getModule().getName();
    HaxeEnvironmentStore.CompileCommand command =
      new HaxeEnvironmentStore.CompileCommand(buildFile.getPath(), null, "");
    HaxeEnvironmentStore.getInstance(getProject()).setCompileCommand(containerId, command);
    // the catalog and the blueprints answer only while the compiler features are on
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setCompilerIdeFeaturesEnabled(true);
    settings.setGeneratedMemberPreviewEnabled(true);
  }

  @AfterEach
  void stopCompilationServer() {
    HaxeCompilationServerManager.getInstance(getProject()).stopAll();
    HaxeCompilerSettings.getInstance(getProject()).setDisplayConnectionMode(HaxeServerGeneration.HAXE_4, HaxeDisplayConnectionMode.DEFAULT);
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

  @Test
  @Timeout(180)
  @DisplayName("go to declaration on a build macro member opens the generated preview")
  public void testGoToDeclarationOnABuildMacroMemberOpensTheGeneratedPreview() {
    PsiElement[] targets = gotoDeclarationTargetsOfGeneratedGetter();

    assertEquals(1, targets.length);
    PomTargetPsiElement wrapper = assertInstanceOf(PomTargetPsiElement.class, targets[0]);
    HaxeGeneratedPreviewTarget preview = assertInstanceOf(HaxeGeneratedPreviewTarget.class, wrapper.getTarget());
    assertEquals(HaxeGeneratedPreviewSpot.ofType("Settings", "getWidth"), preview.spot());
  }

  @Test
  @Timeout(180)
  @DisplayName("go to declaration on a build macro member opens the generated preview without a build command")
  public void testGoToDeclarationOnABuildMacroMemberOpensTheGeneratedPreviewWithoutABuildCommand() {
    String containerId = myFixture.getModule().getName();
    HaxeEnvironmentStore.getInstance(getProject()).setCompileCommand(containerId, null);

    PsiElement[] targets = gotoDeclarationTargetsOfGeneratedGetter();

    assertEquals(1, targets.length);
    PomTargetPsiElement wrapper = assertInstanceOf(PomTargetPsiElement.class, targets[0]);
    assertInstanceOf(HaxeGeneratedPreviewTarget.class, wrapper.getTarget());
  }

  @Test
  @Timeout(180)
  @DisplayName("go to declaration on a build macro member lands on the macro position while the preview is off")
  public void testGoToDeclarationOnABuildMacroMemberLandsOnTheMacroPositionWhileThePreviewIsOff() {
    HaxeCompilerSettings.getInstance(getProject()).setGeneratedMemberPreviewEnabled(false);

    PsiElement[] targets = gotoDeclarationTargetsOfGeneratedGetter();

    assertEquals(1, targets.length);
    HaxeNamedComponent sourceField = PsiTreeUtil.getParentOfType(targets[0], HaxeNamedComponent.class, false);
    assertNotNull(sourceField, "the macro gave the getter the position of its source field");
    assertEquals("width", sourceField.getName());
  }

  @Test
  @Timeout(60)
  @DisplayName("sdk detection reads the compiler version")
  public void testSdkDetectionReadsTheCompilerVersion() throws Exception {
    HaxeSdkData sdkData = HaxeSdkUtil.testHaxeSdk(haxeHomeOnPath());

    assertNotNull(sdkData, "the folder of the haxe on the PATH is an SDK home");
    assertEquals(haxeVersionOnPath(), sdkData.getVersion());
  }

  @Test
  @Timeout(180)
  @DisplayName("the build server answers when it is the connection mode")
  public void testTheBuildServerAnswersWhenItIsTheConnectionMode() throws Exception {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setDisplayConnectionMode(HaxeServerGeneration.HAXE_4, HaxeDisplayConnectionMode.SHARED_SERVER);
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(getProject());
    DisplayContext context = HaxeReadActions.compute(() -> displayService.contextFor(mainFile));

    Connected completion = onPooledThread(() -> displayService.connectFor(context, DisplayMethods.COMPLETION));

    assertNotNull(completion, "the build server, started with --server-listen, answers completion");
    boolean buildServerRuns = HaxeCompilationServerManager.getInstance(getProject()).runningPort(sharedServer().id()) > 0;
    assertTrue(buildServerRuns, "the requests went to the build server");
  }

  /**
   * The FIRST fill spawns the compilation server, whose start event queues a
   * cache invalidation on the EDT (correct in production: caches die with
   * server state, background fills re-run). Dispatch that event, then fill
   * again against the now-running server so the catalog stays stable for the
   * test's assertions.
   */
  /** The project default SDK's shared server instance. */
  private HaxeServerAssignment sharedServer() {
    return HaxeServerAssignment.shared(getProject(), null);
  }

  private void fillCatalogStably() {
    HaxeCompilerTypeCatalogService catalog = HaxeCompilerTypeCatalogService.getInstance(getProject());
    catalog.fillNowForTests();
    UIUtil.dispatchAllInvocationEvents();
    catalog.fillNowForTests();
  }

  /** Blueprint hydration against the running server, twice for the reason {@link #fillCatalogStably()} gives. */
  private void hydrateStably(@NotNull DisplayContext context, @NotNull String dotPath) {
    HaxeCompilerResolveService resolveService = HaxeCompilerResolveService.getInstance(getProject());
    resolveService.hydrateNowForTests(context, dotPath);
    UIUtil.dispatchAllInvocationEvents();
    resolveService.hydrateNowForTests(context, dotPath);
  }

  /** The compiler handler's targets for {@code settings.getWidth()} in Main.hx, once the blueprint declaring it is in. */
  private PsiElement @NotNull [] gotoDeclarationTargetsOfGeneratedGetter() {
    myFixture.configureFromExistingVirtualFile(mainFile);
    DisplayContext context = HaxeCompilerDisplayService.getInstance(getProject()).contextFor(mainFile);
    assertNotNull(context, "Main.hx must have the build's display context");
    hydrateStably(context, "Settings");

    int offset = myFixture.getEditor().getDocument().getText().indexOf("getWidth") + 1;
    PsiElement source = myFixture.getFile().findElementAt(offset);
    HaxeCompilerGotoDeclarationHandler handler = new HaxeCompilerGotoDeclarationHandler();
    PsiElement[] targets = handler.getGotoDeclarationTargets(source, offset, myFixture.getEditor());
    assertNotNull(targets, "the compiler handler must answer for the generated getter");
    return targets;
  }
}
