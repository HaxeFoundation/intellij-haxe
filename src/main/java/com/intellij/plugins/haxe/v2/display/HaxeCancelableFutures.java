package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Waits for work that runs on a pooled thread and blocks in socket IO, so it
 * cannot check for cancellation itself. The waiting caller checks instead: a
 * cancel ends the wait at once, while the work runs on and finishes on its
 * own.
 */
@CustomLog
final class HaxeCancelableFutures {

  private static final long POLL_MS = 100;

  private HaxeCancelableFutures() {
  }

  /**
   * Waits from a caller that holds the read lock. A pending write action
   * (typing) or a cancelled progress ends the wait. The result, or null when
   * the work failed or gave no result within {@code timeoutMs};
   * {@code label} names the work in the log.
   */
  @Nullable
  static <T> T awaitUnderReadLock(@NotNull Future<T> future, long timeoutMs, @NotNull String label) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    ProgressIndicatorUtils.awaitWithCheckCanceled(() -> future.isDone() || System.currentTimeMillis() >= deadline);
    if (!future.isDone()) {
      log.info(label + " gave no answer within " + timeoutMs + " ms");
      return null;
    }
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException e) {
      log.warn(label + " failed: " + e.getCause());
      return null;
    }
  }

  /**
   * Waits under a cancelable progress, with no time limit. A cancel throws the
   * platform's cancellation. The result, or null when the work failed or the
   * wait was interrupted.
   */
  @Nullable
  static <T> T awaitUnderProgress(@NotNull Future<T> future, @NotNull ProgressIndicator indicator, @NotNull String failureMessage) {
    while (true) {
      indicator.checkCanceled();
      try {
        return future.get(POLL_MS, TimeUnit.MILLISECONDS);
      }
      catch (TimeoutException stillRunning) {
        // keep polling; the wait must stay short so cancellation is prompt
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      }
      catch (ExecutionException e) {
        log.warn(failureMessage, e.getCause());
        return null;
      }
    }
  }
}
