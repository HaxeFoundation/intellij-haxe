/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe;

import com.intellij.codeInsight.daemon.impl.HighlightVisitorBasedInspection;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.plugins.haxe.ide.HaxeInspectionTestTools;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.application.options.CodeStyle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import com.intellij.testFramework.fixtures.impl.ModuleFixtureBuilderImpl;
import com.intellij.testFramework.fixtures.impl.ModuleFixtureImpl;
import com.intellij.testFramework.junit5.RunInEdt;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Jupiter code-insight fixture base. Lifecycle stays template-method shaped:
 * jupiter drives the final run* hooks, which call the overridable
 * {@link #setUp()}/{@link #tearDown()} so subclasses keep their
 * {@code super.setUp()} ordering (several set {@code myHaxeToolkit} first).
 * All methods run on the EDT, matching the JUnit3-era runBare dispatch.
 */
@RunInEdt(writeIntent = true)
abstract public class HaxeCodeInsightFixtureTestCase {
  private final IdeaTestFixtureFactory testFixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();

  protected CodeInsightTestFixture myFixture;
  private ModuleFixtureBuilder moduleFixtureBuilder;
  private Disposable testRootDisposable;
  private String testName;
  private final List<Throwable> suppressedExceptions = new ArrayList<>();

  protected String myHaxeToolkit = null;

  protected HaxeCodeInsightFixtureTestCase() {
    Logger.setUnitTestMode();
    Logger.setFactory(category -> {
      DefaultLogger logger = new DefaultLogger(category);
      logger.setLevel(LogLevel.WARNING);
      return logger;
    });
    //HaxeDebugLogger.configurePrimaryLoggerToSwallowLogs();
  }

  @BeforeEach
  final void runSetUp(TestInfo info) throws Exception {
    testName = info.getTestMethod().orElseThrow().getName();
    testRootDisposable = Disposer.newDisposable(getClass().getName());
    setUp();
  }

  @AfterEach
  final void runTearDown() throws Exception {
    try {
      tearDown();
    }
    finally {
      Disposer.dispose(testRootDisposable);
    }
    if (!suppressedExceptions.isEmpty()) {
      Exception failure = new Exception("suppressed exception(s) during tearDown");
      suppressedExceptions.forEach(failure::addSuppressed);
      suppressedExceptions.clear();
      throw failure;
    }
  }

  protected void setUp() throws Exception {
    testFixtureFactory.registerFixtureBuilder(MyHaxeModuleFixtureBuilderImpl.class, MyHaxeModuleFixtureBuilderImpl.class);
    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = testFixtureFactory.createFixtureBuilder(getName());
    myFixture = testFixtureFactory.createCodeInsightFixture(projectBuilder.getFixture());
    moduleFixtureBuilder = projectBuilder.addModule(MyHaxeModuleFixtureBuilderImpl.class);

    if (toAddSourceRoot()) {
      moduleFixtureBuilder.addSourceContentRoot(myFixture.getTempDirPath());
    }
    else {
      moduleFixtureBuilder.addContentRoot(myFixture.getTempDirPath());
    }

    if (usingHaxeToolkit()) {
      moduleFixtureBuilder.addSourceContentRoot(myHaxeToolkit);
    }

    tuneFixture(moduleFixtureBuilder);

    String path = getTestDataPath();
    configureRootAccess(path);

    myFixture.setTestDataPath(path);
    myFixture.setUp();

    // disable RecursionPrevention assert as type inference will cause several RecursionPrevention events.
    RecursionManager.disableAssertOnRecursionPrevention(myFixture.getProjectDisposable());
    RecursionManager.disableMissedCacheAssertions(myFixture.getProjectDisposable());

  }

  private void configureRootAccess(String path) {
    // we need to add PluginsPath to avoid problems wih Flex plugin
    // after 2026.2 update it would throw VfsRootAccessNotAllowedError during fixture.setUp()
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), PathManager.getPluginsPath());
    // required for tests that compare results to files in testdata
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), path);
  }

  protected boolean toAddSourceRoot() {
    return true;
  }

  protected boolean usingHaxeToolkit() {
    return null != myHaxeToolkit;
  }

  protected void tearDown() throws Exception {
    try {
      HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
      myFixture.tearDown();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      myFixture = null;
    }
  }

  /** The JUnit3-style test name ({@code testFoo}); still drives fixture-file lookup. */
  public String getName() {
    return testName;
  }

  public String getTestName(boolean lowercaseFirstLetter) {
    return PlatformTestUtil.getTestName(testName, lowercaseFirstLetter);
  }

  public Disposable getTestRootDisposable() {
    return testRootDisposable;
  }

  protected void addSuppressedException(@NotNull Throwable e) {
    suppressedExceptions.add(e);
  }

  /**
   * Return relative path to the test data. Path is relative to the
   * {@link PathManager#getHomePath()}
   *
   * @return relative path to the test data.
   */
  @NonNls
  abstract protected String getBasePath();

  /**
   * Return absolute path to the test data. Not intended to be overridden.
   *
   * @return absolute path to the test data.
   */
  @NonNls
  public String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }

  @SuppressWarnings("rawtypes")
  protected void tuneFixture(final ModuleFixtureBuilder moduleBuilder) throws Exception {
  }

  protected Project getProject() {
    return myFixture.getProject();
  }

  protected PsiManagerEx getPsiManager() {
    return PsiManagerEx.getInstanceEx(getProject());
  }

  public PsiElementFactory getElementFactory() {
    return JavaPsiFacade.getElementFactory(getProject());
  }

  protected Module getModule() {
    return myFixture.getModule();
  }

  /**
   * Use reflection to load an annotator inspection class.
   * Specific to annotation tests, but placed here just to avoid adding yet another single-function base class.
   * <p>
   * When we don't support versions of the plugin prior to v2016.1, we can revert the code to importing
   * the classes directly and get rid of this function.
   *
   * @return - An annotator-based inspection class instance.
   */
  protected InspectionProfileEntry getAnnotatorBasedInspection() {
    HighlightVisitorBasedInspection inspection = new HighlightVisitorBasedInspection();
    inspection.setHighlightErrorElements(false);
    inspection.setRunAnnotators(true);
    return inspection;
  }

  /** Fixture prefix inside the base path; suites whose fixtures sit in a subfolder override. */
  protected String fixturePrefix() {
    return "";
  }

  /**
   * Highlighting over the test-named fixture: the annotator-based inspection
   * plus the semantic-inspection block, minus {@code unsetInspections}.
   */
  protected void doHighlightingTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings,
                                    @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections,
                                    String... additionalFiles) throws Exception {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{fixturePrefix() + getTestName(false) + ".hx"}, additionalFiles));
    myFixture.enableInspections(getAnnotatorBasedInspection());
    myFixture.enableInspections(HaxeInspectionTestTools.semanticInspections(unsetInspections));
    myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
  }

  public void setTestStyleSettings() {
    setTestStyleSettings(2);
  }

  public void setTestStyleSettings(int indent) {
    Project project = getProject();
    CodeStyleSettings currSettings = CodeStyle.getSettings(project);
    assertNotNull(currSettings);
    CodeStyleSettings tempSettings = CodeStyleSettingsManager.getInstance(project).cloneSettings(currSettings);
    CodeStyleSettings.IndentOptions indentOptions = tempSettings.getIndentOptions(HaxeFileType.INSTANCE);
    indentOptions.INDENT_SIZE = indent;
    assertNotNull(indentOptions);
    CodeStyleSettingsManager.getInstance(project).setTemporarySettings(tempSettings);
  }

  public String[] useToolkitFiles(String... files) {
    List<String> tkFiles = HaxeTestUtils.useToolkitFiles(this, HaxeTestUtils.LATEST, files);
    return tkFiles.toArray(new String[tkFiles.size()]);
  }

  public void useHaxeToolkit() {
    useHaxeToolkit(HaxeTestUtils.LATEST);
  }

  public void useHaxeToolkit(String version) {
    String relativeParent = HaxeTestUtils.getAbsoluteToolkitPath(version);
    assert (null != relativeParent);
    myHaxeToolkit = relativeParent;
  }

  public CodeInsightTestFixture getFixture() {
    return myFixture;
  }

  /** The element at the editor caret — the context anchor the debugger/fragment tests hang their expressions on. */
  protected PsiElement contextAtCaret() {
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(context, "context element at the caret");
    return context;
  }

  /** Whether a haxe compiler is on the PATH - the gate live tests check via assumeTrue before compiling anything. */
  public static boolean haxeAvailable() {
    return toolAvailable("haxe", "--version");
  }

  /** Whether the utest haxelib is installed - the gate for live tests compiling against real utest. */
  public static boolean utestAvailable() {
    return toolAvailable("haxelib", "path", "utest");
  }

  /** Whether the munit haxelib is installed - the gate for live tests compiling against real munit. */
  public static boolean munitAvailable() {
    return toolAvailable("haxelib", "path", "munit");
  }

  /** Whether the buddy haxelib is installed - the gate for live tests compiling against real buddy. */
  public static boolean buddyAvailable() {
    return toolAvailable("haxelib", "path", "buddy");
  }

  /** Whether the tink_unittest haxelib is installed - the gate for live tests compiling against real tink. */
  public static boolean tinkAvailable() {
    return toolAvailable("haxelib", "path", "tink_unittest");
  }

  /** Whether a neko runtime is on the PATH - the gate for live tests launching neko artifacts. */
  public static boolean nekoAvailable() {
    return toolAvailable("neko", "-version");
  }

  /** Whether the AIR_SDK environment variable points at an SDK with adl - the gate for live flash-family test runs. */
  public static boolean adlAvailable() {
    String airSdk = System.getenv("AIR_SDK");
    if (airSdk == null || airSdk.isBlank()) return false;
    return Files.isRegularFile(Path.of(airSdk, "bin", HaxeSdkUtilBase.getExecutableName("adl")));
  }

  private static boolean toolAvailable(String... command) {
    try {
      Process process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .start();
      // haxelib can stop on an interactive prompt with nobody on its stdin
      // pipe; a hung probe must read as "not available", not hang the suite
      if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return false;
      }
      return process.exitValue() == 0;
    }
    catch (IOException | InterruptedException e) {
      return false;
    }
  }

  public static class MyHaxeModuleFixtureBuilderImpl extends ModuleFixtureBuilderImpl {
    public MyHaxeModuleFixtureBuilderImpl(final TestFixtureBuilder<? extends IdeaProjectTestFixture> testFixtureBuilder) {
      super(new HaxeModuleType(), testFixtureBuilder);
    }

    @NotNull
    @Override
    protected ModuleFixture instantiateFixture() {
      return new ModuleFixtureImpl(this);
    }
  }
}
