package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.IdeDefineNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on an IDE define entry: edits its name/value. A REMOVE
 * entry stays a REMOVE entry unless a value is entered, so renaming a
 * struck-through row does not silently turn it into a SET.
 */
public final class HaxeEditDefineAction extends HaxeSelectedNodeAction<IdeDefineNode> {

  public HaxeEditDefineAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, IdeDefineNode.class, HaxeBundle.message("haxe.toolwindow.edit.define"));
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull IdeDefineNode define) {
    String initial = define.value().isEmpty() ? define.name() : define.name() + "=" + define.value();
    DefinePrompt.DefineInput input = DefinePrompt.show(project, initial);
    if (input == null) return;

    HaxeIdeOverridesStore store = HaxeIdeOverridesStore.getInstance(project);
    String buildFilePath = define.buildFile().file().getPath();
    if (!input.name().equals(define.name())) {
      store.removeDefine(buildFilePath, define.name());
    }
    boolean keepsRemove = define.effect() == DefineEffect.REMOVE && input.value().isEmpty();
    if (keepsRemove) {
      store.putUndefine(buildFilePath, input.name());
    }
    else {
      store.putDefine(buildFilePath, input.name(), input.value());
    }
  }
}
