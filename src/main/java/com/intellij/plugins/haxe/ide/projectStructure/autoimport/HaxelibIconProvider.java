package com.intellij.plugins.haxe.ide.projectStructure.autoimport;

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxelibIconProvider implements ExternalSystemIconProvider {
  @NotNull
  @Override
  public Icon getReloadIcon() {
    return HaxeIcons.HAXE_RELOAD;
  }
}
