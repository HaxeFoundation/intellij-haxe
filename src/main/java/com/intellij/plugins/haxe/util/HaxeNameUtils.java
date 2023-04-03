/*
 * Copyright 2019-2020 Eric Bishton
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
package com.intellij.plugins.haxe.util;

public class HaxeNameUtils {

  private HaxeNameUtils() {}

  /**
   * Determine the relative file name that a (possibly qualified) class
   * will (should!) be found in.
   *
   * @param className - class name to convert to a file name.
   * @return a file name corresponding to the given class name.  NOTE: The file
   *         is neither checked for existence or whether it contains said class.
   */
  public static String classNameToFileName(String className) {
    String parts[] = className.split("\\.");

    int leaveOff = 0;
    for (int i = parts.length - 2; i > 0; --i) {  // Start at the second to last entry.
      if (isValidClassName(parts[i])) {
        leaveOff++;
      }
    }

    StringBuffer fileName = new StringBuffer();
    for (int i = 0; i < parts.length - leaveOff; i++) {
      if (i > 0) fileName.append(HaxeFileUtil.SEPARATOR);
      fileName.append(parts[i]);
    }
    return fileName.toString();
  }

  public static boolean isValidClassName(String className) {
    // TODO: Add more name detection rules.
    for (int i = 0; i < className.length(); i++) {
      char c = className.charAt(i);
      if (!isValidClassNameChar(c)) {
        return false;
      }
    }
    return Character.isUpperCase(className.charAt(0));
  }

  public static boolean isValidClassNameChar(char c) {
    // TODO: Figure out if non-ascii chars are valid for class names.
    // TODO: Maybe make this faster by using a hash or lookup table.
    return '_' == c || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z' || (c >= '0' && c <= '9'));
  }

  public static boolean isValidIdentifierChar(char c) {
    return isValidClassNameChar(c);
  }
}
