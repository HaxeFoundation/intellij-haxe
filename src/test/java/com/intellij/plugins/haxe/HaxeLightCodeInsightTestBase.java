package com.intellij.plugins.haxe;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.junit5.RunInEdt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Jupiter front for the platform's JUnit3-style
 * {@link LightPlatformCodeInsightTestCase}: the engine keeps the platform
 * editor/fixture logic, this class exposes the pieces the Haxe tests use.
 * Lifecycle is template-method shaped ({@link #setUp()}/{@link #tearDown()}
 * overridable, super-chained) and everything runs on the EDT.
 */
@RunInEdt(allMethods = true, writeIntent = true)
public abstract class HaxeLightCodeInsightTestBase {
  private final Engine engine = new Engine();
  private final List<Throwable> suppressedExceptions = new ArrayList<>();
  private String testName;

  @BeforeEach
  final void runSetUp(TestInfo info) throws Exception {
    testName = info.getTestMethod().orElseThrow().getName();
    engine.setName(testName);
    setUp();
  }

  @AfterEach
  final void runTearDown() throws Exception {
    tearDown();
    if (!suppressedExceptions.isEmpty()) {
      Exception failure = new Exception("suppressed exception(s) during tearDown");
      suppressedExceptions.forEach(failure::addSuppressed);
      suppressedExceptions.clear();
      throw failure;
    }
  }

  protected void setUp() throws Exception {
    engine.start();
  }

  protected void tearDown() throws Exception {
    engine.stop();
  }

  @NotNull
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  public String getName() {
    return testName;
  }

  public String getTestName(boolean lowercaseFirstLetter) {
    return PlatformTestUtil.getTestName(testName, lowercaseFirstLetter);
  }

  protected void addSuppressedException(@NotNull Throwable e) {
    suppressedExceptions.add(e);
  }

  protected Project getProject() {
    return engine.project();
  }

  protected Editor getEditor() {
    return engine.editor();
  }

  protected PsiFile getFile() {
    return engine.file();
  }

  protected void configureByFile(String filePath) {
    engine.configureByFile(filePath);
  }

  protected void configureFromFileText(String fileName, String fileText) {
    engine.configureText(fileName, fileText);
  }

  protected void checkResultByFile(String filePath) {
    engine.checkResultByFile(filePath);
  }

  protected void checkResultByFile(String message, String filePath, boolean ignoreTrailingSpaces) {
    engine.checkResultByFile(message, filePath, ignoreTrailingSpaces);
  }

  protected void checkResultByText(String message, String fileText, boolean ignoreTrailingSpaces) {
    engine.checkResultByText(message, fileText, ignoreTrailingSpaces);
  }

  protected void type(char c) {
    engine.type(c);
  }

  private final class Engine extends LightPlatformCodeInsightTestCase {
    @NotNull
    @Override
    protected String getTestDataPath() {
      return HaxeLightCodeInsightTestBase.this.getTestDataPath();
    }

    void start() throws Exception {
      setUp();
    }

    void stop() throws Exception {
      tearDown();
    }

    Project project() {
      return getProject();
    }

    Editor editor() {
      return getEditor();
    }

    PsiFile file() {
      return getFile();
    }

    @Override
    public void configureByFile(@NotNull String filePath) {
      super.configureByFile(filePath);
    }

    void configureText(@NotNull String fileName, @NotNull String fileText) {
      configureFromFileText(fileName, fileText);
    }

    @Override
    public void checkResultByFile(@NotNull String filePath) {
      super.checkResultByFile(filePath);
    }

    @Override
    public void checkResultByFile(String message, @NotNull String filePath, boolean ignoreTrailingSpaces) {
      super.checkResultByFile(message, filePath, ignoreTrailingSpaces);
    }

    @Override
    public void checkResultByText(String message, @NotNull String fileText, boolean ignoreTrailingSpaces) {
      super.checkResultByText(message, fileText, ignoreTrailingSpaces);
    }

    @Override
    public void type(char c) {
      super.type(c);
    }
  }
}
