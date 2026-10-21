package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.display.HaxeUsageSearch.UsageState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The compiler's verdicts on whether a declaration is used. A verdict is
 * keyed by the declaration's display context, file, name and offset, and
 * stamped with the modification stamp of its file; a lookup at another
 * stamp misses, since the offsets may have moved. Saving another file can
 * add or remove references the compiler sees, so a save drops the UNUSED
 * verdicts of every other file. USED verdicts stay, because a stale USED
 * only hides an unused-declaration hint.
 */
final class HaxeUsageVerdictCache {

  record Key(@NotNull String contextKey, @NotNull String filePath, @NotNull String memberName, int offset) {
  }

  private record Verdict(long fileStamp, @NotNull UsageState state) {
  }

  private final Map<Key, Verdict> verdicts = new ConcurrentHashMap<>();

  /** The verdict for the declaration at this stamp of its file, or null when there is none or it belongs to another stamp. */
  @Nullable
  UsageState get(@NotNull Key key, long fileStamp) {
    Verdict verdict = verdicts.get(key);
    return verdict != null && verdict.fileStamp() == fileStamp ? verdict.state() : null;
  }

  void put(@NotNull Key key, long fileStamp, @NotNull UsageState state) {
    verdicts.put(key, new Verdict(fileStamp, state));
  }

  void clear() {
    verdicts.clear();
  }

  /**
   * Drops the UNUSED verdicts of every file except the saved one, whose own
   * verdicts its new stamp already retires. Returns whether any were dropped.
   */
  boolean dropUnusedInOtherFiles(@NotNull String savedPath) {
    return verdicts.entrySet().removeIf(entry -> isUnusedInOtherFile(entry, savedPath));
  }

  private static boolean isUnusedInOtherFile(Map.Entry<Key, Verdict> entry, String savedPath) {
    return entry.getValue().state() == UsageState.UNUSED && !entry.getKey().filePath().equals(savedPath);
  }
}
