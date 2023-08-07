package com.intellij.plugins.haxe.ide.actions.haxelib;

import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class HaxeActionGroup extends DefaultActionGroup {

  public HaxeActionGroup() {
    getTemplatePresentation().setHideGroupIfEmpty(true);
  }
}
