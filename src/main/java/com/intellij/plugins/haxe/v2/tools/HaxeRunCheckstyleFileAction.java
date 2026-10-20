package com.intellij.plugins.haxe.v2.tools;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolConfigs;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Lints the file through the checkstyle haxelib, with the config found up the tree. */
public final class HaxeRunCheckstyleFileAction extends HaxeFileToolAction {

  @Override
  @NotNull
  String configName() {
    return HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME;
  }

  @Override
  @NotNull
  String toolHaxelib() {
    return HaxeToolConfigs.CHECKSTYLE_HAXELIB;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    runFileTool(e, List.of(), null);
  }
}
