package com.intellij.plugins.haxe.v2

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.plugins.haxe.util.HaxeModuleDetection
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildFilesProjectAware
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeModuleSdkApplier
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectSync
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeSourceRootsInitializer
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync
import com.intellij.plugins.haxe.v2.display.HaxeCompilerCaches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Registers the v2 build file watcher so changed hxml/project.xml files show the
 * platform's floating "reload" icon, wired to the v2 sync pipeline. Also gives
 * freshly opened folders their initial setup: source/excluded roots from the
 * build files they contain, and — when the choice is unambiguous — the active
 * build file. Every open of a trusted project syncs its libraries.
 */
class HaxeV2ProjectActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    val projectAware = HaxeBuildFilesProjectAware(project)
    val tracker = ExternalSystemProjectTracker.getInstance(project)
    tracker.register(projectAware)
    tracker.activate(projectAware.projectId)

    HaxeSourceRootsInitializer.initialize(project)
    applyEffectiveSdks(project)
    syncLibrariesWhenTrusted(project)
    rehydrateWhenTrusted(project)
  }

  /**
   * The library list is saved with the project, but what a library sync
   * derives beside it (library defines, the dependency graph, external
   * classpaths) lives for the session only, so every open derives it again.
   * An untrusted project waits for trust instead (see rehydrateWhenTrusted).
   */
  private suspend fun syncLibrariesWhenTrusted(project: Project) {
    if (!HaxeProjectTrust.isTrusted(project)) return
    withContext(Dispatchers.EDT) {
      HaxeLibrarySync.sync(project, null)
    }
  }

  /**
   * An untrusted project opens with every project-code-executing evaluation
   * skipped (see HaxeProjectTrust); granting trust re-runs the same
   * re-hydration the reload icon does, so the project springs to life
   * without a reopen.
   */
  private fun rehydrateWhenTrusted(project: Project) {
    HaxeProjectTrust.whenTrusted(project) {
      HaxeProjectSync.sync(project) {
        HaxeCompilerCaches.clearAndRehighlight(project, "haxe: project trusted")
      }
    }
  }

  /**
   * Every haxe module's EFFECTIVE SDK (environment override, else the Build
   * Tools default) becomes a real module SDK dependency — resolution reads
   * only the project model, so an unapplied choice is a broken resolve scope.
   * Non-haxe modules (a Java module in a mixed project) are never touched.
   */
  private suspend fun applyEffectiveSdks(project: Project) {
    val applier = HaxeModuleSdkApplier.getInstance(project)
    val effectiveByModule = smartReadAction(project) {
      ModuleManager.getInstance(project).modules
        .filter { HaxeModuleDetection.isHaxeModule(it) }
        .associate { it.name to HaxeToolPathResolver.effectiveSdkName(project, it.name) }
    }
    for ((moduleName, sdkName) in effectiveByModule) {
      if (sdkName != null) {
        applier.applyAsync(moduleName, sdkName)
      }
    }
  }
}
