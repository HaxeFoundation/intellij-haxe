package com.intellij.plugins.haxe.v2.editor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContexts;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import org.jetbrains.annotations.NotNull;

/**
 * Carries the resolve context across navigation: a Haxe file opened from
 * another Haxe file's editor takes that file's context when the context
 * reaches it ({@link HaxeResolveContexts#follow}), so the user sees the
 * target as the origin sees it. Only a newly opened editor follows, never a
 * switch to an open tab: the selection change names the origin, the editor's
 * opening marks the open as new. The platform delivers the two in either
 * order, so whichever arrives first leaves a marker on the file for the
 * other. The file is then re-lexed, since its tree may have been parsed
 * under another context.
 */
// TODO: multiverse: EditorContextManager.setEditorContext on the opened editor replaces the follow
public final class HaxeResolveContextFollower implements FileEditorManagerListener {

  /** The file selected before this one, recorded while this one's editor may still be opening. */
  private static final Key<VirtualFile> NAVIGATED_FROM = Key.create("haxe.resolve.context.navigated.from");
  /** The editor was created; the selection change naming the origin is still due. */
  private static final Key<Boolean> OPENED = Key.create("haxe.resolve.context.opened");

  private final Project project;

  public HaxeResolveContextFollower(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    VirtualFile to = event.getNewFile();
    VirtualFile from = event.getOldFile();
    if (to == null || from == null || from.equals(to) || !isHaxe(to)) return;
    if (to.getUserData(OPENED) != null) {
      to.putUserData(OPENED, null);
      follow(from, to);
    }
    else {
      to.putUserData(NAVIGATED_FROM, from);
    }
  }

  @Override
  public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    if (!isHaxe(file)) return;
    VirtualFile from = file.getUserData(NAVIGATED_FROM);
    if (from != null) {
      file.putUserData(NAVIGATED_FROM, null);
      follow(from, file);
    }
    else {
      file.putUserData(OPENED, Boolean.TRUE);
    }
  }

  @Override
  public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    file.putUserData(NAVIGATED_FROM, null);
    file.putUserData(OPENED, null);
  }

  private void follow(@NotNull VirtualFile from, @NotNull VirtualFile to) {
    // the tabs restored while the project opens are not navigation
    if (!StartupManager.getInstance(project).postStartupActivityPassed() || !isHaxe(from)) return;
    boolean changed = HaxeReadActions.compute(() -> HaxeResolveContexts.getInstance(project).follow(from, to));
    if (changed) {
      HaxeDefineContextService.getInstance(project).relexFile(to);
    }
  }

  private static boolean isHaxe(@NotNull VirtualFile file) {
    return FileTypeRegistry.getInstance().isFileOfType(file, HaxeFileType.INSTANCE);
  }
}
