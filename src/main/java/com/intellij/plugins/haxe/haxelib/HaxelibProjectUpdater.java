/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Eric Bishton
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

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLFileType;
import com.intellij.plugins.haxe.buildsystem.hxml.model.HXMLProjectModel;
import com.intellij.plugins.haxe.buildsystem.nmml.NMMLFileType;
import com.intellij.plugins.haxe.config.HaxeConfiguration;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.Lambda;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;


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

@CustomLog
public class HaxelibProjectUpdater {

  static {      // Take this out when finished debugging.
    log.setLevel(LogLevel.DEBUG);
  }

  /**
   * Set this to run in the foreground for speed testing.
   * It overrides myRunInForeground.  The UI is blocked with no updates.
   */
  private static final boolean myTestInForeground = false;

  public static final HaxelibProjectUpdater INSTANCE = new HaxelibProjectUpdater();

  private ProjectUpdateQueue myQueue = null;
  private ProjectMap myProjects = null;

  private HaxelibProjectUpdater() {
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
   * @param project that is being opened.
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

  public  ProjectTracker findProjectTracker(Project project) {
    return myProjects.get(project);
  }

  /**
   * Retrieve the HaxelibLibraryCacheManager for a given module/project.
   * <p>
   * Convenience function that doesn't quite match the purpose of this class,
   * but we haven't made the CacheManager a singleton -- and we really can't
   * unless we move the notion of a project into it.
   *
   * @param module that we need the HaxelibLibraryCacheManager for.
   * @return the appropriate HaxelibLibraryCacheManager.
   */
  @Nullable
  public HaxelibLibraryCacheManager getLibraryCacheManager(@NotNull Module module) {
    return getLibraryCacheManager(module.getProject());
  }

  /**
   * Retrieve the HaxelibLibraryCacheManager for a given module/project.
   * <p>
   * Convenience function that doesn't quite match the purpose of this class,
   * but we haven't made the CacheManager a singleton -- and we really can't
   * unless we move the notion of a project into it.
   *
   * @param project that we need the HaxelibLibraryCacheManager for.
   * @return the appropriate HaxelibLibraryCacheManager.
   */
  @Nullable
  public HaxelibLibraryCacheManager getLibraryCacheManager(@NotNull Project project) {
    ProjectTracker tracker = myProjects.get(project);
    return null == tracker ? null : tracker.getSdkManager();
  }

  /**
   * Retrieve the library cache for a given module.
   * <p>
   * Convenience function that doesn't quite match the purpose of this class,
   * but we haven't made the CacheManager a singleton -- and we really can't
   * unless we move the notion of a project into it.
   *
   * @param module
   * @return the HaxelibLibraryCache for the module, if found; null, otherwise.
   */
  @Nullable
  public static HaxelibLibraryCache getLibraryCache(@NotNull Module module) {
    HaxelibLibraryCacheManager mgr = HaxelibProjectUpdater.getInstance().getLibraryCacheManager(module);
    return mgr != null ? mgr.getLibraryManager(module) : null;
  }

  /**
   * Retrieve the library cache for a given module.
   * <p>
   * Convenience function that doesn't quite match the purpose of this class,
   * but we haven't made the CacheManager a singleton -- and we really can't
   * unless we move the notion of a project into it.
   *
   * @param project
   * @return the HaxelibLibraryCache for the project, if found; null, otherwise.
   */
  @Nullable
  public static HaxelibLibraryCache getLibraryCache(@NotNull Project project) {
    HaxelibLibraryCacheManager mgr = HaxelibProjectUpdater.getInstance().getLibraryCacheManager(project);
    Sdk projectSdk = HaxelibSdkUtils.lookupSdk(project);
    return (mgr != null && projectSdk != null) ? mgr.getLibraryCache(projectSdk) : null;
  }

  /**
   * Resolve the classpath/library entries for a module.  Determines which
   * libraries to add and remove from the module.  Only libraries that have
   * previously been added may be removed, if they have become redundant
   * or otherwise specified.
   *
   * @param tracker      for the project being updated.
   * @param module       being updated.
   * @param externalLibs potential new libraries that must be available
   *                     to the module when this routine finishes.  These are
   *                     typically specified in the Haxe project files. (e.g. -lib)
   */
  private void resolveModuleLibraries(ProjectTracker tracker, Module module, HaxeLibraryList externalLibs) {
    HaxeLibraryList toAdd;
    HaxeLibraryList toRemove;

    toAdd = new HaxeLibraryList(module);
    toRemove = new HaxeLibraryList(module);
    Sdk moduleSdk = ModuleRootManager.getInstance(module).getSdk();
    if (null == moduleSdk) {
      log.debug("No SDK for module " + module.getName() + ".  Not syncing haxelibs.");
      return; // Nothing to do if there is no SDK.
    }
    syncLibraryLists(moduleSdk,
                     HaxelibUtil.getModuleLibraries(module),
                     externalLibs,
        /*modifies*/ toAdd,
        /*modifies*/ toRemove);

    updateModule(tracker, module, toRemove, toAdd);
  }

  /**
   * Find an IDEA library matching a HaxeLibraryReference.
   *
   * @param iter the table iterator.
   * @param ref the library to look for.
   * @return the Library, if found; null, otherwise.
   */
  @Nullable
  private Library lookupLibrary(@NotNull Iterator<Library> iter, @NotNull HaxeLibraryReference ref) {
    Library found = null;
    while (null == found && iter.hasNext()) {
      Library toTest = iter.next();
      if (ref.matchesIdeaLib(toTest)) {
        found = toTest;
      }
    }
    return found;
  }

  /**
   * Find an IDEA library in a project's LibraryTable matching a HaxeLibraryReference.
   *
   * @param table - the LibraryTable to look in.
   * @param ref - the library to find.
   * @return the Library, if found; null, otherwise.
   */
  @Nullable
  private Library lookupProjectLibrary(@NotNull LibraryTable table, @NotNull HaxeLibraryReference ref) {
    return lookupLibrary(table.getLibraryIterator(), ref);
  }

  /**
   * Find an IDEA library in a module's LibraryTable (actually, its ModifiableModel)
   * matching a HaxeLibraryReference.
   *
   * @param table - the LibraryTable to look in.
   * @param ref - the library to find.
   * @return the Library, if found; null, otherwise.
   */
  @Nullable
  private Library lookupModelLibrary(@NotNull LibraryTable.ModifiableModel table, @NotNull HaxeLibraryReference ref) {
    return lookupLibrary(table.getLibraryIterator(), ref);
  }

  /**
   * Remove libraries from a library table.
   *
   * @param toRemove - The list of libraries to remove.
   * @param libraryTableModel - The (modifiable model of the) table to remove them from.
   * @param timeLog - Debugging time log.
   */
  private void removeLibraries(@NotNull final HaxeLibraryList toRemove,
                               @NotNull final LibraryTable.ModifiableModel libraryTableModel,
                               @NotNull final HaxeDebugTimeLog timeLog) {
    timeLog.stamp("Removing libraries.");
    toRemove.iterate(new HaxeLibraryList.Lambda() {
      @Override
      public boolean processEntry(HaxeLibraryReference entry) {
        Library library = lookupModelLibrary(libraryTableModel, entry);
        if (null != library) {
          // Why use this?: ModuleHelper.removeDependency(rootManager, library);
          libraryTableModel.removeLibrary(library);
          timeLog.stamp("Removed library " + library.getName());
        }
        else {
          log.warn(
            "Internal inconsistency: library to remove was not found: " +
            entry.getName());
        }
        return true;
      }
    });
  }

  /**
   * Add libraries to a library table.
   *
   * @param toAdd - List of libraries to add.
   * @param projectLibraries - libraries that can be referenced instead of created.
   * @param projectTable - the library table that projectLibraries belong to.
   * @param moduleModel - the module we are adding libraries to
   * @param libraryTableModel - the library table for that module
   * @param timeLog - Debugging info and timer.
   */
  private void addLibraries(@NotNull final HaxeLibraryList toAdd,
                            @NotNull final HaxeLibraryList projectLibraries,
                            @NotNull final LibraryTable projectTable,
                            @NotNull final ModifiableRootModel moduleModel,
                            @NotNull final LibraryTable.ModifiableModel libraryTableModel,
                            @NotNull final HaxeDebugTimeLog timeLog) {
    timeLog.stamp("Locating libraries and adding dependencies.");
    toAdd.iterate(new HaxeLibraryList.Lambda() {
      @Override
      public boolean processEntry(HaxeLibraryReference entry) {
        Library moduleLibrary = lookupModelLibrary(libraryTableModel, entry);

        // If the lib is in the project list, then we just add a reference to it.
        if (projectLibraries.contains(entry)) {
          if (null != moduleLibrary) {
            log.warn("Internal inconsistency: attempting to add library that is already in the module.");
          } else {
            Library projectLibrary = lookupProjectLibrary(projectTable, entry);
            if (null == projectLibrary) {
              // EMB: Not writing recovery code because this really shouldn't happen.
              log.warn("Internal inconsistency: Could not find project library when it should exist.");
            } else {
              LibraryOrderEntry libraryOrderEntry = moduleModel.addLibraryEntry(projectLibrary);
              libraryOrderEntry.setExported(false);
              libraryOrderEntry.setScope(DependencyScope.PROVIDED);
              timeLog.stamp("Added module-level reference to project lib " + projectLibrary.getName());
            }
          }
        }
        else {  // Not in the project, so add it to the module.

          if (moduleLibrary == null) {
            Library.ModifiableModel libraryModelToDispose = null;
            try {
              final Library newLibrary = libraryTableModel.createLibrary(entry.getPresentableName());
              moduleLibrary = newLibrary;

              final Library.ModifiableModel libraryModifiableModel = newLibrary.getModifiableModel();
              libraryModelToDispose = libraryModifiableModel;

              HaxeLibrary entryLibrary = entry.getLibrary();
              HaxeClasspath classpath = entryLibrary != null ? entryLibrary.getClasspathEntries() : null;
              if (null != classpath) {
                classpath.iterate(new HaxeClasspath.Lambda() {
                  @Override
                  public boolean processEntry(HaxeClasspathEntry cp) {
                    String url = HaxeFileUtil.fixUrl(cp.getUrl());
                    VirtualFile directory = VirtualFileManager.getInstance().findFileByUrl(url);
                    if (null == directory) {
                      timeLog.stamp("Skipping classpath for " + newLibrary.getName() + ", no directory entry for " + url);
                    }
                    else {
                      libraryModifiableModel.addRoot(directory, OrderRootType.CLASSES);
                      libraryModifiableModel.addRoot(directory, OrderRootType.SOURCES);
                    }
                    return true;
                  }
                });
              }

              LibraryOrderEntry libraryOrderEntry = moduleModel.findLibraryOrderEntry(newLibrary);
              libraryOrderEntry.setExported(false);
              libraryOrderEntry.setScope(DependencyScope.PROVIDED);

              libraryModifiableModel.commit();
              libraryModelToDispose = null; // So we don't dispose of it now that we've committed.
              timeLog.stamp("Added library " + newLibrary.getName());
            }
            finally {
              if (null != libraryModelToDispose) {
                timeLog.stamp("Failure to create library " + moduleLibrary.getName());
                Disposer.dispose(libraryModelToDispose);
              }
            }
          }
          else {
            log.warn("Internal inconsistency: library to add was already in the module's library table.");
          }
        }
        return true;
      }
    });
  }

  /**
   * Ensure that all entries in the given list are managed.
   *
   * @param list - List of entries to check.
   * @param msg - Message to display on failure.
   */
  private void assertEntriesAreManaged(HaxeLibraryList list, final String msg) {
    if (null != list) {
      list.iterate(new HaxeLibraryList.Lambda() {
        @Override
        public boolean processEntry(HaxeLibraryReference entry) {
          log.assertTrue(entry.isManaged(), msg);
          return true;
        }
      });
    }
  }

  /**
   * Workhorse routine for resolveModuleLibraries.  This does the actual
   * update of the module.  It will block until all of the running events
   * on the AWT thread have completed, and then this will run on that thread.
   *
   * @param module   to update.
   * @param toRemove libraries that need to be removed from the module.
   * @param toAdd    libraries that need to be added to the module.
   */
  private void updateModule(final ProjectTracker tracker,
                            final Module module,
                            final HaxeLibraryList toRemove,
                            final HaxeLibraryList toAdd) {
    if ((null == toRemove || toRemove.isEmpty()) && (null == toAdd || toAdd.isEmpty())) {
      return;
    }

    // Some internal error checking.
    assertEntriesAreManaged(toRemove, "Attempting to automatically remove a library that was not marked as managed.");
    assertEntriesAreManaged(toAdd, "Attempting to automatically add a library that is not marked as managed.");

    final HaxeDebugTimeLog timeLog = new HaxeDebugTimeLog("Write action:");
    timeLog.stamp("Queueing write action...");

    doWriteAction(() -> {
      timeLog.stamp("<-- Time elapsed waiting for write access on the AWT thread.");
      timeLog.stamp("Begin: Updating module libraries for " + module.getName());

      // Figure out the list of project libraries that we should reference, if we can.
      HaxeLibraryList projectLibraries = ModuleRootManager.getInstance(module).isSdkInherited()
                                       ? getProjectLibraryList(tracker)
                                       : new HaxeLibraryList(module);

      final LibraryTable projectTable = LibraryTablesRegistrar.getInstance().getLibraryTable(tracker.getProject());

      timeLog.stamp("<-- Time elapsed retrieving project libraries.");

      ModifiableRootModel moduleRootModel = null;
      LibraryTable.ModifiableModel libraryTableModel = null;
      try {
        moduleRootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        libraryTableModel = moduleRootModel.getModuleLibraryTable().getModifiableModel();

        // Remove unused packed "haxelib|<lib_name>" libraries from the module and project library.
        if (null != toRemove) {
          removeLibraries(toRemove, libraryTableModel, timeLog);
        }

        // Add new dependencies to modules.
        if (null != toAdd) {
          addLibraries(toAdd, projectLibraries, projectTable, moduleRootModel, libraryTableModel, timeLog);
        }

        timeLog.stamp("Committing changes to module libraries");
        libraryTableModel.commit();
        libraryTableModel = null;
        moduleRootModel.commit();
        moduleRootModel = null;
      }
      finally {
        if (null != moduleRootModel || null != libraryTableModel)
          timeLog.stamp("Failure to update module libraries");
        if (null != libraryTableModel)
          libraryTableModel.dispose();
        if (null != moduleRootModel)
          moduleRootModel.dispose();
      }
      timeLog.stamp("Finished: Updating module libraries");
    });

    timeLog.print();
  }

  /**
   * The guts of syncModuleClasspaths, separated so that it can be run as
   * either a foreground or background task.
   *
   * @param tracker for the project being updated.
   * @param module  being updated.
   * @param timeLog where to log timing results
   */
  private void syncOneModule(@NotNull final ProjectTracker tracker, @NotNull Module module, @NotNull HaxeDebugTimeLog timeLog) {

    Project project = tracker.getProject();
    HaxeLibraryList haxelibExternalItems = new HaxeLibraryList(module);
    HaxelibLibraryCache libManager = tracker.getSdkManager().getLibraryManager(module);
    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    //ApplicationManager.getApplication().runReadAction(() -> {
      // If the module says not to keep libs synched, then don't.
      if (!settings.isKeepSynchronizedWithProjectFile()) {
        timeLog.stamp("Module " + module.getName() + " is set to not synchronize dependencies.");
        return;
      }

      switch (settings.getBuildConfiguration()) {
        case NMML:
          syncNMMLModule(module, timeLog, project, haxelibExternalItems, libManager, settings);
          break;

        case OPENFL:
          syncOpenFLModule(module, timeLog, project, haxelibExternalItems, libManager, settings);
          break;

        case HXML:
          syncHxml(module, timeLog, project, haxelibExternalItems,libManager, settings);

          break;

        case CUSTOM:
          timeLog.stamp("Start loading haxelibs from properties.");

          // TODO: Grab the command line?? Run it through the algorithm for USE_HXML.

          timeLog.stamp("Finish loading haxelibs from properties.");
          break;

        default:
          throw new HaxeDebugUtil.InvalidCaseException(settings.getBuildConfiguration());
      }

      // We can't just remove all of the project classpaths from the module's
      // library list here because we need to remove any managed classpaths that
      // are no longer valid in the modules.  We can't do that if we don't have
      // the list of valid ones.  :/
    //});
    timeLog.stamp("Adding libraries to module.");
    resolveModuleLibraries(tracker, module, haxelibExternalItems);
    timeLog.stamp("Finished adding libraries to module.");
  }

  private static void syncHxml(@NotNull Module module,
                               @NotNull HaxeDebugTimeLog timeLog,
                               Project project,
                               HaxeLibraryList haxelibExternalItems,
                               HaxelibLibraryCache libManager, HaxeModuleSettings settings) {
    timeLog.stamp("Start loading haxelibs from HXML file.");
    String hxmlPath = settings.getHxmlPath();

    // TODO: Walk the command line looking for libs, too.

    if (hxmlPath != null && !hxmlPath.isEmpty()) {
      doWriteAction(()->VirtualFileManager.getInstance().syncRefresh());
      //VirtualFileManager.getInstance().syncRefresh();
      VirtualFile file = LocalFileFinder.findFile(hxmlPath);

      if (file != null && file.getFileType().equals(HXMLFileType.INSTANCE)) {
        List<String> libList = ApplicationManager.getApplication().runReadAction(new Computable<>() {
          @Override
          public List<String> compute() {
            VirtualFile file = LocalFileFinder.findFile(hxmlPath);
            if (file != null && file.getFileType().equals(HXMLFileType.INSTANCE)) {
              PsiFile hxmlFile = PsiManager.getInstance(project).findFile(file);
              if (hxmlFile != null) {
                HXMLProjectModel hxml = new HXMLProjectModel(hxmlFile);
                return hxml.getLibraries();
              }
            }
            return List.of();
          }
        });
          List<HaxelibUtil.HaxeLibData> list = libList.stream().map(HaxelibProjectUpdater::toLibData).toList();
          haxelibExternalItems.addAll(HaxelibUtil.createHaxelibsFromHaxeLibData(module, list, libManager));
      }
    }
    timeLog.stamp("Finish loading haxelibs from HXML file.");

    // TODO: Add classpaths from the HXML file and CL to module sources
  }

  private static HaxelibUtil.HaxeLibData toLibData(String libString) {
    String[] split = libString.split(":");
    if (split.length == 1) {
      return new HaxelibUtil.HaxeLibData(split[0], null, HaxelibSemVer.ANY_VERSION);
    }
    else {
      return new HaxelibUtil.HaxeLibData(split[0], split[1], HaxelibSemVer.create(split[1]));
    }
  }

  private static void syncOpenFLModule(@NotNull Module module,
                                @NotNull HaxeDebugTimeLog timeLog,
                                Project project,
                                HaxeLibraryList haxelibExternalItems,
                                HaxelibLibraryCache libManager,
                                HaxeModuleSettings settings) {
    timeLog.stamp("Start loading haxelibs from openFL configuration file.");
    HaxeLibrary openfl = libManager.getLibrary("openfl", HaxelibSemVer.ANY_VERSION);
    if (null != openfl) {
      haxelibExternalItems.add(openfl.createReference(HaxelibSemVer.ANY_VERSION)); // No specific version.
    }
    else {
      // TODO: Figure out how to report this to the user.
      log.warn("Required library 'openfl' is not known to haxelib.");
      HaxelibNotifier.notifyMissingLibrary(module, "openfl", null);
    }

    // TODO: Pull libs off of the command line, too.

    String openFLXmlPath = settings.getOpenFLPath();
    if (openFLXmlPath != null && !openFLXmlPath.isEmpty()) {
      doWriteAction(()->VirtualFileManager.getInstance().syncRefresh());
      //VirtualFileManager.getInstance().syncRefresh();
      VirtualFile file = LocalFileFinder.findFile(openFLXmlPath);

      if (file != null && file.getFileType().equals(XmlFileType.INSTANCE)) {
        List<HaxelibUtil.HaxeLibData> data = ApplicationManager.getApplication().runReadAction(new Computable<>() {
          @Override
          public List<HaxelibUtil.HaxeLibData> compute() {
            VirtualFile file = LocalFileFinder.findFile(openFLXmlPath);
            if (file != null && file.getFileType().equals(XmlFileType.INSTANCE)) {
              PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
              if (psiFile instanceof XmlFile xmlFile) {
                return HaxelibUtil.getHaxelibDataFromXmlFile(xmlFile);
              }
            }
            return List.of();
          }
        });

        haxelibExternalItems.addAll(HaxelibUtil.createHaxelibsFromHaxeLibData(module, data, libManager));
      }
    }
    else {
      // XXX: EMB - Not sure of the validity of using this path if xml lib isn't specified.

      String projectBasePath = project.getBasePath();
      if (null == projectBasePath) {
        projectBasePath = "";
      }
      File dir = new File(projectBasePath);
      List<String> projectClasspaths =
        HaxelibClasspathUtils.getProjectDisplayInformation(project, dir, "openfl",
                                                           HaxelibSdkUtils.lookupSdk(module));

      for (String classpath : projectClasspaths) {
        VirtualFile file = LocalFileFinder.findFile(classpath);
        if (file != null) {
          HaxeLibrary lib = libManager.getLibraryByPath(file.getPath());
          if (null != lib) {
            haxelibExternalItems.add(lib.createReference());
          }
          else {
            // TODO: Figure out how to communicate this to the user.
            log.warn("Library referenced by openFL configuration is not known to haxelib " + classpath);
          }
        }
      }
    }
    haxelibExternalItems.debugDump("haxelibExternalItems for module " + module.getName());
    timeLog.stamp("Finished loading haxelibs from openfl file.");

    // TODO: Add classpaths from the xml file and the CL to the module sources.
  }

  private static void syncNMMLModule(@NotNull Module module,
                                     @NotNull HaxeDebugTimeLog timeLog,
                                     Project project,
                                     HaxeLibraryList haxelibExternalItems,
                                     HaxelibLibraryCache libManager,
                                     HaxeModuleSettings settings) {
    timeLog.stamp("Start loading haxelibs from NMML file.");
    HaxeLibrary nme = libManager.getLibrary("nme", HaxelibSemVer.ANY_VERSION);
    if (null != nme) {
      haxelibExternalItems.add(nme.createReference(HaxelibSemVer.ANY_VERSION));
    }
    else {
      // TODO: Figure out how to communicate this to the user.
      log.warn("Required library 'nme' is not known to haxelib.");
    }

    // TODO: Pull libs off of the command line, too.

    String nmmlPath = settings.getNmmlPath();
    if (nmmlPath != null && !nmmlPath.isEmpty()) {
      doWriteAction(()->VirtualFileManager.getInstance().syncRefresh());
      //VirtualFileManager.getInstance().syncRefresh();
      List<HaxelibUtil.HaxeLibData> data = ApplicationManager.getApplication().runReadAction(new Computable<>() {
        @Override
        public List<HaxelibUtil.HaxeLibData> compute() {
          VirtualFile file = LocalFileFinder.findFile(nmmlPath);
          if (file != null && file.getFileType().equals(NMMLFileType.INSTANCE)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile instanceof XmlFile xmlFile) {
              return HaxelibUtil.getHaxelibDataFromXmlFile(xmlFile);
            }
          }
          return List.of();
        }
      });

      haxelibExternalItems.addAll(HaxelibUtil.createHaxelibsFromHaxeLibData(module, data, libManager));


    }
    timeLog.stamp("Finished loading haxelibs from NMML file.");

    // TODO: Load classpaths from the NMML file, CL, and ensure that they are included in the module sources.
  }


