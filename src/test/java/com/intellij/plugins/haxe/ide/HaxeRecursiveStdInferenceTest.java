package com.intellij.plugins.haxe.ide;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
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

    long firstPassStart = System.nanoTime();
    myFixture.doHighlighting();
    long firstPassMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - firstPassStart);

    // same PSI tick: evaluation/call caches must hold, so a re-run is cheap
    long secondPassStart = System.nanoTime();
    myFixture.doHighlighting();
    long secondPassMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - secondPassStart);

    System.out.println("ArraySort highlighting: first pass " + firstPassMs + "ms, second pass " + secondPassMs + "ms");
    assertTrue(firstPassMs < 120_000, "first highlighting pass took " + firstPassMs + "ms - inference is not converging");
    assertTrue(secondPassMs < 10_000, "second pass took " + secondPassMs + "ms - results are not being served from caches");
  }
}
