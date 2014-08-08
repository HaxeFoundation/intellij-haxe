package com.intellij.plugins.haxe.hxml;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class HXMLFileType extends LanguageFileType {

  public static final HXMLFileType INSTANCE = new HXMLFileType();

  @NonNls
  public static final String DEFAULT_EXTENSION = "hxml";

  private HXMLFileType() {
    super(HXMLLanguage.INSTANCE);
  }

  @NotNull
  @NonNls
  public String getName() {
    return HaxeBundle.message("hxml.file.type.name");
  }

  @NonNls
  @NotNull
  public String getDescription() {
    return HaxeBundle.message("hxml.file.type.description");
  }

  @NotNull
  @NonNls
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  public Icon getIcon() {
    return icons.HaxeIcons.Haxe_16;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, byte[] content) {
    return CharsetToolkit.UTF8;
  }
}
