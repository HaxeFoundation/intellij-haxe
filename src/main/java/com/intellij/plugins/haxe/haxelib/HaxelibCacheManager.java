package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A cache manager for library information retrieved from haxelib
 */
@CustomLog
public class HaxelibCacheManager implements Disposable {

  // current version of haxelib shows versions as date + version + description
  private static Pattern HAXELIB_VERSION_LINE =
    Pattern.compile("(?<date>\\d{4}-\\d{2}-\\d{2}\s\\d{2}:\\d{2}:\\d{2})\s(?<version>.*?)\s:\s?(?<description>.*)");

  private static Map<Module, HaxelibCacheManager> instances = new HashMap<>();

  public static Collection<HaxelibCacheManager> getAllInstances() {
    return instances.values();
  }

  public static HaxelibCacheManager getInstance(@NotNull Module module) {
    if (!instances.containsKey(module)) {
      instances.put(module, new HaxelibCacheManager(module));
    }
    return instances.get(module);
  }

  public static void removeInstance(@NotNull Module module) {
    instances.remove(module);
  }


  private final Map<String, Set<String>> installedLibraries = new HashMap<>();
  private final Map<String, Set<String>> availableLibraries = new HashMap<>();

  private Module module;

  private HaxelibCacheManager(Module module) {
    Disposer.register(module, this);
    this.module = module;
  }


  public void clear() {
    installedLibraries.clear();
    availableLibraries.clear();
  }

  public void reload() {
    clear();
    getInstalledLibraries();
    getAvailableLibraries();
  }


  public Map<String, Set<String>> getInstalledLibraries() {
    if (installedLibraries.isEmpty()) {
      fetchInstalledLibraryData();
    }
    return new HashMap<>(installedLibraries);
  }


  public Map<String, Set<String>> getAvailableLibraries() {
    if (availableLibraries.isEmpty()) {
      fetchAvailableForDownload();
    }
    return new HashMap<>(availableLibraries);
  }

  private void fetchInstalledLibraryData() {
    Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
    if(!HaxelibSdkUtils.isValidHaxeSdk(sdk)) {
      log.warn("Unable to fetchInstalledLibraryData, invalid SDK paths");
      return;
    }
    installedLibraries.putAll(readInstalledLibraries(sdk));
  }

  private void fetchAvailableForDownload() {
    Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
    if(!HaxelibSdkUtils.isValidHaxeSdk(sdk)) {
      log.warn("Unable to fetchAvailableForDownload, invalid SDK paths");
      return;
    }
    availableLibraries.putAll(readAvailableOnline(sdk));
  }


  private static Map<String, Set<String>> readAvailableOnline(Sdk sdk) {
    // "Empty" string means all of them. (whitespace needed for argument not to be dropped)
    List<String> searchResults = HaxelibClasspathUtils.getAvailableLibrariesMatching(sdk, " ");
    Map<String, Set<String>> libMap = new HashMap<>();
    searchResults.forEach(libName -> libMap.put(libName, Set.of()));
    return libMap;
  }

  private Map<String, Set<String>> readInstalledLibraries(@NotNull Sdk sdk) {
    VirtualFile file = ProjectUtil.guessModuleDir(module);
    HaxelibInstalledIndex index = HaxelibInstalledIndex.fetchFromHaxelib(sdk, file);
    return index.getInstalledLibrariesAndVersions();

  }

  public Set<String> fetchAvailableVersions(String name) {
    if (getAvailableLibraries().getOrDefault(name, Set.of()).isEmpty()) {
      Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
      if(!HaxelibSdkUtils.isValidHaxeSdk(sdk)) {
        log.warn("Unable to fetch Available Versions, invalid SDK paths");
        return Set.of();
      }
      VirtualFile file = ProjectUtil.guessModuleDir(module);
      List<String> list = HaxelibCommandUtils.issueHaxelibCommand(sdk, file,"info", name);
      // filter to find version numbers

      Set<String> versions = list.stream()
        .map(String::trim)
        .map(HaxelibCacheManager::extractVersion)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
      availableLibraries.put(name, new ConcurrentSkipListSet<>(versions));
    }
    return new HashSet<>(availableLibraries.get(name));
  }

  private static String extractVersion(String line) {
    Matcher matcher = HAXELIB_VERSION_LINE.matcher(line);
    if (matcher.matches()) {
      return matcher.group("version").trim();
    }
    return null;
  }

  @Override
  public void dispose() {
    instances.clear();
    module = null;
  }
}
