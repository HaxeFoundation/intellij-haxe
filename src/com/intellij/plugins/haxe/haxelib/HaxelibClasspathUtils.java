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
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VfsUtilCore;
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Static interface to haxelib class path functionality.
 */
public class HaxelibClasspathUtils {

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxelibClasspathUtils");
  {
    LOG.setLevel(Level.DEBUG);
  }

  /**
   * Gets all of the libraries specified for the IDEA project; source paths
   * class paths for global libraries, SDK, and exported module dependencies.
   *
   * @param project a project to get the class path settings for.
   * @return a list of class path URLs.
   */
  @NotNull
  public static List<String> getAllProjectLibraryClasspathsAsUrls(@NotNull Project project) {
    List<String> classpath = new ArrayList<String>();

    LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
    Library[] libraries = libraryTable.getLibraries();
    for (Library library : libraries) {
      // XXX: What's the deal with checking that the library name is null?
      // XXX: Actually, this is checking that the library name doesn't match "haxelib|lib_name".
      // XXX: That is, if it /is/ a haxelib, ignore it; grab the classpaths for libs that aren't haxelibs.
      // XXX: Doing the addAll for every library duplicates a large number (most!) of the entries.
      if (HaxelibParser.parseHaxelib(library.getName()) == null) {
        String sources[] = library.getUrls(OrderRootType.SOURCES);
        String classes[] = library.getUrls(OrderRootType.CLASSES);
        classpath.addAll(Arrays.asList(sources));  // Library source root
        // Rudimentary attempt to not add duplicate entries from libraries.
        if (!Arrays.equals(classes, sources)) {
          classpath.addAll(Arrays.asList(classes));  // Library class root
        }
      }
    }
    return classpath;
  }


  /**
   * Issue a 'haxelib' command to the OS, capturing its output.
   *
   * @param args arguments to be provided to the haxelib command.
   * @return a set of Strings, possibly empty, one per line of command output.
   */
  @NotNull
  private static List<String> issueHaxelibCommand(String ... args) {
    // TODO: Wrap this invocation with a timer??

    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add("haxelib");
    for (String arg : args) {
      commandLineArguments.add(arg);
    }

    List<String> strings = getProcessStdout(commandLineArguments);
    return strings;
  }

  /**
   * Locate files and dependencies using 'haxelib path <name>'
   *
   * @param name name of the base file or library to search for.
   * @return a set of path name URLs, may be an empty list.
   */
  @NotNull
  public static List<String> getHaxelibPathUrl(@NotNull String name) {
    List<String> strings = issueHaxelibCommand("path", name);
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
  public static List<HaxelibItem> getHaxelibPath(@NotNull String name) {
    List<String> strings = issueHaxelibCommand("path", name);
    List<HaxelibItem> classpathUrls = new ArrayList<HaxelibItem>(strings.size());

    for (String string : strings) {
      if (!string.startsWith("-L") && !string.startsWith("-D")) {
        VirtualFile file = LocalFileFinder.findFile(string);
        if (file != null) {
          classpathUrls.add(new HaxelibItem(file.getPath(), file.getUrl()));
        }
      }
    }

    return classpathUrls;
  }

  /**
   * Run a shell command, capturing its standard output.
   *
   * @param commandLineArguments a command and its arguments, as a list of strings.
   * @param dir directory in which to run the command.
   * @return the output of the command, as a list of strings, one line per string.
   */
  @NotNull
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments, @Nullable File dir) {
    List<String> strings = new ArrayList<String>();

    try {
      ProcessBuilder builder = new ProcessBuilder(commandLineArguments);
      if (dir != null) {
        builder = builder.directory(dir);
      }
      Process process = builder.start();
      InputStreamReader reader = new InputStreamReader(process.getInputStream());
      Scanner scanner = new Scanner(reader);
      process.waitFor();

      while (scanner.hasNextLine()) {
        String nextLine = scanner.nextLine();
        strings.add(nextLine);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    return strings;
  }

  /**
   * Run a shell command in the (IDEA's) current directory, capturing its standard output.
   *
   * @param commandLineArguments a command and its arguments, as a list of strings.
   * @return the output of the command, as a list of strings, one line per string.
   */
  @NotNull
  public static List<String> getProcessStdout(@NotNull ArrayList<String> commandLineArguments) {
    return getProcessStdout(commandLineArguments, null);
  }

  public static List<String> getInstalledLibraries() {

    // haxelib list output looks like:
    //      lime-tools: 1.4.0 [1.5.6]
    // The library name comes first, followed by a colon, followed by a
    // list of the available versions.

    List<String> installedHaxelibs = new ArrayList<String>();
    for (String s : issueHaxelibCommand("list")) {
      installedHaxelibs.add(s.split(":")[0]);
    }

    return installedHaxelibs;
  }

  /**
   * Find haxe libraries matching a search word
   * @param word
   * @return
   */
  @NotNull
  public static List<String> getAvailableLibrariesMatching(String word) {
    return issueHaxelibCommand("search", word);
  }

  @NotNull
  public static List<String> getProjectDisplayInformation(@NotNull Project project, @NotNull File dir, @NotNull String executable) {
    List<String> strings1 = Collections.EMPTY_LIST;

    if (getInstalledLibraries().contains(executable)) {
      ArrayList<String> commandLineArguments = new ArrayList<String>();
      commandLineArguments.add("haxelib");
      commandLineArguments.add("run");
      commandLineArguments.add(executable);
      commandLineArguments.add("display");
      commandLineArguments.add("flash");

      List<String> strings = getProcessStdout(commandLineArguments, dir);
      String s = Joiner.on("\n").join(strings);
      strings1 = getHXMLFileClasspaths(project, s);
    }

    return strings1;
  }

  @NotNull
  public static List<String> getHXMLFileClasspaths(@NotNull Project project, @NotNull String text) {
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
   * @return a list of dependent libraries; may be empty.
   */
  @NotNull
  public static List<HaxelibItem> getHaxelibsFromXmlFile(@NotNull XmlFile psiFile, HaxelibLibraryManager libraryManager) {
    List<HaxelibItem> haxelibNewItems = new ArrayList<HaxelibItem>();

    XmlFile xmlFile = (XmlFile)psiFile;
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] haxelibTags = rootTag.findSubTags("haxelib");
        for (XmlTag haxelibTag : haxelibTags) {
          String name = haxelibTag.getAttributeValue("name");
          if (name != null) {
            List<HaxelibItem> haxelibNewItemList = libraryManager.getClasspathForLibrary(name);
            haxelibNewItems.addAll(haxelibNewItemList);
          }
        }
      }
    }

    return haxelibNewItems;
  }




}
