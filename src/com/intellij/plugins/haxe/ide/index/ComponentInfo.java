package com.intellij.plugins.haxe.ide.index;

import com.intellij.plugins.haxe.HaxeComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class ComponentInfo {
  @NotNull private final String packageName;
  @Nullable private final HaxeComponentType type;

  public ComponentInfo(@NotNull String name, @Nullable HaxeComponentType type) {
    packageName = name;
    this.type = type;
  }

  @NotNull
  public String getPackageName() {
    return packageName;
  }

  @Nullable
  public HaxeComponentType getType() {
    return type;
  }

  public Icon getIcon() {
    return type == null ? null : type.getIcon();
  }
}
