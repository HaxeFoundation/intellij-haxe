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

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.hxml.HXMLFileType;
import com.intellij.plugins.haxe.hxml.psi.HXMLClasspath;
import com.intellij.plugins.haxe.hxml.psi.HXMLLib;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.nmml.NMMLFileType;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.webcore.ModuleHelper;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages Haxe library class paths across projects.
 *
 * This class is intended to keep the class paths up to date as the projects
 * and module settings change.  It encapsulates reading classpaths from the
 * various types of Haxe project definitions (OpenFL, NME, etc.) and adding
 * them to module settings so that the paths available at runtime are also
 * available when writing the code.
 *
 *
 * Implementation Note: We might need to track each module separately, in a
 * list attached to the project.  We'd need that in case we can update the
 * project fast enough that all of the modules haven't been opened yet.
 * However, this is unlikely, since the first project update run takes place
 * after the project has finished initializing, and the open notifications
 * come during initialization.
 *
 */
public class HaxelibProjectUpdater  {

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxelibManager");
  {
    LOG.setLevel(Level.DEBUG);
  }

  public static final HaxelibProjectUpdater INSTANCE = new HaxelibProjectUpdater();
  public static final List<HaxelibItem> EMPTY_CLASSPATH_LIST = new ArrayList<HaxelibItem>(0);

  private ProjectUpdateQueue myQueue = null;
  private ProjectMap myProjects = null;

  HaxelibProjectUpdater() {
    myQueue = new ProjectUpdateQueue();
    myProjects = new ProjectMap();
  }

  @NotNull
  static HaxelibProjectUpdater getInstance() {
    return INSTANCE;
  }

  /**
   * Adds a new project to the manager.  This is normally called in response to a
   * ModuleComponent.openProject() notification.  Multiple modules referencing
   * the same project cause a counter to be incremented.
   *
   * @param project
   */
  public void openProject(@NotNull Project project) {
    ProjectTracker tracker = myProjects.add(project);
    tracker.setDirty(true);
    myQueue.add(tracker);
                                                             }

  /**
   * Close and possibly remove a project, if the reference count has been exhausted.
   *
   * @param project to close
   * @return whether the close has been delayed because an update is in process.
   */
  public boolean closeProject(Project project) {
    boolean delayed = false;
    boolean removed = false;

    ProjectTracker tracker = myProjects.get(project);
    removed = myProjects.remove(project);
    if (removed) {
      myQueue.remove(tracker);
      if (tracker.equals(myQueue.getUpdatingProject())) {
        delayed = true;
      }
    }
    return delayed;
  }

  /**
   * Retrieve the SdkManager for a given module/project.
   *
   * Convenience function that doesn't quite match the purpose of this class,
   * but we haven't made the SdkManager a singleton -- and we really can't
   * unless we move the notion of a project into it.
   *
   * @param module that we need the SdkManager for.
   * @return the appropriate SdkManager.
   */
  @Nullable
  public HaxelibSdkManager getSdkManager(@NotNull Module module) {
    return getSdkManager(module.getProject());
  }

  /**
   * Retrieve the SdkManager for a given module/project.
   *
   * Convenience function that doesn't quite match the purpose of this class,
   * but we haven't made the SdkManager a singleton -- and we really can't
   * unless we move the notion of a project into it.
   *
   * @param project that we need the SdkManager for.
   * @return the appropriate SdkManager.
   */
  @Nullable
  public HaxelibSdkManager getSdkManager(@NotNull Project project) {
    ProjectTracker tracker = myProjects.get(project);
    return null == tracker ? null : tracker.getSdkManager();
  }


