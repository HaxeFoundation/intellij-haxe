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
package com.intellij.plugins.haxe.ide.module;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.psi.PsiManager;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeModuleBuilder extends JavaModuleBuilder implements SourcePathsBuilder, ModuleBuilderListener {
  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {
    addListener(this);
    super.setupRootModel(modifiableRootModel);
  }

  @Override
  public ModuleType getModuleType() {
    return HaxeModuleType.getInstance();
  }

  @Override
  public boolean isSuitableSdkType(SdkTypeId sdk) {
    return sdk == HaxeSdkType.getInstance();
  }

  @Override
  public void moduleCreated(@NotNull Module module) {
    final CompilerModuleExtension model = (CompilerModuleExtension)CompilerModuleExtension.getInstance(module).getModifiableModel(true);
    model.setCompilerOutputPath(model.getCompilerOutputUrl());
    model.inheritCompilerOutputPath(false);

    Project project = module.getProject();
    List<Pair<String, String>> sourcePaths = getSourcePaths();
    String srcPath = null;

    if ((sourcePaths.size() > 0)) {
      srcPath = sourcePaths.get(0).getFirst();
      VirtualFile dir = VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(srcPath));

      if (dir != null) {
        try {
          HaxeFileTemplateUtil
            .createClass("Main", "", PsiManager.getInstance(project).findDirectory(dir), "HaxeMainClass",
                         null);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      HaxeModuleSettings.getInstance(module).setMainClass("Main");

      assert dir != null;
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(dir.getCanonicalPath() + "/Main.hx");
      assert (file != null);
      FileEditorManager.getInstance(project).openFile(file, true);
    }

    model.commit();
  }
}
