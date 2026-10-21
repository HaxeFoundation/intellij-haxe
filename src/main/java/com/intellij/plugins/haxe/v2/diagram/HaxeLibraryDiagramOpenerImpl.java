package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryDiagramOpener;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the library diagram through the Diagrams plugin's own Show Diagram
 * action, handing it a data context that only the library provider answers
 * to, so the plugin opens that diagram without asking which one.
 */
public final class HaxeLibraryDiagramOpenerImpl implements HaxeLibraryDiagramOpener {

  private static final String SHOW_DIAGRAM_ACTION_ID = "ShowUmlDiagram";
  private static final String PLACE = "HaxeLibraryDiagram";

  @Override
  public void open(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    AnAction showDiagram = ActionManager.getInstance().getAction(SHOW_DIAGRAM_ACTION_ID);
    if (showDiagram == null) return;
    DataContext context = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, project)
      .add(HaxeLibraryDiagramElement.DATA_KEY, HaxeLibraryDiagramElement.buildFile(buildFile.file().getPath()))
      .build();
    AnActionEvent event = AnActionEvent.createEvent(context, null, PLACE, ActionUiKind.NONE, null);
    ActionUtil.performAction(showDiagram, event);
  }
}
