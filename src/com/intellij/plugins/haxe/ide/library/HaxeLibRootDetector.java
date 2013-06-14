/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.library;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.ui.RootDetector;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
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
  protected HaxeLibRootDetector(OrderRootType rootType, String presentableRootTypeName) {
    super(rootType, false, presentableRootTypeName);
  }

  @NotNull
  @Override
  public Collection<VirtualFile> detectRoots(@NotNull VirtualFile rootCandidate, @NotNull ProgressIndicator progressIndicator) {
    List<VirtualFile> result = new ArrayList<VirtualFile>();
    collectRoots(rootCandidate, result, progressIndicator);
    return result;
  }

  public static void collectRoots(VirtualFile file, final List<VirtualFile> result, @Nullable final ProgressIndicator progressIndicator) {
    if (file.getFileSystem() instanceof JarFileSystem) {
      return;
    }
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@NotNull VirtualFile file) {
        if (progressIndicator != null) {
          progressIndicator.checkCanceled();
        }
        if (!file.isDirectory()) return false;
        if (progressIndicator != null) {
          progressIndicator.setText2(file.getPresentableUrl());
        }

        if (file.findChild(".current") != null) {
          for (VirtualFile child : file.getChildren()) {
            if (child.isDirectory() && containsHaxeFiles(child)) {
              result.add(child);
            }
          }
          return false;
        }

        return true;
      }
    });
  }

  private static boolean containsHaxeFiles(final VirtualFile dir) {
    final VirtualFileVisitor.Result result = VfsUtilCore.visitChildrenRecursively(dir, new VirtualFileVisitor() {
      @NotNull
      @Override
      public Result visitFileEx(@NotNull VirtualFile file) {
        return !file.isDirectory() && HaxeFileType.DEFAULT_EXTENSION.equalsIgnoreCase(file.getExtension()) ? skipTo(dir) : CONTINUE;
      }
    });
    return result.skipToParent != null;
  }
}
