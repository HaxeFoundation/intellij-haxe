package com.intellij.plugins.haxe.v2.buildtools.projectmodel

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.plugins.haxe.v2.buildsystem.*
import java.io.File

/**
 * First-open setup for plain modules that contain haxe build files but have no
 * source roots configured yet (a folder opened directly, without a wizard):
 * the build files' classpaths become source roots and their output locations
 * become excluded folders. Modules that already have source roots are left
 * alone — the user's (or the migration's) configuration wins.
 */
object HaxeSourceRootsInitializer {

  // matches how the platform serializes java-like source roots; the v2 wizard's src/ uses the same
  private val SOURCE_ROOT_TYPE = SourceRootTypeId("java-source")

  internal data class RootsPlan(val moduleName: String, val sourceDirs: List<String>, val excludeDirs: List<String>)

  suspend fun initialize(project: Project) {
    val plans = smartReadAction(project) { plan(project) }
    applyPlans(project, plans)
  }

  internal suspend fun applyPlans(project: Project, plans: List<RootsPlan>) {
    if (plans.isEmpty()) return
    val workspaceModel = WorkspaceModel.getInstance(project)
    val urlManager = workspaceModel.getVirtualFileUrlManager()
    workspaceModel.update("Haxe source roots from build files") { builder ->
      for (plan in plans) {
        val module = builder.resolve(ModuleId(plan.moduleName)) ?: continue
        for (contentRoot in module.contentRoots) {
          applyToContentRoot(builder, contentRoot, plan, urlManager)
        }
      }
    }
  }

  /** Whether the module still has no source roots — the state the first-open setup applies to. */
  fun moduleHasNoSourceRoots(project: Project, moduleName: String): Boolean {
    val entity = WorkspaceModel.getInstance(project).currentSnapshot.resolve(ModuleId(moduleName)) ?: return false
    return entity.contentRoots.none { it.sourceRoots.isNotEmpty() }
  }

  /**
   * The build file's classpath directories not covered by an existing source
   * root of the module (equal to or under one). A module whose MAIN sources
   * are configured can still have a tests build whose separate source tree
   * is unmarked — breakpoint resolution needs it marked, so the add/mark
   * offers gate on this instead of on the module having no roots at all.
   */
  fun uncoveredSourceDirs(project: Project, moduleName: String, buildFile: HaxeBuildFile): List<String> {
    val plan = planFor(project, moduleName, listOf(buildFile)) ?: return emptyList()
    val entity = WorkspaceModel.getInstance(project).currentSnapshot.resolve(ModuleId(moduleName)) ?: return emptyList()
    val existingRoots = entity.contentRoots
      .flatMap { it.sourceRoots }
      .map { localPath(it.url) }
    return plan.sourceDirs.filter { dir -> existingRoots.none { root -> FileUtil.isAncestor(root, dir, false) } }
  }

  private fun plan(project: Project): List<RootsPlan> {
    val plans = mutableListOf<RootsPlan>()
    for (module in ModuleManager.getInstance(project).modules) {
      if (!moduleHasNoSourceRoots(project, module.name)) continue

      val buildFiles = HaxeBuildFileScanner.scan(module)
      if (buildFiles.isEmpty()) continue

      planFor(project, module.name, buildFiles)?.let { plans += it }
    }
    return plans
  }

  /** The build files' roots contribution: existing classpath dirs as sources, output locations as excludes. */
  internal fun planFor(project: Project, moduleName: String, buildFiles: List<HaxeBuildFile>): RootsPlan? {
    val sourceDirs = linkedSetOf<String>()
    val excludeDirs = linkedSetOf<String>()
    for (buildFile in buildFiles) {
      val directory = buildFile.file().parent ?: continue
      // every --next section contributes: the module holds all sections' sources
      // and outputs, whichever section the tool window currently follows
      val sections = HaxeBuildFileInspector.inspectSections(project, buildFile)
      for (info in sections) {
        for (classpath in info.classpaths()) {
          resolveDirectory(directory, classpath)?.let { sourceDirs += it }
        }
        if (buildFile.type() == HaxeBuildFileType.HXML) {
          info.targetOutput()?.let { output ->
            // the output FILE's directory, whether or not it exists yet
            val parent = File(FileUtil.toSystemIndependentName(output)).parent
            excludeDirs += resolvePath(directory, parent ?: output)
          }
        }
      }
      when (buildFile.type()) {
        HaxeBuildFileType.OPENFL, HaxeBuildFileType.LIME, HaxeBuildFileType.NMML ->
          ProjectXmlParser.parseAppPath(VfsUtilCore.loadText(buildFile.file()))?.let {
            excludeDirs += resolvePath(directory, it)
          }
        else -> {}
      }
    }
    if (sourceDirs.isEmpty() && excludeDirs.isEmpty()) return null
    return RootsPlan(moduleName, sourceDirs.toList(), excludeDirs.toList())
  }

  /** An existing directory resolved against the build file's directory, or null. */
  private fun resolveDirectory(base: VirtualFile, relative: String): String? {
    val resolved = base.findFileByRelativePath(FileUtil.toSystemIndependentName(relative.trim()))
    return if (resolved != null && resolved.isDirectory) resolved.path else null
  }

  /** A path resolved against the build file's directory; output dirs may not exist yet. */
  private fun resolvePath(base: VirtualFile, relative: String): String {
    val clean = FileUtil.toSystemIndependentName(relative.trim())
    return if (FileUtil.isAbsolute(clean)) clean else base.path + "/" + clean
  }

  private fun localPath(url: VirtualFileUrl): String = url.url.removePrefix("file://")

  private fun applyToContentRoot(
    builder: MutableEntityStorage,
    contentRoot: ContentRootEntity,
    plan: RootsPlan,
    urlManager: VirtualFileUrlManager,
  ) {
    val rootPath = localPath(contentRoot.url)
    val existingSources = contentRoot.sourceRoots.map { it.url.url }.toSet()
    val existingExcludes = contentRoot.excludedUrls.map { it.url.url }.toSet()
    val source = contentRoot.entitySource

    val newSources = plan.sourceDirs
      .filter { FileUtil.isAncestor(rootPath, it, false) }
      .map { urlManager.getOrCreateFromUrl(VfsUtilCore.pathToUrl(it)) }
      .filter { it.url !in existingSources }
    val newExcludes = plan.excludeDirs
      .filter { FileUtil.isAncestor(rootPath, it, true) }
      .map { urlManager.getOrCreateFromUrl(VfsUtilCore.pathToUrl(it)) }
      .filter { it.url !in existingExcludes }
    if (newSources.isEmpty() && newExcludes.isEmpty()) return

    builder.modifyContentRootEntity(contentRoot) {
      sourceRoots = sourceRoots + newSources.map { SourceRootEntity(it, SOURCE_ROOT_TYPE, source) }
      excludedUrls = excludedUrls + newExcludes.map { ExcludeUrlEntity(it, source) }
    }
  }
}
