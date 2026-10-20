package com.intellij.plugins.haxe.v2.toolwindow.tree;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/** The icon an action shows wherever it is listed: the tool window rows, the Run before list and its popup. */
public final class HaxeActionIcons {

  private HaxeActionIcons() {
  }

  /** Actions that only produce output get the build hammer; ones that run something keep the play icon. */
  @NotNull
  public static Icon forAction(@NotNull String actionName) {
    boolean buildAction = actionName.equalsIgnoreCase("build")
                          || actionName.equalsIgnoreCase("compile")
                          || actionName.equalsIgnoreCase("clean");
    return buildAction ? AllIcons.Actions.Compile : AllIcons.Actions.Execute;
  }
}
