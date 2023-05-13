/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.util.FileContentUtil;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeUtil {
  public static void reparseProjectFiles(@NotNull final Project project) {
    Task.Backgroundable task = new Task.Backgroundable(project, HaxeBundle.message("haxe.project.reparsing"), false) {
      public void run(@NotNull ProgressIndicator indicator) {
        final Collection<VirtualFile> haxeFiles = new ArrayList<VirtualFile>();
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
          FileBasedIndex.getInstance().iterateIndexableFiles(new ContentIterator() {
            public boolean processFile(VirtualFile file) {
              if (HaxeFileType.INSTANCE == file.getFileType()) {
                haxeFiles.add(file);
              }
              return true;
            }
          }, project, indicator);
        }
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
          public void run() {
            FileContentUtil.reparseFiles(project, haxeFiles, !project.isDefault());
          }
        }, ModalityState.NON_MODAL);
      }
    };
    ProgressManager.getInstance().run(task);
  }
}
