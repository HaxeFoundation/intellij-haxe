/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * copyright 2017-2018 Eric Bishton
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
public class HaxeCompilerMessage {

  /**
   * Message severity.  A local enum (rather than the platform's CompilerMessageCategory) is
   * used because this class is also loaded inside the external JPS build process, whose
   * classpath does not contain the IDE's compiler-openapi classes.  IDE-side consumers map
   * this to CompilerMessageCategory; the JPS builder maps it to BuildMessage.Kind.
   */
  public enum Category {
    ERROR,
    WARNING,
    INFORMATION
  }

  private final Category category;
  private final String message;
  private final String path;
  private final int line;
  private final int column;

  public HaxeCompilerMessage(Category category, String errorMessage, String path, int line, int column) {
    this.category = category;
    this.message = errorMessage;
    this.path = path;
    this.line = line;
    this.column = column;
  }

  public Category getCategory() {
    return category;
  }

  public String getMessage() {
    return message;
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

  public boolean isInformationalMessage() {
    return Category.INFORMATION.equals(category);
  }

  public boolean isErrorMessage() {
    return Category.ERROR.equals(category);
  }

  public boolean isWarningMessage() {
    return Category.WARNING.equals(category);
  }

  @Nullable
  public static HaxeCompilerMessage create(@NotNull String rootPath, final String message) {
    return create(rootPath, message, true);
  }

    @Nullable
    public static HaxeCompilerMessage create(@NotNull String rootPath,
                                             final String message,
                                             boolean checkExistence)
    {
        Matcher m;

        // Trim the trailing newline, if any.
        String trimmed = message.trim();

        // Library (\S+) (is not installed.*)
        if ((m = pLibraryNotInstalled.matcher(trimmed)).matches()) {
            return new HaxeCompilerMessage(Category.ERROR,
                                         "Library " + m.group(1).trim() +
                                         " " +
                                         m.group(2).trim(), null, -1, -1);
        }

        String rawPath = null, rawLine, rawColumn, text;

        // ([^:]+):([\\d]+): characters ([\\d]+)-[\\d]+ :(.*)
        if ((m = pColumnError.matcher(trimmed)).matches()) {
            rawPath = m.group(1);
            rawLine = m.group(2);
            rawColumn = m.group(3);
            text = m.group(4).trim();
        }
        // ([^:]+):([\\d]+): lines [\\d]+-[\\d]+ :(.*)
        else if ((m = pLineError.matcher(trimmed)).matches()) {
            rawPath = m.group(1);
            rawLine = m.group(2);
            rawColumn = "-1";
            text = m.group(3).trim();
        }
        // ([^:]*)Error:(.*)
        else if ((m = pBareError.matcher(trimmed)).matches()) {
          String msg = buildGenericErrorMessage(m.group(1).trim(), m.group(2).trim());
          return new HaxeCompilerMessage(Category.ERROR,
                                       msg, null, -1, -1);
        }
        // ([^:]+) : (.+)  Keep this pattern *last* because it's the most generic
        // and the least useful to users.  There are a number of messages that
        // match the expression that are not errors.  Those we try to ignore.
        // Windows file paths don't have spaces around the colon, so should not
        // match the pattern.
        else if ((m = pGenericError.matcher(trimmed)).matches()) {
          String error = m.group(1).trim();
          if (matchesInformationalPattern(error)) {
            // Don't trim the message for information.  (Spaces are meaningful in the compiler banners.)
            return new HaxeCompilerMessage(Category.INFORMATION,
                                         message, null, -1, -1);
          }

          String msg = buildGenericErrorMessage(m.group(1).trim(), m.group(2).trim());
          // File-less compiler warnings (no source location) fall through to
          // this generic-error branch.  The Haxe compiler emits them in a
          // couple of shapes, e.g.:
          //   ((unknown)) Warning : (WDeprecatedDefine) The flash target ...
          //   (unknown) : Warning : (WDeprecatedDefine) The flash target ...
          // Both forms contain a "Warning :" token, so detect that and report
          // them as warnings instead of misclassifying them as errors.
          if (pFilelessWarning.matcher(trimmed).matches()) {
            return new HaxeCompilerMessage(Category.WARNING,
                                         msg, null, -1, -1);
          }
          return new HaxeCompilerMessage(Category.ERROR,
                                       msg, null, -1, -1);
        }

        // Anything that doesn't match error patterns is purely informational
        else {
          // Don't trim the message for information.  (Spaces are meaningful in the compiler banners.)
          return new HaxeCompilerMessage(Category.INFORMATION,
                                         message, null, -1, -1);
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
          return new HaxeCompilerMessage(Category.WARNING,
                                       text, filePath, line, column);
        }
        else {
          return new HaxeCompilerMessage(Category.ERROR,
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

    private static boolean matchesInformationalPattern(String message) {
      return pGeneratingStatusMessage.matcher(message).matches()
          || mInformationalMessages.contains(message)
          || pCompilingStatusMessage.matcher(message).matches();
    }

    static Pattern pLibraryNotInstalled = Pattern.compile
        ("Library (\\S+) (is not installed.*)");
    static Pattern pBareError = Pattern.compile("([^:]*)Error:(.*)", Pattern.CASE_INSENSITIVE);
    static Pattern pColumnError =
        Pattern.compile("(.+?):([\\d]+): characters ([\\d]+)-[\\d]+ :(.*)");
    static Pattern pLineError =
        Pattern.compile("(.+?):([\\d]+): lines [\\d]+-[\\d]+ :(.*)");

    // Unfortunately, the Haxe compiler doesn't always mark its error lines with
    // a useful "Warning" or "Error" prefix.  However, the common error output (main.ml)
    // uses the pattern "%s : %s".
    static Pattern pGenericError = Pattern.compile("(.+?) : (.+)");
    // Matches lines that contain a "Warning :" marker from the Haxe compiler.
    // Used to identify file-less warnings which would otherwise fall through
    // to the generic-error branch and be misclassified as errors.  Handles
    // both shapes: "((unknown)) Warning : ..." and "(unknown) : Warning : ...".
    static Pattern pFilelessWarning = Pattern.compile(".*\\bWarning\\s*:.*");

    // These are a few well-known informational patterns that should NOT be marked
    // as errors.  Keeping this up to date will always be an arms race.
    static HashSet<String> mInformationalMessages = new HashSet<String>();
    static {
      String[] nonErrors = { "Defines", "Classpath", "Classes found", "Display file", "Using default windows compiler",
        // Hxcpp 3.3 message lines:
        "- Compile", "-  - Link", "- Link"
      };
      mInformationalMessages.addAll(Arrays.asList(nonErrors));
    }
    static Pattern pGeneratingStatusMessage = Pattern.compile("Generating (.+)");
    static Pattern pCompilingStatusMessage = Pattern.compile("- Compiling (.+)");
}
