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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.Pair;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeStringUtil {
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
        if (name.length() > 0) {
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
    String out = "";
    try {
      char[] chars = str.toCharArray();
      for (int n = 0; n < chars.length;) {
        char c = chars[n++];
        switch (c) {
          case '\\':
            char c2 = chars[n++];
            switch (c2) {
              case '0': out += '\0'; break;
              case 'n': out += '\n'; break;
              case 'r': out += '\r'; break;
              case 't': out += '\t'; break;
              case 'b': out += '\b'; break;
              case 'x':
              {
                String hex = str.substring(n, n + 2);
                n += 2;
                out += (char)Integer.parseInt(hex, 16);
                break;
              }
              case 'u':
              {
                String hex = str.substring(n, n + 4);
                n += 4;
                out += (char)Integer.parseInt(hex, 16);
                break;
              }
              default:
                break;
            }
            break;
          default:
            out += c;
            break;
        }
      }
    } catch (Throwable t) {

    }
    return out;
  }
}
