/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import org.jetbrains.annotations.NotNull;

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
    model.commit();
  }
}
