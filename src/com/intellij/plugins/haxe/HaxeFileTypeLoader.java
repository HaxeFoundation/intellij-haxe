package com.intellij.plugins.haxe;

import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class HaxeFileTypeLoader extends FileTypeFactory {
  public void createFileTypes(@NotNull FileTypeConsumer consumer) {
    consumer.consume(
      HaxeFileType.HAXE_FILE_TYPE,
      new ExtensionFileNameMatcher(HaxeFileType.DEFAULT_EXTENSION));
  }
}
