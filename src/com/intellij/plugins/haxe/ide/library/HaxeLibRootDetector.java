package com.intellij.plugins.haxe.ide.library;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.ui.RootDetector;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLibRootDetector extends RootDetector {
  protected HaxeLibRootDetector() {
    super(OrderRootType.SOURCES, false, HaxeBundle.message("sources.root.detector.name"));
  }

  @NotNull
  @Override
  public Collection<VirtualFile> detectRoots(@NotNull VirtualFile rootCandidate, @NotNull ProgressIndicator progressIndicator) {
    List<VirtualFile> result = new ArrayList<VirtualFile>();
    collectRoots(rootCandidate, result, progressIndicator);
    return result;
  }

  public static void collectRoots(VirtualFile file, List<VirtualFile> result, @Nullable ProgressIndicator progressIndicator) {
    if (progressIndicator != null) {
      progressIndicator.checkCanceled();
    }
    if (!file.isDirectory() || file.getFileSystem() instanceof JarFileSystem) return;
    if (progressIndicator != null) {
      progressIndicator.setText2(file.getPresentableUrl());
    }

    if (file.findChild(".current") != null) {
      for (VirtualFile child : file.getChildren()) {
        if (child.isDirectory() && containsHaxeFiles(child)) {
          result.add(child);
        }
      }
      return;
    }

    for (VirtualFile child : file.getChildren()) {
      collectRoots(child, result, progressIndicator);
    }
  }

  private static boolean containsHaxeFiles(VirtualFile file) {
    boolean result = false;
    for (VirtualFile child : file.getChildren()) {
      if (child.isDirectory()) {
        result = result || containsHaxeFiles(child);
      }
      else {
        result = result || HaxeFileType.DEFAULT_EXTENSION.equalsIgnoreCase(child.getExtension());
      }
    }
    return result;
  }
}
