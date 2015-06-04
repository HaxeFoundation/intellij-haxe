/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.google.common.base.Joiner;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.hxml.HXMLFileType;
import com.intellij.plugins.haxe.hxml.psi.HXMLClasspath;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.LocalFileFinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Static interface to haxelib class path functionality.
 */
public class HaxelibClasspathUtils {

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxelibClasspathUtils");
  {
    LOG.setLevel(Level.DEBUG);
  }

  /**
   * Gets the libraries specified for the IDEA project; source paths and
   * class paths for project libraries excepting those named "haxelib|<lib_name>".
   *
   * @param project a project to get the class path settings for.
   * @return a list of class path URLs.
   */
  @NotNull
  public static HaxeClasspath getUnmanagedProjectLibraryClasspath(@NotNull Project project) {
    LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
    if (null == libraryTable) return HaxeClasspath.EMPTY_CLASSPATH;

    HaxeClasspath classpath = new HaxeClasspath();

    Library[] libraries = libraryTable.getLibraries();
    for (Library library : libraries) {
      //
      // This is checking that the library name doesn't match "haxelib|lib_name".
      // That is, if it /is/ a haxelib entry, ignore it; grab the classpaths for
      // libs that aren't haxelibs.
      //
      if (!HaxelibParser.isManagedLibrary(library.getName())) {
        OrderRootType interestingRootTypes[] = {OrderRootType.SOURCES, OrderRootType.CLASSES};
        for (OrderRootType rootType : interestingRootTypes) {
          for (String url : library.getUrls(rootType)) {
            if (!classpath.containsUrl(url)) {  // The if just keeps us from churning.
              classpath.add(new HaxeIdeaItem(library.getName(), url));
            }
          }
        }
      }
    }
    return classpath;
  }

  /**
   * Get the list of library names specified on the project.  This *does not*
   * filter managed libraries of the form "haxelib|<lib_name>".
   *
   * @param project to get the libraries for.
   * @return a (possibly empty) list of libraries that are specified for the
   *         project (in the library pane).
   */
  @NotNull
  public static List<String> getProjectLibraryNames(@NotNull Project project) {
    return getProjectLibraryNames(project, false);
  }

  /**
   * Get the list of library names specified on the project.  Managed haxelib
   * (of the form "haxelib|<lib_name>") libraries are included unless
   * filterManagedLibs is true.
   *
   * @param project to get the libraries for.
   * @param filterManagedLibs whether to remove managed haxelibs from the list.
   * @return a (possibly empty) list of libraries that are specified for the
   *         project (in the library pane).
   */
  @NotNull
  public static List<String> getProjectLibraryNames(@NotNull Project project, boolean filterManagedLibs) {
    List<String> nameList = new ArrayList<String>();
    LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
    Library[] libraries = libraryTable.getLibraries();
    for (Library library : libraries) {
      if (filterManagedLibs && HaxelibParser.isManagedLibrary(library.getName())) {
        continue;
      }
      nameList.add(library.getName());
    }
    return nameList;
  }

  /**
   * Loads a classpath from the given library table.  Workhorse for other APIs
   * in this class.
   *
   * @param libraryTable to load
   * @return the classpath
   */
  @NotNull
  private static HaxeClasspath loadClasspathFrom(LibraryTable libraryTable) {
    HaxeClasspath classpath = new HaxeClasspath();
    Library[] libraries = libraryTable.getLibraries();
    OrderRootType interestingRootTypes[] = {OrderRootType.SOURCES, OrderRootType.CLASSES};
    for (Library library : libraries) {
      for (OrderRootType rootType : interestingRootTypes) {
        for (String url : library.getUrls(rootType)) {
          if (!classpath.containsUrl(url)) {
            classpath.add(new HaxeIdeaItem(library.getName(), url));
          }
        }
      }
    }
    return classpath;
  }

