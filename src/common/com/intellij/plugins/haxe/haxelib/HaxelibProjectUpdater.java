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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
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
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
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

  /**
   *  Set this to run in the foreground for speed testing.
   *  It overrides myRunInForeground.  The UI is blocked with no updates.
   */
  private static final boolean myTestInForeground = false;
  /**
   *  Set this true to put up a modal dialog and run in the foreground thread
   *  (locking up the UI.)
   *  Set it false to run in a background thread.  Progress is updated in the
   *  status bar and the UI is usable.
   */
  private static final boolean myRunInForeground = false;

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

  /**
   * Retrieve the HaxelibLibraryCacheManager for a given module/project.
   *
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
   *
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
   * Resolve the classpath/library entries for a module.  Determines which
   * libraries to add and remove from the module.  Only libraries that have
   * previously been added may be removed, if they have become redundant
   * or otherwise specified.
   *
   * @param tracker for the project being updated.
   * @param module being updated.
   * @param externalClasspaths potential new classpaths that must be available
   *                           to the module when this routine finishes.
   */
  private void resolveModuleLibraries(ProjectTracker tracker, Module module, HaxeClasspath externalClasspaths) {
    HaxeClasspath toAdd;
    HaxeClasspath toRemove;

    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);

    // Remove project level classpath items from the list of required
    // external libraries.
    //
    // If the SDK is inherited, then we can use the project dependencies to
    // filter against.  Otherwise, the project dependencies may not be
    // reliable, since they are not made using the same haxelib.  Checking
    // whether they resolve to the same thing may be difficult, given that
    // the actual library location, etc. is kept in the external .xml files,
    // or *may* be in the environment (though our environment is constant).
    //
    // For the moment, we'll presume that if the SDK is NOT inherited, the
    // module gets the full list.
    //
    // We're also checking whether the classpath is known to the SDK.  If so,
    // we also want to filter it out.
    //
    HaxeClasspath inheritedClasspaths = HaxelibClasspathUtils.getSdkClasspath(
      HaxelibSdkUtils.lookupSdk(module));
    if (rootManager.isSdkInherited()) {
      inheritedClasspaths.addAll(getProjectClasspath(tracker));
    }

    class NewPathCollector implements HaxeClasspath.Lambda {
      public HaxeClasspath myUninherited = new HaxeClasspath();
      private HaxeClasspath myInherited;
      public NewPathCollector(HaxeClasspath inherited) { myInherited = inherited; }
      @Override
      public boolean processEntry(HaxeClasspathEntry externalPath) {
        if (!myInherited.contains(externalPath)) {
          myUninherited.add(externalPath);
        }
        return true;
      }
    }
    NewPathCollector npCollector = new NewPathCollector(inheritedClasspaths);
    externalClasspaths.iterate(npCollector);
    HaxeClasspath uninheritedExternalClasspaths = npCollector.myUninherited;

    // Figure out which libs from the module to remove. To be candidates for
    // removal:
    // - First, they must to be managed libraries/paths.
    // - Second, they must not appear in the uninherited external classpaths, OR
    //   they appear in the project or SDK classpaths from the module.
    class RemoveCollector implements HaxeClasspath.Lambda {
      HaxeClasspath uninherited;
      HaxeClasspath inherited;
      public HaxeClasspath toRemove = new HaxeClasspath();

      public RemoveCollector(HaxeClasspath uninheritedClasspath, HaxeClasspath inheritedClasspath) {
        uninherited = uninheritedClasspath;
        inherited = inheritedClasspath;
      }

      @Override
      public boolean processEntry(HaxeClasspathEntry entry) {
        if (entry.isManagedEntry()
            &&  (!uninherited.contains(entry) || inherited.contains(entry))) {
          toRemove.add(entry);
        }
        return true;
      }
    }
    RemoveCollector collector = new RemoveCollector(uninheritedExternalClasspaths, inheritedClasspaths);

    HaxeClasspath moduleClasspath = HaxelibClasspathUtils.getModuleClasspath(module);
    moduleClasspath.iterate(collector);
    toRemove = collector.toRemove;

    // uninheritecExternalClaspaths should not contain any non-haxelib entries,
    // so we don't have to worry about that check here.
    toAdd = uninheritedExternalClasspaths;
    toAdd.removeAll(moduleClasspath);

    // Anything new must be marked as managed.
    toAdd.iterate(new HaxeClasspath.Lambda() {
      @Override
      public boolean processEntry(HaxeClasspathEntry entry) {
        entry.markAsManagedEntry();
        return true;
      }
    });

    updateModule(module, toRemove, toAdd);
  }

  /**
   * Workhorse routine for resolveModuleLibraries.  This does the actual
   * update of the module.  It will block until all of the running events
   * on the AWT thread have completed, and then this will run on that thread.
   *
   * @param module to update.
   * @param toRemove libraries that need to be removed from the module.
   * @param toAdd libraries that need to be added to the module.
   */
  private void updateModule(final Module module, final HaxeClasspath toRemove, final HaxeClasspath toAdd) {
    if ((null == toRemove || toRemove.isEmpty()) && (null == toAdd || toAdd.isEmpty())) {
      return;
    }
    if (null != toRemove) {
      toRemove.iterate( new HaxeClasspath.Lambda() {
        @Override
        public boolean processEntry(HaxeClasspathEntry entry) {
          LOG.assertTrue(entry.isManagedEntry(), "Attempting to automatically remove a library that was not marked as managed.");
          return true;
        }
      });
    }
    if (null != toAdd) {
      toAdd.iterate( new HaxeClasspath.Lambda() {
        @Override
        public boolean processEntry(HaxeClasspathEntry entry) {
          LOG.assertTrue(entry.isManagedEntry(), "Attempting to automatically add a library that is not marked as managed.");
          return true;
        }
      });
    }

    final HaxeDebugTimeLog timeLog = new HaxeDebugTimeLog("Write action:");
    timeLog.stamp("Queueing write action...");

    doWriteAction(new Runnable() {
      @Override
      public void run() {
        timeLog.stamp("<-- Time elapsed waiting for write access on the AWT thread.");
        timeLog.stamp("Begin: Updating module libraries for " + module.getName());

        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        ModifiableRootModel modifiableModel = rootManager.getModifiableModel();
        final LibraryTable libraryTable = modifiableModel.getModuleLibraryTable();

        // Remove unused packed "haxelib|<lib_name>" libraries from the module and project library.
        if (null != toRemove) {
          timeLog.stamp("Removing libraries.");
          toRemove.iterate(new HaxeClasspath.Lambda(){
            @Override
            public boolean processEntry(HaxeClasspathEntry entry) {
              Library library = libraryTable.getLibraryByName(entry.getName());
              if (null != library) {
                // Why use this?: ModuleHelper.removeDependency(rootManager, library);
                libraryTable.removeLibrary(library);
                timeLog.stamp("Removed library " + library.getName());
              }
              else {
                LOG.warn(
                  "Internal inconsistency: library to remove was not found: " +
                  entry.getName());
              }
              return true;
            }
          });
        }

        // Add new dependencies to modules.
        if (null != toAdd) {
          timeLog.stamp("Locating libraries and adding dependencies.");
          toAdd.iterate(new HaxeClasspath.Lambda() {
            @Override
            public boolean processEntry(HaxeClasspathEntry entry) {
              Library libraryByName = libraryTable.getLibraryByName(
                entry.getName());
              if (libraryByName == null) {
                libraryByName = libraryTable.createLibrary(entry.getName());
                Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
                libraryModifiableModel.addRoot(entry.getUrl(), OrderRootType.CLASSES);
                libraryModifiableModel.addRoot(entry.getUrl(), OrderRootType.SOURCES);
                libraryModifiableModel.commit();

                timeLog.stamp("Added library " + libraryByName.getName());
              }
              else {
                LOG.warn("Internal inconsistency: library to add was already in the module's library table.");
              }
              return true;
            }
          });
        }

        timeLog.stamp("Committing changes to module libraries");
        modifiableModel.commit();

        timeLog.stamp("Finished: Updating module Libraries");
      }
    });

    timeLog.print();
  }

  /**
   * The guts of syncModuleClasspaths, separated so that it can be run as
   * either a foreground or background task.
   *
   * @param tracker for the project being updated.
   * @param module being updated.
   * @param timeLog where to log timing results
   */
  private void syncOneModule(@NotNull final ProjectTracker tracker, @NotNull Module module, @NotNull HaxeDebugTimeLog timeLog) {

    Project project = tracker.getProject();
    HaxeClasspath haxelibExternalItems = new HaxeClasspath();
    HaxeClasspath haxelibNewItemList;
    HaxelibLibraryCache libManager = tracker.getSdkManager().getLibraryManager(module);
    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    int buildConfig = settings.getBuildConfig();

    switch (buildConfig) {
      case HaxeModuleSettings.USE_NMML:
        timeLog.stamp("Start loading classpaths from NMML file.");
        haxelibNewItemList = libManager.findHaxelibPath("nme");
        haxelibExternalItems.addAll(haxelibNewItemList);

        String nmmlPath = settings.getNmmlPath();
        if (nmmlPath != null && !nmmlPath.isEmpty()) {
          VirtualFile file = LocalFileFinder.findFile(nmmlPath);

          if (file != null && file.getFileType().equals(NMMLFileType.INSTANCE)) {
            VirtualFileManager.getInstance().syncRefresh();
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

            if (psiFile != null && psiFile instanceof XmlFile) {
              haxelibExternalItems.addAll(HaxelibClasspathUtils.getHaxelibsFromXmlFile((XmlFile)psiFile, libManager));
            }
          }
        }
        timeLog.stamp("Finished loading classpaths from NMML file.");
        break;
      case HaxeModuleSettings.USE_OPENFL:
        timeLog.stamp("Start loading classpaths from openfl file.");
        haxelibNewItemList = libManager.findHaxelibPath("openfl");
        haxelibExternalItems.addAll(haxelibNewItemList);

        String openFLXmlPath = settings.getOpenFLPath();
        if (openFLXmlPath != null && !openFLXmlPath.isEmpty()) {
          VirtualFile file = LocalFileFinder.findFile(openFLXmlPath);

          if (file != null && file.getFileType().equals(XmlFileType.INSTANCE)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

            if (psiFile != null && psiFile instanceof XmlFile) {
              haxelibExternalItems.addAll(HaxelibClasspathUtils.getHaxelibsFromXmlFile((XmlFile)psiFile, libManager));
            }
          }
        }
        else {
          File dir = BuildProperties.getProjectBaseDir(project);
          List<String> projectClasspaths =
            HaxelibClasspathUtils.getProjectDisplayInformation(project, dir, "openfl",
                                                               HaxelibSdkUtils.lookupSdk(module));

          for (String classpath : projectClasspaths) {
            VirtualFile file = LocalFileFinder.findFile(classpath);
            if (file != null) {
              haxelibExternalItems.add(new HaxelibItem(classpath, file.getUrl()));
            }
          }
        }
        haxelibExternalItems.debugDump("haxelibExternalItems for module "+ module.getName());
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
                haxelibExternalItems.add(new HaxelibItem(classpath, VfsUtil.pathToUrl(classpath)));
              }

              Collection<HXMLLib> hxmlLibs = PsiTreeUtil.findChildrenOfType(psiFile, HXMLLib.class);
              for (HXMLLib hxmlLib : hxmlLibs) {
                String name = hxmlLib.getValue();
                haxelibNewItemList = libManager.findHaxelibPath(name);
                haxelibExternalItems.addAll(haxelibNewItemList);
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
            haxelibExternalItems.add(new HaxelibItem(classpath, VfsUtil.pathToUrl(classpath)));
          }
        }
        timeLog.stamp("Finish loading classpaths from properties.");
        break;
    }

    // We can't just remove all of the project classpaths from the module's
    // library list here because we need to remove any managed classpaths that
    // are no longer valid in the modules.  We can't do that if we don't have
    // the list of valid ones.  :/
    timeLog.stamp("Adding libraries to module.");
    resolveModuleLibraries(tracker, module, haxelibExternalItems);
    timeLog.stamp("Finished adding libraries to module.");
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

      if (myTestInForeground) {
        syncOneModule(tracker, module, timeLog);
      } else {
        // Running inside of a read action lets the UI run, and messes with the timing.
        doReadAction(new Runnable() { @Override public void run() {
            syncOneModule(tracker, module, timeLog);
        } });
      }
    }
    timeLog.stamp("Completed.");
    timeLog.print();
  }


  private void synchronizeClasspaths(@NotNull ProjectTracker tracker) {
    syncProjectClasspath(tracker);
    syncModuleClasspaths(tracker);
  }

  /**
   * Retrieves the project's classpath, either from the cache if available,
   * or from the project's library table.
   *
   * @param tracker for the project being updated.
   * @return a HaxeClasspath representing the libraries specified at the project level.
   */
  @NotNull
  private HaxeClasspath getProjectClasspath(@NotNull ProjectTracker tracker) {
    ProjectClasspathCache cache = tracker.getCache();
    HaxeClasspath projectClasspath;
    int buildConfig = HaxeModuleSettings.USE_PROPERTIES; // Only properties available.

    if (cache.isClasspathSetFor(buildConfig)) {
      projectClasspath = cache.getClasspathFor(buildConfig);
    } else {
      projectClasspath = HaxelibClasspathUtils.getProjectLibraryClasspath(
        tracker.getProject());
      cache.setClasspathFor(buildConfig, projectClasspath);
    }
    return projectClasspath;
  }

  /**
   * Removes old unneeded libraries and adds new dependencies to the project classpath.
   * Queues an update to the Project.
   *
   * @param tracker for the project being updated.
   */
  @NotNull
  private void syncProjectClasspath(@NotNull ProjectTracker tracker) {
    HaxeDebugTimeLog timeLog = new HaxeDebugTimeLog("syncProjectClasspath");
    timeLog.stamp("Start synchronizing project " + tracker.getProject().getName());

    Sdk sdk = HaxelibSdkUtils.lookupSdk(tracker.getProject());
    HaxelibLibraryCache libCache = tracker.getSdkManager().getLibraryCache(sdk);
    HaxeClasspath currentProjectClasspath = HaxelibClasspathUtils.getProjectLibraryClasspath(
      tracker.getProject());
    List<String> currentLibraryNames = HaxelibClasspathUtils.getProjectLibraryNames(tracker.getProject(), true);
    HaxeClasspath haxelibClasspaths = libCache.getClasspathForHaxelibs(currentLibraryNames);

    // Libraries that we want to remove are those specified as 'haxelib' entries and are
    // no longer referenced.
    class Collector implements HaxeClasspath.Lambda {
      public HaxeClasspath toRemove = new HaxeClasspath();
      HaxeClasspath myFilter;
      public Collector(HaxeClasspath filter) { myFilter = filter; }
      @Override
      public boolean processEntry(HaxeClasspathEntry entry) {
        if (entry.isManagedEntry() && !myFilter.contains(entry)) {
          toRemove.add(entry);
        }
        return true;
      }
    }
    Collector collector = new Collector(haxelibClasspaths);
    currentProjectClasspath.iterate(collector);
    HaxeClasspath toRemove = collector.toRemove;

    // Libraries that we want to add are those that aren't already on the current classpath.
    haxelibClasspaths.removeAll(currentProjectClasspath);
    HaxeClasspath toAdd = haxelibClasspaths;


    if (!toAdd.isEmpty() && !toRemove.isEmpty()) {
      timeLog.stamp("Add/Remove calculations finished.  Queuing write task.");
      updateProject(tracker, toRemove, toAdd);
    }

    timeLog.stamp("Finished synchronizing.");
    timeLog.print();

    // And update the cache.
    currentProjectClasspath.removeAll(toRemove);
    currentProjectClasspath.addAll(toAdd);
    tracker.getCache().setPropertiesClassPath(currentProjectClasspath);
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
  private void updateProject(@NotNull final ProjectTracker tracker, final @Nullable HaxeClasspath toRemove, final @Nullable HaxeClasspath toAdd) {
    if (null == toRemove && null == toAdd) {
      return;
    }
    if (null != toRemove) {
      toRemove.iterate(new HaxeClasspath.Lambda() {
        @Override
        public boolean processEntry(HaxeClasspathEntry entry) {
          LOG.assertTrue(entry.isManagedEntry(), "Attempting to automatically remove a library that was not marked as managed.");
          return true;
        }
      });
    }
    if (null != toAdd) {
      toAdd.iterate(new HaxeClasspath.Lambda() {
        @Override
        public boolean processEntry(HaxeClasspathEntry entry) {
          LOG.assertTrue(entry.isManagedEntry(), "Attempting to automatically add a library that is not marked as managed.");
          return true;
        }
      });
    }

    doWriteAction(new Runnable() {
      @Override
      public void run() {
        final HaxeDebugTimeLog timeLog = new HaxeDebugTimeLog("Write action:");
        timeLog.stamp("Begin: Updating project libraries");

        LibraryTable projectTable = ProjectLibraryTable.getInstance(tracker.getProject());
        final LibraryTable.ModifiableModel projectModifiableModel = projectTable.getModifiableModel();

        // Remove unused packed "haxelib|<lib_name>" libraries from the module and project library.
        if (null != toRemove) {
          timeLog.stamp("Removing unneeded haxelib libraries.");
          toRemove.iterate(new HaxeClasspath.Lambda() {
            @Override
            public boolean processEntry(HaxeClasspathEntry entry) {
              Library library = projectModifiableModel.getLibraryByName(
                entry.getName());
              LOG.assertTrue(null != library, "Library " + entry.getName() + " was not found in the project and will not be removed.");
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
          toAdd.iterate(new HaxeClasspath.Lambda() {
            @Override
            public boolean processEntry(HaxeClasspathEntry newItem) {
              Library libraryByName = projectModifiableModel.getLibraryByName(newItem.getName());
              if (libraryByName == null) {
                libraryByName = projectModifiableModel.createLibrary(newItem.getName());
                Library.ModifiableModel libraryModifiableModel = libraryByName.getModifiableModel();
                libraryModifiableModel.addRoot(newItem.getUrl(), OrderRootType.CLASSES);
                libraryModifiableModel.addRoot(newItem.getUrl(), OrderRootType.SOURCES);
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
      }
    });
  }

  /**
   * Cause a synchronous write action to be run on the AWT thread.
   *
   * @param action action to run.
   */
  private static void doWriteAction(final Runnable action) {
    final Application application = ApplicationManager.getApplication();
    application.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        application.runWriteAction(action);
      }
    }, application.getDefaultModalityState());
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
    application.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        application.runReadAction(action);
      }
    }, application.getDefaultModalityState());
  }

  /**
   *  Cache for project library classpaths.
   */
  final private class ProjectClasspathCache {


    private HaxeClasspath nmmlClassPath;
    private HaxeClasspath openFLClassPath;
    private HaxeClasspath hxmlClassPath;
    private HaxeClasspath propertiesClassPath;
    private boolean nmmlIsSet;
    private boolean openFLIsSet;
    private boolean hxmlIsSet;
    private boolean propertiesIsSet;

    public ProjectClasspathCache() {
      clear();
    }

    public void clear() {
      setNmmlClassPath(HaxeClasspath.EMPTY_CLASSPATH);
      setOpenFLClassPath(HaxeClasspath.EMPTY_CLASSPATH);
      setHxmlClassPath(HaxeClasspath.EMPTY_CLASSPATH);
      setPropertiesClassPath(HaxeClasspath.EMPTY_CLASSPATH);

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
    public HaxeClasspath getClasspathFor(int buildConfig) {
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
      return HaxeClasspath.EMPTY_CLASSPATH;
    }

    public void setClasspathFor(int buildConfig, HaxeClasspath classpath) {
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


    @NotNull
    public HaxeClasspath getNmmlClassPath() {
      return nmmlClassPath != null ? nmmlClassPath : HaxeClasspath.EMPTY_CLASSPATH;
    }

    @NotNull
    public HaxeClasspath getOpenFLClassPath() {
      return openFLClassPath != null ? openFLClassPath : HaxeClasspath.EMPTY_CLASSPATH;
    }

    @NotNull
    public HaxeClasspath getHxmlClassPath() {
      return hxmlClassPath != null ? hxmlClassPath : HaxeClasspath.EMPTY_CLASSPATH;
    }

    @NotNull
    public HaxeClasspath getPropertiesClassPath() {
      return propertiesClassPath != null ? propertiesClassPath : HaxeClasspath.EMPTY_CLASSPATH;
    }

    public void setNmmlClassPath(HaxeClasspath nmmlClasspath)  {
      nmmlClassPath = nmmlClasspath;
      nmmlIsSet = true;
    }

    public void setOpenFLClassPath(HaxeClasspath openFLClassPath) {
      this.openFLClassPath = openFLClassPath;
      openFLIsSet = true;
    }

    public void setHxmlClassPath(HaxeClasspath hxmlClassPath) {
      this.hxmlClassPath = hxmlClassPath;
      hxmlIsSet = true;
    }

    public void setPropertiesClassPath(HaxeClasspath propertiesClassPath) {
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
      myCache = new ProjectClasspathCache();
      mySdkManager = new HaxelibLibraryCacheManager();
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

    public boolean equalsName(@Nullable ProjectTracker tracker) {
      if (null == tracker) {
        return false;
      }
      return myProject.getName().equals(tracker.getProject().getName());
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
        LOG.assertTrue(null == updatingProject);

        // Get the next project from the queue. We're done if there's
        // nothing left.
        updatingProject = queue.poll();  // null if empty.
        if (updatingProject == null) return;

        LOG.assertTrue(updatingProject.isDirty());
        LOG.assertTrue(!updatingProject.isUpdating());

        updatingProject.setUpdating(true);
      }

      // Waiting for runWhenProjectIsInitialized() ensures that the project is
      // fully loaded and accessible.  Otherwise, we crash. ;)
      StartupManager.getInstance(updatingProject.getProject()).runWhenProjectIsInitialized(new Runnable() {
        public void run() {
          LOG.debug("Starting haxelib library sync...");
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
      } else if (myRunInForeground) {
        // TODO: Put this string in a resource bundle.
        ProgressManager.getInstance().run(new Task.Modal(project, "Synchronizing with haxelib libraries...", false) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            indicator.setIndeterminate(true);
            indicator.startNonCancelableSection();
            doUpdateWork();
            indicator.finishNonCancelableSection();
          }
        });
      } else {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            ProgressManager.getInstance().run(
              // TODO: Put this string in a resource bundle.
              new Task.Backgroundable(project, "Synchronizing with haxelib libraries...", false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                  doUpdateWork();
                }
              });
          }
        });
      }
    }

    /**
     * The basic bit of work that an update does.
     */
    private void doUpdateWork() {
      LOG.debug("Loading referenced libraries...");
      ProjectTracker tracker = getUpdatingProject();
      if (null == tracker) {
        LOG.warn("Attempt to load libraries, but no project queued for updating.");
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