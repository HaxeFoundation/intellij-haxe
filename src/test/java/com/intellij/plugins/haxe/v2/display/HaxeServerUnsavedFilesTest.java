package com.intellij.plugins.haxe.v2.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Compiler services: server unsaved files")
public class HaxeServerUnsavedFilesTest {
  private static final int PORT = 6000;
  private static final int OTHER_PORT = 6001;
  private static final String FILE = "/project/std/Reflect.go.hx";

  private final HaxeServerUnsavedFiles unsavedFiles = new HaxeServerUnsavedFiles();

  @Test
  @DisplayName("a saved file is invalidated once after its unsaved text was sent")
  public void testASavedFileIsInvalidatedOnceAfterItsUnsavedTextWasSent() {
    assertFalse(unsavedFiles.needsInvalidate(PORT, FILE, false), "the server read the saved file from disk");

    assertTrue(unsavedFiles.needsInvalidate(PORT, FILE, true), "unsaved text always invalidates");
    unsavedFiles.requestSent(PORT, FILE, true);

    assertTrue(unsavedFiles.needsInvalidate(PORT, FILE, false), "back to the saved state: the server holds the old text");
    unsavedFiles.requestSent(PORT, FILE, false);

    assertFalse(unsavedFiles.needsInvalidate(PORT, FILE, false), "the server reread the saved file");
  }

  @Test
  @DisplayName("each server holds its own unsaved files")
  public void testEachServerHoldsItsOwnUnsavedFiles() {
    unsavedFiles.requestSent(PORT, FILE, true);

    assertFalse(unsavedFiles.needsInvalidate(OTHER_PORT, FILE, false), "another SDK's server never received the text");
  }

  @Test
  @DisplayName("a cleared record forgets every unsaved file")
  public void testAClearedRecordForgetsEveryUnsavedFile() {
    unsavedFiles.requestSent(PORT, FILE, true);

    unsavedFiles.clear();

    assertFalse(unsavedFiles.needsInvalidate(PORT, FILE, false), "a restarted server holds no unsaved text");
  }
}
