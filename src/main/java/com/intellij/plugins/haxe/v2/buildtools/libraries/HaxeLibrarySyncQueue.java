package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The project's library syncs, one at a time: requests arriving while a sync
 * runs fold into a single follow-up ({@link HaxeCoalescingRunner}). A sync
 * asks the evaluation caches for every lime/nme build file of every module
 * and each file's evaluation calls back when it lands; without the folding,
 * every landing would start one sync per sync waiting on it.
 */
@Service(Service.Level.PROJECT)
public final class HaxeLibrarySyncQueue {

  private final HaxeCoalescingRunner runs;
  // the ONE callback the evaluation caches hold for this project: they keep
  // callbacks by identity, so a sync re-asking before a result lands
  // registers nothing new
  private final Runnable resyncRequest = () -> request(null);

  public HaxeLibrarySyncQueue(@NotNull Project project) {
    runs = new HaxeCoalescingRunner(onDone -> HaxeLibrarySync.runOnce(project, onDone));
  }

  @NotNull
  static HaxeLibrarySyncQueue getInstance(@NotNull Project project) {
    return project.getService(HaxeLibrarySyncQueue.class);
  }

  /** Queues a sync; {@code onFinished} runs on the EDT once a sync started after this request completed. */
  void request(@Nullable Runnable onFinished) {
    runs.request(onFinished);
  }

  /** The callback a sync hands the evaluation caches: a landed evaluation asks for one more sync. */
  @NotNull
  Runnable resyncRequest() {
    return resyncRequest;
  }
}
