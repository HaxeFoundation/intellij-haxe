package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.plugins.haxe.lang.psi.stubs.type.HaxeFileElementType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Verifies that HaxeFileElementType exposes a static, swappable readiness gate
 * for stub creation. The gate must be a non-null Runnable so callers can invoke
 * it unconditionally without a null-check.
 *
 * <p>Production wiring of the gate to {@code HaxeDefineDetectionManager.awaitReady}
 * is covered separately by {@code HaxelibProjectStartActivityGateWiringTest}.
 */
public class HaxeFileElementTypeReadinessGateTest {

  private Runnable originalGate;

  @Before
  public void captureOriginal() {
    originalGate = HaxeFileElementType.READINESS_GATE;
  }

  @After
  public void restore() {
    HaxeFileElementType.READINESS_GATE = originalGate;
  }

  @Test
  public void readinessGate_hasNonNullDefault() {
    assertNotNull(
      "Default gate must be non-null so the stub builder can call it unconditionally",
      HaxeFileElementType.READINESS_GATE);
  }

  @Test
  public void readinessGate_canBeReplacedAndInvoked() {
    AtomicInteger calls = new AtomicInteger();
    Runnable spy = calls::incrementAndGet;
    HaxeFileElementType.READINESS_GATE = spy;

    assertSame("Replacement must be observable through the same static field",
               spy, HaxeFileElementType.READINESS_GATE);

    HaxeFileElementType.READINESS_GATE.run();
    HaxeFileElementType.READINESS_GATE.run();
    assertEquals("Each invocation must reach the replacement", 2, calls.get());
  }
}
