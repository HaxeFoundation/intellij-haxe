package com.intellij.plugins.haxe.haxelib.definitions;

import com.intellij.openapi.project.Project;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the per-manager readiness latch wired into HaxeDefineDetectionManager.
 *
 * <ul>
 *   <li>{@code awaitReady(timeout)} returns false before {@code recalculateDefinitionsSync}
 *       has been called.</li>
 *   <li>{@code recalculateDefinitionsSync} makes {@code awaitReady} return true thereafter,
 *       even when the underlying recalculation fails (no IDE platform installed in this test
 *       process). The contract is that the latch never deadlocks on transient errors.</li>
 *   <li>The latch only fires once; repeat calls to {@code recalculateDefinitionsSync} are
 *       idempotent fast-paths.</li>
 * </ul>
 *
 * <p>These tests cannot use {@code HaxeCodeInsightFixtureTestCase} because in unit-test mode
 * {@code HaxeConditionalExpression} bypasses {@code HaxeDefineDetectionManager} entirely;
 * we must exercise the manager directly. The Project is a JDK dynamic proxy so we do not
 * have to spin up the IntelliJ platform.
 */
public class HaxeDefineDetectionManagerReadinessTest {

  private HaxeDefineDetectionManager manager;

  @After
  public void tearDown() {
    if (manager != null) {
      manager.dispose();
      manager = null;
    }
    HaxeDefineDetectionManager.moduleDefinitionsMap.clear();
  }

  @Test
  public void awaitReady_returnsFalse_beforeSyncCalled() {
    Project project = newProjectProxy();
    manager = new HaxeDefineDetectionManager(project);

    assertFalse("Manager must not report ready before recalculateDefinitionsSync runs",
                manager.awaitReady(50));
  }

  @Test
  public void awaitReady_returnsTrue_afterSyncCalled() {
    Project project = newProjectProxy();
    manager = new HaxeDefineDetectionManager(project);

    manager.recalculateDefinitionsSync(project);

    assertTrue("Manager must report ready immediately after sync completes",
               manager.awaitReady(0));
  }

  @Test
  public void recalculateDefinitionsSync_isIdempotent() {
    Project project = newProjectProxy();
    manager = new HaxeDefineDetectionManager(project);

    manager.recalculateDefinitionsSync(project);
    long t0 = System.nanoTime();
    manager.recalculateDefinitionsSync(project); // must short-circuit
    long elapsedNs = System.nanoTime() - t0;

    assertTrue("Second sync call must short-circuit (took " + elapsedNs + " ns)",
               elapsedNs < TimeUnit.MILLISECONDS.toNanos(50));
    assertTrue(manager.awaitReady(0));
  }

  @Test
  public void awaitReady_blocksUntilAnotherThreadCallsSync() throws Exception {
    Project project = newProjectProxy();
    manager = new HaxeDefineDetectionManager(project);

    CountDownLatch entered = new CountDownLatch(1);
    AtomicBoolean waiterSawReady = new AtomicBoolean();
    Thread waiter = new Thread(() -> {
      entered.countDown();
      waiterSawReady.set(manager.awaitReady(2_000));
    }, "ready-waiter");
    waiter.start();
    entered.await();

    Thread.sleep(50); // let the waiter park on the latch
    manager.recalculateDefinitionsSync(project);

    waiter.join(2_000);
    assertFalse("Waiter thread should have completed within timeout", waiter.isAlive());
    assertTrue("Waiter must observe ready after the trigger thread completes sync",
               waiterSawReady.get());
  }

  /**
   * JDK dynamic-proxy Project. Methods return null/false by default; identity is
   * proxy-level (two proxies are not equal). {@code getService(...)} returning null
   * causes the IntelliJ platform calls inside recalculateDefinitions to throw, which
   * the production code swallows via try/catch — the latch must still fire.
   */
  private static Project newProjectProxy() {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
          case "equals":
            return args != null && args.length > 0 && args[0] == proxy;
          case "hashCode":
            return System.identityHashCode(proxy);
          case "toString":
            return "ProjectProxy";
          case "isDisposed":
          case "isDefault":
          case "isInitialized":
          case "isOpen":
            return false;
          default:
            // Includes getService, getName, getBasePath, etc. — return null and
            // let the production code's try/catch handle the downstream NPE.
            return null;
        }
      }
    };
    return (Project) Proxy.newProxyInstance(
      HaxeDefineDetectionManagerReadinessTest.class.getClassLoader(),
      new Class<?>[]{Project.class},
      handler);
  }
}
