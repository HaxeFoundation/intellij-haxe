/*
 * Copyright 2017-2018 Eric Bishton
 * Copyright 2018 Aleksandr Kuzmenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.plugins.haxe.HaxeBundle;

import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.util.HaxeEventLogUtil;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import lombok.CustomLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Various utilities to work with haxe libraries.
 */
@CustomLog
public class HaxelibUtil {
  static public final String LOCAL_REPO = ".haxelib";

  static private  Map<String,String> libBasePathCache = new HashMap<>();
  public static void clearCache() {
    libBasePathCache.clear();
  }

  static {      // Take this out when finished debugging.
    log.setLevel(LogLevel.DEBUG);
  }

  static Key<VirtualFile> HaxelibRootKey = new Key<VirtualFile>("Haxelib.rootDirectory");

  static final String HAXELIB_LOG_ID = "Haxelib Synchronization";

  // TODO: Figure out what the haxelib configuration is coming from and watch for changes, to invalidate the cache.
  //       (In practice this will not change often.

  /**
   * Get the base path that haxelib uses to store libraries.
   *
   * note: We attempt to cache results as libBasePath is more or less haxelib repo dir (global or local)
   *
   * @param sdk
   * @return
   */
  @Nullable
  public static VirtualFile getLibraryBasePath(@NotNull final Sdk sdk, VirtualFile workDir) {
    String workDirPath = workDir.getCanonicalPath();
    VirtualFile file = null;
    if (libBasePathCache.containsKey(workDirPath)) {
      String libBasePath = libBasePathCache.get(workDirPath);
      file = LocalFileSystem.getInstance().findFileByPath(libBasePath);
    }else{
      file = _getLibraryBasePath(sdk, workDir);
      if(file!= null) {
        String path = file.getCanonicalPath();
        libBasePathCache.put(workDirPath, path);
      }
    }
    return file;
  }
  @Nullable
  private static VirtualFile _getLibraryBasePath(@NotNull final Sdk sdk, VirtualFile workDir) {
      List<String> output = HaxelibCommandUtils.issueHaxelibCommand(sdk, workDir, "config");
      for (String s : output) {
        if (s.isEmpty()) continue;
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(s);
        if (null != file) {
          return file;
        }
      }
    return null;
  }


