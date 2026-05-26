package com.intellij.plugins.haxe.haxelib.definitions;

import com.intellij.openapi.module.Module;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Verifies that the static moduleDefinitionsMap can be safely read and written
 * concurrently from the indexing thread and the haxelib updater queue. With a
 * plain HashMap this test fails (ConcurrentModificationException or lost
 * writes); with a ConcurrentHashMap it passes.
 *
 * <p>The Module instances used here are JDK dynamic proxies — they need only
 * identity semantics (equals/hashCode) to function as map keys, and any other
 * Module API call would throw UnsupportedOperationException. This sidesteps the
 * IntelliJ platform's Module interface evolution across SDK versions.
 */
public class HaxeDefineDetectionManagerConcurrencyTest {

  private static final int WRITER_THREADS = 4;
  private static final int READER_THREADS = 4;
  private static final int REMOVER_THREADS = 2;
  // High op count chosen to reliably surface ConcurrentModificationException
  // from HashMap's iterator on Apple Silicon — lower values can pass by accident
  // when the readers happen to never observe a writer mid-rehash. We also
  // include remover threads to maximise iterator turbulence.
  private static final int OPS_PER_THREAD = 50_000;

  @Test
  public void concurrentPutAndIterate_doesNotThrow() throws Exception {
    // We grab the same instance the production code uses so the test stays
    // honest about the actual shared mutable state.
    Map<Module, Map<String, String>> target = HaxeDefineDetectionManager.moduleDefinitionsMap;
    target.clear();

    ExecutorService pool = Executors.newFixedThreadPool(
      WRITER_THREADS + READER_THREADS + REMOVER_THREADS);
    AtomicInteger failures = new AtomicInteger();
    Module[] sentinels = new Module[16];
    for (int i = 0; i < sentinels.length; i++) {
      sentinels[i] = newSentinelModule("m" + i);
    }

    try {
      for (int w = 0; w < WRITER_THREADS; w++) {
        final int wi = w;
        pool.submit(() -> {
          for (int i = 0; i < OPS_PER_THREAD; i++) {
            try {
              Module m = sentinels[(wi + i) % sentinels.length];
              Map<String, String> defs = new HashMap<>();
              defs.put("k" + i, "v" + i);
              target.put(m, defs);
            } catch (RuntimeException e) {
              failures.incrementAndGet();
            }
          }
        });
      }
      for (int r = 0; r < READER_THREADS; r++) {
        pool.submit(() -> {
          for (int i = 0; i < OPS_PER_THREAD; i++) {
            try {
              // Iteration mirrors getAllDefinitions's stream over entries.
              target.entrySet().stream()
                .map(Map.Entry::getValue)
                .flatMap(m -> m.entrySet().stream())
                .forEach(e -> { Object k = e.getKey(); Object v = e.getValue(); });
            } catch (RuntimeException e) {
              failures.incrementAndGet();
            }
          }
        });
      }
      for (int rm = 0; rm < REMOVER_THREADS; rm++) {
        final int rmi = rm;
        pool.submit(() -> {
          for (int i = 0; i < OPS_PER_THREAD; i++) {
            try {
              Module m = sentinels[(rmi * 7 + i) % sentinels.length];
              target.remove(m);
            } catch (RuntimeException e) {
              failures.incrementAndGet();
            }
          }
        });
      }
      pool.shutdown();
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        fail("Concurrency stress did not finish within 60s");
      }
    } finally {
      target.clear();
    }

    assertEquals("Concurrent put/iterate must not throw; saw " + failures.get() + " failures",
                 0, failures.get());
  }

  /**
   * Build a JDK dynamic-proxy Module whose only meaningful behaviour is identity:
   * two sentinels with the same id are equal; their hashCode is the id's hashCode.
   * Any other Module API call throws — those calls should not happen in this test.
   */
  private static Module newSentinelModule(String id) {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        switch (name) {
          case "equals":
            Object other = args[0];
            if (!(other instanceof Module)) return false;
            if (!Proxy.isProxyClass(other.getClass())) return false;
            InvocationHandler otherHandler = Proxy.getInvocationHandler(other);
            return otherHandler == this;
          case "hashCode":
            return id.hashCode();
          case "toString":
            return "SentinelModule[" + id + "]";
          default:
            throw new UnsupportedOperationException("Sentinel module does not implement " + name);
        }
      }
    };
    return (Module) Proxy.newProxyInstance(
      HaxeDefineDetectionManagerConcurrencyTest.class.getClassLoader(),
      new Class<?>[]{Module.class},
      handler);
  }
}
