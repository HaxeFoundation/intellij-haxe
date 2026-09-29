package com.intellij.plugins.haxe.v2.buildtools.projectmodel

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Applies one build file's source/excluded roots to its module on demand —
 * the user-consented counterpart of [HaxeSourceRootsInitializer]'s silent
 * first-open pass, used by the add-build-file actions after the user accepts
 * the offer.
 */
@Service(Service.Level.PROJECT)
class HaxeSourceRootsApplier(private val project: Project, private val scope: CoroutineScope) {

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HaxeSourceRootsApplier = project.service()
  }

  fun applyFromBuildFileAsync(moduleName: String, buildFile: HaxeBuildFile) {
    scope.launch {
      val plan = smartReadAction(project) {
        HaxeSourceRootsInitializer.planFor(project, moduleName, listOf(buildFile))
      } ?: return@launch
      HaxeSourceRootsInitializer.applyPlans(project, listOf(plan))
    }
  }
}
