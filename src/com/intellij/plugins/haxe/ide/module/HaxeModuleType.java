/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectJdkForModuleStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;

import javax.swing.*;

public class HaxeModuleType extends ModuleType<HaxeModuleBuilder> {
  private static final String MODULE_TYPE_ID = "HAXE_MODULE";

  public HaxeModuleType() {
    super(MODULE_TYPE_ID);
  }

  public static HaxeModuleType getInstance() {
    return (HaxeModuleType)ModuleTypeManager.getInstance().findByID(MODULE_TYPE_ID);
  }

  @Override
  public String getName() {
    return HaxeBundle.message("haxe.module.type.name");
  }

  @Override
  public String getDescription() {
    return HaxeBundle.message("haxe.module.type.description");
  }

  @Override
  public Icon getBigIcon() {
    return icons.HaxeIcons.Haxe_24;
  }

  @Override
  public Icon getNodeIcon(boolean isOpened) {
    return icons.HaxeIcons.Haxe_16;
  }

  @Override
  public HaxeModuleBuilder createModuleBuilder() {
    return new HaxeModuleBuilder();
  }


  public ModuleWizardStep[] createWizardSteps(final WizardContext wizardContext,
                                              final HaxeModuleBuilder moduleBuilder,
                                              final ModulesProvider modulesProvider) {
    return new ModuleWizardStep[]{new ProjectJdkForModuleStep(wizardContext, HaxeSdkType.getInstance()) {
      public void updateDataModel() {
        super.updateDataModel();
        moduleBuilder.setModuleJdk(getJdk());
      }
    }};
  }
}
