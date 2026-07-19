package com.intellij.plugins.haxe.ide.statusbar;

import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the Haxe target status bar widget for projects containing Haxe modules.
 */
public class HaxeTargetStatusBarWidgetFactory implements StatusBarWidgetFactory {

  @Override
  public @NotNull String getId() {
    return HaxeTargetStatusBarWidget.WIDGET_ID;
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeBundle.message("haxe.target.widget.display.name");
  }

  @Override
  public boolean isAvailable(@NotNull Project project) {
    return !ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance()).isEmpty();
  }

  @Override
  public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
    return new HaxeTargetStatusBarWidget(project);
  }
}
