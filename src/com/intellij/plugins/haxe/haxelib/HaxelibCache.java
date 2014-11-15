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
package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;

import java.util.List;

/**
 * Created by as3boyan on 15.11.14.
 */
public class HaxelibCache {
  protected static HaxelibCache instance = null;

  public List<String> getAvailableHaxelibs() {
    return availableHaxelibs;
  }

  public List<String> getLocalHaxelibs() {
    return localHaxelibs;
  }

  protected static List<String> availableHaxelibs = null;
  protected static List<String> localHaxelibs = null;

  public HaxelibCache() {
    load();
  }

  public static HaxelibCache getInstance() {
    if (instance == null) {
      instance = new HaxelibCache();
    }

    return instance;
  }

  private void load() {
    Module haxeModule = null;
    if (availableHaxelibs == null) {
      Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
      for (Project project : openProjects) {
        for (Module module : ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance())) {
          haxeModule = module;
          break;
        }
      }

      localHaxelibs = HaxelibManager.getInstalledLibraries(haxeModule);
      availableHaxelibs = HaxelibManager.getAvailableLibraries(haxeModule);
      availableHaxelibs.removeAll(localHaxelibs);
    }
  }
}
