package com.intellij.plugins.haxe.v2.buildtools.projectmodel

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener

/**
 * Asks [HaxeGeneratedSourceRoots] for a reconcile whenever the roots may have
 * to change: a build choice (above all the target), a build file on disk or
 * its evaluation, and the generated folders themselves appearing or
 * disappearing, as a build or a clean makes them. Every method only schedules
 * the reconcile, so the thread a listener is called on does not matter.
 */
class HaxeGeneratedSourceRootsTracker(private val project: Project) : HaxeBuildSettingsListener, HaxeBuildEventListener, BulkFileListener {

  override fun anySettingChanged() {
    request()
  }

  override fun buildFileContentChanged(path: String) {
    request()
  }

  override fun buildFileAdded(path: String) {
    request()
  }

  override fun buildFileDeleted(path: String) {
    request()
  }

  override fun evaluationLanded(buildFilePath: String) {
    request()
  }

  /** A library sync runs on every project open, so this is also the first reconcile of a session. */
  override fun librariesSynced() {
    request()
  }

  override fun after(events: List<VFileEvent>) {
    if (project.isDisposed) return
    val roots = HaxeGeneratedSourceRoots.getInstance(project)
    if (events.any { affectsGeneratedFolders(roots, it) }) roots.requestReconcile()
  }

  /** Whether the event creates, deletes, copies, moves or renames a watched folder or one containing it; a clean deletes a whole export directory. */
  private fun affectsGeneratedFolders(roots: HaxeGeneratedSourceRoots, event: VFileEvent): Boolean = when (event) {
    is VFileCreateEvent, is VFileDeleteEvent, is VFileCopyEvent -> roots.affectsGeneratedFolders(event.path)
    is VFileMoveEvent -> affectsEither(roots, event.oldPath, event.newPath)
    is VFilePropertyChangeEvent -> event.propertyName == VirtualFile.PROP_NAME && affectsEither(roots, event.oldPath, event.newPath)
    else -> false
  }

  /** Whether the path before or after a move or rename is a watched folder or contains one. */
  private fun affectsEither(roots: HaxeGeneratedSourceRoots, oldPath: String, newPath: String): Boolean =
    roots.affectsGeneratedFolders(oldPath) || roots.affectsGeneratedFolders(newPath)

  private fun request() {
    if (!project.isDisposed) HaxeGeneratedSourceRoots.getInstance(project).requestReconcile()
  }
}
