/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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
package com.intellij.plugins.haxe.openfl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.ide.HaxeCompilerCompletionContributor;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Created by as3boyan on 30.01.2016.
 */
public class ProjectFileListener implements com.intellij.openapi.module.ModuleComponent {

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.openfl.ProjectFileListener");
  static {
    LOG.setLevel(Level.DEBUG);
  }

  Module mMyModule = null;
  VirtualFileAdapter myAdapter = null;
  public ProjectFileListener(Module myModule) {
    mMyModule = myModule;
  }

  @Override
  public void projectOpened() {
    HaxeModuleSettings moduleSettings = HaxeModuleSettings.getInstance(mMyModule);
    String openFLPath = moduleSettings.getOpenFLPath();
    setOpenFLPath(openFLPath);
  }

  public void setOpenFLPath(String openFLPath) {
    removeListener();

    if (openFLPath.isEmpty()) {
      //default project file name
      openFLPath = "project.xml";
    }

    final VirtualFile projectXmlFile = VirtualFileManager.getInstance().findFileByUrl(openFLPath);
    if (projectXmlFile != null) {
      myAdapter = new VirtualFileAdapter() {
        @Override
        public void contentsChanged(@NotNull VirtualFileEvent event) {
          super.contentsChanged(event);
          if (event.getFile().equals(projectXmlFile)) {
            HaxeCompilerCompletionContributor.clearOpenFLDisplayArguments();
          }
        }
      };

      VirtualFileManager.getInstance().addVirtualFileListener(myAdapter);
    }
  }

  @Override
  public void projectClosed() {
    removeListener();
  }

  public void removeListener() {
    if (myAdapter != null) {
      VirtualFileManager.getInstance().removeVirtualFileListener(myAdapter);
    }
  }

  @Override
  public void moduleAdded() {

  }

  @Override
  public void initComponent() {

  }

  @Override
  public void disposeComponent() {

  }

  @NotNull
  @Override
  public String getComponentName() {
    return this.getClass().getCanonicalName();
  }
}
