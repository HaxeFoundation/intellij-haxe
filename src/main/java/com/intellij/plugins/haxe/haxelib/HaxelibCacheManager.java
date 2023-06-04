package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A cache manager for library information retrieved from haxelib
 */
public class HaxelibCacheManager {

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


  private final Map<String, List<String>> installedLibraries = new HashMap<>();
  private final Map<String, List<String>> availableLibraries = new HashMap<>();

  private final Module module;

  private HaxelibCacheManager(Module module) {
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


  public Map<String, List<String>> getInstalledLibraries() {
    if (installedLibraries.isEmpty()) {
      fetchInstalledLibraryData();
    }
    return new HashMap<>(installedLibraries);
  }


  public Map<String, List<String>> getAvailableLibraries() {
    if (availableLibraries.isEmpty()) {
      fetchAvailableForDownload();
    }
    return new HashMap<>(availableLibraries);
  }

  private void fetchInstalledLibraryData() {
    Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
    installedLibraries.putAll(readInstalledLibraries(sdk));
  }

  private void fetchAvailableForDownload() {
    Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
    availableLibraries.putAll(readAvailableOnline(sdk));
  }


  private static Map<String, List<String>> readAvailableOnline(Sdk sdk) {
    // "Empty" string means all of them. (whitespace needed for argument not to be dropped)
    List<String> searchResults = HaxelibClasspathUtils.getAvailableLibrariesMatching(sdk, " ");
    Map<String, List<String>> libMap = new HashMap<>();
    searchResults.forEach(libName -> libMap.put(libName, List.of()));
    return libMap;
  }

  private static Map<String, List<String>> readInstalledLibraries(@NotNull Sdk sdk) {

    // haxelib list output looks like:
    //      lime-tools: 1.4.0 [1.5.6]
    // The library name comes first, followed by a colon, followed by a
    // list of the available versions.


    Map<String, List<String>> libMap = new HashMap<>();
    List<String> list = HaxelibCommandUtils.issueHaxelibCommand(sdk, "list");
    for (String s : list) {
      String[] split = s.split(":");
      String libName = split[0];
      String libVersions = split[1];
      libVersions = libVersions.replaceAll("\\[", "");
      libVersions = libVersions.replaceAll("]", "");
      String[] versionArray = libVersions.split("\s+");
      libMap.put(libName, List.copyOf(Arrays.stream(versionArray).toList()));
    }
    return libMap;
  }

}
