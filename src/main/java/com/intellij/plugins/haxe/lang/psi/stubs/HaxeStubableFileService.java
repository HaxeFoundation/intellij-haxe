package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Whether a haxe file can be stubbed: only a file without conditional
 * compilation parses the same under every define set, so only it may feed
 * the shared stub indexes. The verdict is cached on the virtual file until
 * its content changes on disk; the PSI and the disk overloads answer from
 * the text they have, so an unsaved editor may cache the disk verdict or
 * the PSI one first - stubs are built on save, which clears the cache.
 */
@CustomLog
public class HaxeStubableFileService implements AsyncFileListener {

  private static final Key<Boolean> CAN_CREATE_STUB_KEY = Key.create("haxe.file.stub.create");
  private static final String CONDITIONAL_COMPILATION_MARKER = "#if";

  /** Stubbed files skip the file-based indexes: their stub indexes cover them. */
  public static boolean skipFilebasedIndex(@NotNull HaxeFile haxeFile) {
    return isStubable(haxeFile);
  }

  public static boolean isStubable(@Nullable VirtualFile file) {
    if (file == null) return false;
    Boolean cached = file.getUserData(CAN_CREATE_STUB_KEY);
    return cached != null ? cached : evaluateStubable(file);
  }

  public static boolean isStubable(@NotNull HaxeFile haxeFile) {
    VirtualFile file = haxeFile.getVirtualFile();
    if (file == null) return false;
    Boolean cached = file.getUserData(CAN_CREATE_STUB_KEY);
    return cached != null ? cached : cache(file, isStubableText(haxeFile.getText()));
  }

  /** Idempotent, so concurrent evaluation of one file needs no lock. */
  private static boolean evaluateStubable(@NotNull VirtualFile file) {
    try {
      return cache(file, isStubableText(VfsUtilCore.loadText(file)));
    }
    catch (IOException e) {
      log.warn("Unable to determine if file is stubable", e);
      return false;
    }
  }

  private static boolean isStubableText(@NotNull String text) {
    return !text.contains(CONDITIONAL_COMPILATION_MARKER);
  }

  private static boolean cache(@NotNull VirtualFile file, boolean stubable) {
    file.putUserData(CAN_CREATE_STUB_KEY, stubable);
    return stubable;
  }

  private static boolean isHaxeFile(@Nullable VirtualFile file) {
    return file != null && file.getFileType() == HaxeFileType.INSTANCE;
  }

  @Override
  public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        for (VFileEvent event : events) {
          VirtualFile file = event.getFile();
          if (event instanceof VFileContentChangeEvent && isHaxeFile(file)) {
            file.putUserData(CAN_CREATE_STUB_KEY, null);
          }
        }
      }
    };
  }
}
