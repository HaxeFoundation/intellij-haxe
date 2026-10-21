package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.modifier.CodeStyleStatusBarUIContributor;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * The code style status bar widget entry shown while a project hxformat.json
 * governs the current file: names the config, opens it, and offers the same
 * opt-out as {@link HaxeCodeStyleSettings#USE_PROJECT_HXFORMAT}.
 */
class HxformatStatusBarContributor implements CodeStyleStatusBarUIContributor {

  private final VirtualFile configFile;

  HxformatStatusBarContributor(@NotNull VirtualFile configFile) {
    this.configFile = configFile;
  }

  @Override
  public boolean areActionsAvailable(@NotNull VirtualFile file) {
    return true;
  }

  @Override
  public AnAction @Nullable [] getActions(@NotNull PsiFile file) {
    Project project = file.getProject();
    AnAction openConfig = DumbAwareAction.create(HaxeCodeStyleBundle.message("hxformat.status.bar.open.config"),
                                                 e -> new OpenFileDescriptor(project, configFile).navigate(true));
    return new AnAction[]{openConfig};
  }

  @Override
  public @Nullable String getTooltip() {
    return HaxeCodeStyleBundle.message("hxformat.status.bar.tooltip", configFile.getPresentableUrl());
  }

  @Override
  public @NotNull String getStatusText(@NotNull PsiFile psiFile) {
    return HaxeCodeStyleBundle.message("hxformat.status.bar.text");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.VSHAXE;
  }

  @Override
  public @Nullable AnAction createDisableAction(@NotNull Project project) {
    String text = HaxeCodeStyleBundle.message("hxformat.status.bar.disable");
    return DumbAwareAction.create(text, e -> HxformatConfigs.getInstance(project).setIntegrationEnabled(false));
  }
}
