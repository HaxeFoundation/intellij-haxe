/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

public class HaxeModuleModel {
  private Module module;
  private HaxeProjectModel project;

  public HaxeModuleModel(Module module, HaxeProjectModel project) {
    this.module = module;
    this.project = project;
  }

  public List<HaxeSourceRootModel> getRoots() {
    ArrayList<HaxeSourceRootModel> out = new ArrayList<HaxeSourceRootModel>();
    for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(module).recursively().withoutSdk().exportedOnly().sources().getRoots()) {
      out.add(new HaxeSourceRootModel(project.getProject(), sourceRoot));
    }
    return out;
  }

  public String getName() {
    return module.getName();
  }

  public Module getModule() {
    return module;
  }

  public HaxeProjectModel getProject() {
    return project;
  }

  static public HaxeModuleModel fromElement(PsiElement element) {
    return new HaxeModuleModel(ModuleUtilCore.findModuleForPsiElement(element), HaxeProjectModel.fromElement(element));
  }
}
