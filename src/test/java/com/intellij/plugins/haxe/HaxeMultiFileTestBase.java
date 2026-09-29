package com.intellij.plugins.haxe;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.testFramework.junit5.RunInEdt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.List;
import com.intellij.openapi.util.RecursionManager;

/**
 * Jupiter front for the platform's JUnit3-style {@link MultiFileTestCase}
 * (before/after directory-tree comparison). {@link #myPsiManager} and
 * {@link #myProject} mirror the engine's fields after setUp so test bodies
 * read as before.
 */
@RunInEdt(allMethods = true, writeIntent = true)
public abstract class HaxeMultiFileTestBase {
  private final Engine engine = new Engine();
  private final List<Throwable> suppressedExceptions = new ArrayList<>();

  protected PsiManager myPsiManager;
  protected Project myProject;

  @BeforeEach
  final void runSetUp(TestInfo info) throws Exception {
    engine.setName(info.getTestMethod().orElseThrow().getName());
    setUp();
    myPsiManager = engine.psiManager();
    myProject = engine.project();
  }

  @AfterEach
  final void runTearDown() throws Exception {
    try {
      tearDown();
    }
    finally {
      myPsiManager = null;
      myProject = null;
    }
    if (!suppressedExceptions.isEmpty()) {
      Exception failure = new Exception("suppressed exception(s) during tearDown");
      suppressedExceptions.forEach(failure::addSuppressed);
      suppressedExceptions.clear();
      throw failure;
    }
  }

  protected void addSuppressedException(@NotNull Throwable e) {
    suppressedExceptions.add(e);
  }

  protected void setUp() throws Exception {
    engine.start();
    // Same opt-outs as HaxeCodeInsightFixtureTestCase: type inference fires
    // recursion preventions by design, and the resolver deliberately
    // suppresses ResolveCache writes for uncertain resolves (see
    // HaxeResolveFrames.suppressCacheWrite) - the platform's missed-cache
    // assertion would turn both into test failures.
    RecursionManager.disableAssertOnRecursionPrevention(engine.getTestRootDisposable());
    RecursionManager.disableMissedCacheAssertions(engine.getTestRootDisposable());
  }

  protected void tearDown() throws Exception {
    engine.stop();
  }

  protected abstract String getTestRoot();

  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  protected ModuleType getModuleType() {
    return HaxeModuleType.getInstance();
  }

  /** Mirror of MultiFileTestCase.PerformAction, which is protected there. */
  @FunctionalInterface
  public interface PerformAction {
    void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception;
  }

  protected void doTest(PerformAction performAction) throws Exception {
    engine.run(performAction);
  }

  private final class Engine extends MultiFileTestCase {
    @Override
    protected String getTestDataPath() {
      return HaxeMultiFileTestBase.this.getTestDataPath();
    }

    @NotNull
    @Override
    protected String getTestRoot() {
      return HaxeMultiFileTestBase.this.getTestRoot();
    }

    @Override
    protected ModuleType getModuleType() {
      return HaxeMultiFileTestBase.this.getModuleType();
    }

    void start() throws Exception {
      setUp();
    }

    void stop() throws Exception {
      tearDown();
    }

    PsiManager psiManager() {
      return myPsiManager;
    }

    Project project() {
      return myProject;
    }

    void run(HaxeMultiFileTestBase.PerformAction performAction) throws Exception {
      doTest(performAction::performAction);
    }
  }
}
