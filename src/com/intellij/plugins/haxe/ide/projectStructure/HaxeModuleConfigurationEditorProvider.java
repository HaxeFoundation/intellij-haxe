package com.intellij.plugins.haxe.ide.projectStructure;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.plugins.haxe.ide.HaxeModuleType;
import com.intellij.plugins.haxe.ide.projectStructure.ui.HaxeContentEntriesEditor;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeModuleConfigurationEditorProvider implements ModuleConfigurationEditorProvider {
  public ModuleConfigurationEditor[] createEditors(final ModuleConfigurationState state) {
    Module module = state.getRootModel().getModule();
    if (ModuleType.get(module) != HaxeModuleType.getInstance()) {
      return ModuleConfigurationEditor.EMPTY;
    }
    return new ModuleConfigurationEditor[]{new HaxeContentEntriesEditor(state)};
  }
}
