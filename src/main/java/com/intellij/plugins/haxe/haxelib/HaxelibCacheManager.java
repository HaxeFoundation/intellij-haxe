package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A cache manager for library information retrieved from haxelib
 */
@CustomLog
public class HaxelibCacheManager {

  // current version of haxelib shows versions as date + version + description
  private static Pattern HAXELIB_VERSION_LINE =
    Pattern.compile("(?<date>\\d{4}-\\d{2}-\\d{2}\s\\d{2}:\\d{2}:\\d{2})\s(?<version>.*)\s:\s(?<description>.*)");

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
      String[] versionArray = libVersions.trim().split("\s+");
      libMap.put(libName, List.copyOf(Arrays.stream(versionArray).toList()));
    }
    return libMap;
  }

  public List<String> fetchAvailableVersions(String name) {
    if (getAvailableLibraries().getOrDefault(name, List.of()).isEmpty()) {
      Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
      if(!HaxelibSdkUtils.isValidHaxeSdk(sdk)) {
        log.warn("Unable to fetch Available Versions, invalid SDK paths");
        return List.of();
      }
      List<String> list = HaxelibCommandUtils.issueHaxelibCommand(sdk, "info", name);
      // filter to find version numbers

      List<String> versions = list.stream()
        .map(String::trim)
        .map(HaxelibCacheManager::extractVersion)
        .filter(Objects::nonNull)
        .toList();
      availableLibraries.put(name, versions);
    }
    return new LinkedList<>(availableLibraries.get(name));
  }

  private static String extractVersion(String line) {
    Matcher matcher = HAXELIB_VERSION_LINE.matcher(line);
    if (matcher.matches()) {
      return matcher.group("version").trim();
    }
    return null;
  }
}
