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
package com.intellij.plugins.haxe.ide.projectStructure;

import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.projectStructure.ui.HaxeConfigurationEditor;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeModuleConfigurationEditor implements ModuleConfigurationEditor {
  private HaxeConfigurationEditor haxeConfigurationEditor;

  public HaxeModuleConfigurationEditor(ModuleConfigurationState state) {
    haxeConfigurationEditor = new HaxeConfigurationEditor(state.getCurrentRootModel().getModule(), state.getModifiableRootModel().getModuleExtension(
      CompilerModuleExtension.class));
  }

  @Override
  public void saveData() {
  }

  @Override
  public void moduleStateChanged() {
  }

  @Nls
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.module.editor.haxe");
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return haxeConfigurationEditor == null ? null : haxeConfigurationEditor.getMainPanel();
  }

  @Override
  public boolean isModified() {
    return haxeConfigurationEditor != null && haxeConfigurationEditor.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    // FIXME: This is hacky workaround - need to investigate the issue https://github.com/HaxeFoundation/intellij-haxe/issues/728
    if (haxeConfigurationEditor != null) {
      haxeConfigurationEditor.apply();
      haxeConfigurationEditor = null;
    }
  }

  @Override
  public void reset() {
    if (haxeConfigurationEditor != null) {
      haxeConfigurationEditor.reset();
    }
  }

  @Override
  public void disposeUIResources() {
  }
}
