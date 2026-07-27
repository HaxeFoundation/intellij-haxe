package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.junit5.RunInEdt;
import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Jupiter front for the platform's JUnit3-style
 * {@link DeclarativeInlayHintsProviderTestCase}. The engine keeps the platform
 * comparison logic but swaps its light fixture for the same heavy Haxe module
 * fixture the other code-insight tests use. Lifecycle stays template-method
 * shaped so subclasses keep their {@code useHaxeToolkit(); super.setUp();}
 * ordering; everything runs on the EDT like the JUnit3-era dispatch.
 */
@RunInEdt(writeIntent = true)
public abstract class HaxeInlayTestBase {
  private final Engine engine = new Engine();
  private String testName;

  protected CodeInsightTestFixture myFixture;
  protected String myHaxeToolkit = null;

  protected HaxeInlayTestBase() {
    Logger.setUnitTestMode();
    Logger.setFactory(category -> {
      DefaultLogger logger = new DefaultLogger(category);
      logger.setLevel(LogLevel.WARNING);
      return logger;
    });
  }

  @BeforeEach
  final void runSetUp(TestInfo info) throws Exception {
    testName = info.getTestMethod().orElseThrow().getName();
    engine.setName(testName);
    setUp();
  }

  @AfterEach
  final void runTearDown() throws Exception {
    tearDown();
  }

  protected abstract String getBasePath();

  protected boolean toAddSourceRoot() {
    return true;
  }

  protected boolean usingHaxeToolkit() {
    return null != myHaxeToolkit;
  }

  public void setUp() throws Exception {
    engine.start();
    myFixture = engine.fixture();
  }

  protected void tearDown() throws Exception {
    try {
      engine.stop();
    }
    finally {
      myFixture = null;
    }
  }

  public String getName() {
    return testName;
  }

  public String getTestName(boolean lowercaseFirstLetter) {
    return PlatformTestUtil.getTestName(testName, lowercaseFirstLetter);
  }

  public String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }

  public void setTestStyleSettings(int indent) {
    Project project = myFixture.getProject();
    CodeStyleSettings currSettings = CodeStyleSettingsManager.getSettings(project);
    assertNotNull(currSettings);
    CodeStyleSettings tempSettings = currSettings.clone();
    CodeStyleSettings.IndentOptions indentOptions = tempSettings.getIndentOptions(HaxeFileType.INSTANCE);
    indentOptions.INDENT_SIZE = indent;
    assertNotNull(indentOptions);
    CodeStyleSettingsManager.getInstance(project).setTemporarySettings(tempSettings);
  }

  public void useHaxeToolkit() {
    useHaxeToolkit(HaxeTestUtils.LATEST);
  }

  public void useHaxeToolkit(String version) {
    String relativeParent = HaxeTestUtils.getAbsoluteToolkitPath(version);
    assert (null != relativeParent);
    myHaxeToolkit = relativeParent;
  }

  protected void doTest(InlayHintsProvider inlayHintsProvider) throws Exception {
    String name = getTestDataPath() + getTestName(false) + ".hx";
    String data = Files.readString(Path.of(name));
    //seems to be an issue with windows line endings and inlays so to avoid any issues we replace them here.
    data = data.replaceAll("\\r\\n?", "\n");

    engine.run(data, inlayHintsProvider);
  }

  private final class Engine extends DeclarativeInlayHintsProviderTestCase {
    private final IdeaTestFixtureFactory testFixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();
    private ModuleFixtureBuilder moduleFixtureBuilder;

    // replaces the platform light fixture with the heavy Haxe module fixture;
    // deliberately no super.setUp()/tearDown(), matching the pre-jupiter code
    @Override
    protected void setUp() throws Exception {
      testFixtureFactory.registerFixtureBuilder(HaxeCodeInsightFixtureTestCase.MyHaxeModuleFixtureBuilderImpl.class,
                                                HaxeCodeInsightFixtureTestCase.MyHaxeModuleFixtureBuilderImpl.class);
      final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = testFixtureFactory.createFixtureBuilder(getName());
      myFixture = testFixtureFactory.createCodeInsightFixture(projectBuilder.getFixture());
      moduleFixtureBuilder = projectBuilder.addModule(HaxeCodeInsightFixtureTestCase.MyHaxeModuleFixtureBuilderImpl.class);

      if (toAddSourceRoot()) {
        moduleFixtureBuilder.addSourceContentRoot(myFixture.getTempDirPath());
      }
      else {
        moduleFixtureBuilder.addContentRoot(myFixture.getTempDirPath());
      }

      if (usingHaxeToolkit()) {
        moduleFixtureBuilder.addSourceContentRoot(myHaxeToolkit);
      }
      myFixture.setTestDataPath(getTestDataPath());
      myFixture.setUp();

      // disable RecursionPrevention assert as type inference will cause several RecursionPrevention events,
      // and want to be able to test inlays for inferred types
      RecursionManager.disableAssertOnRecursionPrevention(myFixture.getProjectDisposable());
      RecursionManager.disableMissedCacheAssertions(myFixture.getProjectDisposable());
    }

    @Override
    protected void tearDown() throws Exception {
      try {
        HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
        myFixture.tearDown();
      }
      catch (Throwable e) {
        addSuppressedException(e);
      }
    }

    void start() throws Exception {
      setUp();
    }

    void stop() throws Exception {
      tearDown();
    }

    CodeInsightTestFixture fixture() {
      return myFixture;
    }

    void run(String data, InlayHintsProvider provider) {
      doTestProvider("testFile.hx", data, provider, Map.of(), null, true);
    }
  }
}
