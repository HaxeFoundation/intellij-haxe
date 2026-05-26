package com.intellij.plugins.haxe.haxelib;

import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import com.intellij.plugins.haxe.lang.psi.stubs.type.HaxeFileElementType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/**
 * Verifies that {@code HaxelibProjectStartActivity.installHaxeFileReadinessGate}
 * replaces the default no-op gate on {@link HaxeFileElementType} with the
 * production version. We do not exercise the full {@code ProjectActivity}
 * here — that requires a Project, which is expensive to construct outside the
 * fixture infrastructure. Instead we test the single seam point of installation.
 */
public class HaxelibProjectStartActivityGateWiringTest {

  // HotSpot caches non-capturing lambdas, so repeated production installs share an
  // instance — plant a test-local sentinel and assert the install replaced *that*.
  private static final Runnable SENTINEL_NOOP = () -> {};

  private Runnable original;

  @Before
  public void captureOriginal() {
    original = HaxeFileElementType.READINESS_GATE;
    HaxeFileElementType.READINESS_GATE = SENTINEL_NOOP;
  }

  @After
  public void restoreOriginal() {
    HaxeFileElementType.READINESS_GATE = original;
    HaxeDefineDetectionManager.moduleDefinitionsMap.clear();
  }

  @Test
  public void installHaxeFileReadinessGate_replacesDefault() {
    HaxelibProjectStartActivity.installHaxeFileReadinessGate();
    assertNotSame("Gate must be replaced with the production version",
                  SENTINEL_NOOP, HaxeFileElementType.READINESS_GATE);
    assertNotNull(HaxeFileElementType.READINESS_GATE);
  }
}
