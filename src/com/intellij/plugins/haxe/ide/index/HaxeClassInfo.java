package com.intellij.plugins.haxe.ide.index;

import com.intellij.plugins.haxe.HaxeComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassInfo {
  @NotNull private final String value;
  @Nullable private final HaxeComponentType type;

  public HaxeClassInfo(@NotNull String name, @Nullable HaxeComponentType type) {
    value = name;
    this.type = type;
  }

  @NotNull
  public String getValue() {
    return value;
  }

  @Nullable
  public HaxeComponentType getType() {
    return type;
  }

  @Nullable
  public Icon getIcon() {
    return type == null ? null : type.getIcon();
  }
}
