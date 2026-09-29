package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedSymbolInspection;
import com.intellij.testFramework.InspectionTestUtil;
import com.intellij.testFramework.InspectionsKt;
import com.intellij.testFramework.junit5.RunInEdt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.util.HaxeTestUtils.BASE_TEST_DATA_PATH;

/**
 * Jupiter front for the platform's JUnit3-style {@link LightQuickFixTestCase};
 * the engine keeps the before/after-file comparison logic and enables the
 * unresolved-symbol inspection before each action.
 */
@RunInEdt(writeIntent = true)
public abstract class HaxeQuickFixTestBase {
  private final Engine engine = new Engine();

  public HaxeQuickFixTestBase() {
    Logger.setUnitTestMode();
    Logger.setFactory(category -> {
      DefaultLogger logger = new DefaultLogger(category);
      logger.setLevel(LogLevel.WARNING);
      return logger;
    });
  }

  @BeforeEach
  final void runSetUp(TestInfo info) throws Exception {
    engine.setName(info.getTestMethod().orElseThrow().getName());
    engine.start();
  }

  @AfterEach
  final void runTearDown() throws Exception {
    engine.stop();
  }

  protected abstract String getBasePath();

  protected void doSingleTest(String fileSuffix) {
    engine.doSingleTest(fileSuffix);
  }

  private final class Engine extends LightQuickFixTestCase {
    @Override
    protected @NonNls @NotNull String getTestDataPath() {
      return BASE_TEST_DATA_PATH + "/quickfix";
    }

    @Override
    protected @NotNull String getBasePath() {
      return HaxeQuickFixTestBase.this.getBasePath();
    }

    @Override
    protected void setUp() throws Exception {
      myTestDataPath = BASE_TEST_DATA_PATH;
      super.setUp();
    }

    @Override
    protected void beforeActionStarted(String testName, String contents) {
      List<InspectionProfileEntry> inspections = InspectionTestUtil.instantiateTools(Set.of(HaxeUnresolvedSymbolInspection.class));
      InspectionsKt.enableInspectionTools(getProject(), getTestRootDisposable(), inspections.toArray(new InspectionProfileEntry[0]));
    }

    void start() throws Exception {
      setUp();
    }

    void stop() throws Exception {
      tearDown();
    }

    @Override
    public void doSingleTest(String fileSuffix) {
      super.doSingleTest(fileSuffix);
    }
  }
}
