package com.intellij.plugins.haxe.v2.editor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import com.intellij.plugins.haxe.HaxeBundle;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;

/** Registers {@link HaxeResolveContextWidget}; the widget hides itself for files no Haxe build contains. */
public final class HaxeResolveContextWidgetFactory extends StatusBarEditorBasedWidgetFactory {

  @Override
  public @NotNull String getId() {
    return HaxeResolveContextWidget.ID;
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeBundle.message("haxe.resolve.context.widget.name");
  }

  @Override
  public @NotNull StatusBarWidget createWidget(@NotNull Project project, @NotNull CoroutineScope scope) {
    return new HaxeResolveContextWidget(project, scope);
  }
}
