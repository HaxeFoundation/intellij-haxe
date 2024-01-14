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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Manage the module lifecycle, primarily for class path info.
 */
@Service
@CustomLog
public final class HaxelibModuleManagerService implements ProjectManagerListener, ModuleListener, Disposable {

  static {      // Take this out when finished debugging.
    log.setLevel(LogLevel.DEBUG);
  }

  public HaxelibModuleManagerService() { }


  //For project open see HaxelibProjectStartActivity
  @Override
  public void projectClosed(@NotNull Project project) {
    log.debug("Project closed event for  " + project.getName());

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      HaxelibProjectUpdater.getInstance().closeProject(project);
    }
  }

  public void modulesAdded(@NotNull Project project, @NotNull List<? extends Module> modules) {
    for (Module module : modules) {
      log.debug("Module added event for " + module.getName());
      ExternalSystemProjectTracker.getInstance(project).scheduleProjectRefresh();
    }
  }


  @Override
  public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
    HaxelibCacheManager.removeInstance(module);
    HaxeDefineDetectionManager.getInstance(project).removeDetectedDefinitions(module);
    HaxelibProjectUpdater.getInstance().findProjectTracker(project).moduleRemoved(module);
    ExternalSystemProjectTracker.getInstance(project).scheduleProjectRefresh();
  }

  @Override
  public void dispose() {
    log.debug("disposeComponent()" );
  }

}
