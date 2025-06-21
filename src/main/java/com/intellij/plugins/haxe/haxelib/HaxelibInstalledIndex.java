package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class HaxelibInstalledIndex {

  public static  HaxelibInstalledIndex EMPTY = new HaxelibInstalledIndex();
  final Hashtable<String, Set<String>> installedLibraries = new Hashtable<>();
  final  Hashtable<String, String> selectedVersions = new Hashtable<>();

  private HaxelibInstalledIndex() {
  }

  /**
   * Retrieve the list of libraries known to 'haxelib'
   *
   * @return a (possibly empty) list of libraries
   */
  public Set<String> getInstalledLibraries() {
    return installedLibraries.keySet();
  }
  public Hashtable<String, Set<String>> getInstalledLibrariesAndVersions() {
    return new Hashtable(installedLibraries);
  }

  /**
   * Retrieve the list of  versions installed for a given library

   * @return a (possibly empty) list of libraries
   */
  public Set<String> getInstalledVersions(String Library) {
    return installedLibraries.getOrDefault(Library, Set.of());
  }
  public String getSelectedVersion(String Library) {
    return selectedVersions.getOrDefault(Library, null);
  }

  public static HaxelibInstalledIndex fetchFromHaxelib(@NotNull Sdk sdk, VirtualFile workDir){
    // haxelib list output looks like:
    //      lime-tools: 1.4.0 [1.5.6]
    // The library name comes first, followed by a colon, followed by a
    // list of the available versions.

    HaxelibInstalledIndex index = new HaxelibInstalledIndex();

    List<String> listCmdOutput = HaxelibCommandUtils.issueHaxelibCommand(sdk, workDir,  "list");
    if ((!listCmdOutput.isEmpty()) && (!listCmdOutput.getFirst().contains("Unknown command"))) {
      for (String line : listCmdOutput) {
        int firstColon = line.indexOf(":");
        String libName = line.substring(0, firstColon);
        String libVersions = line.substring(firstColon + 1);
        processVersions(libName, libVersions.trim(), index);
      }
    }
    return index;
  }

  /*
   *  Custom logic to extract version numbers in a way that allow dev paths to contain whitespaces.
   *  we used to just split on whitespace to separate versions, but this breaks once you have paths with whitespaces.
   *
   *  It looks like you cannot toggle between dev and other versions so to simplify things we extract the selected version
   *  and handle it separately, that way we can use the normal whitespace split for the rest of the string.
   */
  private static void processVersions(String libName, String libVersions, HaxelibInstalledIndex index) {
    Set<String> versionList  = new ConcurrentSkipListSet<>();
    index.installedLibraries.put(libName, versionList);

    int selectedBegin = libVersions.indexOf("[") ;
    int selectedEnd = libVersions.indexOf("]");

    if (selectedBegin > -1 && selectedEnd > -1) {
      String selectedVersion = libVersions.substring(selectedBegin+1, selectedEnd);
      if (!selectedVersion.isBlank()) {
        String version = selectedVersion.trim();
        if (selectedVersion.startsWith("dev:")) {
          version = "dev";
        }
        index.selectedVersions.put(libName, version);
        versionList.add(version);
      }
      String beforeSelect = libVersions.substring(0, selectedBegin);
      String afterSelect = libVersions.substring(selectedEnd+1);
      libVersions = (beforeSelect+afterSelect).trim();
    }
    if(!libVersions.isBlank()) {
      String[] split = libVersions.split("\\s+");
      versionList.addAll(Arrays.asList(split));
    }
  }
}
