package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedCodePreview.PreparedPreview;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedCodePreview.PreviewOrigin;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A preview knows which sources its dump was built from: it turns stale when they change, and a newer one replaces it. */
@DisplayName("Navigation: generated preview staleness")
public class HaxeGeneratedPreviewStalenessTest extends HaxeCodeInsightFixtureTestCase {
  private static final String MAIN_DUMP_SOURCE = """
    class Main {
    \tstatic function main() {
    \t\treturn;
    \t}
    }
    """;

  @TempDir
  Path dumpDirectory;

  @Override
  protected String getBasePath() {
    // the fixtures are built from text; no test-data directory
    return "";
  }

  @Test
  @DisplayName("a preview turns stale when a haxe source changes on disk")
  public void testAPreviewTurnsStaleWhenAHaxeSourceChangesOnDisk() throws Exception {
    PreparedPreview preview = prepareMainPreview();
    assertFalse(HaxeGeneratedCodePreview.isStale(getProject(), preview.file()));

    myFixture.addFileToProject("src/Other.hx", "class Other {}\n");

    assertTrue(HaxeGeneratedCodePreview.isStale(getProject(), preview.file()));
  }

  @Test
  @DisplayName("a newer preview of the module closes the older one")
  public void testANewerPreviewOfTheModuleClosesTheOlderOne() throws Exception {
    PreparedPreview older = prepareMainPreview();
    HaxeGeneratedCodePreview.openPrepared(getProject(), older);

    Path dumpFile = dumpDirectory.resolve("Main.dump");
    Files.writeString(dumpFile, MAIN_DUMP_SOURCE + "// rebuilt\n");
    long rebuiltAt = Files.getLastModifiedTime(dumpFile).toMillis() + 2_000;
    Files.setLastModifiedTime(dumpFile, FileTime.fromMillis(rebuiltAt));
    PreparedPreview newer = prepareMainPreview();
    HaxeGeneratedCodePreview.openPrepared(getProject(), newer);

    assertNotSame(older.file(), newer.file(), "a rebuilt dump renders a new preview file");
    List<?> openFiles = List.of(FileEditorManager.getInstance(getProject()).getOpenFiles());
    assertTrue(openFiles.contains(newer.file()));
    assertFalse(openFiles.contains(older.file()), "the older preview of the same module is closed");
  }

  @Test
  @DisplayName("a regenerated preview opens at the stale ones line")
  public void testARegeneratedPreviewOpensAtTheStaleOnesLine() throws Exception {
    PreparedPreview preview = prepareMainPreview();
    LightVirtualFile file = preview.file();
    String text = file.getContent().toString();

    PreparedPreview atSecondLine = preview.atLine(1);
    PreparedPreview pastTheEnd = preview.atLine(10_000);

    assertEquals(text.indexOf('\n') + 1, atSecondLine.offset());
    assertEquals(text.lastIndexOf('\n') + 1, pastTheEnd.offset(), "a line past the end opens at the last line");
  }

  /** Writes a dump of module Main (unless one exists) and prepares its preview, stamped with the current sources. */
  private PreparedPreview prepareMainPreview() throws Exception {
    Path dumpFile = dumpDirectory.resolve("Main.dump");
    if (!Files.exists(dumpFile)) Files.writeString(dumpFile, MAIN_DUMP_SOURCE);
    var overrides = HaxeDisplayConfiguration.OverrideArguments.EMPTY;
    var context = new HaxeCompilerDisplayService.DisplayContext(List.of(), null, null, overrides, "container");
    long sourcesStamp = HaxeSavedSourcesStamp.getInstance(getProject()).get();
    PreviewOrigin origin = new PreviewOrigin(context, "Main", sourcesStamp);
    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.ofType("Main", null);
    // preparing parses under a non-blocking read action, which runs off the EDT
    PreparedPreview prepared = ApplicationManager.getApplication()
      .executeOnPooledThread(() -> HaxeGeneratedCodePreview.prepare(getProject(), dumpFile, spot, origin))
      .get();
    assertNotNull(prepared, "the dump renders a preview");
    return prepared;
  }
}
