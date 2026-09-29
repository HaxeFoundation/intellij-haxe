package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Per-module cache over haxelib's answers: the installed index (scoped to the
 * module's repository — a local {@code .haxelib} when one exists), the full
 * online catalog (one match-all search), and per-library {@code haxelib info}
 * metadata. Everything server-fetched stays cached for the session;
 * {@link #reload()} and {@link #refreshLibraryInfo} are the explicit
 * invalidations. Fetches run external processes — call off the EDT. A
 * disposed cache (its module closed while a background sweep was in flight)
 * answers nothing.
 */
@CustomLog
public class HaxelibCacheManager implements Disposable {

  private static final Map<Module, HaxelibCacheManager> instances = new ConcurrentHashMap<>();

  public static Collection<HaxelibCacheManager> getAllInstances() {
    return instances.values();
  }

  public static HaxelibCacheManager getInstance(@NotNull Module module) {
    return instances.computeIfAbsent(module, HaxelibCacheManager::new);
  }

  public static void removeInstance(@NotNull Module module) {
    instances.remove(module);
  }


  // written from explorer/completion pooled threads while others read
  private final Map<String, Set<String>> installedLibraries = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> availableLibraries = new ConcurrentHashMap<>();
  private final Map<String, HaxelibLibraryInfo> libraryInfos = new ConcurrentHashMap<>();
  private volatile HaxelibInstalledIndex installedIndex = HaxelibInstalledIndex.EMPTY;

  private final Module module;
  private volatile boolean disposed;

  private HaxelibCacheManager(Module module) {
    Disposer.register(module, this);
    this.module = module;
  }


  public void clear() {
    installedLibraries.clear();
    availableLibraries.clear();
    libraryInfos.clear();
    installedIndex = HaxelibInstalledIndex.EMPTY;
  }

  /** The explicit force-update: drops every cached answer (catalog, infos, installed) and refetches the lists. */
  public void reload() {
    clear();
    getInstalledLibraries();
    getAvailableLibraries();
  }

  /** Drops only the installed picture; the next read refetches. Cheap enough to run after every install/remove. */
  public void refreshInstalled() {
    installedLibraries.clear();
    installedIndex = HaxelibInstalledIndex.EMPTY;
  }


  public Map<String, Set<String>> getInstalledLibraries() {
    if (installedLibraries.isEmpty()) {
      fetchInstalledLibraryData();
    }
    return new HashMap<>(installedLibraries);
  }

  /** The installed index behind {@link #getInstalledLibraries} — selected versions included. */
  @NotNull
  public HaxelibInstalledIndex getInstalledIndex() {
    getInstalledLibraries();
    return installedIndex;
  }

  public Map<String, Set<String>> getAvailableLibraries() {
    if (availableLibraries.isEmpty()) {
      fetchAvailableForDownload();
    }
    return new HashMap<>(availableLibraries);
  }

  /**
   * The library's server metadata, from cache or one {@code haxelib info}
   * call; null when the library is unknown to the server or the call failed
   * (failures are NOT cached — the next ask retries).
   */
  @Nullable
  public HaxelibLibraryInfo getLibraryInfo(@NotNull String name) {
    HaxelibLibraryInfo cached = libraryInfos.get(name);
    if (cached != null) return cached;
    HaxelibLibraryInfo fetched = fetchLibraryInfo(name);
    if (fetched != null) {
      libraryInfos.put(name, fetched);
    }
    return fetched;
  }

  /** The cached metadata only — null means "not fetched yet", never triggers a fetch. */
  @Nullable
  public HaxelibLibraryInfo getCachedLibraryInfo(@NotNull String name) {
    return libraryInfos.get(name);
  }

  /** Drops one library's cached metadata; the next {@link #getLibraryInfo} refetches. */
  public void refreshLibraryInfo(@NotNull String name) {
    libraryInfos.remove(name);
  }

  private void fetchInstalledLibraryData() {
    SdkContext context = sdkContext();
    if (context == null) {
      log.warn("Unable to fetchInstalledLibraryData, invalid SDK paths");
      return;
    }
    HaxelibInstalledIndex index = HaxelibInstalledIndex.fetchFromHaxelib(context.sdk(), context.moduleDir());
    installedIndex = index;
    installedLibraries.putAll(index.getInstalledLibrariesAndVersions());
  }

  private void fetchAvailableForDownload() {
    SdkContext context = sdkContext();
    if (context == null) {
      log.warn("Unable to fetchAvailableForDownload, invalid SDK paths");
      return;
    }
    availableLibraries.putAll(readAvailableOnline(context.sdk()));
  }

  @Nullable
  private HaxelibLibraryInfo fetchLibraryInfo(@NotNull String name) {
    SdkContext context = sdkContext();
    if (context == null) {
      log.warn("Unable to fetch library info, invalid SDK paths");
      return null;
    }
    return HaxelibLibraryInfo.parse(
      HaxelibCommandUtils.issueHaxelibCommand(context.sdk(), context.moduleDir(), "info", name));
  }

  private record SdkContext(@NotNull Sdk sdk, @Nullable VirtualFile moduleDir) {
  }

  /**
   * The module's SDK and directory. Model lookups take the read lock; the
   * default-SDK fallback probes {@code haxe -help} (a process) and therefore
   * runs OUTSIDE it.
   */
  @Nullable
  private SdkContext sdkContext() {
    // an in-flight background sweep can outlive the module - answer nothing
    // instead of touching a disposed module's model
    if (disposed) {
      return null;
    }
    // per-thread read form: the explorer fetches run on pooled threads
    // (where computeBlocking would contend with the UI), but reload() is
    // also reachable from EDT actions (where the non-blocking form asserts)
    Sdk moduleSdk = HaxeReadActions.compute(() -> {
      ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
      return rootManager == null ? null : rootManager.getSdk();
    });
    Sdk sdk = moduleSdk != null ? moduleSdk : defaultSdk();
    if (!HaxelibSdkUtils.isValidHaxeSdk(sdk)) {
      return null;
    }
    VirtualFile moduleDir = HaxeReadActions.compute(() -> ProjectUtil.guessModuleDir(module));
    return new SdkContext(sdk, moduleDir);
  }

  /** The library's release versions; empty when the info fetch failed. */
  @NotNull
  private static Set<String> releaseVersions(@Nullable HaxelibLibraryInfo info) {
    if (info == null) return Set.of();
    return info.releases().stream()
      .map(HaxelibLibraryInfo.Release::version)
      .collect(Collectors.toSet());
  }

  /** The application-default Haxe SDK, standing in for a module without one (logs a warning with the module's name). */
  private Sdk defaultSdk() {
    return HaxelibSdkUtils.getDefaultSDK(HaxeBundle.message("haxe.haxelib.invalid.sdk.for.module", module.getName()));
  }

  private static Map<String, Set<String>> readAvailableOnline(Sdk sdk) {
    // "Empty" string means all of them. (whitespace needed for argument not to be dropped)
    List<String> searchResults = HaxelibClasspathUtils.getAvailableLibrariesMatching(sdk, " ");
    Map<String, Set<String>> libMap = new HashMap<>();
    searchResults.forEach(libName -> libMap.put(libName, Set.of()));
    return libMap;
  }

  public Set<String> fetchAvailableVersions(String name) {
    if (getAvailableLibraries().getOrDefault(name, Set.of()).isEmpty()) {
      HaxelibLibraryInfo info = getLibraryInfo(name);
      availableLibraries.put(name, new ConcurrentSkipListSet<>(releaseVersions(info)));
    }
    return new HashSet<>(availableLibraries.get(name));
  }

  @Override
  public void dispose() {
    disposed = true;
    // only this module's entry - clearing the whole map would orphan every
    // other module's cache on the first project close
    instances.remove(module);
  }
}
