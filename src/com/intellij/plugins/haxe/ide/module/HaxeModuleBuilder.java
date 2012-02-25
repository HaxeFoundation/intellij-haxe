package com.intellij.plugins.haxe.ide.module;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import org.jetbrains.annotations.NotNull;

public class HaxeModuleBuilder extends JavaModuleBuilder implements SourcePathsBuilder, ModuleBuilderListener {
  private Sdk haxeSdk;

  public void setHaxeSdk(Sdk haxeSdk) {
    this.haxeSdk = haxeSdk;
  }

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {
    addListener(this);

    if (haxeSdk != null) {
      modifiableRootModel.setSdk(haxeSdk);
    }
    super.setupRootModel(modifiableRootModel);
  }

  @Override
  public ModuleType getModuleType() {
    return HaxeModuleType.getInstance();
  }

  @Override
  public boolean isSuitableSdk(Sdk sdk) {
    return sdk.getSdkType() == HaxeSdkType.getInstance();
  }

  @Override
  public void moduleCreated(@NotNull Module module) {
    final CompilerModuleExtension model = (CompilerModuleExtension)CompilerModuleExtension.getInstance(module).getModifiableModel(true);
    model.setCompilerOutputPath(model.getCompilerOutputUrl());
    model.inheritCompilerOutputPath(false);
    model.commit();
  }
}
