package com.intellij.plugins.haxe.nmml;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class NMMLFileTypeFactory extends FileTypeFactory {
  public void createFileTypes(final @NotNull FileTypeConsumer consumer) {
    final NMMLFileType fileType = NMMLFileType.INSTANCE;
    consumer.consume(fileType, fileType.getDefaultExtension());
  }
}
