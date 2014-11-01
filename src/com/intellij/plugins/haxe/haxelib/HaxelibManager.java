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
import com.intellij.compiler.ant.BuildProperties;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.hxml.HXMLFileType;
import com.intellij.plugins.haxe.hxml.psi.HXMLClasspath;
import com.intellij.plugins.haxe.hxml.psi.HXMLLib;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.nmml.NMMLFileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by as3boyan on 31.10.14.
 */
public class HaxelibManager implements com.intellij.openapi.module.ModuleComponent {
  public static void addLibraryWithClasspath(final Module myModule, final String name, final String classpathUrl) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        LibraryTable libraryTable = ProjectLibraryTable.getInstance(myModule.getProject());

        LibraryTable.ModifiableModel libraryTableModifiableModel = libraryTable.getModifiableModel();

        //String name = "3,3,5";
        //"/usr/lib/haxe/lib/flixel/3,3,5"
        Library libraryByName = libraryTableModifiableModel.getLibraryByName(name);
        if (libraryByName == null) {
          libraryByName = libraryTableModifiableModel.createLibrary(name);
          Library[] libraries = libraryTableModifiableModel.getLibraries();
          Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
          libraryModifiableModel.addRoot(classpathUrl, OrderRootType.CLASSES);
          libraryModifiableModel.addRoot(classpathUrl, OrderRootType.SOURCES);
          libraryModifiableModel.commit();
          libraryTableModifiableModel.commit();

          ModuleRootModificationUtil.addDependency(myModule, libraryByName);
        }
      }
    });
  }

  public static void addLibraryWithClasspath(final Module myModule, final String name) {
    if (getInstalledLibraries().contains(name)) {
      String haxelibPathUrl = getHaxelibPathUrl(myModule, name);

      if (haxelibPathUrl != null) {
        addLibraryWithClasspath(myModule, VfsUtilCore.urlToPath(haxelibPathUrl), haxelibPathUrl);
      }
    }
  }

  public static List<String> getAllLibrariesClasspaths(Module myModule) {
    List<String> classpath = new ArrayList<String>();

    LibraryTable libraryTable = ProjectLibraryTable.getInstance(myModule.getProject());
    Library[] libraries = libraryTable.getLibraries();
    for (Library library : libraries) {
      classpath.addAll(Arrays.asList(library.getUrls(OrderRootType.SOURCES)));
      classpath.addAll(Arrays.asList(library.getUrls(OrderRootType.CLASSES)));
    }
    return classpath;
  }

  public static String getHaxelibPathUrl(Module myModule, String name) {
    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add("haxelib");
    commandLineArguments.add("path");
    commandLineArguments.add(name);

    //"nme"

    List<String> classpath = getAllLibrariesClasspaths(myModule);

    List<String> strings = getProcessStdout(commandLineArguments);

    for (String string : strings) {
      if (!string.startsWith("-L") && !string.startsWith("-D")) {
        VirtualFile file = LocalFileFinder.findFile(string);
        if (file != null && !classpath.contains(file.getUrl())) {
          return file.getUrl();
        }
      }
    }

    return null;
  }

  public static List<String> getProcessStdout(ArrayList<String> commandLineArguments, @Nullable File dir) {
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

  public static List<String> getProcessStdout(ArrayList<String> commandLineArguments) {
    return getProcessStdout(commandLineArguments, null);
  }

  public static List<HaxelibItem> getAllLibraries(Module myModule) {
    List<HaxelibItem> haxelibItems = new ArrayList<HaxelibItem>();
    LibraryTable libraryTable = ProjectLibraryTable.getInstance(myModule.getProject());

    for (Library library : libraryTable.getLibraries()) {
      HaxelibItem haxelibItem = HaxelibParser.parseHaxelib(library.getName());
      if (haxelibItem != null) {
        haxelibItems.add(haxelibItem);
      }
    }

    return haxelibItems;
  }

  public static boolean isLibraryAdded(List<HaxelibItem> haxelibItems, String name) {
    for (HaxelibItem item : haxelibItems) {
      if (item.name.equals(name)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isNMELibraryAdded(List<HaxelibItem> haxelibItems) {
    return isLibraryAdded(haxelibItems, "nme");
  }
  public static boolean isOpenFLLibraryAdded(List<HaxelibItem> haxelibItems) {
    return isLibraryAdded(haxelibItems, "openfl");
  }

  public static List<String> getInstalledLibraries() {
    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add("haxelib");
    commandLineArguments.add("list");

    List<String> installedHaxelibs = new ArrayList<String>();

    for (String s : getProcessStdout(commandLineArguments)) {
      installedHaxelibs.add(s.split(":")[0]);
    }

    return installedHaxelibs;
  }

  public static List<String> getAvailableLibraries() {
    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add("haxelib");
    commandLineArguments.add("search");
    commandLineArguments.add("");

    return getProcessStdout(commandLineArguments);
  }

  public static List<String> getProjectDisplayInformation(Project project, File dir, String executable) {
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

  public static List<String> getHXMLFileClasspaths(Project project, String text) {
    List<String> strings1;
    strings1 = new ArrayList<String>();
    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(HXMLFileType.INSTANCE, "data.hxml", text, 0, text.length() - 1);

    Collection<HXMLClasspath> hxmlClasspaths = PsiTreeUtil.findChildrenOfType(psiFile, HXMLClasspath.class);
    for (HXMLClasspath hxmlClasspath : hxmlClasspaths) {
      strings1.add(hxmlClasspath.getValue());
    }
    return strings1;
  }

  @Override
  public void projectOpened() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();

    for (Project project : projects) {
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        List<HaxelibItem> allLibraries = HaxelibManager.getAllLibraries(module);

        HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
        switch (settings.getBuildConfig()) {
          case HaxeModuleSettings.USE_NMML:
            if (!HaxelibManager.isNMELibraryAdded(allLibraries)) {
              HaxelibManager.addLibraryWithClasspath(module, "nme");
            }

            String nmmlPath = settings.getNmmlPath();
            if (nmmlPath != null && !nmmlPath.isEmpty()) {
              VirtualFile file = LocalFileFinder.findFile(nmmlPath);

              if (file != null && file.getFileType().equals(XmlFileType.INSTANCE)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

                if (psiFile != null && psiFile instanceof XmlFile) {
                  addHaxelibsFromXmlProjectFile(module, (XmlFile)psiFile);
                }
              }
            }
            else {
              File dir = BuildProperties.getProjectBaseDir(project);
              List<String> projectClasspaths = getProjectDisplayInformation(project, dir, "nme");

              for (String classpath : projectClasspaths) {
                VirtualFile file = LocalFileFinder.findFile(classpath);
                if (file != null) {
                  HaxelibManager.addLibraryWithClasspath(module, classpath, file.getUrl());
                }
              }
            }
            break;
          case HaxeModuleSettings.USE_OPENFL:
            if (!HaxelibManager.isOpenFLLibraryAdded(allLibraries)) {
              HaxelibManager.addLibraryWithClasspath(module, "openfl");
            }

            String openFLXmlPath = settings.getOpenFLXmlPath();
            if (openFLXmlPath != null && !openFLXmlPath.isEmpty()) {
              VirtualFile file = LocalFileFinder.findFile(openFLXmlPath);

              if (file != null && file.getFileType().equals(NMMLFileType.INSTANCE)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

                if (psiFile != null && psiFile instanceof XmlFile) {
                  addHaxelibsFromXmlProjectFile(module, (XmlFile)psiFile);
                }
              }
            }
            else {
              File dir = BuildProperties.getProjectBaseDir(project);
              List<String> projectClasspaths = getProjectDisplayInformation(project, dir, "openfl");

              for (String classpath : projectClasspaths) {
                VirtualFile file = LocalFileFinder.findFile(classpath);
                if (file != null) {
                  HaxelibManager.addLibraryWithClasspath(module, classpath, file.getUrl());
                }
              }
            }
            break;
          case HaxeModuleSettings.USE_HXML:
            String hxmlPath = settings.getHxmlPath();

            if (hxmlPath != null && !hxmlPath.isEmpty()) {
              VirtualFile file = LocalFileFinder.findFile(hxmlPath);

              if (file != null && file.getFileType().equals(HXMLFileType.INSTANCE)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile != null) {
                  Collection<HXMLClasspath> hxmlClasspaths = PsiTreeUtil.findChildrenOfType(psiFile, HXMLClasspath.class);
                  for (HXMLClasspath hxmlClasspath : hxmlClasspaths) {
                    String classpath = hxmlClasspath.getValue();
                    HaxelibManager.addLibraryWithClasspath(module, classpath, VfsUtil.pathToUrl(classpath));
                  }

                  Collection<HXMLLib> hxmlLibs = PsiTreeUtil.findChildrenOfType(psiFile, HXMLLib.class);
                  for (HXMLLib hxmlLib : hxmlLibs) {
                    String name = hxmlLib.getValue();
                    HaxelibManager.addLibraryWithClasspath(module, name);
                  }
                }
              }
            }

            break;

          case HaxeModuleSettings.USE_PROPERTIES:
            String arguments = settings.getArguments();
            if (!arguments.isEmpty()) {
              List<String> classpaths = getHXMLFileClasspaths(project, arguments);

              for (String classpath : classpaths) {
                HaxelibManager.addLibraryWithClasspath(module, classpath, VfsUtil.pathToUrl(classpath));
              }
            }
        }
      }
    }
  }

  public void addHaxelibsFromXmlProjectFile(Module module, XmlFile psiFile) {
    XmlFile xmlFile = (XmlFile)psiFile;
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] haxelibTags = rootTag.findSubTags("haxelib");
        for (XmlTag haxelibTag : haxelibTags) {
          String name = haxelibTag.getAttributeValue("name");
          if (name != null) {
            HaxelibManager.addLibraryWithClasspath(module, name);
          }
        }
      }
    }
  }

  @Override
  public void projectClosed() {

  }

  @Override
  public void moduleAdded() {

  }

  @Override
  public void initComponent() {

  }

  @Override
  public void disposeComponent() {

  }

  @NotNull
  @Override
  public String getComponentName() {
    return "com.intellij.plugins.haxe.haxelib.HaxelibManager";
  }
}