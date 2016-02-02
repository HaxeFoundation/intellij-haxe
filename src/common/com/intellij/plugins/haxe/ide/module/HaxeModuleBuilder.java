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

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.module.impl.HaxeModuleSettingsBaseImpl;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.psi.PsiManager;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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

    final Project project = module.getProject();
    List<Pair<String, String>> sourcePaths = getSourcePaths();
    String srcPath = null;

    if ((sourcePaths.size() > 0)) {
      srcPath = sourcePaths.get(0).getFirst();
      VirtualFile dir = VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(srcPath));

      if (dir != null) {
        try {
          VirtualFile mainClassFile = dir.findFileByRelativePath("Main.hx");
          if (mainClassFile == null) {
            HaxeFileTemplateUtil
              .createClass("Main", "", PsiManager.getInstance(project).findDirectory(dir), "HaxeMainClass",
                           null);
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      HaxeModuleSettings moduleSettings = HaxeModuleSettings.getInstance(module);
      moduleSettings.setMainClass("Main");
      moduleSettings.setArguments("");
      moduleSettings.setNmeFlags("");
      moduleSettings.setHaxeTarget(HaxeTarget.NEKO);
      moduleSettings.setExcludeFromCompilation(false);
      moduleSettings.setBuildConfig(HaxeModuleSettingsBaseImpl.USE_PROPERTIES);
      moduleSettings.setOutputFileName("Main.n");
      String outputFolder = PathUtil.toSystemIndependentName(project.getBasePath() + "/out/production");
      moduleSettings.setOutputFolder(outputFolder);
      //String releaseFolder = PathUtil.toSystemIndependentName(outputFolder + "/release");
      //boolean mkdirs = new File(releaseFolder).mkdirs();

      String url = VfsUtil.pathToUrl(outputFolder);
      model.setCompilerOutputPath(url);

      assert dir != null;
      final VirtualFile file = dir.findFileByRelativePath("Main.hx");
      assert (file != null);

      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          FileEditorManager.getInstance(project).openFile(file, true);
        }
      });

      ConfigurationFactory[] factories =
        ConfigurationTypeUtil.findConfigurationType(HaxeRunConfigurationType.class).getConfigurationFactories();

      if ((factories.length > 0)) {
        RunConfiguration configuration = factories[0].createTemplateConfiguration(project);
        RunManager manager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings runnerAndConfigurationSettings = manager.createConfiguration(configuration, factories[0]);
        RunConfiguration configuration1 = runnerAndConfigurationSettings.getConfiguration();
        if (configuration1 instanceof HaxeApplicationConfiguration) {
          HaxeApplicationConfiguration haxeApplicationConfiguration = (HaxeApplicationConfiguration)configuration1;
          haxeApplicationConfiguration.setModule(module);
        }
        manager.addConfiguration(runnerAndConfigurationSettings, false);
        manager.setSelectedConfiguration(runnerAndConfigurationSettings);
      }


    }

    model.commit();
  }
}
