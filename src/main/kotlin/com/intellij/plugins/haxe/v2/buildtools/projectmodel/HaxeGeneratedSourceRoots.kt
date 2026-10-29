package com.intellij.plugins.haxe.v2.buildtools.projectmodel

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.plugins.haxe.v2.buildtools.HaxeGeneratedSourceFolders
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings
import com.intellij.workspaceModel.ide.NonPersistentEntitySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keeps the generated source roots of Lime, OpenFL and NME builds in the
 * project model. For every build file of a module whose framework has "Mark
 * generated classes as sources" on, the generated folders of its selected
 * target ([HaxeGeneratedSourceFolders]) are generated source roots of the
 * module while they exist on disk. They are nested roots, so the classes in
 * the swf library's `_generated` folder keep their empty package.
 *
 * The roots carry [NonPersistentEntitySource]: they live in the IDE session
 * only and never reach the .iml, because the target they follow is a
 * personal choice. [HaxeGeneratedSourceRootsTracker] asks for a reconcile
 * when the target, a build file or the setting changes, and when the folders
 * appear or disappear: a build creates them, a clean deletes them.
 */
@Service(Service.Level.PROJECT)
class HaxeGeneratedSourceRoots(private val project: Project, private val scope: CoroutineScope) {

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HaxeGeneratedSourceRoots = project.service()

    /** Reconciles every open project, for an application-level settings change. */
    @JvmStatic
    fun requestReconcileInOpenProjects() {
      for (project in ProjectManager.getInstance().openProjects) {
        getInstance(project).requestReconcile()
      }
    }

    // matches how the platform serializes java-like source roots; the build files' own roots use the same
    private val SOURCE_ROOT_TYPE = SourceRootTypeId("java-source")
  }

  /** What the roots should be: per module name the folders that exist on disk, and every folder watched, existing or not. */
  private data class Wanted(val existingByModule: Map<String, Set<String>>, val watched: Set<String>)

  /** Every folder the last reconcile wanted, existing or not: the paths the tracker watches. */
  @Volatile
  private var watchedPaths: Set<String> = emptySet()
  private val requested = AtomicInteger()
  private val applied = AtomicInteger()
  private val reconciling = Mutex()

  /** Whether a change at the path (created, deleted or moved) can affect a watched folder: the path is one of them or contains one. */
  fun affectsGeneratedFolders(path: String): Boolean = watchedPaths.any { FileUtil.isAncestor(path, it, false) }

  /** Brings the roots up to date in the background and returns the run that covers this request; a burst of requests runs one reconcile. */
  fun requestReconcile(): Job {
    val generation = requested.incrementAndGet()
    return scope.launch {
      reconciling.withLock {
        // a run that started after this request already covered it
        if (applied.get() >= generation) return@withLock
        val covered = requested.get()
        reconcile()
        applied.set(covered)
      }
    }
  }

  private suspend fun reconcile() {
    if (project.isDisposed) return
    val wanted = smartReadAction(project) { wantedRoots() }
    watchedPaths = wanted.watched
    val workspaceModel = WorkspaceModel.getInstance(project)
    if (!differs(workspaceModel.currentSnapshot, wanted.existingByModule)) return
    val urlManager = workspaceModel.getVirtualFileUrlManager()
    workspaceModel.update("Haxe generated source roots") { builder ->
      updateRoots(builder, wanted.existingByModule, urlManager)
    }
  }

  private fun wantedRoots(): Wanted {
    val settings = HaxeFrameworkTargetSettings.getInstance()
    val existingByModule = mutableMapOf<String, MutableSet<String>>()
    val watched = mutableSetOf<String>()
    for (module in ModuleManager.getInstance(project).modules) {
      for (buildFile in HaxeKnownBuildFiles.forModule(project, module)) {
        val framework = HaxeGeneratedSourceFolders.frameworkOf(buildFile.type()) ?: continue
        if (!settings.marksGeneratedSources(framework)) continue
        for (folder in HaxeGeneratedSourceFolders.of(project, buildFile)) {
          val path = FileUtil.toSystemIndependentName(folder.toString())
          watched += path
          // the disk decides: the VFS may not have seen a build's folders yet
          if (Files.isDirectory(folder)) existingByModule.getOrPut(module.name) { linkedSetOf() } += path
        }
      }
    }
    return Wanted(existingByModule, watched)
  }

  /** Whether any module's managed roots differ from the wanted ones, so that an update, and the roots change it announces, is due. */
  private fun differs(storage: EntityStorage, wantedByModule: Map<String, Set<String>>): Boolean =
    storage.entities(ModuleEntity::class.java).any { module ->
      module.contentRoots.any { contentRoot -> diff(contentRoot, wantedByModule[module.name].orEmpty()).hasChanges }
    }

  private fun updateRoots(builder: MutableEntityStorage, wantedByModule: Map<String, Set<String>>, urlManager: VirtualFileUrlManager) {
    for (module in builder.entities(ModuleEntity::class.java).toList()) {
      val wanted = wantedByModule[module.name].orEmpty()
      for (contentRoot in module.contentRoots.toList()) {
        val diff = diff(contentRoot, wanted)
        if (!diff.hasChanges) continue
        diff.stale.forEach { builder.removeEntity(it) }
        if (diff.missing.isNotEmpty()) {
          builder.modifyContentRootEntity(contentRoot) {
            sourceRoots = sourceRoots + diff.missing.map { generatedRoot(urlManager.getOrCreateFromUrl(it)) }
          }
        }
      }
    }
  }

  /** A content root's managed roots against the wanted folders under it: the roots to remove and the urls to add. */
  private class Diff(val stale: List<SourceRootEntity>, val missing: List<String>) {
    val hasChanges: Boolean get() = stale.isNotEmpty() || missing.isNotEmpty()
  }

  private fun diff(contentRoot: ContentRootEntity, wantedPaths: Set<String>): Diff {
    val rootPath = localPath(contentRoot.url)
    val wantedUrls = wantedPaths
      .filter { FileUtil.isAncestor(rootPath, it, false) }
      .map { VfsUtilCore.pathToUrl(it) }
      .toSet()
    val managed = contentRoot.sourceRoots.filter { isManagedRoot(it) }
    val present = managed.map { it.url.url }.toSet()
    return Diff(managed.filter { it.url.url !in wantedUrls }, wantedUrls.filter { it !in present })
  }

  private fun generatedRoot(url: VirtualFileUrl) =
    SourceRootEntity(url, SOURCE_ROOT_TYPE, NonPersistentEntitySource) {
      javaSourceRoots = listOf(JavaSourceRootPropertiesEntity(generated = true, packagePrefix = "", entitySource = NonPersistentEntitySource))
    }

  /** Whether this service added the root: non-persistent and generated. A generated root the user marked is persisted and stays the user's. */
  private fun isManagedRoot(root: SourceRootEntity): Boolean =
    root.entitySource == NonPersistentEntitySource && root.javaSourceRoots.any { it.generated }

  private fun localPath(url: VirtualFileUrl): String = url.url.removePrefix("file://")
}
