package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * Read-action helper for MIXED-context code paths. The platform splits the
 * synchronous forms by thread: {@code ReadAction.computeBlocking} is the
 * EDT/modal form and can freeze the UI when used from pooled threads, while
 * {@code NonBlockingReadAction.executeSynchronously} is the background form
 * and asserts against the EDT. A computation reached from both kinds of
 * caller picks the sanctioned form for the current thread here; one already
 * holding read access just runs.
 */
public final class HaxeReadActions {

  private HaxeReadActions() {
  }

  /** The computation's result under the read lock, via the form the current thread sanctions. */
  public static <T> T compute(@NotNull Supplier<T> computation) {
    Application application = ApplicationManager.getApplication();
    if (application.isReadAccessAllowed()) {
      return computation.get();
    }
    if (application.isDispatchThread()) {
      return ReadAction.computeBlocking(computation::get);
    }
    return ReadAction.nonBlocking(computation::get).executeSynchronously();
  }
}
