package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.display.haxe3.Haxe3DisplayClient;
import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeServerAssignment;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeDisplayProcessManager;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeDisplayConnectionMode;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeServerGeneration;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.Connected;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The compiler services against a real haxe 3 compilation server, opt-in with
 * {@code -PliveCompilerTests=true}; skips itself unless the haxe on the PATH
 * is a haxe 3. No SDK is registered, so the services tell the server's
 * protocol from its {@code -version} answer.
 */
@DisplayName("Compiler services: haxe3 integration (live)")
public class HaxeLiveHaxe3IntegrationTest extends HaxeCodeInsightFixtureTestCase {

  private static final String BUILD_HXML_SOURCE = """
    -cp src
    -main Main
    -js out.js
    """;
  // "é" before the code makes the byte columns haxe 3 reports differ from the editor's characters
  private static final String MAIN_HX_SOURCE = """
    import shapes.Circle;

    class Main {
      static var label = "é";

      static function main() {
        var circle = new Circle(2.0);
        trace(label + circle.area());
      }
    }
    """;
  private static final String CIRCLE_HX_SOURCE = """
    package shapes;

    class Circle {
      public var radius:Float;

      public function new(radius:Float) {
        this.radius = radius;
      }

      public function area():Float {
        return Math.PI * radius * radius;
      }
    }
    """;
  // a file the build does not reach from its main: haxe 3 types a display file all the same
  private static final String TYPING_HX_SOURCE = """
    import shapes.Circle;

    class Typing {
      static function draw(circle:Circle) {
        circle.ra;
      }
    }
    """;

  private VirtualFile mainFile;
  private VirtualFile typingFile;

  @Override
  protected String getBasePath() {
    // every fixture is created in-project by the test itself
    return "";
  }

  @BeforeEach
  void wireHaxe3Project() throws IOException, InterruptedException {
    assumeTrue(haxe3OnPath(), "haxe on the PATH is no haxe 3 - skipping live haxe 3 integration test");

    VirtualFile buildFile = myFixture.addFileToProject("build.hxml", BUILD_HXML_SOURCE).getVirtualFile();
    mainFile = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE).getVirtualFile();
    myFixture.addFileToProject("src/shapes/Circle.hx", CIRCLE_HX_SOURCE);
    typingFile = myFixture.addFileToProject("src/Typing.hx", TYPING_HX_SOURCE).getVirtualFile();