  public static VirtualFile getLibraryRoot(@NotNull Sdk sdk, ModuleLibraryCache libraryCache, @NotNull String libName, String libVersion) {
    LocalFileSystem lfs = LocalFileSystem.getInstance();

    VirtualFile workDirectory = libraryCache.getHaxelibWorkDirectory();
    VirtualFile haxelibRoot =  libraryCache.getRepositoryPath();

    if(haxelibRoot == null) {

      haxelibRoot = getLibraryBasePath(sdk, workDirectory);
      libraryCache.setRepositoryPath(haxelibRoot);
    }
    if(haxelibRoot == null) {
      log.debug("Haxe libraries base path was not found for current project sdk");
      return null;
    }
    String rootName = haxelibRoot.getPath();

    // Lib directory name expected to use commas in place of periods.
    String libFSName = libName.replaceAll("\\.", ",");

    // Forking 'haxelib path' is slow, so we will do what it does without forking.
    // In this case, it locates a subdirectory named $HAXELIB_PATH/libFSName/.dev, and
    // if it exists, uses the path found in that file.
    // Failing that, it looks for .current in the same path, and uses the semantic
    // version found in that file to compute the path name.

    String libDirName = HaxeFileUtil.joinPath(rootName, libFSName);
    VirtualFile libDir = lfs.findFileByPath(libDirName);
    if (null != libDir) {
      if(libVersion == null) {
        // Hidden ".current" file contains the semantic version (not the path!) of the
        // library that haxelib will use.
        VirtualFile dotCurrent = libDir.findChild(".current");
        if (null != dotCurrent) {
          try {
            String currentVer = FileUtil.loadFile(new File(dotCurrent.getPath()));
            HaxelibSemVer semver = HaxelibSemVer.create(currentVer.trim());
            String libRootName = HaxeFileUtil.joinPath(rootName, libFSName, semver.toDirString());
            VirtualFile libRoot = lfs.findFileByPath(libRootName);
            if (null != libRoot) {
              return libRoot;
            }
          }
          catch (IOException e) {
            log.debug("IOException reading .current file for library " + libName, e);
          }
        }
      } else if(libVersion.equalsIgnoreCase("dev")) {
          // Hidden ".dev" file takes precedence.  It contains the path to the library root.
          VirtualFile dotDev = libDir.findChild(".dev");
          if (null != dotDev) {
            try {
              String libRootName = FileUtil.loadFile(new File(dotDev.getPath()));
              VirtualFile libRoot = lfs.findFileByPath(libRootName);
              if (null != libRoot) {
                return libRoot;
              }
            }
            catch (IOException e) {
              log.debug("IOException reading .dev file for library " + libName, e);
            }
          }
      }else {
        VirtualFile libVersionDir = libDir.findChild(libVersion.replaceAll("\\.", ","));
        if(libVersionDir!= null  && libVersionDir.exists()) {
          return libVersionDir;
        }
      }
    } else {
      if (log.isDebugEnabled())
        log.debug("Couldn't find directory " + libDirName + " for library " + libName);
    }

    // If we got here, then see what haxelib can give us.  This takes >40ms on average.
    List<String> output = HaxelibCommandUtils.issueHaxelibCommand(sdk, workDirectory,  "path", libName);
    for (String s : output) {
      if (s.isEmpty()) continue;
      if (s.startsWith("-D")) continue;
      if (s.startsWith("-L")) continue;
      if (s.matches("Error: .*")) {
        logWarningEvent(HaxeBundle.message("haxelib.synchronization.title"), s);
        continue; // break??
      }
      s = FileUtil.normalize(s);
      if (FileUtil.startsWith(s, rootName)) {
        String libClasspath = FileUtil.getRelativePath(rootName, s, '/');
        String[] libParts = libClasspath.split("/");  // Instead of FileUtil.splitpath() because normalized names use '/'
        // First one is always for the current library.
        if (log.isDebugEnabled()) {
          if (!libParts[0].equals(libName)) { // TODO: capitalization issue on windows??
            log.debug("Library found directory name '" + libParts[0] + "' did not match library name '" +
                      libName + ".");
          }
          // Second is normally the version string.  But that's not *always* the case, if it's a dev path.
          if (!libParts[1].matches(HaxelibSemVer.VERSION_REGEX)
              && !(new File(HaxeStringUtil.join("/", rootName, libParts[0], ".dev"))).exists()) {
            log.debug("Library version '" + libParts[1] + "' didn't match the regex.");
          }
        }
        String libRoot = FileUtil.join(rootName, libParts[0], libParts[1]);
        VirtualFile srcroot = LocalFileSystem.getInstance().refreshAndFindFileByPath(libRoot);
        if (null == srcroot) {
          logWarningEvent(HaxeBundle.message("haxelib.synchronization.title"),
                          HaxeBundle.message("library.source.root.was.not.found.0", libRoot));
        }
        return srcroot;
      }
    }

    logWarningEvent(HaxeBundle.message("haxelib.synchronization.title"),
                    HaxeBundle.message("could.not.determine.library.source.root.0", libName));
    return null;
  }



