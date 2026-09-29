package com.intellij.plugins.haxe.v2.wizard

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore
import java.io.IOException

/**
 * The generation steps every template shares: module creation (same
 * {@link HaxeModuleBuilder} the language wizard uses), file writing, and the
 * v2 store wiring that makes the tool window open configured — the active
 * build file and, for lime-family projects, the selected target.
 */
internal object HaxeTemplateScaffold {

  /** Creates the module under base name/path; returns the content root path, or null when the wizard lacks base data. */
  fun createModule(project: Project, step: NewProjectWizardStep): String? {
    val base = step.baseData ?: return null
    val contentRoot = contentRootOf(step) ?: return null

    HaxeSdkType.getInstance().ensureSdk()

    val builder = HaxeModuleBuilder()
    builder.name = base.name
    builder.contentEntryPath = contentRoot
    builder.moduleFilePath = "$contentRoot/${base.name}${ModuleFileType.DOT_DEFAULT_EXTENSION}"
    builder.commit(project).firstOrNull() ?: return null
    return contentRoot
  }

  /**
   * Writes the template's files (paths relative to the content root, parent
   * dirs created). When {@code buildFileName} names one of them, that file
   * becomes the project's active build file and its container's compile
   * command, with the target selection stored when {@code targetId} is given.
   */
  fun writeAndRegister(project: Project,
                       contentRoot: String,
                       files: Map<String, String>,
                       buildFileName: String? = null,
                       targetId: String? = null) {
    WriteAction.runAndWait<IOException> {
      val root = VfsUtil.createDirectoryIfMissing(contentRoot) ?: return@runAndWait
      var buildFile: VirtualFile? = null
      for ((relativePath, content) in files) {
        val fileName = relativePath.substringAfterLast('/')
        val directory = relativePath.substringBeforeLast('/', "")
        val parent = if (directory.isEmpty()) root else VfsUtil.createDirectoryIfMissing(root, directory)
        val file = parent.findOrCreateChildData(this, fileName)
        VfsUtil.saveText(file, content)
        if (fileName == buildFileName) buildFile = file
      }

      val registered = buildFile ?: return@runAndWait
      val activeStore = HaxeActiveBuildFileStore.getInstance(project)
      if (activeStore.activeFilePath == null) {
        activeStore.setActiveFile(registered.path)
      }
      if (targetId != null) {
        HaxeTargetSelectionStore.getInstance(project).setSelectedTargetId(registered, targetId)
      }
      // a fresh project should build out of the box: default the container's
      // Compile command to the generated build file's default action
      // (plain `haxe file.hxml` / `lime build <target>`)
      val containerId = HaxeContainers.containerIdFor(project, registered)
      val environment = HaxeEnvironmentStore.getInstance(project)
      if (environment.getCompileCommand(containerId) == null) {
        environment.setCompileCommand(containerId, HaxeEnvironmentStore.CompileCommand(registered.path, null, ""))
      }
    }
  }

  /**
   * Creates the project-local haxelib repository: an empty {@code .haxelib}
   * directory in the content root — the haxelib binary uses it as the
   * repository for every command run inside the project (what
   * {@code haxelib newrepo} creates). Excluded from the module (its content
   * is installed libraries, not sources); a .gitkeep keeps the otherwise
   * empty directory alive when the project is a git repository.
   */
  fun createLocalHaxelibRepo(project: Project, step: NewProjectWizardStep, gitEnabled: Boolean) {
    val contentRoot = contentRootOf(step) ?: return
    WriteAction.runAndWait<IOException> {
      val root = VfsUtil.createDirectoryIfMissing(contentRoot) ?: return@runAndWait
      val repo = VfsUtil.createDirectoryIfMissing(root, ".haxelib")
      if (gitEnabled) {
        repo.findOrCreateChildData(this, ".gitkeep")
      }
      excludeFromModule(project, repo)
    }
  }

  private fun contentRootOf(step: NewProjectWizardStep): String? =
    step.baseData?.let { base -> "${base.path}/${base.name}" }

  private fun excludeFromModule(project: Project, folder: VirtualFile) {
    val module = ModuleUtilCore.findModuleForFile(folder, project) ?: return
    ModuleRootModificationUtil.updateModel(module) { model ->
      val owningEntry = model.contentEntries.firstOrNull { entry ->
        entry.file?.let { contentRoot -> VfsUtilCore.isAncestor(contentRoot, folder, false) } == true
      }
      owningEntry?.addExcludeFolder(folder)
    }
  }
}