  /**
   * Get the classpath for all libraries in the project.  This *does not*
   * filter managed libraries of the form "haxelib|<lib_name>".
   *
   * @param project to get the classpath for.
   * @return a (possibly empty) list of the classpaths for all of the libraries
   *         that are specified on the project (in the library pane).
   */
  @NotNull
  public static HaxeClasspath getProjectLibraryClasspath(@NotNull Project project) {
    LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
    if (null == libraryTable) return HaxeClasspath.EMPTY_CLASSPATH;
    return loadClasspathFrom(libraryTable);
  }

  /**
   * Get the classpath for the given SDK.  This does not include
   * any paths from the project or modules.
   *
   * @param sdk to get path info from.
   * @return a (possibly empty) collection of class paths.  These are NOT
   *         necessarily properly ordered, but they are unique.
   */
  @NotNull
  public static HaxeClasspath getSdkClasspath(@NotNull Sdk sdk) {
    HaxeClasspath classpath = new HaxeClasspath();
    RootProvider rootProvider = sdk.getRootProvider();
    OrderRootType interestingRootTypes[] = {OrderRootType.SOURCES, OrderRootType.CLASSES};
    for (OrderRootType rootType : interestingRootTypes) {
      for (VirtualFile file : rootProvider.getFiles(rootType)) {
        if (!classpath.containsUrl(file.getUrl())) {
          classpath.add(new HaxelibItem(file.getName(), file.getUrl()));
        }
      }
    }
    return classpath;
  }

  /**
   * Get the classpath for the given module.  This does not include any
   * paths from projects or SDKs.
   *
   * @param module to look up haxelib for.
   * @return a (possibly empty) collection of classpaths.  These are NOT
   *         necessarily properly ordered, but they are unique.
   */
  @NotNull
  public static HaxeClasspath getModuleClasspath(@NotNull Module module) {
    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    if (null == rootManager) return HaxeClasspath.EMPTY_CLASSPATH;

    ModifiableRootModel rootModel = rootManager.getModifiableModel();
    LibraryTable libraryTable = rootModel.getModuleLibraryTable();
    HaxeClasspath moduleClasspath = loadClasspathFrom(libraryTable);
    rootModel.dispose();    // MUST dispose of the model.
    return moduleClasspath;
  }


  /**
   * Locate files and dependencies using 'haxelib path <name>'
   *
   * @param name name of the base file or library to search for.
   * @return a set of path name URLs, may be an empty list.
   */
  @NotNull
  public static List<String> getHaxelibLibraryPathUrl(@NotNull Sdk sdk, @NotNull String name) {
    List<String> strings = HaxelibCommandUtils.issueHaxelibCommand(sdk, "path",
                                                                   name);
    List<String> classpathUrls = new ArrayList<String>(strings.size());

    for (String string : strings) {
      if (!string.startsWith("-L") && !string.startsWith("-D")) {
        VirtualFile file = LocalFileFinder.findFile(string);
        if (file != null) {
          classpathUrls.add(file.getUrl());
        }
      }
    }

    return classpathUrls;
  }

  /**
   * Locate files and dependencies using 'haxelib path <name>'
   *
   * @param name name of the base file or library to search for.
   * @return a set of HaxelibItems, may be an empty list.
   */
  @NotNull
  public static HaxeClasspath getHaxelibLibraryPath(@NotNull Sdk sdk, @NotNull String name) {
    List<String> strings = HaxelibCommandUtils.issueHaxelibCommand(sdk, "path", name);
    HaxeClasspath classpath = new HaxeClasspath(strings.size());

    for (String string : strings) {
      if (!string.startsWith("-L") && !string.startsWith("-D")) {
        VirtualFile file = LocalFileFinder.findFile(string);
        if (file != null) {
          // There are no duplicates in the return from haxelib, so no need to check contains().
          classpath.add(new HaxelibItem(file.getPath(), file.getUrl()));
        }
      }
    }

    return classpath;
  }