  /**
   * Get the libraries for the given module.  This does not include any
   * libraries from projects or SDKs.
   *
   * @param module to look up haxelib for.
   * @return a (possibly empty) collection of libraries.  These are NOT
   *         necessarily properly ordered, but they are unique.
   */
  // XXX: EMB - Not sure that I like this method here.  It could be a static on HaxeLibraryList,
  //      but that's not so great, either.  The reason I don't like it in this module is
  //      that this has to reach back out to HaxelibProjectUpdater to find the library cache for
  //      the module.
  @NotNull
  public static HaxeLibraryList getModuleLibraries(@NotNull final Module module) {
    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    if (null == rootManager) return new HaxeLibraryList(module);

    final HaxeLibraryList moduleLibs = new HaxeLibraryList(module);
    if (rootManager instanceof ModuleRootModel) {
      OrderEnumerator entries = rootManager.orderEntries();       // Can't fail.

      entries.forEachLibrary(new Processor<Library>() {
        @Override
        public boolean process(Library library) {
          if (library == null || library.getName() == null) return true;
          HaxeLibraryReference ref = HaxeLibraryReference.create(module, library.getName());
          if (null != ref) {
            moduleLibs.add(ref);
          }
          return true;
        }
      });
    } else {
      log.assertTrue(false, "Expected a ModuleRootManagerImpl, but didn't get one.");
    }

    return moduleLibs;
  }



