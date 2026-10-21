package com.intellij.plugins.haxe.ide;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Termination guard for type inference over mutually recursive, untyped std
 * code. haxe/ds/ArraySort.hx (rec/doMerge/compare/swap, parameters without
 * type tags) historically drove usage-based inference into call-evaluation
 * cycles that never converged: highlighting in the IDE ran for minutes at
 * full CPU without producing annotations. The timeout is the assertion -
 * a hang here means an inference cycle escaped its caches and budgets again.
 */
@DisplayName("Annotation: recursive std inference (ArraySort)")
public class HaxeRecursiveStdInferenceTest extends HaxeToolkitLightFixtureTestCase {

  /** A second pass on the same PSI tick is served from the resolve caches; the uncached case runs thousands. */
  private static final int CACHED_PASS_RUNS_BOUND = 0;

  @Override
  protected String getBasePath() {
    return "/annotation/";
  }

  @Test
  @DisplayName("array sort highlighting terminates in bounded time")
  @Timeout(value = 300, unit = TimeUnit.SECONDS)
  public void testArraySortHighlightingTerminates() {
    String toolkit = HaxeTestUtils.getAbsoluteToolkitPath(HaxeTestUtils.LATEST);
    String path = FileUtil.toSystemIndependentName(toolkit + "/haxe/ds/ArraySort.hx");
    VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    assertNotNull(file, "ArraySort.hx missing from the test toolkit std: " + path);

    myFixture.openFileInEditor(file);
    myFixture.enableInspections(getAnnotatorBasedInspection());
    HaxeResolver resolver = HaxeResolver.getInstance(getProject());

    myFixture.doHighlighting();

    // same PSI tick: evaluation/call caches must hold, so a re-run resolves nothing anew
    int before = resolver.pipelineRuns();
    myFixture.doHighlighting();
    int secondPassRuns = resolver.pipelineRuns() - before;

    assertTrue(secondPassRuns <= CACHED_PASS_RUNS_BOUND, "pipeline runs on the second pass: " + secondPassRuns);
  }
}
