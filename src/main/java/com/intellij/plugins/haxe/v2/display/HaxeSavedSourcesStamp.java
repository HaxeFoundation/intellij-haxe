package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A counter that moves whenever a Haxe source changes on disk: saved,
 * created, deleted or moved. Compiler answers come from the files on disk,
 * so an answer taken at an older value describes older sources. Counting
 * starts with the first {@link #getInstance} call.
 */
@Service(Service.Level.PROJECT)
public final class HaxeSavedSourcesStamp implements Disposable {
  private final AtomicLong stamp = new AtomicLong();

  public HaxeSavedSourcesStamp(@NotNull Project project) {
    project.getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        if (ContainerUtil.exists(events, HaxeSavedSourcesStamp::isHaxeSourceChange)) {
          stamp.incrementAndGet();
        }
      }
    });
  }

  @NotNull
  public static HaxeSavedSourcesStamp getInstance(@NotNull Project project) {
    return project.getService(HaxeSavedSourcesStamp.class);
  }

  public long get() {
    return stamp.get();
  }

  /** Whether the event touches a Haxe source file. */
  public static boolean isHaxeSourceChange(@NotNull VFileEvent event) {
    VirtualFile file = event.getFile();
    return file != null && HaxeFileType.INSTANCE.getDefaultExtension().equals(file.getExtension());
  }

  @Override
  public void dispose() {
  }
}
