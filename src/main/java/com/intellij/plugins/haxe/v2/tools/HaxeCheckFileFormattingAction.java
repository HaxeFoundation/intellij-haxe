package com.intellij.plugins.haxe.v2.tools;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolConfigs;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Checks the file's formatting through the formatter haxelib without writing. */
public final class HaxeCheckFileFormattingAction extends HaxeFileToolAction {

  @Override
  @NotNull
  String configName() {
    return HaxeToolConfigs.FORMATTER_CONFIG_NAME;
  }

  @Override
  @NotNull
  String toolHaxelib() {
    return HaxeToolConfigs.FORMATTER_HAXELIB;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    runFileTool(e, List.of("--check"), null);
  }
}
