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
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
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
  //static Module module;
  //static List<String> names = new ArrayList<String>();
  //static List<String> classpathUrls = new ArrayList<String>();

  /*public static void addLibraryWithClasspath(final Module myModule, final String name, final String classpathUrl) {
    module = myModule;
    names.add(name);
    classpathUrls.add(classpathUrl);
  }

  public static void addLibraryWithClasspath(final Module myModule, final String name) {
    if (getInstalledLibraries().contains(name)) {
      String haxelibPathUrl = getHaxelibPathUrl(myModule, name);

      if (haxelibPathUrl != null) {
        addLibraryWithClasspath(myModule, VfsUtilCore.urlToPath(haxelibPathUrl), haxelibPathUrl);
      }
    }
  }*/

  /*
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
  */

  public static List<String> getHaxelibPathUrl(Module myModule, String name) {
    List<String> classpathUrls = new ArrayList<String>();

    ArrayList<String> commandLineArguments = new ArrayList<String>();
    commandLineArguments.add("haxelib");
    commandLineArguments.add("path");
    commandLineArguments.add(name);

    //"nme"

    //List<String> classpath = getAllLibrariesClasspaths(myModule);

    List<String> strings = getProcessStdout(commandLineArguments);

    for (String string : strings) {
      if (!string.startsWith("-L") && !string.startsWith("-D")) {
        VirtualFile file = LocalFileFinder.findFile(string);
        if (file != null) {
          classpathUrls.add(file.getUrl());
        }
        //!classpath.contains(file.getUrl())
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

  public static List<Library> getAllLibraries(Module myModule) {
    List<Library> haxelibItems = new ArrayList<Library>();
    LibraryTable libraryTable = ProjectLibraryTable.getInstance(myModule.getProject());

    for (Library library : libraryTable.getLibraries()) {
      //HaxelibItem haxelibItem = HaxelibParser.parseHaxelib(library.getName());
      //if (haxelibItem != null) {
        haxelibItems.add(library);
      //}
    }

    return haxelibItems;
  }

  public static boolean isLibraryAdded(List<HaxelibItem> haxelibItems, String classpath) {
    for (HaxelibItem item : haxelibItems) {
      if (item.classpath.equals(classpath)) {
        return true;
      }
    }

    return false;
  }

  /*
  public static boolean isNMELibraryAdded(List<HaxelibItem> haxelibItems) {
    //return isLibraryAdded(haxelibItems, "nme");
    return false;
  }
  */
  /*public static boolean isOpenFLLibraryAdded(List<HaxelibItem> haxelibItems) {
    //return isLibraryAdded(haxelibItems, "openfl");
    return false;
  }*/

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

  /*
  public static void commit() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        //List<Library> allLibraries = HaxelibManager.getAllLibraries(myModule);

          */
/*LibraryTable libraryTable2 = ProjectLibraryTable.getInstance(myModule.getProject());
        LibraryTable.ModifiableModel modifiableModel = libraryTable2.getModifiableModel();

        for (Library library : allLibraries) {
          if (HaxelibParser.parseHaxelib(library.getName()) != null) {
            ModuleHelper.removeDependency(ModuleRootManager.getInstance(myModule), library);
            modifiableModel.removeLibrary(library);
          }
        }*/  /*

        //modifiableModel.commit();

        LibraryTable libraryTable = ProjectLibraryTable.getInstance(module.getProject());

        LibraryTable.ModifiableModel libraryTableModifiableModel = libraryTable.getModifiableModel();

        //String name = "3,3,5";
        //"/usr/lib/haxe/lib/flixel/3,3,5"

        Library[] libraryTableModifiableModelLibraries = libraryTableModifiableModel.getLibraries();
        for (Library library : libraryTableModifiableModelLibraries) {
          if (HaxelibParser.parseHaxelib(library.getName()) != null) {
            ModuleHelper.removeDependency(ModuleRootManager.getInstance(module), library);
            libraryTableModifiableModel.removeLibrary(library);
          }
        }

        //libraryTableModifiableModel.commit();

        for (int i = 0; i < names.size(); i++) {
          String haxelib = HaxelibParser.stringifyHaxelib(new HaxelibItem(names.get(i)));

          Library libraryByName = libraryTableModifiableModel.getLibraryByName(haxelib);
          if (libraryByName == null) {
            libraryByName = libraryTableModifiableModel.createLibrary(haxelib);
            Library[] libraries = libraryTableModifiableModel.getLibraries();
            Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
            libraryModifiableModel.addRoot(classpathUrls.get(i), OrderRootType.CLASSES);
            libraryModifiableModel.addRoot(classpathUrls.get(i), OrderRootType.SOURCES);
            libraryModifiableModel.commit();

            ModuleRootModificationUtil.addDependency(module, libraryByName);
          }
        }

        libraryTableModifiableModel.commit();

        //module = null;
        //names.clear();
        //classpathUrls.clear();
      }
    });
  }
  */

  public static List<HaxelibNewItem> findHaxelibPath(Module myModule, String name) {
    List<HaxelibNewItem> haxelibNewItems = new ArrayList<HaxelibNewItem>();

    if (getInstalledLibraries().contains(name)) {
      List<String> haxelibPathUrls = getHaxelibPathUrl(myModule, name);

      for (String url : haxelibPathUrls) {
        haxelibNewItems.add(new HaxelibNewItem(myModule, VfsUtilCore.urlToPath(url), url));
      }
    }

    return haxelibNewItems;
  }

  public static void addLibraries(final List<HaxelibNewItem> haxelibNewItems) {
    if (haxelibNewItems.isEmpty()) {
      return;
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        //List<Library> allLibraries = HaxelibManager.getAllLibraries(myModule);


/*LibraryTable libraryTable2 = ProjectLibraryTable.getInstance(myModule.getProject());
        LibraryTable.ModifiableModel modifiableModel = libraryTable2.getModifiableModel();

        for (Library library : allLibraries) {
          if (HaxelibParser.parseHaxelib(library.getName()) != null) {
            ModuleHelper.removeDependency(ModuleRootManager.getInstance(myModule), library);
            modifiableModel.removeLibrary(library);
          }
        }*/

        //modifiableModel.commit();

        Module module = haxelibNewItems.get(0).module;
        LibraryTable libraryTable = ProjectLibraryTable.getInstance(module.getProject());

        LibraryTable.ModifiableModel libraryTableModifiableModel = libraryTable.getModifiableModel();

        //String name = "3,3,5";
        //"/usr/lib/haxe/lib/flixel/3,3,5"

        List<String> strings = new ArrayList<String>();

        for (HaxelibNewItem haxelibNewItem : haxelibNewItems) {
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

        //libraryTableModifiableModel.commit();

        for (int i = 0; i < haxelibNewItems.size(); i++) {
          Library libraryByName = libraryTableModifiableModel.getLibraryByName(strings.get(i));
          if (libraryByName == null) {
            libraryByName = libraryTableModifiableModel.createLibrary(strings.get(i));
            Library[] libraries = libraryTableModifiableModel.getLibraries();
            Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
            libraryModifiableModel.addRoot(haxelibNewItems.get(i).classpath, OrderRootType.CLASSES);
            libraryModifiableModel.addRoot(haxelibNewItems.get(i).classpath, OrderRootType.SOURCES);
            libraryModifiableModel.commit();

            ModuleRootModificationUtil.addDependency(module, libraryByName);
          }
        }

        libraryTableModifiableModel.commit();

        //module = null;
        //names.clear();
        //classpathUrls.clear();
      }
    });
  }

  @Override
  public void projectOpened() {
    findUsedLibrariesAndAddToProject();
  }

  public void findUsedLibrariesAndAddToProject() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();

    for (final Project project : projects) {
      for (final Module module : ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance())) {
        List<HaxelibNewItem> haxelibNewItems = new ArrayList<HaxelibNewItem>();

        HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
        int buildConfig = settings.getBuildConfig();
        List<HaxelibNewItem> haxelibNewItemList;

        switch (buildConfig) {
          case HaxeModuleSettings.USE_NMML:
            //if (!HaxelibManager.isNMELibraryAdded(allLibraries)) {
            //HaxelibManager.addLibraryWithClasspath(module, "nme");
            haxelibNewItemList = findHaxelibPath(module, "nme");
            haxelibNewItems.addAll(haxelibNewItemList);
            //}

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
            //if (!HaxelibManager.isOpenFLLibraryAdded(allLibraries)) {
            //HaxelibManager.addLibraryWithClasspath(module, "openfl");
            haxelibNewItemList = findHaxelibPath(module, "openfl");
            haxelibNewItems.addAll(haxelibNewItemList);
            //}

            String openFLXmlPath = settings.getOpenFLXmlPath();
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
                  //HaxelibManager.addLibraryWithClasspath(module, classpath, file.getUrl());
                  haxelibNewItems.add(new HaxelibNewItem(module, classpath, file.getUrl()));
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
                    //HaxelibManager.addLibraryWithClasspath(module, classpath, VfsUtil.pathToUrl(classpath));
                    haxelibNewItems.add(new HaxelibNewItem(module, classpath, VfsUtil.pathToUrl(classpath)));
                  }

                  Collection<HXMLLib> hxmlLibs = PsiTreeUtil.findChildrenOfType(psiFile, HXMLLib.class);
                  for (HXMLLib hxmlLib : hxmlLibs) {
                    String name = hxmlLib.getValue();
                    //HaxelibManager.addLibraryWithClasspath(module, name);
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
                //HaxelibManager.addLibraryWithClasspath(module, classpath, VfsUtil.pathToUrl(classpath));
                haxelibNewItems.add(new HaxelibNewItem(module, classpath, VfsUtil.pathToUrl(classpath)));
              }
            }

            break;
        }

        //HaxelibManager.commit();
        addLibraries(haxelibNewItems);
      }
    }
  }

  public List<HaxelibNewItem> getHaxelibsFromXmlProjectFile(Module module, XmlFile psiFile) {
    List<HaxelibNewItem> haxelibNewItems = new ArrayList<HaxelibNewItem>();

    XmlFile xmlFile = (XmlFile)psiFile;
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] haxelibTags = rootTag.findSubTags("haxelib");
        for (XmlTag haxelibTag : haxelibTags) {
          String name = haxelibTag.getAttributeValue("name");
          if (name != null) {
            //HaxelibManager.addLibraryWithClasspath(module, name);
            List<HaxelibNewItem> haxelibNewItemList = findHaxelibPath(module, name);
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