package com.intellij.plugins.haxe.ide.projectStructure.processor;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.trusted.ExternalSystemTrustedProjectDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectFileDetectionUtil;
import icons.HaxeIcons;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

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
    return HaxeProjectFileDetectionUtil.isOpenFLProject(file);
  }
}