  /**
   * Synchronize library dependencies with haxelib's dependencies for the given
   * module.
   *
   * @param module
   * @param haxelibNewItems the list of classpaths that should exist when finished.
   */
  private static void addLibraries(final Module module, final List<HaxelibItem> haxelibNewItems) {
    if (haxelibNewItems.isEmpty()) {
      LOG.debug("Skipping addLibraries");
      return;
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        HaxeDebugTimeLog timeLog = new HaxeDebugTimeLog("HaxelibProjectUpdater.addLibraries");
        timeLog.stamp("Processing module " + module.getName());

        // Load up the project classpath so that we can remove those entries from the module.
        // XXX: At the moment, this does NOT return any existing packed "haxelib|<lib_name>" classpath entries.
        // XXX: This could be optimized by returning a HashSet or a ConcurrentSkipListSet (for faster lookups).
        List<String> knownProjectClasspaths = HaxelibClasspathUtils.getAllProjectLibraryClasspathsAsUrls(module.getProject());

        // Keep the (unknown) new items that aren't already in the project.
        // No need to process those we already know about.
        // Note -- and this is subtle -- that there are no packed entries returned,
        // so /all/ haxelibs are considered new. This is necessary for the
        // removal process below.
        List<HaxelibItem> unknownNewItems = new ArrayList<HaxelibItem>();
        for (HaxelibItem haxelibNewItem : haxelibNewItems) {
          if (!knownProjectClasspaths.contains(haxelibNewItem.classpathUrl)) {
            unknownNewItems.add(haxelibNewItem);
          }
        }

        // Pack the new (unknown) items into "haxelib|<lib_path>" entries -- NOT URLs.
        List<String> newPackedEntries = new ArrayList<String>();
        for (HaxelibItem haxelibNewItem : unknownNewItems) {
          String haxelib = HaxelibParser.stringifyHaxelib(haxelibNewItem.name);
          newPackedEntries.add(haxelib);
        }

        // Re-load the libraries in a modifiable model (writable instance).
        // XXX: Is there a different set of libraries that we get from the modifiable
        //      model than through ProjectLibraryTable.getLibraries()?
        LibraryTable projectTable = ProjectLibraryTable.getInstance(module.getProject());
        LibraryTable.ModifiableModel projectModifiableModel = projectTable.getModifiableModel();
        Library[] libraryTableModifiableModelLibraries = projectModifiableModel.getLibraries();

        // Remove unused packed "haxelib|<lib_name>" libraries from the module and project library.
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        for (Library library : libraryTableModifiableModelLibraries) {
          // parseHaxeLib returns null if the name isn't packed.
          if (HaxelibParser.parseHaxelib(library.getName()) != null && !newPackedEntries.contains(library.getName())) {
            ModuleHelper.removeDependency(rootManager, library);
            projectModifiableModel.removeLibrary(library);
            timeLog.stamp("Removed library " + library.getName());
          }
        }

        // Add new dependencies to modules.
        timeLog.stamp("Locating libraries and adding dependencies.");
        int addedCount = 0;
        for (int i = 0; i < unknownNewItems.size(); i++) {
          Library libraryByName = projectModifiableModel.getLibraryByName(newPackedEntries.get(i));
          if (libraryByName == null) {
            libraryByName = projectModifiableModel.createLibrary(newPackedEntries.get(i));
            Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
            libraryModifiableModel.addRoot(unknownNewItems.get(i).classpathUrl, OrderRootType.CLASSES);
            libraryModifiableModel.addRoot(unknownNewItems.get(i).classpathUrl, OrderRootType.SOURCES);
            libraryModifiableModel.commit();

            ModuleRootModificationUtil.addDependency(module, libraryByName);
            ++addedCount;
            timeLog.stamp("Added library " + libraryByName.getName());
          }
        }

        timeLog.stamp("Libraries added = " + addedCount);

        projectModifiableModel.commit();

        timeLog.stamp("Commit complete");
        timeLog.print();
      }
    });
  }



  private void findUsedLibrariesAndAddToProject(ProjectTracker tracker) {
    HaxeDebugTimeLog timeLog = HaxeDebugTimeLog.startNew("findUsedLibrariesAndAddToProject");

    Project project = tracker.getProject();

    //LOG.debug("Scanning project " + project.getName());
    timeLog.stamp("Scanning project " + project.getName());

    List<HaxelibItem> haxelibNewItems = new ArrayList<HaxelibItem>();
    List<HaxelibItem> haxelibNewItemList;

    Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());
    int i = 0;
    int count = modules.size();
    for (final Module module : modules) {
      //LOG.debug("Scanning module " + (++i) + " of " + count + ": " + module.getName());
      timeLog.stamp("Scanning module " + (++i) + " of " + count + ": " + module.getName());

      HaxelibLibraryManager libManager = tracker.getSdkManager().getLibraryManager(module);
      HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
      int buildConfig = settings.getBuildConfig();

      switch (buildConfig) {
        case HaxeModuleSettings.USE_NMML:
          timeLog.stamp("Start loading classpaths from NMML file.");
          haxelibNewItemList = libManager.findHaxelibPath("nme");
          haxelibNewItems.addAll(haxelibNewItemList);

          String nmmlPath = settings.getNmmlPath();
          if (nmmlPath != null && !nmmlPath.isEmpty()) {
            VirtualFile file = LocalFileFinder.findFile(nmmlPath);

            if (file != null && file.getFileType().equals(NMMLFileType.INSTANCE)) {
              VirtualFileManager.getInstance().syncRefresh();
              PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

              if (psiFile != null && psiFile instanceof XmlFile) {
                haxelibNewItems.addAll(HaxelibClasspathUtils.getHaxelibsFromXmlFile((XmlFile)psiFile, libManager));
              }
            }
          }
          timeLog.stamp("Finished loading classpaths from NMML file.");
          break;
        case HaxeModuleSettings.USE_OPENFL:
          timeLog.stamp("Start loading classpaths from openfl file.");
          haxelibNewItemList = libManager.findHaxelibPath("openfl");
          haxelibNewItems.addAll(haxelibNewItemList);

          String openFLXmlPath = settings.getOpenFLPath();
          if (openFLXmlPath != null && !openFLXmlPath.isEmpty()) {
            VirtualFile file = LocalFileFinder.findFile(openFLXmlPath);

            if (file != null && file.getFileType().equals(XmlFileType.INSTANCE)) {
              PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

              if (psiFile != null && psiFile instanceof XmlFile) {
                haxelibNewItems.addAll(HaxelibClasspathUtils.getHaxelibsFromXmlFile((XmlFile)psiFile, libManager));
              }
            }
          }
          else {
            File dir = BuildProperties.getProjectBaseDir(project);
            List<String> projectClasspaths = HaxelibClasspathUtils.getProjectDisplayInformation(project, dir, "openfl",
                                                                                                HaxelibClasspathUtils.lookupSdk(module));

            for (String classpath : projectClasspaths) {
              VirtualFile file = LocalFileFinder.findFile(classpath);
              if (file != null) {
                haxelibNewItems.add(new HaxelibItem(classpath, file.getUrl()));
              }
            }
          }
          timeLog.stamp("Finished loading classpaths from openfl file.");
          break;
        case HaxeModuleSettings.USE_HXML:
          timeLog.stamp("Start loading classpaths from HXML file.");
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
                  haxelibNewItemList = libManager.findHaxelibPath(name);
                  haxelibNewItems.addAll(haxelibNewItemList);
                }
              }
            }
          }
          timeLog.stamp("Finish loading classpaths from HXML file.");
          break;

        case HaxeModuleSettings.USE_PROPERTIES:
          timeLog.stamp("Start loading classpaths from properties.");
          String arguments = settings.getArguments();
          if (!arguments.isEmpty()) {
            List<String> classpaths = HaxelibClasspathUtils.getHXMLFileClasspaths(project, arguments);

            for (String classpath : classpaths) {
              haxelibNewItems.add(new HaxelibItem(classpath, VfsUtil.pathToUrl(classpath)));
            }
          }
          timeLog.stamp("Finish loading classpaths from properties.");
          break;
      }
      timeLog.stamp("Adding libraries to module.");
      addLibraries(module, haxelibNewItems);
      timeLog.stamp("Finished adding libraries to module.");
    }
    timeLog.stamp("Completed.");
    timeLog.print();
  }



  //
  ///**
  // * Retrieve the project classpath from the project configuration files.
  // *
  // * @param tracker for the project of interest.
  // * @param buildConfig which compiler to use, from the module settings screen.
  // * @return the classpath to use with the particular compiler, using the requested project.
  // */
  //@NotNull
  //private List<HaxelibItem> getProjectClasspath(ProjectTracker tracker, int buildConfig) {
  //
  //  ProjectClasspathCache cache = tracker.getCache();
  //  List<HaxelibItem> haxelibClasspathList;
  //
  //  if (cache.isClasspathSetFor(buildConfig)) {
  //    haxelibClasspathList = cache.getClasspathFor(buildConfig);
  //  } else {
  //    haxelibClasspathList = loadClasspathFor(tracker, buildConfig);
  //    cache.setClasspathFor(buildConfig, haxelibClasspathList);
  //  }
  //  return haxelibClasspathList;
  //}
  //
  ///**
  // * Reads the project classpath from the project configuration files.
  // * Use getProjectClasspath() to retrieve cached data.
  // *
  // * @param tracker for the project of interest.
  // * @param buildConfig which compiler to use, from the module settings screen.
  // * @return the classpath to use with the particular compiler, using the requested project.
  // */
  //@NotNull
  //public List<HaxelibItem> loadClasspathFor(@NotNull ProjectTracker tracker, int buildConfig) {
  //
  //  Project project = tracker.getProject();
  //  List<HaxelibItem> haxelibNewItems = new ArrayList<HaxelibItem>();
  //  List<HaxelibItem> haxelibNewItemList;
  //
  //  // TODO: Fix these settings.  They /will/ crash this function.
  //  HaxeModuleSettings settings = null;
  //
  //  switch (buildConfig) {
  //    case HaxeModuleSettings.USE_NMML:
  //      haxelibNewItemList = tracker.getLibraryManager().findHaxelibPath("nme");
  //      haxelibNewItems.addAll(haxelibNewItemList);
  //
  //      String nmmlPath = settings.getNmmlPath();
  //      if (nmmlPath != null && !nmmlPath.isEmpty()) {
  //        VirtualFile file = LocalFileFinder.findFile(nmmlPath);
  //
  //        if (file != null && file.getFileType().equals(NMMLFileType.INSTANCE)) {
  //          VirtualFileManager.getInstance().syncRefresh();
  //          PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
  //
  //          if (psiFile != null && psiFile instanceof XmlFile) {
  //            haxelibNewItems.addAll(HaxelibClasspathUtils.getHaxelibsFromXmlFile((XmlFile)psiFile, tracker.getLibraryManager()));
  //          }
  //        }
  //      }
  //      break;
  //    case HaxeModuleSettings.USE_OPENFL:
  //      haxelibNewItemList = tracker.getLibraryManager().findHaxelibPath("openfl");
  //      haxelibNewItems.addAll(haxelibNewItemList);
  //
  //      String openFLXmlPath = settings.getOpenFLPath();
  //      if (openFLXmlPath != null && !openFLXmlPath.isEmpty()) {
  //        VirtualFile file = LocalFileFinder.findFile(openFLXmlPath);
  //
  //        if (file != null && file.getFileType().equals(XmlFileType.INSTANCE)) {
  //          PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
  //
  //          if (psiFile != null && psiFile instanceof XmlFile) {
  //            haxelibNewItems.addAll(HaxelibClasspathUtils.getHaxelibsFromXmlFile((XmlFile)psiFile, tracker.getLibraryManager()));
  //          }
  //        }
  //      }
  //      else {
  //        File dir = BuildProperties.getProjectBaseDir(project);
  //        List<String> projectClasspaths = HaxelibClasspathUtils.getProjectDisplayInformation(project, dir, "openfl");
  //
  //        for (String classpath : projectClasspaths) {
  //          VirtualFile file = LocalFileFinder.findFile(classpath);
  //          if (file != null) {
  //            haxelibNewItems.add(new HaxelibItem(classpath, file.getUrl()));
  //          }
  //        }
  //      }
  //      break;
  //    case HaxeModuleSettings.USE_HXML:
  //      String hxmlPath = settings.getHxmlPath();
  //
  //      if (hxmlPath != null && !hxmlPath.isEmpty()) {
  //        VirtualFile file = LocalFileFinder.findFile(hxmlPath);
  //
  //        if (file != null && file.getFileType().equals(HXMLFileType.INSTANCE)) {
  //          PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
  //          if (psiFile != null) {
  //            Collection<HXMLClasspath> hxmlClasspaths = PsiTreeUtil.findChildrenOfType(psiFile, HXMLClasspath.class);
  //            for (HXMLClasspath hxmlClasspath : hxmlClasspaths) {
  //              String classpath = hxmlClasspath.getValue();
  //              haxelibNewItems.add(new HaxelibItem(classpath, VfsUtil.pathToUrl(classpath)));
  //            }
  //
  //            Collection<HXMLLib> hxmlLibs = PsiTreeUtil.findChildrenOfType(psiFile, HXMLLib.class);
  //            for (HXMLLib hxmlLib : hxmlLibs) {
  //              String name = hxmlLib.getValue();
  //              haxelibNewItemList = tracker.getLibraryManager().findHaxelibPath(name);
  //              haxelibNewItems.addAll(haxelibNewItemList);
  //            }
  //          }
  //        }
  //      }
  //
  //      break;
  //
  //    case HaxeModuleSettings.USE_PROPERTIES:
  //      String arguments = settings.getArguments();
  //      if (!arguments.isEmpty()) {
  //        List<String> classpaths = HaxelibClasspathUtils.getHXMLFileClasspaths(project, arguments);
  //
  //        for (String classpath : classpaths) {
  //          haxelibNewItems.add(new HaxelibItem(classpath, VfsUtil.pathToUrl(classpath)));
  //        }
  //      }
  //
  //      break;
  //  }
  //  return haxelibNewItems;
  //}
  //
  //



  final private class ProjectClasspathCache {


    private List<HaxelibItem> nmmlClassPath;
    private List<HaxelibItem> openFLClassPath;
    private List<HaxelibItem> hxmlClassPath;
    private List<HaxelibItem> propertiesClassPath;
    private boolean nmmlIsSet;
    private boolean openFLIsSet;
    private boolean hxmlIsSet;
    private boolean propertiesIsSet;

    public ProjectClasspathCache() {
      clear();
    }

    public void clear() {
      setNmmlClassPath(EMPTY_CLASSPATH_LIST);
      setOpenFLClassPath(EMPTY_CLASSPATH_LIST);
      setHxmlClassPath(EMPTY_CLASSPATH_LIST);
      setPropertiesClassPath(EMPTY_CLASSPATH_LIST);

      // Reset the 'set' bits.
      nmmlIsSet = openFLIsSet = hxmlIsSet = propertiesIsSet = false;
    }

    public boolean isClasspathSetFor(int buildConfig) {
      switch(buildConfig) {
        case HaxeModuleSettings.USE_NMML:
          return nmmlIsSet;
        case HaxeModuleSettings.USE_OPENFL:
          return openFLIsSet;
        case HaxeModuleSettings.USE_HXML:
          return hxmlIsSet;
        case HaxeModuleSettings.USE_PROPERTIES:
          return propertiesIsSet;
      }
      return false;
    }

    @NotNull
    public List<HaxelibItem> getClasspathFor(int buildConfig) {
      switch(buildConfig) {
        case HaxeModuleSettings.USE_NMML:
          return getNmmlClassPath();
        case HaxeModuleSettings.USE_OPENFL:
          return getOpenFLClassPath();
        case HaxeModuleSettings.USE_HXML:
          return getHxmlClassPath();
        case HaxeModuleSettings.USE_PROPERTIES:
          return getPropertiesClassPath();
      }
      return EMPTY_CLASSPATH_LIST;
    }

    public void setClasspathFor(int buildConfig, List<HaxelibItem> classpath) {
      switch(buildConfig) {
        case HaxeModuleSettings.USE_NMML:
          setNmmlClassPath(classpath);
        case HaxeModuleSettings.USE_OPENFL:
          setOpenFLClassPath(classpath);
        case HaxeModuleSettings.USE_HXML:
          setHxmlClassPath(classpath);
        case HaxeModuleSettings.USE_PROPERTIES:
          setPropertiesClassPath(classpath);
      }
    }


    @Nullable
    public List<HaxelibItem> getNmmlClassPath() {
      return nmmlClassPath;
    }

    @Nullable
    public List<HaxelibItem> getOpenFLClassPath() {
      return openFLClassPath;
    }

    @Nullable
    public List<HaxelibItem> getHxmlClassPath() {
      return hxmlClassPath;
    }

    @Nullable
    public List<HaxelibItem> getPropertiesClassPath() {
      return propertiesClassPath;
    }

    public void setNmmlClassPath(List<HaxelibItem> list)  {
      nmmlClassPath = list;
      nmmlIsSet = true;
    }

    public void setOpenFLClassPath(List<HaxelibItem> openFLClassPath) {
      this.openFLClassPath = openFLClassPath;
      openFLIsSet = true;
    }

    public void setHxmlClassPath(List<HaxelibItem> hxmlClassPath) {
      this.hxmlClassPath = hxmlClassPath;
      hxmlIsSet = true;
    }

    public void setPropertiesClassPath(List<HaxelibItem> propertiesClassPath) {
      this.propertiesClassPath = propertiesClassPath;
      propertiesIsSet = true;
    }
  }



  /**
   * Tracks the state of a project for updating class paths.
   */
  public final class ProjectTracker {
    final Project myProject;
    boolean myIsDirty;
    boolean myIsUpdating;
    ProjectClasspathCache myCache;
    HaxelibSdkManager mySdkManager;

    // TODO: Determine if we need to track whether the project is still open.

    /**
     * Typically, a project gets open and closed events for all of the modules it
     * contains.  We don't want to destroy or de-queue ProjectTrackers until all
     * of the modules have been destroyed.
     */
    int myReferenceCount;


    public ProjectTracker(Project project) {
      myProject = project;
      myIsDirty = true;
      myIsUpdating = false;
      myReferenceCount = 0;
      myCache = new ProjectClasspathCache();
      mySdkManager = new HaxelibSdkManager();
    }

    /**
     * Get the settings cache.
     */
    @NotNull
    public ProjectClasspathCache getCache() {
      return myCache;
    }

    /**
     * Get the library classpath cache.
     */
    @NotNull
    public HaxelibSdkManager getSdkManager() {
      return mySdkManager;
    }

    /**
     * Tell whether this project is dirty (needs updating).
     *
     * @return true if the project needs updating.
     */
    public boolean isDirty() {
      boolean ret = false;
      synchronized (this) {
        ret = myIsDirty;
      }
      return ret;
    }

    /**
     * Set/clear the dirty state.  Marking the project dirty clears the cache.
     *
     * @param newState the new state to set.
     * @return the state prior to setting it.
     */
    public boolean setDirty(boolean newState) {
      boolean ret = false;
      synchronized (this) {
        ret = myIsDirty;
        myIsDirty = newState;
        if (myIsDirty) {
          // XXX: May need something more sophisicated than just clearing it.
          //      It could be that a module is getting changed, but not
          //      the project.  In that case, we would want to detect whether
          //      the project settings really changed, and act accordingly.
          myCache.clear();
        }
      }
      return ret;
    }

    /**
     * Tell whether this project is currently updating.
     *
     * @return true if the project is currently running an update.
     */
    public boolean isUpdating() {
      boolean ret = false;

      synchronized(this) {
        ret = myIsUpdating;
      }

      return ret;
    }

    /**
     * Set/clear the 'updating' state.
     *
     * @param newState the new state to set.
     * @return the state prior to setting it.
     */
    public boolean setUpdating(boolean newState) {
      boolean ret = false;
      synchronized(this) {
        ret = myIsUpdating;
        myIsUpdating = newState;
      }
      return ret;
    }

    /**
     * Increase the reference count.
     */
    public void addReference() {
      synchronized(this) {
        myReferenceCount++;
      }
    }

    /**
     * Decrease the reference count.
     *
     * @return the number of references still attached to the object.
     */
    public int removeReference() {
      int refs;
      synchronized(this) {
        refs = --myReferenceCount;
      }
      // XXX: Maybe we don't need an assertion for this.
      LOG.assertTrue(refs >= 0);

      return refs;
    }

    /**
     * Get the project we are tracking.  Note that the project may
     * not still be open at the moment that this is retrieved.
     */
    @NotNull
    public Project getProject() {
      return myProject;
    }

    @NotNull
    public String toString() {
      return "ProjectTracker:" + myProject.getName();
    }
  } // end class ProjectTracker


  /**
   * Tracks all of the projects that are currently open.  (As opposed to those
   * that are bing queued for update, which the ProjectUpdateQueue does.)
   * ProjectTrackers are shared between this map and the queue.
   */
  public final class ProjectMap {

    // Hashtable is already synchronized, so we don't have to manage that ourselves.
    final Hashtable<String, ProjectTracker> myMap;


    public ProjectMap() {
      myMap = new Hashtable<String, ProjectTracker>();
    }

    /**
     * Adds a new project to the map, if it doesn't exist there already.
     *
     * @param project An open project to be tracked.
     *
     * @return a new ProjectTracker for the project added, or the existing
     *         ProjectTracker for an existing project.
     */
    @NotNull
    public ProjectTracker add(@NotNull Project project) {
      ProjectTracker tracker;

      synchronized (this) {
        tracker = myMap.get(project.getName());
        if (null == tracker) {
          tracker = new ProjectTracker(project);
          myMap.put(project.getName(), tracker);
        }

        tracker.addReference();
      }
      return tracker;
    }

    public boolean remove(@NotNull Project project) {
      boolean removed = false;
      synchronized(this) {
        ProjectTracker tracker = myMap.get(project.getName());
        if (null != tracker) {
          int refs = tracker.removeReference();
          if (refs == 0) {
            removed = null != myMap.remove(project.getName());
          }
        }
      }
      return removed;
    }

    @Nullable
    public ProjectTracker get(@NotNull Project project) {
      ProjectTracker tracker;
      synchronized(this) {
        tracker = myMap.get(project.getName());
      }
      return tracker;
    }
  }


  /**
   * A FIFO queue for projects that need updating.  Projects are tracked
   * through the ProjectTracker class. When a project placed is in this queue,
   * it is marked dirty.  When the project is being updated, it's marked
   * as updating.  The currently updating project can be retrieved with
   * getUpdatingProject().
   */
  final class ProjectUpdateQueue {

    final Object updateSyncToken;
    ConcurrentLinkedQueue<ProjectTracker> queue;
    ProjectTracker updatingProject = null;

    public ProjectUpdateQueue() {
      queue = new ConcurrentLinkedQueue<ProjectTracker>();
      updateSyncToken = new Object();
      updatingProject = null;
    }

    /**
     * Determine whether there are any projects awaiting updating.
     *
     * The queue may be empty even though a project is currently updating.
     *
     * @return whether there are any projects waiting to be updated.
     */
    public boolean isEmpty() {
      return queue.isEmpty();
    }

    /**
     * Adds a new project to the update queue.  If the project already
     * exists in the queue (as described by equals()) then it will not
     * be added.
     *
     * @param tracker for the project that needs to be updated.
     * @return true if the project was added to the update queue.
     */
    public boolean add(@NotNull ProjectTracker tracker) {
      boolean ret = false;
      if (!tracker.equals(getUpdatingProject())) {
        if (queue.isEmpty() || !queue.contains(tracker)) {
          ret = queue.add(tracker);
          if (null == getUpdatingProject()) {
            queueNextProject();
          }
        }
      }
      return ret;
    }

    /**
     * Remove a project from the update queue.
     *
     * Projects that are currently updating are not canceled or removed, but will
     * be as soon as they are finished.
     *
     * @param tracker project to remove
     * @return true if the project was removed; false otherwise, or if it wasn't queued.
     */
    public boolean remove(@NotNull ProjectTracker tracker) {
      boolean removed = false;

      synchronized(updateSyncToken) {
        if (queue.remove(tracker)) {
          tracker.setUpdating(false);
          // We haven't changed anything, so it's still dirty.
          removed = true;
        }
      }
      return removed;
    }

    /**
     * @return the project currently being updated, if any.
     */
    @Nullable
    public ProjectTracker getUpdatingProject() {
      ProjectTracker tracker;
      synchronized (updateSyncToken) {
        tracker = updatingProject;
      }
      return tracker;
    }

    /**
     * Puts the next project on the event queue.
     */
    private void queueNextProject() {
      synchronized (updateSyncToken) {
        LOG.assertTrue(null == updatingProject);

        // Get the next project from the queue. We're done if there's
        // nothing left.
        updatingProject = queue.poll();  // null if empty.
        if (updatingProject == null) return;

        LOG.assertTrue(updatingProject.isDirty());
        LOG.assertTrue(!updatingProject.isUpdating());

        updatingProject.setUpdating(true);
      }

      // TODO: Put project classpath updating on a worker thread (including progress indicators)...
      // Waiting for runWhenProjectIsInitialized() ensures that the project is
      // fully loaded and accessible.  Otherwise, we crash. ;)
      StartupManager.getInstance(updatingProject.getProject()).runWhenProjectIsInitialized(new Runnable() {
        public void run() {
          LOG.debug("Loading referenced libraries...");
          findUsedLibrariesAndAddToProject(updatingProject);
          finishUpdate(updatingProject);
        }
      });

    }

    /**
     * Cleanup and queue the next in line, if any.
     *
     * @param up - the project that is finishing its update run.
     */
    private void finishUpdate(ProjectTracker up) {
      synchronized (updateSyncToken) {
        LOG.assertTrue(null != updatingProject);
        LOG.assertTrue(up.equals(updatingProject));

        updatingProject.setUpdating(false);
        updatingProject.setDirty(false);
        updatingProject = null;
      }
      queueNextProject();
    }
  } // end class projectUpdateQueue


}