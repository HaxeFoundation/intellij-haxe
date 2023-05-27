package com.intellij.plugins.haxe.ide.projectStructure.processor;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectFileDetectionUtil;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeOpenNMEProjectProcessor extends HaxeProjectProcessor {
  @NotNull
  @Override
  public String[] getSupportedExtensions() {
    return new String[]{"nmml"};
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return HaxeIcons.NMML_LOGO;
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    return HaxeProjectFileDetectionUtil.isNMMLProject(file);
  }
}
