package com.intellij.plugins.haxe.v2.buildtools.libraries

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Applies resolved haxelib classpaths to the workspace model: one module-level
 * [LibraryEntity] per haxelib (managed-name prefixed) plus a [LibraryDependency]
 * on the owning module.
 */
@Service(Service.Level.PROJECT)
class HaxeLibraryWorkspace(private val project: Project, private val scope: CoroutineScope) {

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HaxeLibraryWorkspace = project.service()
  }

  /**
   * Replaces every module's managed "haxelib: *" libraries with its resolved
   * name→classpaths set, in one atomic model update. [onFinished] runs on the
   * EDT afterwards.
   */
  fun applyAllAsync(resolvedByModuleName: Map<String, Map<String, List<String>>>, onFinished: Runnable?) {
    scope.launch {
      val workspaceModel = WorkspaceModel.getInstance(project)
      val urlManager = workspaceModel.getVirtualFileUrlManager()
      workspaceModel.update("Haxe haxelib library sync") { builder ->
        resolvedByModuleName.forEach { (moduleName, libraries) ->
          applyModule(builder, urlManager, moduleName, libraries)
        }
      }
      if (onFinished != null) {
        withContext(Dispatchers.EDT) { onFinished.run() }
      }
    }
  }

  private fun applyModule(
    builder: MutableEntityStorage,
    urlManager: VirtualFileUrlManager,
    moduleName: String,
    libraries: Map<String, List<String>>,
  ) {
    val moduleId = ModuleId(moduleName)
    val module = builder.resolve(moduleId) ?: return
    val tableId = LibraryTableId.ModuleLibraryTableId(moduleId)

    // stale managed libraries: both the entity and the module's dependency on it
    builder.entities(LibraryEntity::class.java)
      .filter { it.tableId == tableId && it.name.startsWith(HaxeLibrarySync.MANAGED_PREFIX) }
      .toList()
      .forEach { builder.removeEntity(it) }

    val newDependencies = libraries.map { (name, classpaths) ->
      val roots = classpaths.flatMap { classpath ->
        val url = urlManager.getOrCreateFromUrl(VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(classpath)))
        listOf(LibraryRoot(url, LibraryRootTypeId.COMPILED), LibraryRoot(url, LibraryRootTypeId.SOURCES))
      }
      builder.addEntity(LibraryEntity(name, tableId, roots, module.entitySource))
      LibraryDependency(LibraryId(name, tableId), false, DependencyScope.COMPILE)
    }

    builder.modifyModuleEntity(module) {
      dependencies.removeAll { dependency ->
        dependency is LibraryDependency
        && dependency.library.tableId == tableId
        && dependency.library.name.startsWith(HaxeLibrarySync.MANAGED_PREFIX)
      }
      dependencies.addAll(newDependencies)
    }
  }
}