    String containerId = myFixture.getModule().getName();
    HaxeEnvironmentStore.CompileCommand command = new HaxeEnvironmentStore.CompileCommand(buildFile.getPath(), null, "");
    HaxeEnvironmentStore.getInstance(getProject()).setCompileCommand(containerId, command);
    HaxeCompilerSettings.getInstance(getProject()).setCompilerIdeFeaturesEnabled(true);
  }

  @AfterEach
  void stopCompilationServer() {
    HaxeCompilationServerManager.getInstance(getProject()).stopAll();
    HaxeCompilerSettings.getInstance(getProject()).setDisplayConnectionMode(HaxeServerGeneration.HAXE_3, HaxeDisplayConnectionMode.DEFAULT);
  }

  @Test
  @Timeout(180)
  @DisplayName("a haxe 3 server gets the haxe 3 client and no server introspection")
  public void testAHaxe3ServerGetsTheHaxe3ClientAndNoServerIntrospection() throws Exception {
    DisplayContext context = contextOf(mainFile);

    Connected completion = onPooledThread(() -> displayService().connectFor(context, DisplayMethods.COMPLETION));
    Connected introspection = onPooledThread(() -> displayService().connectFor(context, DisplayMethods.SERVER_CONTEXTS));

    assertNotNull(completion, "a haxe 3 server answers completion");
    assertInstanceOf(Haxe3DisplayClient.class, completion.client());
    assertNull(introspection, "a haxe 3 server has no server introspection");
  }

  @Test
  @Timeout(180)
  @DisplayName("a haxe 3 project gets a dedicated display process")
  public void testAHaxe3ProjectGetsADedicatedDisplayProcess() throws Exception {
    DisplayContext context = contextOf(mainFile);

    Connected completion = onPooledThread(() -> displayService().connectFor(context, DisplayMethods.COMPLETION));

    assertNotNull(completion, "a haxe 3 server answers completion");
    boolean dedicated = HaxeDisplayProcessManager.getInstance(getProject()).runningIdFor(sharedServer()) > 0;
    assertTrue(dedicated, "the requests run on a dedicated display process over stdio");
  }

  @Test
  @Timeout(180)
  @DisplayName("definition of a method call becomes its declaration")
  public void testDefinitionOfAMethodCallBecomesItsDeclaration() throws Exception {
    int areaCall = MAIN_HX_SOURCE.indexOf("circle.area") + "circle.".length();
    HaxeCompilerNavigationService navigation = HaxeCompilerNavigationService.getInstance(getProject());

    List<Location> locations = onPooledThread(() -> navigation.definition(mainFile, areaCall));

    assertEquals(1, locations.size());
    PsiElement declaration = HaxeReadActions.compute(() -> declarationAt(locations.getFirst()));
    assertEquals("area", assertInstanceOf(HaxeNamedComponent.class, declaration).getName());
  }

  @Test
  @Timeout(180)
  @DisplayName("completion after a dot offers the fields")
  public void testCompletionAfterADotOffersTheFields() throws Exception {
    int caret = TYPING_HX_SOURCE.indexOf("circle.ra") + "circle.ra".length();
    HaxeCompilerCompletionService completionService = HaxeCompilerCompletionService.getInstance(getProject());

    CompletionList list = HaxeReadActions.compute(() -> completionService.complete(typingFile, caret, false));

    assertNotNull(list, "the compiler answers the completion");
    List<String> names = list.items().stream().map(CompletionItem::name).toList();
    assertTrue(names.containsAll(List.of("radius", "area")), "fields of Circle: " + names);
  }

  @Test
  @Timeout(180)
  @DisplayName("diagnostics follow an unsaved buffer")
  public void testDiagnosticsFollowAnUnsavedBuffer() throws Exception {
    String broken = MAIN_HX_SOURCE.replace("circle.area()", "circle.missing()");
    DisplayContext context = contextOf(mainFile);

    List<FileDiagnostics> results = onPooledThread(() -> displayService().diagnostics(context, mainFile.getPath(), broken));

    assertNotNull(results, "the compiler answers the diagnostics");
    boolean flagged = results.stream()
      .flatMap(file -> file.diagnostics().stream())
      .anyMatch(diagnostic -> diagnostic.messageArg().contains("missing"));
    assertTrue(flagged, "the unsaved buffer's unknown field must be reported: " + results);
  }

  @Test
  @Timeout(180)
  @DisplayName("the build server answers when it is the connection mode")
  public void testTheBuildServerAnswersWhenItIsTheConnectionMode() throws Exception {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setDisplayConnectionMode(HaxeServerGeneration.HAXE_3, HaxeDisplayConnectionMode.SHARED_SERVER);
    DisplayContext context = contextOf(mainFile);

    Connected completion = onPooledThread(() -> displayService().connectFor(context, DisplayMethods.COMPLETION));

    assertNotNull(completion, "the build server, started with --wait, answers completion");
    boolean buildServerRuns = HaxeCompilationServerManager.getInstance(getProject()).runningPort(sharedServer().id()) > 0;
    assertTrue(buildServerRuns, "the requests went to the build server");
  }

  @Test
  @Timeout(60)
  @DisplayName("sdk detection reads the version haxe 3 prints to stderr")
  public void testSdkDetectionReadsTheVersionHaxe3PrintsToStderr() throws Exception {
    HaxeSdkData sdkData = HaxeSdkUtil.testHaxeSdk(haxeHomeOnPath());

    assertNotNull(sdkData, "the folder of the haxe on the PATH is an SDK home");
    assertEquals(haxeVersionOnPath(), sdkData.getVersion());
  }

  /** The project default SDK's shared server instance. */
  private HaxeServerAssignment sharedServer() {
    return HaxeServerAssignment.shared(getProject(), null);
  }

  private HaxeCompilerDisplayService displayService() {
    return HaxeCompilerDisplayService.getInstance(getProject());
  }

  private DisplayContext contextOf(VirtualFile file) {
    DisplayContext context = HaxeReadActions.compute(() -> displayService().contextFor(file));
    assertNotNull(context, "the file belongs to the build");
    return context;
  }

  private PsiElement declarationAt(Location location) {
    PsiFile file = HaxeCompilerLocations.psiFileOf(getProject(), location);
    return file == null ? null : HaxeCompilerLocations.declarationIn(file, location.range());
  }

  /** Whether the haxe on the PATH is a haxe 3. */
  private static boolean haxe3OnPath() throws IOException, InterruptedException {
    return haxeAvailable() && haxeVersionOnPath().startsWith("3.");
  }
}
