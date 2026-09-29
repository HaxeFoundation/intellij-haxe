package com.intellij.plugins.haxe.v2.buildtools.projectmodel

import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildConfigListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.InheritedSdkDependency
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.jps.entities.sdkId
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Makes the tool window's Environment SDK choice REAL in the project model:
 * the module gets its own SDK dependency, the way a Java module overrides the
 * project SDK. That is what feeds the std roots into the module's resolve
 * scope and satisfies the editor's SDK banner — no Project SDK required, so
 * mixed-language projects keep one SDK per module.
 */
@Service(Service.Level.PROJECT)
class HaxeModuleSdkApplier(private val project: Project, private val scope: CoroutineScope) {

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HaxeModuleSdkApplier = project.service()
  }

  /** No-op for container ids that are not modules (the project-root container). */
  fun applyAsync(moduleName: String, sdkName: String?) {
    scope.launch {
      WorkspaceModel.getInstance(project).update("Haxe module SDK") { builder ->
        val module = builder.resolve(ModuleId(moduleName)) ?: return@update
        builder.modifyModuleEntity(module) {
          // the sdkId setter replaces an explicit SDK but leaves an
          // inherited-SDK marker alone - drop it, inheriting is exactly
          // what a per-module haxe SDK is meant to avoid
          dependencies.removeAll { it == InheritedSdkDependency }
          sdkId = sdkName?.let { SdkId(it, HaxeSdkType.getInstance().name) }
        }
      }
      // UI showing derived state (the tool window's SDK row) refreshes on this
      withContext(Dispatchers.EDT) {
        if (!project.isDisposed) {
          project.messageBus.syncPublisher(HaxeBuildConfigListener.TOPIC).buildConfigurationChanged()
        }
      }
    }
  }
}
