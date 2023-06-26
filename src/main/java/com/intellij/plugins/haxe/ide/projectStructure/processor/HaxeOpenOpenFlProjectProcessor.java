package com.intellij.plugins.haxe.ide.projectStructure.processor;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectFileDetectionUtil;
import icons.HaxeIcons;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@CustomLog
public class HaxeOpenOpenFlProjectProcessor extends HaxeProjectProcessor {
  @NotNull
  @Override
  public String[] getSupportedExtensions() {
    return new String[]{"xml"};
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return HaxeIcons.OPENFL_LOGO;
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    return super.canOpenProject(file) && HaxeProjectFileDetectionUtil.isOpenFLProject(file);
  }
}