  /**
   * Get the list of libraries specified on the project.  Managed haxelib
   * (of the form "haxelib|<lib_name>") libraries are included unless
   * filterManagedLibs is true.
   *
   *
   * @param project to get the classpath for.
   * @return a (possibly empty) list of the classpaths for all of the libraries
   *         that are specified on the project (in the library pane).
   */
  @NotNull
  public static HaxeLibraryList getProjectLibraries(@NotNull Project project, boolean filterManagedLibs, boolean filterUnmanagedLibs) {

    LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
    if (filterManagedLibs && filterUnmanagedLibs) {
      return new HaxeLibraryList(HaxelibSdkUtils.lookupSdk(project));
    }

    HaxeLibraryList libs = new HaxeLibraryList(HaxelibSdkUtils.lookupSdk(project));
    Library[] libraries = libraryTable.getLibraries();
    for (Library library : libraries) {
      String name = library.getName();
      if (name == null) continue;

      boolean isManaged = HaxelibNameUtil.isManagedLibrary(name);
      if (filterManagedLibs && isManaged) continue;
      if (filterUnmanagedLibs && !isManaged) continue;

      Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());

      modules.forEach( module -> libs.add(HaxeLibraryReference.create(module, name)));
    }
    return libs;
  }


  /**
   * Get the relative path from the SDK's library path to the given path.
   * Trim the library path out of the beginning of path.
   * @return The relative path, or null if the path isn't a library path.
   */
  @Nullable
  public static String getLibraryRelativeDirectory(@NotNull Sdk sdk, VirtualFile workDirectory, String path) {
    if (null == path || path.isEmpty()) {
      return null;
    }

    VirtualFile haxelibRoot = getLibraryBasePath(sdk, workDirectory);
    String rootName = haxelibRoot.getPath();

    String s = FileUtil.toSystemIndependentName(path);
    if (FileUtil.startsWith(s, rootName)) {
      return FileUtil.getRelativePath(rootName, s, HaxeFileUtil.SEPARATOR);
    }
    return null;
  }



  /**
   * Get information derivable from a library classpath.
   * @param libraryCache
   * @param path
   * @return
   */
  @Nullable
  public static HaxeLibraryInfo deriveLibraryInfoFromPath(@NotNull ModuleLibraryCache libraryCache, String path) {
    // TODO: Figure out how to get info from paths that are dev paths?  Don't need that for current callers.
    VirtualFile workDirectory = libraryCache.getHaxelibWorkDirectory();
    Sdk sdk = libraryCache.getSdk();

    String rel = getLibraryRelativeDirectory(sdk, workDirectory , path);
    if (null != rel && !rel.isEmpty()) {
      List<String> libParts = HaxeFileUtil.splitPath(rel);
      if (libParts.size() >= 2) {
        return new HaxeLibraryInfo(libParts.get(0),   // First part is the name
                                   libParts.get(1),   // Second part is the semantic version
                                   // The rest are the relative classpath.
                                   libParts.size() > 2 ? HaxeFileUtil.joinPath(libParts.subList(2, libParts.size())) : null);
      }
    }
    return null;
  }


  /**
   * Retrieves the list of dependent haxe libraries from an XML-based
   * configuration file.
   *
   * @param xmlFile name of the configuration file to read
   * @return a list of dependent libraries; may be empty, won't have duplicates.
   */
  @NotNull
  public static List<HaxeLibData> getHaxelibDataFromXmlFile(@NotNull XmlFile xmlFile) {
    List<HaxeLibraryReference> haxelibNewItems = new ArrayList<HaxeLibraryReference>();
    List<HaxeLibData> haxeLibData = new ArrayList<>();
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] haxelibTags = rootTag.findSubTags("haxelib");
        for (XmlTag haxelibTag : haxelibTags) {
          String name = haxelibTag.getAttributeValue("name");
          String ver = haxelibTag.getAttributeValue("version");
          HaxelibSemVer semver = HaxelibSemVer.create(ver);
          haxeLibData.add(new HaxeLibData(name, ver, semver));
        }
      }
    }
    return haxeLibData;
  }
  @NotNull
  public static HaxeLibraryList createHaxelibsFromHaxeLibData(@NotNull Module module, @NotNull List<HaxeLibData> haxeLibData, ModuleLibraryCache libraryManager) {
    List<HaxeLibraryReference> haxelibNewItems = new ArrayList<>();
    List<MissingLibInfo> missingList = new ArrayList<>();

    haxeLibData.forEach(data -> {
      String name = data.name;
      HaxelibSemVer semver = data.semver;
      if (name != null) {
        HaxeLibrary lib = libraryManager.getLibrary(name, semver);

        if (lib == null)  {
          // if not found using case-sensitive name try lowercase search
          // in some cases `haxelib list` and `haxelib search` does not return the same casing.
          // it looks like manually installed libs maintain casing while libs installed from haxelib repo are converted to lowercase
            lib = libraryManager.getLibrary(name.toLowerCase(), semver);
        }
        if (lib != null) {
          haxelibNewItems.add(lib.createReference(semver));
        }
        else {
          log.warn("Library specified in XML file is not found: " + name);
          HaxelibCacheManager cacheManager = HaxelibCacheManager.getInstance(module);
          Map<String, Set<String>> libraries = cacheManager.getAvailableLibraries();

          boolean libAvailable = libraries.containsKey(name);
          if(libAvailable) {
            Set<String> versions = libraries.getOrDefault(name, Set.of());
            if(versions.isEmpty()) {
              // attempt to fetch  versions available online for lib
              versions = cacheManager.fetchAvailableVersions(name);
            }
            boolean versionAvailable = HaxelibSemVer.isAny(semver) || versions.contains(data.version);
            if (versionAvailable){
              missingList.add(new MissingLibInfo(name, data.version, true));
            }else {
              missingList.add(new MissingLibInfo(name, data.version, false));
            }
          }else {
            missingList.add(new MissingLibInfo(name, data.version, false));
          }
        }
      }
    });
    HaxelibNotifier.notifyMissingLibraries(module, missingList);
    return new HaxeLibraryList(libraryManager.getSdk(), haxelibNewItems);
  }
  //TODO mlo make a proper class out of these
  public record MissingLibInfo(String name, String version, boolean installable){};
  public record HaxeLibData(String name, String version, HaxelibSemVer semver){};



  /**
   * Post an error event to the Event Log.
   *
   * @param title
   * @param details
   */
  public static void logErrorEvent(String title, String... details) {
    HaxeEventLogUtil.error(HAXELIB_LOG_ID, title, details);
  }

  /**
   * Post a warning event to the Event Log.
   *
   * @param title
   * @param details
   */
  public static void logWarningEvent(String title, String... details) {
    HaxeEventLogUtil.warn(HAXELIB_LOG_ID, title, details);
  }

  /**
   * Post an informational message to the Event Log.
   *
   * @param title
   * @param details
   */
  public static void logInfoEvent(String title, String... details) {
    HaxeEventLogUtil.info(HAXELIB_LOG_ID, title, details);
  }

}
