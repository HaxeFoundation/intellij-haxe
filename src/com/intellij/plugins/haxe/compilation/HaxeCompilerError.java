package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerError {
  private final String errorMessage;
  private final String url;
  private final int line;

  public HaxeCompilerError(String errorMessage, String url, int line) {
    this.errorMessage = errorMessage;
    this.url = url;
    this.line = line;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getUrl() {
    return url;
  }

  public int getLine() {
    return line;
  }

  @Nullable
  public static HaxeCompilerError create(final String message) {
    final int index = message.indexOf(' ');
    if (index < 1) {
      return null;
    }
    String error = message.substring(0, index - 1);
    final int semicolonIndex = error.lastIndexOf(':');
    int line = -1;
    try {
      line = semicolonIndex == -1 ? -1 : Integer.parseInt(error.substring(semicolonIndex + 1));
    }
    catch (NumberFormatException ignored) {
    }

    final String path = semicolonIndex == -1 ? error : error.substring(0, semicolonIndex);

    final int semicolonIndex2 = message.indexOf(':', index);
    String errorMessage = null;
    if (semicolonIndex2 != -1) {
      errorMessage = message.substring(semicolonIndex2 + 1);
    }

    return new HaxeCompilerError(errorMessage, VfsUtil.getUrlForLibraryRoot(new File(path)), line);
  }
}
