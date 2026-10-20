package com.intellij.plugins.haxe.ide.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.toolWindow.haxelib.HaxelibExplorerPanel;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

/**
 * The Haxelib tool window: a permanent Explorer tab (browse/search the
 * library catalog and the installed state), with install-command console
 * tabs appended beside it as they run.
 */
public class HaxelibConsoleWindowFactory implements ToolWindowFactory, DumbAware {

  /** The window's plugin.xml id — keep in sync with the {@code <toolWindow>} registration. */
  public static final String TOOL_WINDOW_ID = "Haxelib";

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    HaxelibExplorerPanel explorer = new HaxelibExplorerPanel(project);
    Content content = toolWindow.getContentManager().getFactory()
      .createContent(explorer, HaxeBundle.message("haxelib.explorer.tab.title"), false);
    content.setCloseable(false);
    content.setDisposer(explorer);
    toolWindow.getContentManager().addContent(content);
  }
}
