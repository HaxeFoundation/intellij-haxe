package com.intellij.plugins.haxe;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeFileType extends LanguageFileType {
  public static final HaxeFileType HAXE_FILE_TYPE = new HaxeFileType();
  public static final Language HAXE_LANGUAGE = HAXE_FILE_TYPE.getLanguage();

  @NonNls
  public static final String DEFAULT_EXTENSION = "hx";

  private HaxeFileType() {
    super(new HaxeLanguage());
  }

  @NotNull
  @NonNls
  public String getName() {
    return "haXe";
  }

  @NonNls
  @NotNull
  public String getDescription() {
    return "haXe Files";
  }

  @NotNull
  @NonNls
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  public Icon getIcon() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  public boolean isJVMDebuggingSupported() {
    return false;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, byte[] content) {
    return CharsetToolkit.UTF8;
  }
}
