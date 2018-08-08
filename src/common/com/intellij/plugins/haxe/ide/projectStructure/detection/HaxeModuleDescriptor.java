/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2018 AS3Boyan
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
package com.intellij.plugins.haxe.ide.projectStructure.detection;

import com.intellij.ide.util.importProject.ModuleDescriptor;
import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeConfiguration;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.projectStructure.HXMLData;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

public class HaxeModuleDescriptor extends ModuleDescriptor {
  @Nullable
  private String hxml;

  HaxeModuleDescriptor(File file, ModuleType type, Collection<? extends DetectedSourceRoot> collection) {
    super(file, type, collection);
  }

  @Override
  public void updateModuleConfiguration(Module module, ModifiableRootModel model) {
    super.updateModuleConfiguration(module, model);
    setupCompilationSettings(module, model);
  }

  private void setupCompilationSettings(Module module, ModifiableRootModel model) {
    if(hxml == null) {
      return;
    }

    Project project = module.getProject();
    for(VirtualFile root:model.getContentRoots()) {
      if(hasCompilationTarget(project, root.getPath(), hxml)) {
        HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
        settings.setBuildConfig(HaxeConfiguration.HXML.asBuildConfigValue());
        settings.setHxmlPath(getPath(root.getPath(), hxml));
        //TODO: Proper implementation of running the output of every Haxe target.
        settings.setHaxeTarget(HaxeTarget.INTERP);
        break;
      }
    }
  }

  /**
   * Check if specified hxml file contains the compilation target settings. (e.g. `-js` flag)
   */
  static private boolean hasCompilationTarget(Project project, String root, String hxml) {
    try {
      List<HXMLData> dataList = HXMLData.load(project, root, hxml);
      for(HXMLData data:dataList) {
        if(data.hasTarget()) {
          return true;
        }
      }
    }
    catch (HXMLData.HXMLDataException e) {
      //e.printStackTrace();
    }
    return false;
  }

  @Nullable
  public String getHxml() {
    return hxml;
  }

  public void setHxml(@Nullable String hxml) {
    this.hxml = hxml;
  }

  private static String getPath(String dir, String path) {
    return Paths.get(path).isAbsolute() ? path : Paths.get(dir, path).toString();
  }
}
