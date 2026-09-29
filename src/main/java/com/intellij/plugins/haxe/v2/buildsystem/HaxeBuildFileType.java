package com.intellij.plugins.haxe.v2.buildsystem;

import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The kinds of Haxe build/project files shown in the Haxe tool window tree.
 */
public enum HaxeBuildFileType {
  HXML(HaxeIcons.HAXE_LOGO),
  OPENFL(HaxeIcons.OPENFL_LOGO),
  LIME(HaxeIcons.LIME_LOGO),
  NMML(HaxeIcons.NMML_LOGO),
  /** A lime/openfl project script ({@code class X extends HXProject}) - lime targets and actions apply. */
  HXP_PROJECT(HaxeIcons.LIME_LOGO),
  /** A plain hxp build script (arbitrary Haxe run via {@code haxelib run hxp}) - no lime semantics. */
  HXP_SCRIPT(HaxeIcons.HAXE_LOGO);

  private final Icon icon;

  HaxeBuildFileType(@NotNull Icon icon) {
    this.icon = icon;
  }

  @NotNull
  public Icon getIcon() {
    return icon;
  }
}