  private void syncModuleClasspaths(final ProjectTracker tracker) {
    final HaxeDebugTimeLog timeLog = HaxeDebugTimeLog.startNew("syncModuleClasspaths");

    final Project project = tracker.getProject();

    //LOG.debug("Scanning project " + project.getName());
    timeLog.stamp("Scanning project " + project.getName());

    Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());
    int i = 0;
    final int count = modules.size();
    for (final Module module : modules) {

      final int num = ++i;

      //LOG.debug("Scanning module " + (++i) + " of " + count + ": " + module.getName());
      timeLog.stamp("\nScanning module " + (num) + " of " + count + ": " + module.getName());
      syncOneModule(tracker, module, timeLog);
    }
    timeLog.stamp("Completed.");
    timeLog.print();
  }


  public void synchronizeClasspaths(@NotNull ProjectTracker tracker) {

    //
    // Either of these commented-out sections will cause indexing to not be attempted
    // while the haxelibs are synchronizing.  However, they also hide the fact that
    // haxelibs are updating by not allowing the haxelib progress dialog to start.
    // Instead, it looks like indexing restarts over and over and over (which it
    // kinda does).
    //

    //DumbServiceImpl dumbService = DumbServiceImpl.getInstance(tracker.getProject());
    //dumbService.queueTask(new DumbModeTask() {
    //  @Override
    //  public void performInDumbMode(@NotNull ProgressIndicator indicator) {
    //    syncProjectClasspath(tracker);
    //    syncModuleClasspaths(tracker);
    //  }
    //});

    //TransactionGuard tg = TransactionGuard.getInstance();
    //tg.submitTransactionAndWait(new Runnable() {
    //  @Override
    //  public void run() {
    //    syncProjectClasspath(tracker);
    //    syncModuleClasspaths(tracker);
    //  }
    //});

    //syncProjectClasspath(tracker);
    //syncModuleClasspaths(tracker);



    ProgressManager progressManager = ProgressManager.getInstance();
    progressManager.runProcessWithProgressSynchronously(()-> syncProjectClasspath(tracker), "Synchronizing Project Classpaths...", false, tracker.getProject());
    progressManager.runProcessWithProgressSynchronously(()-> syncModuleClasspaths(tracker), "Synchronizing Module Classpaths...", false, tracker.getProject());

  }

  /**
   * Retrieves the project's libraries, either from the cache if available,
   * or from the project's library table.
   *
   * @param tracker for the project being updated.
   * @return a HaxeClasspath representing the libraries specified at the project level.
   */
  @NotNull
  private HaxeLibraryList getProjectLibraryList(@NotNull ProjectTracker tracker) {
    ProjectLibraryCache cache = tracker.getCache();
    HaxeLibraryList projectLibraries;
    HaxeConfiguration buildConfig = HaxeConfiguration.CUSTOM; // Only properties available.

    if (cache.isListSetFor(buildConfig)) {
      projectLibraries = cache.getListFor(buildConfig);
    }
    else {
      projectLibraries = HaxelibUtil.getProjectLibraries(tracker.getProject(), false, false);
      cache.setListFor(buildConfig, projectLibraries);
    }
    return projectLibraries;
  }

  /**
   * Collect all dependencies for libraries in list.
   *
   * @param list of libraries to collect dependencies for.
   * @return a list of libraries that list depends upon.  May include entries
   * from list itself, if there are cross-dependencies.
   */
  @NotNull
  private HaxeLibraryList collectDependencies(@NotNull HaxeLibraryList list) {
    final HaxeLibraryList dependencies = new HaxeLibraryList(list.getOwner());
    list.iterate(new HaxeLibraryList.Lambda() {
      @Override
      public boolean processEntry(HaxeLibraryReference entry) {
        if (entry.isAvailable()) {
          dependencies.addAll(entry.getLibrary().collectDependents());
        }
        return true;
      }
    });
    return dependencies;
  }

  /**
   * Mark all entries in a list as managed...
   *
   * @param list
   */
  @NotNull
  private void markAsManaged(@NotNull HaxeLibraryList list) {
    list.iterate(new HaxeLibraryList.Lambda() {
      @Override
      public boolean processEntry(HaxeLibraryReference entry) {
        entry.markAsManagedEntry();
        return true;
      }
    });
  }

  /**
   * Find libraries with roots that exist in the classpath.
   *
   * @param libs libs to match to classpaths.
   * @param classpath classpath to match.
   * @return a list of libraries whose directory entries match entries from the classpath.
   */
  @NotNull
  private HaxeLibraryList findLibsMatchingClasspath(@NotNull final HaxeLibraryList libs, @NotNull final HaxeClasspath classpath) {
    final HaxeLibraryList matchingLibs = new HaxeLibraryList(libs.getOwner());
    libs.iterate(new HaxeLibraryList.Lambda() {
      @Override
      public boolean processEntry(HaxeLibraryReference entry) {
        HaxeLibrary lib = entry.getLibrary();
        if (null != lib && classpath.contains(lib.getLibraryRoot()))
          matchingLibs.add(entry);
        return true;
      }
    });
    return matchingLibs;
  }

  /**
   * Synchronize library lists to figure out which dependencies need to be added or removed.
   *
   * @param currentList the list of libraries currently defined in the module/project/sdk.
   * @param externallyRequired the list of libraries required by project settings. (e.g. openfl, nme)
   * @param newLibrariesToAdd the resultant list of libraries to add to currentList
   * @param oldLibrariesToRemove the resultant list of libraries to remove from currentList.
   */
  @NotNull
  private void syncLibraryLists(@NotNull Sdk sdk,
                                @NotNull HaxeLibraryList currentList,
                                @NotNull HaxeLibraryList externallyRequired,
                   /*modifies*/ @NotNull HaxeLibraryList newLibrariesToAdd,
                   /*modifies*/ @NotNull HaxeLibraryList oldLibrariesToRemove) {

    final HaxeLibraryList currentManagedEntries = new HaxeLibraryList(sdk);
    final HaxeLibraryList currentUnmanagedEntries = new HaxeLibraryList(sdk);
    currentList.iterate(new HaxeLibraryList.Lambda() {
      @Override
      public boolean processEntry(HaxeLibraryReference entry) {
        if (entry.isManaged()) { // (e.g. starts with "haxelib|"
          currentManagedEntries.add(entry);
        }  else {
          currentUnmanagedEntries.add(entry);
        }
        return true;
      }
    });

    HaxeLibraryList dependencies = new HaxeLibraryList(collectDependencies(externallyRequired));
    dependencies.addAll(collectDependencies(currentUnmanagedEntries));

    // We want to remove all managed entries that we don't need any more.
    HaxeLibraryList toRemove = currentManagedEntries;

    // We want to add all externally required entries
    HaxeLibraryList toAdd = new HaxeLibraryList(sdk);
    toAdd.addAll(externallyRequired);
    toAdd.addAll(dependencies);
    toAdd.removeAll(currentUnmanagedEntries);
    // At this point, we could remove libs that exist via alternative names (find by classpath).
    // However, they can't be used with the compiler anyway, so the better part of valor
    // is to include them by the name that haxelib knows, even if it technically duplicates
    // the entry.
    markAsManaged(toAdd);

    // Anything that is in both lists, we don't want to touch.
    newLibrariesToAdd.addAll(toAdd);
    newLibrariesToAdd.removeAll(toRemove);

    oldLibrariesToRemove.addAll(toRemove);
    oldLibrariesToRemove.removeAll(toAdd);
  }


  /**
   * Removes old unneeded libraries and adds new dependencies to the project classpath.
   * Queues an update to the Project.
   *
   * @param tracker for the project being updated.
   */
  @NotNull
  private void syncProjectClasspath(@NotNull ProjectTracker tracker) {
    Sdk sdk = HaxelibSdkUtils.lookupSdk(tracker.getProject());
    boolean isHaxeSDK = sdk.getSdkType().equals(HaxeSdkType.getInstance());

    if (!isHaxeSDK) {
      return;
    }

    HaxeDebugTimeLog timeLog = new HaxeDebugTimeLog("syncProjectClasspath");
    timeLog.stamp("Start synchronizing project " + tracker.getProject().getName());

    HaxeLibraryList toAdd = new HaxeLibraryList(sdk);
    HaxeLibraryList toRemove = new HaxeLibraryList(sdk);
    syncLibraryLists(sdk,
                     HaxelibUtil.getProjectLibraries(tracker.getProject(),false, false),
                     new HaxeLibraryList(sdk),
        /*modifies*/ toAdd,
        /*modifies*/ toRemove );

    if (!toAdd.isEmpty() && !toRemove.isEmpty()) {
      timeLog.stamp("Add/Remove calculations finished.  Queuing write task.");
      updateProject(tracker, toRemove, toAdd);
    }

    timeLog.stamp("Finished synchronizing.");
    timeLog.print();

    // And update the cache.
    tracker.getCache().setPropertiesList(HaxelibUtil.getProjectLibraries(tracker.getProject(), false, false));
  }


  /**
   * Workhorse routine for syncProjectClasspath.  This does the actual update of the
   * project.  It will block until all of the running events on the AWT thread have
   * completed, and then this will run on that thread.
   *
   * @param tracker for the project to update.
   * @param toRemove libraries that need to be removed from the project.
   * @param toAdd libraries that need to be added to the project.
   */
  private void updateProject(@NotNull final ProjectTracker tracker,
                             final @Nullable HaxeLibraryList toRemove,
                             final @Nullable HaxeLibraryList toAdd) {
    if (null == toRemove && null == toAdd) {
      return;
    }
    if (null != toRemove) {
      toRemove.iterate(new HaxeLibraryList.Lambda() {
        @Override
        public boolean processEntry(HaxeLibraryReference entry) {
          log.assertTrue(entry.isManaged(), "Attempting to automatically remove a library that was not marked as managed.");
          return true;
        }
      });
    }
    if (null != toAdd) {
      toAdd.iterate(new HaxeLibraryList.Lambda() {
        @Override
        public boolean processEntry(HaxeLibraryReference entry) {
          log.assertTrue(entry.isManaged(), "Attempting to automatically add a library that is not marked as managed.");
          return true;
        }
      });
    }

    doWriteAction(() -> {
      final HaxeDebugTimeLog timeLog = new HaxeDebugTimeLog("Write action:");
      timeLog.stamp("Begin: Updating project libraries");

      LibraryTable projectTable = LibraryTablesRegistrar.getInstance().getLibraryTable(tracker.getProject());
      final LibraryTable.ModifiableModel projectModifiableModel = projectTable.getModifiableModel();

      // Remove unused packed "haxelib|<lib_name>" libraries from the module and project library.
      if (null != toRemove) {
        timeLog.stamp("Removing unneeded haxelib libraries.");
        toRemove.iterate(new HaxeLibraryList.Lambda() {
          @Override
          public boolean processEntry(HaxeLibraryReference entry) {
            Library library = projectModifiableModel.getLibraryByName(
              entry.getName());
            log.assertTrue(null != library, "Library " + entry.getName() + " was not found in the project and will not be removed.");
            if (null != library) {
              projectModifiableModel.removeLibrary(library);
              timeLog.stamp("Removed library " + entry.getName());
            }
            else {
              timeLog.stamp(
                "Library to remove was not found: " + entry.getName());
            }
            return true;
          }
        });
      }

      // Add new dependencies to modules.
      if (null != toAdd) {
        timeLog.stamp("Adding haxelib dependencies.");
        toAdd.iterate(new HaxeLibraryList.Lambda() {
          @Override
          public boolean processEntry(HaxeLibraryReference newItem) {
            Library libraryByName = projectModifiableModel.getLibraryByName(newItem.getName());
            if (libraryByName == null) {
              assert newItem.isAvailable(); // Should have been removed if unavailable.
              libraryByName = projectModifiableModel.createLibrary(newItem.getName());  // TODO: Presentable Name??
              Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
              libraryModifiableModel.addRoot(newItem.getLibrary().getSourceRoot().getUrl(), OrderRootType.CLASSES);
              libraryModifiableModel.addRoot(newItem.getLibrary().getSourceRoot().getUrl(), OrderRootType.SOURCES);
              libraryModifiableModel.commit();

              timeLog.stamp("Added library " + libraryByName.getName());
            }
            return true;
          }
        });
      }

      timeLog.stamp("Committing project changes.");
      projectModifiableModel.commit();
      timeLog.stamp("Finished: Updating project Libraries");
      timeLog.print();
    });
  }

  /**
   * Cause a synchronous write action to be run on the AWT thread.
   *
   * @param action action to run.
   */
  private static void doWriteAction(final Runnable action) {
    final Application application = ApplicationManager.getApplication();
    application.invokeAndWait( () -> application.runWriteAction(action));

    //application.invokeAndWait(new Runnable() {
    //  @Override
    //  public void run() {
    //    application.runWriteAction(action);
    //  }
    //}, application.getDefaultModalityState());
  }

  /**
   * Cause a synchronous read action to be run.  Blocks if a write action is
   * currently running in the AWT thread.  Also blocks write actions from
   * occuring while this is being run.  So don't let tasks take too long, or
   * the UI gets choppy.
   *
   * @param action action to run.
   */
  private static void doReadAction(final Runnable action) {
    final Application application = ApplicationManager.getApplication();
    application.runReadAction(action);
    //if (application.isDispatchThread()) {
    //  application.executeOnPooledThread(action);
    //}else {
    //  application.invokeAndWait(action, ModalityState.defaultModalityState());
    //}
    //application.invokeAndWait(action, ModalityState.defaultModalityState());
    //application.invokeLater(action, ModalityState.defaultModalityState());
  }

  /**
   *  Cache for project library lists.
   */
  static final private class ProjectLibraryCache {

    private Sdk sdk;
    private HaxeLibraryList nmmlList;
    private HaxeLibraryList openFLList;
    private HaxeLibraryList hxmlList;
    private HaxeLibraryList propertiesList;
    private boolean nmmlIsSet;
    private boolean openFLIsSet;
    private boolean hxmlIsSet;
    private boolean propertiesIsSet;

    public ProjectLibraryCache(@NotNull Sdk sdk) {
      this.sdk = sdk;
      clear();
    }

    public void clear() {
      setNmmlList(new HaxeLibraryList(sdk));
      setOpenFLList(new HaxeLibraryList(sdk));
      setHxmlList(new HaxeLibraryList(sdk));
      setPropertiesList(new HaxeLibraryList(sdk));

      // Reset the 'set' bits.
      nmmlIsSet = openFLIsSet = hxmlIsSet = propertiesIsSet = false;
    }

    public boolean isListSetFor(HaxeConfiguration buildConfig) {
      switch(buildConfig) {
        case NMML:
          return nmmlIsSet;
        case OPENFL:
          return openFLIsSet;
        case HXML:
          return hxmlIsSet;
        case CUSTOM:
          return propertiesIsSet;
      }
      return false;
    }

    @NotNull
    public HaxeLibraryList getListFor(HaxeConfiguration buildConfig) {
      switch(buildConfig) {
        case NMML:
          return getNmmlList();
        case OPENFL:
          return getOpenFLList();
        case HXML:
          return getHxmlList();
        case CUSTOM:
          return getPropertiesList();
      }
      return new HaxeLibraryList(sdk);
    }

    public void setListFor(HaxeConfiguration buildConfig, HaxeLibraryList list) {
      switch(buildConfig) {
        case NMML:
          setNmmlList(list);
        case OPENFL:
          setOpenFLList(list);
        case HXML:
          setHxmlList(list);
        case CUSTOM:
          setPropertiesList(list);
      }
    }


    @NotNull
    public HaxeLibraryList getNmmlList() {
  return nmmlList != null ? nmmlList : new HaxeLibraryList(sdk);
    }

    @NotNull
    public HaxeLibraryList getOpenFLList() {
      return openFLList != null ? openFLList : new HaxeLibraryList(sdk);
    }

    @NotNull
    public HaxeLibraryList getHxmlList() {
      return hxmlList != null ? hxmlList : new HaxeLibraryList(sdk);
    }

    @NotNull
    public HaxeLibraryList getPropertiesList() {
      return propertiesList != null ? propertiesList : new HaxeLibraryList(sdk);
    }

    public void setNmmlList(HaxeLibraryList nmmlList)  {
      this.nmmlList = nmmlList;
      nmmlIsSet = true;
    }

    public void setOpenFLList(HaxeLibraryList openFLList) {
      this.openFLList = openFLList;
      openFLIsSet = true;
    }

    public void setHxmlList(HaxeLibraryList hxmlList) {
      this.hxmlList = hxmlList;
      hxmlIsSet = true;
    }

    public void setPropertiesList(HaxeLibraryList propertiesList) {
      this.propertiesList = propertiesList;
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
    ProjectLibraryCache myCache;
    HaxelibLibraryCacheManager mySdkManager;

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
      myCache = new ProjectLibraryCache(HaxelibSdkUtils.lookupSdk(project));
      mySdkManager = new HaxelibLibraryCacheManager();

      VirtualFileManager mgr = VirtualFileManager.getInstance();
      mgr.addAsyncFileListener(this::lookForLibChanges, project);
    }

    /**
     * Get the settings cache.
     */
    @NotNull
    public ProjectLibraryCache getCache() {
      return myCache;
    }

    /**
     * Get the library classpath cache.
     */
    @NotNull
    public HaxelibLibraryCacheManager getSdkManager() {
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

      synchronized (this) {
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
      synchronized (this) {
        ret = myIsUpdating;
        myIsUpdating = newState;
      }
      return ret;
    }

    /**
     * Increase the reference count.
     */
    public void addReference() {
      synchronized (this) {
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
      synchronized (this) {
        refs = --myReferenceCount;
      }
      // XXX: Maybe we don't need an assertion for this.
      log.assertTrue(refs >= 0, "Assertion failed");

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

    public boolean equalsName(@Nullable ProjectTracker tracker) {
      if (null == tracker) {
        return false;
      }
      return myProject.getName().equals(tracker.getProject().getName());
    }

    private AsyncFileListener.ChangeApplier lookForLibChanges(List<? extends VFileEvent> events) {
      if(getProject().isDisposed()) return null;
      List<String> list = events.stream()
        .map(VFileEvent::getFile)
        .filter(Objects::nonNull)
        .map(VirtualFile::getCanonicalPath)
        .toList();

      updateLibs(list);
      return null;
    }

    private void updateLibs(List<String> list) {
      if(getProject().isDisposed()) return;
      Collection<Module> type = ModuleUtil.getModulesOfType(getProject(), HaxeModuleType.getInstance());
      Map<String, Module> moduleMap = type.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(m-> getBuildConfigFile(HaxeModuleSettings.getInstance(m)), m -> m));



      list.stream().filter(moduleMap::containsKey).forEach(k -> {
        Module module = moduleMap.get(k);
        HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
        if (settings.isKeepSynchronizedWithProjectFile()) {
          HaxeDebugTimeLog timeLog = HaxeDebugTimeLog.startNew("syncModuleClasspaths");
          HaxelibProjectUpdater instance = HaxelibProjectUpdater.getInstance();
          ProjectTracker tracker = instance.findProjectTracker(module.getProject());
          instance.syncOneModule(tracker, module, timeLog);
        }
      });
    }

    private String getBuildConfigFile(HaxeModuleSettings settings) {
      HaxeConfiguration configuration = settings.getBuildConfiguration();
      return switch (configuration) {
        case OPENFL -> settings.getOpenFLPath();
        case HXML -> settings.getHxmlPath();
        case NMML -> settings.getNmmlPath();
        default -> "N/A";
      };
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

    public boolean iterate(@NotNull Lambda<ProjectTracker> lambda) {
      synchronized(this) {
         for(ProjectTracker tracker : myMap.values()) {
           if (!lambda.process(tracker)) {
             return false;
           }
         }
      }
      return true;
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
      // XXX: Something seems wrong about skipping the currently updating project.
      //      What if a project change happens while the project is already running?
      //      Should we cancel and restart instead?  And, if so, should we have a
      //      short delay to ensure that all identical messages are already queued?
      if (!tracker.equalsName(getUpdatingProject())) {
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
        log.assertTrue(null == updatingProject, "Assertion failed");

        // Get the next project from the queue. We're done if there's
        // nothing left.
        updatingProject = queue.poll();  // null if empty.
        if (updatingProject == null) return;

        log.assertTrue(updatingProject.isDirty(), "Assertion failed");
        log.assertTrue(!updatingProject.isUpdating(), "Assertion failed");

        updatingProject.setUpdating(true);
      }

      // Waiting for runWhenProjectIsInitialized() ensures that the project is
      // fully loaded and accessible.  Otherwise, we crash. ;)
      StartupManager.getInstance(updatingProject.getProject()).runWhenProjectIsInitialized(new Runnable() {
        public void run() {
          log.debug("Starting haxelib library sync...");
          runUpdate();
        }
      });

    }

    /**
     * Runs the update, either in the foreground or background, depending upon
     * the state of the myTestInForeground debug flag.
     */
    private void runUpdate() {
      final ProjectTracker tracker = getUpdatingProject();
      final Project project = tracker == null ? null : tracker.getProject();

      if (myTestInForeground) {
        doUpdateWork();
      } else {
        ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(
          // TODO: Put this string in a resource bundle.
          new Task.Backgroundable(project, "Synchronizing with haxelib libraries...", false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              doUpdateWork();
            }
          }));
      }
    }

    /**
     * The basic bit of work that an update does.
     */
    private void doUpdateWork() {
      log.debug("Loading referenced libraries...");
      ProjectTracker tracker = getUpdatingProject();
      if (null == tracker) {
        log.warn("Attempt to load libraries, but no project queued for updating.");
        return;
      }
      synchronizeClasspaths(tracker);
      finishUpdate(tracker);
    }

    /**
     * Cleanup and queue the next in line, if any.
     *
     * @param up - the project that is finishing its update run.
     */
    private void finishUpdate(ProjectTracker up) {
      synchronized (updateSyncToken) {
        log.assertTrue(null != updatingProject, "Assertion failed");
        log.assertTrue(up.equals(updatingProject), "Assertion failed");

        updatingProject.setUpdating(false);
        updatingProject.setDirty(false);
        updatingProject = null;
      }
      queueNextProject();
    }
  } // end class projectUpdateQueue

}
