package com.intellij.plugins.haxe.ide;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeIcons;

import javax.swing.*;

public class HaxeModuleType extends ModuleType<HaxeModuleBuilder> {
  public static final String MODULE_TYPE_ID = "HAXE_MODULE";

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
    return HaxeBundle.message("haxe.module.type.name");
  }

  @Override
  public HaxeModuleBuilder createModuleBuilder() {
    return new HaxeModuleBuilder();
  }

  @Override
  public Icon getBigIcon() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  @Override
  public Icon getNodeIcon(boolean isOpened) {
    return HaxeIcons.HAXE_ICON_16x16;
  }
}