  /**
   * Retrieve the list of libraries known to 'haxelib', using the version of
   * haxelib specified in the SDK.
   *
   * @param sdk the SDK to get installed libraries from.
   * @return a (possibly empty) list of libraries
   */
  @NotNull
  public static List<String> getInstalledLibraries(@NotNull Sdk sdk) {

    // haxelib list output looks like:
    //      lime-tools: 1.4.0 [1.5.6]
    // The library name comes first, followed by a colon, followed by a
    // list of the available versions.

    List<String> installedHaxelibs = new ArrayList<String>();
    for (String s : HaxelibCommandUtils.issueHaxelibCommand(sdk, "list")) {
      installedHaxelibs.add(s.split(":")[0]);
    }

    return installedHaxelibs;
  }


  /**
   * Find haxe libraries matching a search word, using the version of haxelib
   * specified by the SDK.
   *
   * @param sdk the SDK to get installed libraries from.
   * @param word search word.  Must follow haxelib conventions: partial words are OK, no globbing.
   *             Empty matches everything.
   * @return
   */
  @NotNull
  public static List<String> getAvailableLibrariesMatching(@NotNull Sdk sdk, @NotNull String word) {
    List<String> stringList = HaxelibCommandUtils.issueHaxelibCommand(sdk, "search", word);
    stringList.remove(stringList.size() - 1);
    return stringList;
  }

  /**
   *
   * @param project
   * @param dir
   * @param executable
   * @param sdk
   * @return
   */
  @NotNull
  public static List<String> getProjectDisplayInformation(@NotNull Project project, @NotNull File dir, @NotNull String executable, @NotNull Sdk sdk) {
    List<String> strings1 = Collections.EMPTY_LIST;

    if (getInstalledLibraries(sdk).contains(executable)) {
      ArrayList<String> commandLineArguments = new ArrayList<String>();
      commandLineArguments.add(HaxelibCommandUtils.getHaxelibPath(sdk));
      commandLineArguments.add("run");
      commandLineArguments.add(executable);
      commandLineArguments.add("display");
      commandLineArguments.add("flash");

      List<String> strings = HaxelibCommandUtils.getProcessStdout(commandLineArguments, dir);
      String s = Joiner.on("\n").join(strings);
      strings1 = getHXMLFileClasspaths(project, s);
    }

    return strings1;
  }

  /**
   * Turn some text into a file, parse it using the .hxml parser, and
   * return any HXML classpaths.
   *
   * @param project to include created files in.
   * @param text to parse.
   * @return A (possibly empty) list containing all of the classpaths found in the text.
   */
  @NotNull
  public static List<String> getHXMLFileClasspaths(@NotNull Project project, @NotNull String text) {
    if (text.isEmpty()) {
      return Collections.EMPTY_LIST;
    }

    List<String> strings1;
    strings1 = new ArrayList<String>();
    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(HXMLFileType.INSTANCE, "data.hxml", text, 0, text.length() - 1);

    Collection<HXMLClasspath> hxmlClasspaths = PsiTreeUtil.findChildrenOfType(psiFile, HXMLClasspath.class);
    for (HXMLClasspath hxmlClasspath : hxmlClasspaths) {
      strings1.add(hxmlClasspath.getValue());
    }
    return strings1;
  }

  /**
   * Retrieves the list of dependent haxe libraries from an XML-based
   * configuration file.
   *
   * @param psiFile name of the configuration file to read
   * @return a list of dependent libraries; may be empty, may have duplicates.
   * TODO: Collect names and pass them all to libraryManager.getClasspathForHaxelib(List) at once.
   */
  @NotNull
  public static HaxeClasspath getHaxelibsFromXmlFile(@NotNull XmlFile psiFile, HaxelibLibraryCache libraryManager) {
    HaxeClasspath haxelibNewItems = new HaxeClasspath();

    XmlFile xmlFile = (XmlFile)psiFile;
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] haxelibTags = rootTag.findSubTags("haxelib");
        for (XmlTag haxelibTag : haxelibTags) {
          String name = haxelibTag.getAttributeValue("name");
          if (name != null) {
            HaxeClasspath newPath = libraryManager.getClasspathForHaxelib(name);
            haxelibNewItems.addAll(newPath);
          }
        }
      }
    }

    return haxelibNewItems;
  }


}
