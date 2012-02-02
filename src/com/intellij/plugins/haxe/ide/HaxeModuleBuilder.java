package com.intellij.plugins.haxe.ide;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import org.jetbrains.annotations.NotNull;

public class HaxeModuleBuilder extends JavaModuleBuilder implements SourcePathsBuilder, ModuleBuilderListener {
  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {
    addListener(this);
    super.setupRootModel(modifiableRootModel);
  }

  public void moduleCreated(@NotNull Module module) {
  }

  @Override
  public ModuleType getModuleType() {
    return HaxeModuleType.getInstance();
  }

  @Override
  public boolean isSuitableSdk(Sdk sdk) {
    return sdk.getSdkType() == HaxeSdkType.getInstance();
  }
}
