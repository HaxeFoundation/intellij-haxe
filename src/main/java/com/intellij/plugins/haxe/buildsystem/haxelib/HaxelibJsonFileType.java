package com.intellij.plugins.haxe.buildsystem.haxelib;

import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxelibJsonFileType extends JsonFileType {

  public static final HaxelibJsonFileType INSTANCE = new HaxelibJsonFileType();
  public static final String DEFAULT_EXTENSION = "json";

  public HaxelibJsonFileType() {
    super(JsonLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return HaxeBundle.message("haxelib.project.file.name");
  }

  @NotNull
  @Override
  public String getDescription() {
    return HaxeBundle.message("haxelib.project.file.description");
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  @Override
  public Icon getIcon() {
    return icons.HaxeIcons.HAXELIB_JSON;
  }


}
