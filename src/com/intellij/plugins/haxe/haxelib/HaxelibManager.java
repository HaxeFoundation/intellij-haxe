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
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.hxml.HXMLFileType;
import com.intellij.plugins.haxe.hxml.psi.HXMLClasspath;
import com.intellij.plugins.haxe.hxml.psi.HXMLLib;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.nmml.NMMLFileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.webcore.ModuleHelper;
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

  Module mMyModule = null;
  public HaxelibManager(Module myModule) {
    mMyModule = myModule;
  }

  public static List<String> getAllLibrariesClasspathsUrls(Module myModule) {
    List<String> classpath = new ArrayList<String>();

    LibraryTable libraryTable = ProjectLibraryTable.getInstance(myModule.getProject());
    Library[] libraries = libraryTable.getLibraries();
    for (Library library : libraries) {
      if (HaxelibParser.parseHaxelib(library.getName()) == null) {
        classpath.addAll(Arrays.asList(library.getUrls(OrderRootType.SOURCES)));
        classpath.addAll(Arrays.asList(library.getUrls(OrderRootType.CLASSES)));
      }
    }
    return classpath;
  }

  public static List<String> getHaxelibPathUrl(Module myModule, String name) {
    List<String> classpathUrls = new ArrayList<String>();

    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add("haxelib");
    commandLineArguments.add("path");
    commandLineArguments.add(name);

    List<String> strings = getProcessStdout(commandLineArguments);

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

  public static List<HaxelibItem> findHaxelibPath(Module myModule, String name) {
    List<HaxelibItem> haxelibNewItems = new ArrayList<HaxelibItem>();

    if (getInstalledLibraries().contains(name)) {
      List<String> haxelibPathUrls = getHaxelibPathUrl(myModule, name);

      for (String url : haxelibPathUrls) {
        haxelibNewItems.add(new HaxelibItem(VfsUtilCore.urlToPath(url), url));
      }
    }

    return haxelibNewItems;
  }

  public static void addLibraries(final Module module, final List<HaxelibItem> haxelibNewItems) {
    if (haxelibNewItems.isEmpty()) {
      return;
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        LibraryTable libraryTable = ProjectLibraryTable.getInstance(module.getProject());

        LibraryTable.ModifiableModel libraryTableModifiableModel = libraryTable.getModifiableModel();

        List<String> strings = new ArrayList<String>();

        List<String> allLibrariesClasspaths = getAllLibrariesClasspathsUrls(module);

        List<HaxelibItem> haxelibNewItems1 = new ArrayList<HaxelibItem>();

        for (HaxelibItem haxelibNewItem : haxelibNewItems) {
          if (!allLibrariesClasspaths.contains(haxelibNewItem.classpathUrl)) {
            haxelibNewItems1.add(haxelibNewItem);
          }
        }

        for (HaxelibItem haxelibNewItem : haxelibNewItems1) {
          String haxelib = HaxelibParser.stringifyHaxelib(new HaxelibItem(haxelibNewItem.name));
          strings.add(haxelib);
        }

        Library[] libraryTableModifiableModelLibraries = libraryTableModifiableModel.getLibraries();
        for (Library library : libraryTableModifiableModelLibraries) {
          if (HaxelibParser.parseHaxelib(library.getName()) != null && !strings.contains(library.getName())) {
            ModuleHelper.removeDependency(ModuleRootManager.getInstance(module), library);
            libraryTableModifiableModel.removeLibrary(library);
          }
        }

        for (int i = 0; i < haxelibNewItems1.size(); i++) {
          Library libraryByName = libraryTableModifiableModel.getLibraryByName(strings.get(i));
          if (libraryByName == null) {
            libraryByName = libraryTableModifiableModel.createLibrary(strings.get(i));
            Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
            libraryModifiableModel.addRoot(haxelibNewItems1.get(i).classpathUrl, OrderRootType.CLASSES);
            libraryModifiableModel.addRoot(haxelibNewItems1.get(i).classpathUrl, OrderRootType.SOURCES);
            libraryModifiableModel.commit();

            ModuleRootModificationUtil.addDependency(module, libraryByName);
          }
        }

        libraryTableModifiableModel.commit();
      }
    });
  }


  @Override
  public void projectOpened() {
    StartupManager.getInstance(mMyModule.getProject()).runWhenProjectIsInitialized(new Runnable() {
      public void run() {
        findUsedLibrariesAndAddToProject();
      }
    });
  }

  public void findUsedLibrariesAndAddToProject() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();

    for (final Project project : projects) {
      for (final Module module : ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance())) {
        List<HaxelibItem> haxelibNewItems = new ArrayList<HaxelibItem>();

        HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
        int buildConfig = settings.getBuildConfig();
        List<HaxelibItem> haxelibNewItemList;

        switch (buildConfig) {
          case HaxeModuleSettings.USE_NMML:
            haxelibNewItemList = findHaxelibPath(module, "nme");
            haxelibNewItems.addAll(haxelibNewItemList);

            String nmmlPath = settings.getNmmlPath();
            if (nmmlPath != null && !nmmlPath.isEmpty()) {
              VirtualFile file = LocalFileFinder.findFile(nmmlPath);

              if (file != null && file.getFileType().equals(NMMLFileType.INSTANCE)) {
                VirtualFileManager.getInstance().syncRefresh();
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

                if (psiFile != null && psiFile instanceof XmlFile) {
                  haxelibNewItems.addAll(getHaxelibsFromXmlProjectFile(module, (XmlFile)psiFile));
                }
              }
            }
            break;
          case HaxeModuleSettings.USE_OPENFL:
            haxelibNewItemList = findHaxelibPath(module, "openfl");
            haxelibNewItems.addAll(haxelibNewItemList);

            String openFLXmlPath = settings.getOpenFLPath();
            if (openFLXmlPath != null && !openFLXmlPath.isEmpty()) {
              VirtualFile file = LocalFileFinder.findFile(openFLXmlPath);

              if (file != null && file.getFileType().equals(XmlFileType.INSTANCE)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

                if (psiFile != null && psiFile instanceof XmlFile) {
                  haxelibNewItems.addAll(getHaxelibsFromXmlProjectFile(module, (XmlFile)psiFile));
                }
              }
            }
            else {
              File dir = BuildProperties.getProjectBaseDir(project);
              List<String> projectClasspaths = getProjectDisplayInformation(project, dir, "openfl");

              for (String classpath : projectClasspaths) {
                VirtualFile file = LocalFileFinder.findFile(classpath);
                if (file != null) {
                  haxelibNewItems.add(new HaxelibItem(classpath, file.getUrl()));
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
                    haxelibNewItems.add(new HaxelibItem(classpath, VfsUtil.pathToUrl(classpath)));
                  }

                  Collection<HXMLLib> hxmlLibs = PsiTreeUtil.findChildrenOfType(psiFile, HXMLLib.class);
                  for (HXMLLib hxmlLib : hxmlLibs) {
                    String name = hxmlLib.getValue();
                    haxelibNewItemList = findHaxelibPath(module, name);
                    haxelibNewItems.addAll(haxelibNewItemList);
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
                haxelibNewItems.add(new HaxelibItem(classpath, VfsUtil.pathToUrl(classpath)));
              }
            }

            break;
        }

        addLibraries(module, haxelibNewItems);
      }
    }
  }

  public List<HaxelibItem> getHaxelibsFromXmlProjectFile(Module module, XmlFile psiFile) {
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
            List<HaxelibItem> haxelibNewItemList = findHaxelibPath(module, name);
            haxelibNewItems.addAll(haxelibNewItemList);
          }
        }
      }
    }

    return haxelibNewItems;
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