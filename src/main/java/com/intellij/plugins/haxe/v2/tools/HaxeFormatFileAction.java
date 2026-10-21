package com.intellij.plugins.haxe.v2.tools;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolConfigs;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Formats the file in place through the formatter haxelib. */
public final class HaxeFormatFileAction extends HaxeFileToolAction {

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
    VirtualFile file = haxeFile(e);
    if (file == null) return;
    // the tool rewrites the file on disk behind the editor's back
    Runnable refresh = () -> file.refresh(true, false);
    runFileTool(e, List.of(), refresh);
  }
}
