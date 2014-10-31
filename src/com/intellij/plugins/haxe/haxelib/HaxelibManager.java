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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.util.HaxeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.LocalFileFinder;

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
        addLibraryWithClasspath(myModule, name, haxelibPathUrl);
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

  public static List<String> getProcessStdout(ArrayList<String> commandLineArguments) {
    List<String> strings = new ArrayList<String>();

    try {
      Process process = new ProcessBuilder(commandLineArguments).start();
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
            break;
          case HaxeModuleSettings.USE_OPENFL:
            if (!HaxelibManager.isOpenFLLibraryAdded(allLibraries)) {
              HaxelibManager.addLibraryWithClasspath(module, "openfl");
            }
            break;
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