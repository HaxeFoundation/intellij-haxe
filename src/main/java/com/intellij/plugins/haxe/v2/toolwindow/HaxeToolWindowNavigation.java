package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileNavigation;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Jump-to-source targets for tool window rows: build file and action rows open the
 * build file, define rows land on the define's declaration (best effort), library
 * rows select the library under External Libraries in the Project view.
 */
public final class HaxeToolWindowNavigation {

  private HaxeToolWindowNavigation() {
  }

  @Nullable
  public static Navigatable forSelection(@NotNull Project project, @Nullable Object userObject) {
    return switch (userObject) {
      case BuildFileRow row -> fileDescriptor(project, row.buildFile().file());
      case TargetNode targetNode -> fileDescriptor(project, targetNode.buildFile().file());
      case ActionsGroupNode actionsGroup -> pathDescriptor(project, actionsGroup.ownerId());
      case ActionNode actionNode -> pathDescriptor(project, actionNode.ownerId());
      case ProgramNode programNode -> fileDescriptor(project, programNode.buildFile().file());
      case DefineNode defineNode -> new DefineNavigatable(project, defineNode);
      case LibraryNode libraryNode -> new LibraryNavigatable(project, libraryNode.name());
      case null, default -> null;
    };
  }

  @Nullable
  private static Navigatable fileDescriptor(@NotNull Project project, @NotNull VirtualFile file) {
    return file.isValid() ? new OpenFileDescriptor(project, file) : null;
  }

  @Nullable
  private static Navigatable pathDescriptor(@NotNull Project project, @NotNull String path) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    return file == null ? null : fileDescriptor(project, file);
  }

  /** Locates the define's declaration lazily, at navigation time. */
  private record DefineNavigatable(@NotNull Project project, @NotNull DefineNode define) implements Navigatable {

    @Override
    public void navigate(boolean requestFocus) {
      VirtualFile file = define.owner().file();
      if (!file.isValid()) return;
      int offset = HaxeBuildFileNavigation.findDefineOffset(loadText(file), define.owner().type(), define.name());
      new OpenFileDescriptor(project, file, offset).navigate(requestFocus);
    }

    @NotNull
    private static String loadText(@NotNull VirtualFile file) {
      Document document = FileDocumentManager.getInstance().getCachedDocument(file);
      if (document != null) {
        return document.getText();
      }
      try {
        return VfsUtilCore.loadText(file);
      }
      catch (IOException e) {
        return "";
      }
    }

    @Override
    public boolean canNavigate() {
      return define.owner().file().isValid();
    }

    @Override
    public boolean canNavigateToSource() {
      return canNavigate();
    }
  }

  /** Selects the synced haxelib module library's root under External Libraries. */
  private record LibraryNavigatable(@NotNull Project project, @NotNull String libraryName) implements Navigatable {

    @Override
    public void navigate(boolean requestFocus) {
      VirtualFile root = findLibraryRoot();
      if (root == null) return;
      ToolWindow projectView = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
      if (projectView != null) {
        projectView.activate(() -> ProjectView.getInstance(project).select(null, root, requestFocus));
      }
    }

    /** Order-entry walks need a read action - navigate/canNavigate arrive on raw EDT clicks. */
    @Nullable
    private VirtualFile findLibraryRoot() {
      return HaxeReadActions.compute(this::findLibraryRootUnderLock);
    }

    @Nullable
    private VirtualFile findLibraryRootUnderLock() {
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
          if (orderEntry instanceof LibraryOrderEntry libraryEntry
              && HaxeLibrarySync.isManagedEntryFor(libraryEntry.getLibraryName(), libraryName)) {
            VirtualFile[] roots = libraryEntry.getRootFiles(OrderRootType.CLASSES);
            if (roots.length > 0) {
              return roots[0];
            }
          }
        }
      }
      return null;
    }

    @Override
    public boolean canNavigate() {
      return findLibraryRoot() != null;
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }
  }
}
