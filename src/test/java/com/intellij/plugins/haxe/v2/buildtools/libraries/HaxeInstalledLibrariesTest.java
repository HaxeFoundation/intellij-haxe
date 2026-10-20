package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries.InstalledLibrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("Build tools: installed libraries")
public class HaxeInstalledLibrariesTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // the shared light project would carry the seed into the next test class
      HaxeInstalledLibraries.getInstance(getProject()).setSnapshotForTests(null);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("a tree refresh reads the snapshot without fetching")
  public void testATreeRefreshReadsTheSnapshotWithoutFetching() {
    HaxeInstalledLibraries libraries = HaxeInstalledLibraries.getInstance(getProject());
    Map<String, InstalledLibrary> seeded = Map.of("utest", new InstalledLibrary("1.0.0", Set.of("1.0.0")));
    libraries.setSnapshotForTests(seeded);

    Map<String, InstalledLibrary> current = libraries.current();

    // a fetch would have replaced the seed with haxelib's answer, which this project has none of
    assertSame(seeded, current);
    assertSame(seeded, libraries.snapshot());
  }
}
