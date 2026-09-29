package com.intellij.plugins.haxe.v2

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
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner
import com.intellij.plugins.haxe.v2.display.HaxeCompilerCaches

/**
 * Registers the v2 build file watcher so changed hxml/project.xml files show the
 * platform's floating "reload" icon, wired to the v2 sync pipeline. Also gives
 * freshly opened folders their initial setup: source/excluded roots from the
 * build files they contain, and — when the choice is unambiguous — the active
 * build file.
 */
class HaxeV2ProjectActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    val projectAware = HaxeBuildFilesProjectAware(project)
    val tracker = ExternalSystemProjectTracker.getInstance(project)
    tracker.register(projectAware)
    tracker.activate(projectAware.projectId)

    HaxeSourceRootsInitializer.initialize(project)
    initializeActiveBuildFile(project)
    applyEffectiveSdks(project)
    rehydrateWhenTrusted(project)
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

  /** A project with exactly ONE build file and no active file gets it activated; ambiguity stays with the user. */
  private suspend fun initializeActiveBuildFile(project: Project) {
    val store = HaxeActiveBuildFileStore.getInstance(project)
    if (!store.activeFilePath.isNullOrBlank()) return
    val buildFiles = smartReadAction(project) {
      ModuleManager.getInstance(project).modules.flatMap { HaxeBuildFileScanner.scan(it) }
    }
    val single = buildFiles.singleOrNull() ?: return
    store.setActiveFile(single.file().path)
  }
}
