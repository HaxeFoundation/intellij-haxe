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
    if ((listCmdOutput.size() > 0) && (!listCmdOutput.get(0).contains("Unknown command"))) {
      for (String line : listCmdOutput) {
        int firstColon = line.indexOf(":");

        String libName = line.substring(0,firstColon);;
        String libVersions = line.substring(firstColon+1);

        index.installedLibraries.put(libName, new ConcurrentSkipListSet<>());

        String[] versionArray = libVersions.trim().split("\s+");
        for (String version : versionArray) {
          if (version.startsWith("[") && version.endsWith("]")) {
            version = version.replaceAll("\\[", "").replaceAll("]", "");
            // remove the dev path from "version"
            if (version.startsWith("dev:")) {
              version = "dev";
            }
            index.selectedVersions.put(libName, version);
            index.installedLibraries.get(libName).add(version);
          }else {
            index.installedLibraries.get(libName).add(version);
          }
        }
      }
    }
    return index;
  }
}
