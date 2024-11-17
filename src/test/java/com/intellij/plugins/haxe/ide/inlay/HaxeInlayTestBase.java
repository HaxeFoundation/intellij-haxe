package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public abstract class HaxeInlayTestBase extends DeclarativeInlayHintsProviderTestCase {

  private final IdeaTestFixtureFactory testFixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();
  private ModuleFixtureBuilder moduleFixtureBuilder;


  protected HaxeInlayTestBase() {
    super();
    Logger.setUnitTestMode();
    Logger.setFactory(category -> {
      DefaultLogger logger = new DefaultLogger(category);
      logger.setLevel(LogLevel.WARNING);
      return logger;
    });
  }

  @Override
  protected abstract String getBasePath();

  protected boolean toAddSourceRoot() {
    return true;
  }

  protected boolean usingHaxeToolkit() {
    return null != myHaxeToolkit;
  }


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
    LanguageLevelProjectExtension.getInstance(getProject()).setLanguageLevel(LanguageLevel.JDK_1_8);

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


  public String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }


  public void setTestStyleSettings(int indent) {
    Project project = getProject();
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

  protected String myHaxeToolkit = null;

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

    //doTestProvider("testFile.hx", data, inlayHintsProvider, Map.of(),  true);
    doTestProvider("testFile.hx", data, inlayHintsProvider, Map.of(), null, true);
  }
}
