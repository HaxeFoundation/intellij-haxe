package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.PathUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-project memo of {@link HaxeToolConfigs#findConfigDirectory}. An action's
 * update reads the memo and never searches itself: the first ask for a
 * directory answers "unknown" and starts the search on a pooled thread, so a
 * menu opens at once without the tool entries and carries them from the next
 * opening on. A tool config appearing, vanishing, moving or renamed, or a
 * directory renamed, moved or gone, drops every answer.
 */
@Service(Service.Level.PROJECT)
public final class HaxeToolConfigDetector implements Disposable {

  private static final Set<String> CONFIG_NAMES = Set.of(HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME, HaxeToolConfigs.FORMATTER_CONFIG_NAME);
  private static final int AWAIT_TIMEOUT_MS = 10_000;

  /** One memo entry per directory and config name. */
  private record Lookup(@NotNull String directoryUrl, @NotNull String configName) {
  }

  private final Project project;
  private final Map<Lookup, Optional<VirtualFile>> answers = new ConcurrentHashMap<>();
  private final Map<Lookup, CancellablePromise<VirtualFile>> searches = new ConcurrentHashMap<>();

  public static HaxeToolConfigDetector getInstance(@NotNull Project project) {
    return project.getService(HaxeToolConfigDetector.class);
  }

  public HaxeToolConfigDetector(@NotNull Project project) {
    this.project = project;
    project.getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        if (ContainerUtil.exists(events, HaxeToolConfigDetector::changesScope)) forget();
      }
    });
  }

  /**
   * The directory holding the config for the file when already detected; null
   * while the answer is unknown (the search then runs in the background) and
   * when no such directory exists.
   */
  @Nullable
  public VirtualFile knownConfigDirectory(@NotNull VirtualFile file, @NotNull String configName) {
    VirtualFile directory = file.isDirectory() ? file : file.getParent();
    if (directory == null) return null;
    Lookup lookup = new Lookup(directory.getUrl(), configName);
    Optional<VirtualFile> answer = answers.get(lookup);
    if (answer == null) {
      search(lookup, directory);
      return null;
    }
    return answer.filter(VirtualFile::isValid).orElse(null);
  }

  /** Waits for every running search; the memo then answers every directory asked so far. */
  @TestOnly
  public void awaitSearchesForTests() throws Exception {
    for (CancellablePromise<VirtualFile> search : List.copyOf(searches.values())) {
      search.blockingGet(AWAIT_TIMEOUT_MS);
    }
  }

  @Override
  public void dispose() {
    forget();
  }

  private void search(@NotNull Lookup lookup, @NotNull VirtualFile directory) {
    CancellablePromise<VirtualFile> running = searches.get(lookup);
    if (running != null && !running.isDone()) return;
    // memoized inside the read action: no VFS change (and so no forget()) can
    // slip between the search and its answer
    CancellablePromise<VirtualFile> search = ReadAction.nonBlocking(() -> {
        VirtualFile found = HaxeToolConfigs.findConfigDirectory(project, directory, lookup.configName());
        answers.put(lookup, Optional.ofNullable(found));
        return found;
      })
      .expireWith(this)
      .submit(AppExecutorUtil.getAppExecutorService());
    searches.put(lookup, search);
  }

  private void forget() {
    answers.clear();
    searches.clear();
  }

  /** A tool config appearing, vanishing, moving or renamed, or a directory renamed, moved or gone. */
  private static boolean changesScope(@NotNull VFileEvent event) {
    if (CONFIG_NAMES.contains(PathUtil.getFileName(event.getPath()))) return true;
    return switch (event) {
      case VFileMoveEvent move -> move.getFile().isDirectory();
      case VFileDeleteEvent delete -> delete.getFile().isDirectory();
      case VFilePropertyChangeEvent change -> change.isRename()
                                              && (change.getFile().isDirectory() || CONFIG_NAMES.contains(change.getOldValue()));
      default -> false;
    };
  }
}
