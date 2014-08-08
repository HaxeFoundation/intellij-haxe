package com.intellij.plugins.haxe.hxml;

import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Created by eliasku on 8/8/14.
 */
public class HXMLFileTypeFactory extends FileTypeFactory {
  public void createFileTypes(@NotNull FileTypeConsumer consumer) {
    consumer.consume(
      HXMLFileType.INSTANCE,
      new ExtensionFileNameMatcher(HXMLFileType.DEFAULT_EXTENSION));
  }
}