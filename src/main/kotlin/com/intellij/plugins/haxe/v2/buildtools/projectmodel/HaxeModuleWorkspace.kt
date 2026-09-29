package com.intellij.plugins.haxe.v2.buildtools.projectmodel

import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildConfigListener
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.GeneralModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creates and removes plain v2 modules from the Haxe tool window: a project
 * subfolder holding a haxe project becomes a real module (content root = the
 * folder), which is what makes it a container in the tree with its own SDK,
 * language level, build files and resolve scope. Nesting is fine — the
 * platform assigns files to the INNERMOST content root, so the folder
 * automatically leaves its parent module. Removal deletes only the module
 * entity; files on disk stay.
 */
@Service(Service.Level.PROJECT)
class HaxeModuleWorkspace(private val project: Project, private val scope: CoroutineScope) {

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HaxeModuleWorkspace = project.service()
  }

  /** Registers [directory] as a module named after it. No-op when the name is taken. */
  fun addModuleAsync(directory: VirtualFile) {
    scope.launch {
      val workspaceModel = WorkspaceModel.getInstance(project)
      val url = directory.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
      val name = directory.name
      workspaceModel.update("Add Haxe module") { builder ->
        if (builder.resolve(ModuleId(name)) != null) return@update
        // JPS entity source so the module persists as a regular .iml
        val source = LegacyBridgeJpsEntitySourceFactory.getInstance(project).createEntitySourceForModule(url, null)
        builder.addEntity(ModuleEntity(name, listOf(ModuleSourceDependency), source) {
          type = ModuleTypeId(GeneralModuleType.TYPE_ID)
          contentRoots = listOf(ContentRootEntity(url, emptyList(), source))
        })
      }
      // same first-open setup fresh projects get: source roots from the
      // module's build files, and its effective SDK applied to the entity
      HaxeSourceRootsInitializer.initialize(project)
      HaxeToolPathResolver.effectiveSdkName(project, name)?.let { sdkName ->
        HaxeModuleSdkApplier.getInstance(project).applyAsync(name, sdkName)
      }
      fireChanged()
    }
  }

  /** Removes the module entity (files stay on disk) and drops its v2 per-container state. */
  fun removeModuleAsync(moduleName: String) {
    scope.launch {
      WorkspaceModel.getInstance(project).update("Remove Haxe module") { builder ->
        builder.resolve(ModuleId(moduleName))?.let { builder.removeEntity(it) }
      }
      HaxeEnvironmentStore.getInstance(project).clearContainer(moduleName)
      fireChanged()
    }
  }

  private suspend fun fireChanged() {
    withContext(Dispatchers.EDT) {
      if (!project.isDisposed) {
        project.messageBus.syncPublisher(HaxeBuildConfigListener.TOPIC).buildConfigurationChanged()
      }
    }
  }
}
