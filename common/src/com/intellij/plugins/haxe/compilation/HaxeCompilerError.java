/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerError {
  private final CompilerMessageCategory category;
  private final String errorMessage;
  private final String path;
  private final int line;
  private final int column;

  public HaxeCompilerError(CompilerMessageCategory category, String errorMessage, String path, int line, int column) {
    this.category = category;
    this.errorMessage = errorMessage;
    this.path = path;
    this.line = line;
    this.column = column;
  }

  public CompilerMessageCategory getCategory() {
    return category;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getPath() {
    return path;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  @Nullable
  public static HaxeCompilerError create(@NotNull String rootPath, final String message) {
    return create(rootPath, message, true);
  }

  @Nullable
  public static HaxeCompilerError create(@NotNull String rootPath, final String message, boolean checkExistence) {
    final int pathSeparationIndex = message.indexOf(' ');
    if (pathSeparationIndex < 1) {
      return null;
    }
    String pathAndLine = message.substring(0, pathSeparationIndex - 1);
    final int lineSeparationIndex = pathAndLine.lastIndexOf(':');
    int line = -1;
    try {
      line = (lineSeparationIndex == -1) ? -1 : Integer.parseInt(pathAndLine.substring(lineSeparationIndex + 1));
    }
    catch (NumberFormatException ignored) {
    }

    final String path = (lineSeparationIndex == -1) ? pathAndLine : pathAndLine.substring(0, lineSeparationIndex);

    final int errorSeparationIndex = message.indexOf(':', pathSeparationIndex);
    CompilerMessageCategory category = CompilerMessageCategory.ERROR;
    String errorMessage = "";
    if (errorSeparationIndex != -1) {
      errorMessage = message.substring(errorSeparationIndex + 1).trim();
      if (errorMessage.startsWith("Warning : ")) {
        errorMessage = errorMessage.substring("Warning : ".length()).trim();
        category = CompilerMessageCategory.WARNING;
      }
    }

    String charsOrLines;
    if (errorSeparationIndex != -1) {
      charsOrLines = message.substring(pathSeparationIndex + 1, errorSeparationIndex);
    }
    else {
      charsOrLines = message.substring(pathSeparationIndex + 1);
    }

    int column = -1;
    if (charsOrLines.startsWith("characters")) {
      try {
        final int columnSeparationIndex = charsOrLines.indexOf('-');
        String columnstr = charsOrLines.substring("characters".length() + 1, columnSeparationIndex);
        column = Integer.parseInt(columnstr);
      }
      catch (NumberFormatException ignored) {
      }
    }

    String filePath = FileUtil.toSystemIndependentName(path);
    if (!FileUtil.isAbsolute(path)) {
      filePath = rootPath + "/" + filePath;
    }

    if (checkExistence && !(new File(FileUtil.toSystemDependentName(filePath)).exists())) {
      return null;
    }

    return new HaxeCompilerError(category, errorMessage, filePath, line, column);
  }
}
