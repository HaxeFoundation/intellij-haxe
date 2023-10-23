/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2019 Eric Bishton
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

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeStringUtil {
  public static final String EMPTY_STRING = "";
  public static final String ELLIPSES = "...";

  /**
   * @see com.intellij.openapi.util.text.StringUtil#getWordsWithOffset
   */
  public static List<Pair<String, Integer>> getWordsWithOffset(String s) {
    List<Pair<String, Integer>> res = new ArrayList<Pair<String, Integer>>();
    s += " ";
    StringBuilder name = new StringBuilder();
    int startInd = -1;
    for (int i = 0; i < s.length(); i++) {
      // fix
      if (!Character.isJavaIdentifierPart(s.charAt(i))) {
        if (!name.isEmpty()) {
          res.add(Pair.create(name.toString(), startInd));
          name.setLength(0);
          startInd = -1;
        }
      }
      else {
        if (startInd == -1) {
          startInd = i;
        }
        name.append(s.charAt(i));
      }
    }
    return res;
  }

  static public String unescapeString(String str) {
    if (str.startsWith("'") || str.startsWith("\"")) {
      return _unescapeString(str.substring(1, str.length() - 1));
    } else {
      return _unescapeString(str);
    }
  }

  static private String _unescapeString(String str) {
    StringBuilder out = new StringBuilder();
      char[] chars = str.toCharArray();
      for (int n = 0; n < chars.length;) {
        char c = chars[n++];
        if (c == '\\') {
          char c2 = chars[n++];
          switch (c2) {
            case '0' -> out.append('\0');
            case 'n' -> out.append('\n');
            case 'r' -> out.append('\r');
            case 't' -> out.append('\t');
            case 'b' -> out.append('\b');
            case 'x' -> {
              String hex = str.substring(n, n + 2);
              n += 2;
              out.append((char)Integer.parseInt(hex, 16));
            }
            case 'u' -> {
              String hex = str.substring(n, n + 4);
              n += 4;
              out.append((char)Integer.parseInt(hex, 16));
            }
            default -> {
            }
          }
        }
        else {
          out.append(c);
        }
      }
    return out.toString();
  }

  public static String join(String separator, CharSequence... elements) {
    // TODO: Replace this function with String.joinHaxeLib when we no longer support Java6/7.
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < elements.length; i++) {
      builder.append(elements[i]);
      if (i < elements.length - 1) {
        builder.append(separator);
      }
    }
    return builder.toString();
  }

  public static String join(String separator, Iterable<? extends CharSequence> elements) {
    // TODO: Replace this function with String.join when we no longer support Java6/7.
    StringBuilder builder = new StringBuilder();
    Iterator<? extends CharSequence> iterator = elements.iterator();
    while (iterator.hasNext()) {
      builder.append(iterator.next());
      if (iterator.hasNext()) {
        builder.append(separator);
      }
    }
    return builder.toString();
  }

  /**
   * Fast splitting.  This outperforms String.split() by a factor of 3x and
   * StringTokenizer-based solutions by ~2x.  Particularly when used with
   * regex meta characters (e.g. ".$|()[{^?*+\\").
   *
   * Successive split characters will add empty strings to the output list.
   *
   * @param s String to parse
   * @param c Character to split on.
   * @return A List containing the constituent strings.
   */
  @NotNull
  public static List<String> split(String s, char c) {
    ArrayList<String> list = new ArrayList<>();

    int len = s.length();
    int current = 0;
    int index = 0;
    while (current < len &&  -1 != (index = s.indexOf(c, current))) {
      list.add(s.substring(current, index));
      current = index + 1;
    }
    if (current < len) {
      list.add(s.substring(current));
    }
    return list;
  }


  public static String stripPrefix(String s, String prefix) {
    if (null == s) return null;
    if (null == prefix) return s;

    if (s.startsWith(prefix)) {
      return s.substring(prefix.length());
    }
    return s;
  }

  public static String stripSuffix(String s, String postfix) {
    if (null == s) return null;
    if (null == postfix) return s;

    if (s.endsWith(postfix)) {
      return s.substring(0, s.length() - postfix.length());
    }
    return s;
  }

  public static String terminateAt(String s, char c) {
    if (null == s) return null;

    int pos = s.indexOf(c);
    if (pos >= 0) {
      return s.substring(0, pos).trim();
    }
    return s;
  }

  /**
   * Shorten a string to a maximum of n characters.
   *
   * @param s - string to limit.
   * @param len - maximum length of string, may be longer that s.length().
   * @return a new String of the maximum length, or s, if it is shorter than len.
   */
  @Nullable
  public static String clip(@Nullable String s, int len) {
    if (len <=0)  return EMPTY_STRING;
    if (null == s || s.length() <= len) return s;

    return s.substring(0, len);
  }

  /**
   * Shorten a string to a maximum of n characters, using a trailing ellipses to
   * denote truncation.  If the requested length is less than the ELLIPSES length (3),
   * then the shortened string has no ellipses appended.
   *
   * @param s - string to limit.
   * @param len - maximum length of string, may be longer than s.len().
   * @return a new String of the maximum length, or s, if it is shorter than len.
   */
  @Nullable
  public static String elide(@Nullable String s, int len) {
    if (len <= ELLIPSES.length()) return clip(s, len);
    if (null == s || s.length() <= len) return s;

    return s.substring(0, len - ELLIPSES.length()) + ELLIPSES;
  }

  /**
   * Elides all characters between the first incidence of startChar and the last
   * incidence of endChar.
   *
   * @param s String to elide.
   * @param startChar Beginning character to start eliding at. Character is kept.
   * @param endChar Last character to end eliding at. Character is kept.
   * @return An elided string.
   */
  @Nullable
  public static String elideBetween(@Nullable String s, char startChar, char endChar) {
    if (null == s) return null;

    int startPos = s.indexOf(startChar);
    int endPos = s.lastIndexOf(endChar);
    if (startPos < endPos - ELLIPSES.length()) {
      StringBuilder b = new StringBuilder(s.substring(0,startPos+1));
      b.append("...");
      b.append(s.substring(endPos));
      return b.toString();
    }
    return s;
  }


  /**
   * Gets a version of the given string with a lower-cased first character.
   * @param s - string to change.
   */
  @Nullable
  public static String toLowerFirst(@Nullable String s) {
    if (null == s) return null;
    if (s.isEmpty()) return s;
    if (!Character.isUpperCase(s.charAt(0))) return s;
    char lower = Character.toLowerCase(s.charAt(0));
    StringBuilder builder = new StringBuilder(lower);
    if (s.length() > 1) {
      builder.append(s.substring(1));
    }
    return builder.toString();
  }
}
