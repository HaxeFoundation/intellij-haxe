package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.haxelib.HaxelibInstalledIndex;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerCaches;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The installed picture of haxelib - which libraries and versions it has
 * installed - as the tool window and the build-file inspections see it.
 * Fetching it runs a haxelib process, so readers take the last
 * {@link #snapshot}: code under the read lock requests a
 * {@link #refreshAsync} when there is none, a tree refresh reads
 * {@link #current}. A fetch is requested only where the picture can change:
 * after a library sync (which an install or remove ends with) and from the
 * Purge action. A fetch whose result differs restarts highlighting so the
 * inspections follow; a fetch requested while one runs shares that run.
 */
@Service(Service.Level.PROJECT)
public final class HaxeInstalledLibraries {

  /** One installed haxelib: the selected version (null when none is set) and every installed version. */
  public record InstalledLibrary(@Nullable String selectedVersion, @NotNull Set<String> versions) {
  }

  /**
   * A dependency against the picture: {@code installed} when its name is
   * installed in the pinned version (any version when unpinned);
   * {@code selectedVersion} is the version haxelib selects for the name,
   * null when the name is not installed at all.
   */
  public record LibraryStatus(boolean installed, @Nullable String selectedVersion) {
  }

  private final Project project;
  private volatile @Nullable Map<String, InstalledLibrary> snapshot;
  private @Nullable CompletableFuture<@Nullable Map<String, InstalledLibrary>> inFlight;

  public HaxeInstalledLibraries(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeInstalledLibraries getInstance(@NotNull Project project) {
    return project.getService(HaxeInstalledLibraries.class);
  }

  /** The last fetched picture by lower-cased name; null until a fetch landed with haxelib available. Never fetches. */
  @Nullable
  public Map<String, InstalledLibrary> snapshot() {
    return snapshot;
  }

  /**
   * The picture for a tree refresh: a fetch in flight is awaited (the refresh
   * after an install must show the new library), otherwise the snapshot
   * serves, and only without one a fetch runs. Blocks - call on a pooled
   * thread. Null when haxelib is unavailable.
   */
  @Nullable
  public Map<String, InstalledLibrary> current() {
    CompletableFuture<@Nullable Map<String, InstalledLibrary>> pending = pendingFetch();
    if (pending != null) return pending.join();
    Map<String, InstalledLibrary> known = snapshot;
    return known != null ? known : refresh().join();
  }

  /** Fetches on a pooled thread; a request during a fetch shares that fetch. */
  public void refreshAsync() {
    refresh();
  }

  /** The dependency's state against {@code installed}; an unknown picture (null) counts every dependency as installed. */
  @NotNull
  public static LibraryStatus status(@Nullable Map<String, InstalledLibrary> installed, @NotNull HaxeLibDependency library) {
    if (installed == null) return new LibraryStatus(true, null);
    InstalledLibrary installedLibrary = installed.get(library.key());
    if (installedLibrary == null) return new LibraryStatus(false, null);
    // a pinned version must itself be installed - the name alone is not
    // enough. A git checkout is listed as version "git", never as its
    // url#ref spec.
    // TODO: compare a git pin's #ref against the installed checkout (HaxelibLocalDocs.gitCheckout) and flag mismatches
    String requiredVersion = HaxelibGitSpec.parse(library.version()) != null ? HaxelibSemVer.GIT_SCM : library.version();
    boolean satisfied = requiredVersion == null || installedLibrary.versions().contains(requiredVersion);
    return new LibraryStatus(satisfied, installedLibrary.selectedVersion());
  }

  /** Seeds the picture; a fetch still in flight is awaited first so it cannot overwrite the seed. */
  @TestOnly
  public void setSnapshotForTests(@Nullable Map<String, InstalledLibrary> snapshot) {
    CompletableFuture<?> pending = pendingFetch();
    if (pending != null) pending.handle((result, failure) -> null).join();
    this.snapshot = snapshot;
  }

  @Nullable
  private synchronized CompletableFuture<@Nullable Map<String, InstalledLibrary>> pendingFetch() {
    return inFlight != null && !inFlight.isDone() ? inFlight : null;
  }

  @NotNull
  private synchronized CompletableFuture<@Nullable Map<String, InstalledLibrary>> refresh() {
    if (inFlight != null && !inFlight.isDone()) return inFlight;
    inFlight = CompletableFuture.supplyAsync(this::fetchAndPublish, AppExecutorUtil.getAppExecutorService());
    return inFlight;
  }

  /** The fresh picture, stored; a change re-runs the build-file inspections. */
  @Nullable
  private Map<String, InstalledLibrary> fetchAndPublish() {
    Map<String, InstalledLibrary> fetched = fetchFromHaxelib(project);
    boolean changed = !Objects.equals(snapshot, fetched);
    snapshot = fetched;
    if (changed) HaxeCompilerCaches.restartHighlightingLater(project, "haxe: installed libraries changed");
    return fetched;
  }

  /** Haxelib's install state per lower-cased library name, or null when haxelib is unavailable. */
  @Nullable
  private static Map<String, InstalledLibrary> fetchFromHaxelib(@NotNull Project project) {
    Sdk sdk = HaxeToolPathResolver.findConfiguredSdk(project);
    VirtualFile workDir = ProjectUtil.guessProjectDir(project);
    if (sdk == null || workDir == null) return null;

    // the project-wide picture: the project node's haxelib, run from the project directory
    String containerId = HaxeContainers.projectNodeContainerId(project);
    HaxelibInstalledIndex index = HaxelibInstalledIndex.parse(HaxelibCommands.output(project, containerId, Path.of(workDir.getPath()), List.of("list")));
    Map<String, InstalledLibrary> byName = new HashMap<>();
    for (String name : index.getInstalledLibraries()) {
      InstalledLibrary library = new InstalledLibrary(index.getSelectedVersion(name), index.getInstalledVersions(name));
      byName.put(HaxeLibDependency.key(name), library);
    }
    return byName;
  }
}
