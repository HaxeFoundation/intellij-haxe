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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Manage the module lifecycle, primarily for class path info.
 */
public class HaxelibModuleManager implements com.intellij.openapi.module.ModuleComponent {

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxelibManager");
  static {
    LOG.setLevel(Level.DEBUG);
  }

  Module mMyModule = null;
  public HaxelibModuleManager(Module myModule) {
    mMyModule = myModule;
  }

  static int debugQueueCounter = 0;
  @Override
  public void projectOpened() {
    debugQueueCounter++;
    final Project project = mMyModule.getProject();
    LOG.debug("Project opened event (" + debugQueueCounter + ") for " + project);

    HaxelibProjectUpdater.getInstance().openProject(mMyModule.getProject());
  }

  @Override
  public void projectClosed() {
    LOG.debug("Project closed event for module " + mMyModule.getName());

    HaxelibProjectUpdater.getInstance().closeProject(mMyModule.getProject());
  }

  @Override
  public void moduleAdded() {
    LOG.debug("Module added event for " + mMyModule.getName());
  }

  @Override
  public void initComponent() {
    LOG.debug("initComponent() for module " + mMyModule.getName());
  }

  @Override
  public void disposeComponent() {
    LOG.debug("disposeComponent() for module " + mMyModule.getName());
  }

  @NotNull
  @Override
  public String getComponentName() {
    return this.getClass().getCanonicalName();//"com.intellij.plugins.haxe.haxelib.HaxelibManager";
  }

}
