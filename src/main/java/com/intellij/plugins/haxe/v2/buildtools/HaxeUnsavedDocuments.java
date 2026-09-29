package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;

/**
 * Flushes unsaved editor buffers to disk before a build tool runs — the
 * spawned compiler reads FILES, not editor documents, so an unflushed buffer
 * builds stale code (and a compilation-server build then caches it by mtime).
 * Every command launch path calls this first.
 */
public final class HaxeUnsavedDocuments {

  private HaxeUnsavedDocuments() {
  }

  /** Saves all unsaved documents; callable from any thread, blocks until saved. */
  public static void saveAll() {
    Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      FileDocumentManager.getInstance().saveAllDocuments();
    }
    else {
      application.invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
    }
  }
}
