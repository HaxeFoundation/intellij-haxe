/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeConfiguration;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    createHelloWorldIfEligible(module);

    model.commit();
  }

  private void createHelloWorldIfEligible(Module module) {
    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    VirtualFile[] srcDirs = rootManager.getSourceRoots();
    VirtualFile[] rootDirs = rootManager.getContentRoots();
    if(rootDirs.length != 1 || srcDirs.length != 1) {
      return;
    }

    VirtualFile root = rootDirs[0];
    VirtualFile src = srcDirs[0];

    if(src.getChildren().length != 0) {
      return;
    }
    for(VirtualFile item:root.getChildren()) {
      if(item.getExtension() == "hxml") {
        return;
      }
    }


    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          VirtualFile mainHx = src.createChildData(this, "Main.hx");
          String mainHxSource = "class Main {\n"
            + "    static public function main() {\n"
            + "        trace(\"Hello, world!\");\n"
            + "    }\n"
            + "}\n";
          mainHx.setBinaryContent(mainHxSource.getBytes(StandardCharsets.UTF_8));

          VirtualFile buildHxml = root.createChildData(this, "build.hxml");
          String buildHxmlSource = "-cp src\n"
            + "-D analyzer-optimize\n"
            + "-main Main\n"
            + "--interp";
          buildHxml.setBinaryContent(buildHxmlSource.getBytes(StandardCharsets.UTF_8));

          createDefaultRunConfiguration(module, buildHxml.getPath());

          FileEditorManager editorManager = FileEditorManager.getInstance(module.getProject());
          editorManager.openFile(mainHx, true);

        } catch (IOException e) {
        }
      }
    });
  }

  private void createDefaultRunConfiguration(Module module, String buildHxmlPath) {
    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    settings.setHaxeTarget(HaxeTarget.INTERP);
    settings.setBuildConfig(HaxeConfiguration.HXML.asBuildConfigValue());
    settings.setHxmlPath(buildHxmlPath);
    RunManager manager = RunManager.getInstance(module.getProject());
    HaxeRunConfigurationType configType = HaxeRunConfigurationType.getInstance();
    if(manager.getConfigurationsList(configType).isEmpty()) {
      ConfigurationFactory factory = configType.getConfigurationFactories()[0];
      HaxeApplicationConfiguration config = (HaxeApplicationConfiguration)factory.createTemplateConfiguration(module.getProject());
      config.setName("Execute");
      config.setModule(module);
      RunnerAndConfigurationSettings runSettings = manager.createConfiguration(config, factory);
      manager.addConfiguration(runSettings, false);
      manager.setSelectedConfiguration(runSettings);
    }
  }
}
