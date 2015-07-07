/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    public static HaxeCompilerError create(@NotNull String rootPath,
                                           final String message,
                                           boolean checkExistence)
    {
        Matcher m;

        // Error: Library ([^\w]+) is not installed(.*)
        if ((m = pLibraryNotInstalled.matcher(message)).matches()) {
            return new HaxeCompilerError(CompilerMessageCategory.WARNING,
                                         "Library " + m.group(1).trim() +
                                         "is not installed " +
                                         m.group(2).trim(), "", -1, -1);
        }

        String rawPath = null, rawLine, rawColumn, text;

        // ([^:]+):([\\d]+): characters ([\\d]+)-[\\d]+ :(.*)
        if ((m = pColumnError.matcher(message)).matches()) {
            rawPath = m.group(1);
            rawLine = m.group(2);
            rawColumn = m.group(3);
            text = m.group(4).trim();
        }
        // ([^:]+):([\\d]+): lines [\\d]+-[\\d]+ :(.*)
        else if ((m = pLineError.matcher(message)).matches()) {
            rawPath = m.group(1);
            rawLine = m.group(2);
            rawColumn = "-1";
            text = m.group(3).trim();
        }
        // ([^:]*)Error:(.*)
        else if ((m = pBareError.matcher(message)).matches()) {
          String msg = buildGenericErrorMessage(m.group(1).trim(), m.group(2).trim());
          return new HaxeCompilerError(CompilerMessageCategory.ERROR,
                                       msg, "", -1, -1);
        }
        // ([^:]+) : (.+)  Keep this pattern *last* because it's the most generic
        // and the least useful to users.  There are a number of messages that
        // match the expression that are not errors.  Those we try to ignore.
        // Windows file paths don't have spaces around the colon, so should not
        // match the pattern.
        else if ((m = pGenericError.matcher(message)).matches()) {
          String error = m.group(1).trim();
          if (isInformationalMessage(error)) {
            return new HaxeCompilerError(CompilerMessageCategory.INFORMATION,
                                         message.trim(), "", -1, -1);
          }

          String msg = buildGenericErrorMessage(m.group(1).trim(), m.group(2).trim());
          return new HaxeCompilerError(CompilerMessageCategory.ERROR,
                                       msg, "", -1, -1);
        }

        // Anything that doesn't match error patterns is purely informational
        else {
            return new HaxeCompilerError(CompilerMessageCategory.INFORMATION,
                                         message.trim(), "", -1, -1);
        }

        // Got a real file error, so handle it

        String filePath = FileUtil.toSystemIndependentName(rawPath);
        if (!FileUtil.isAbsolute(filePath)) {
            filePath = rootPath + "/" + filePath;
        }

        if (checkExistence &&
            !(new File(FileUtil.toSystemDependentName(filePath)).exists())) {
                filePath = "Missing file: " + filePath;
        }

        int line, column;

        try {
            line = Integer.parseInt(rawLine);
        }
        catch (NumberFormatException e) {
            line = -1;
        }

        try {
            column = Integer.parseInt(rawColumn);
        }
        catch (NumberFormatException e) {
            column = -1;
        }

        final String warningStr = "Warning";
        if (0 == text.indexOf(warningStr)) {
          text = text.substring(warningStr.length()).trim();
          final String colonChar = ":";
          if (0 == text.indexOf(colonChar)) {
            text = text.substring(colonChar.length()).trim();
          }
          return new HaxeCompilerError(CompilerMessageCategory.WARNING,
                                       text, filePath, line, column);
        }
        else {
          return new HaxeCompilerError(CompilerMessageCategory.ERROR,
                                       text, filePath, line, column);
        }
    }

    private static String buildGenericErrorMessage(String error, String reason) {
      StringBuilder msg = new StringBuilder();
      String errType = error;
      if (!errType.isEmpty()) {
        msg.append(" (");
        msg.append(errType);
        msg.append(") ");
      }
      msg.append(reason);
      return msg.toString();
    }

    private static boolean isInformationalMessage(String message) {
      return pGeneratingStatusMessage.matcher(message).matches() || mInformationalMessages.contains(message);
    }

    static Pattern pLibraryNotInstalled = Pattern.compile
        ("Error: Library ([\\S]+) is not installed(.*)");
    static Pattern pBareError = Pattern.compile("([^:]*)Error:(.*)", Pattern.CASE_INSENSITIVE);
    static Pattern pColumnError =
        Pattern.compile("([^:]+):([\\d]+): characters ([\\d]+)-[\\d]+ :(.*)");
    static Pattern pLineError =
        Pattern.compile("([^:]+):([\\d]+): lines [\\d]+-[\\d]+ :(.*)");
    // Unfortunately, the Haxe compiler doesn't always mark its error lines with
    // a useful "Warning" or "Error" prefix.  However, the common error output (main.ml)
    // uses the pattern "%s : %s".
    static Pattern pGenericError = Pattern.compile("([^:]+) : (.+)");

    // These are a few well-known informational patterns that should NOT be marked
    // as errors.  Keeping this up to date will always be an arms race.
    static HashSet<String> mInformationalMessages = new HashSet<String>();
    static {
      String[] nonErrors = { "Defines", "Classpath", "Classes found", "Display file" };
      mInformationalMessages.addAll(Arrays.asList(nonErrors));
    }
    static Pattern pGeneratingStatusMessage = Pattern.compile("Generating (.+)");
}
