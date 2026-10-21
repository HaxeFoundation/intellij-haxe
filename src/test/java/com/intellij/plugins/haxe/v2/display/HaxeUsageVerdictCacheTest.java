package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.display.HaxeUsageSearch.UsageState;
import com.intellij.plugins.haxe.v2.display.HaxeUsageVerdictCache.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Compiler services: usage verdict cache")
public class HaxeUsageVerdictCacheTest {
  private static final String DECLARING_FILE = "/project/src/Library.hx";
  private static final String OTHER_FILE = "/project/src/Main.hx";
  private static final long REVISION = 7;

  private final HaxeUsageVerdictCache cache = new HaxeUsageVerdictCache();

  @Test
  @DisplayName("a verdict answers only for the revision it was fetched at")
  public void testAVerdictAnswersOnlyForTheRevisionItWasFetchedAt() {
    Key key = keyFor(DECLARING_FILE, "helper");
    cache.put(key, REVISION, UsageState.UNUSED);

    assertEquals(UsageState.UNUSED, cache.get(key, REVISION));
    assertNull(cache.get(key, REVISION + 1), "an edit of the declaring file retires the verdict");
  }

  @Test
  @DisplayName("saving another file drops unused verdicts and keeps used ones")
  public void testSavingAnotherFileDropsUnusedVerdictsAndKeepsUsedOnes() {
    Key unusedElsewhere = keyFor(DECLARING_FILE, "helper");
    Key usedElsewhere = keyFor(DECLARING_FILE, "run");
    Key unusedInSavedFile = keyFor(OTHER_FILE, "local");
    cache.put(unusedElsewhere, REVISION, UsageState.UNUSED);
    cache.put(usedElsewhere, REVISION, UsageState.USED);
    cache.put(unusedInSavedFile, REVISION, UsageState.UNUSED);

    boolean dropped = cache.dropUnusedInOtherFiles(OTHER_FILE);

    assertTrue(dropped);
    assertNull(cache.get(unusedElsewhere, REVISION), "the saved file may now reference the member");
    assertEquals(UsageState.USED, cache.get(usedElsewhere, REVISION), "a stale USED only hides a hint");
    assertEquals(UsageState.UNUSED, cache.get(unusedInSavedFile, REVISION), "the saved file's own verdicts retire by revision");
  }

  @Test
  @DisplayName("a save that drops nothing says so")
  public void testASaveThatDropsNothingSaysSo() {
    cache.put(keyFor(DECLARING_FILE, "run"), REVISION, UsageState.USED);

    assertFalse(cache.dropUnusedInOtherFiles(OTHER_FILE));
  }

  private static Key keyFor(String filePath, String memberName) {
    return new Key("context", filePath, memberName, 0);
  }
}
