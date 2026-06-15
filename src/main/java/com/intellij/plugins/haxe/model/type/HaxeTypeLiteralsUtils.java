/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model.type;

public class HaxeTypeLiteralsUtils {
  static public int getIntValue(Object value) {
    return (int)getDoubleValue(value);
  }
  static public double getDoubleValue(Object value) {
    if (value instanceof Boolean) return ((Boolean)value) ? 1 : 0;
    if (value instanceof Long) return (Long)value;
    if (value instanceof Integer) return (Integer)value;
    if (value instanceof Double) return (Double)value;
    if (value instanceof Float) return (Float)value;
    return Double.NaN;
  }

  static public boolean getBoolValue(Object value) {
    return getDoubleValue(value) != 0;
  }

  static public Object applyUnaryOperator(Object right, String operator) {
    double rightv = getDoubleValue(right);
    if (operator.equals("-")) return -rightv;
    if (operator.equals("~")) return ~(int)rightv;
    if (operator.equals("!")) return !getBoolValue(right);
    if (operator.equals("")) return rightv;
    throw new RuntimeException("Unsupporteed operator '" + operator + "'");
  }

  static public Object applyBinOperator(Object left, Object right, String operator) {
    if (operator.equals("??")) return left == null ? right : left;

    double leftv = getDoubleValue(left);
    double rightv = getDoubleValue(right);
    if (operator.equals("+")) return leftv + rightv;
    if (operator.equals("-")) return leftv - rightv;
    if (operator.equals("*")) return leftv * rightv;
    if (operator.equals("/")) return leftv / rightv;
    if (operator.equals("%")) return leftv % rightv;
    if (operator.equals("==")) return leftv == rightv;
    if (operator.equals("!=")) return leftv != rightv;
    if (operator.equals("<")) return leftv < rightv;
    if (operator.equals("<=")) return leftv <= rightv;
    if (operator.equals(">")) return leftv > rightv;
    if (operator.equals(">=")) return leftv >= rightv;
    if (operator.equals("<<")) return (int)leftv << (int)rightv;
    if (operator.equals(">>")) return (int)leftv >> (int)rightv;
    if (operator.equals(">>>")) return (int)leftv >>> (int)rightv;
    if (operator.equals("&")) return (int)leftv & (int)rightv;
    if (operator.equals("|")) return (int)leftv | (int)rightv;
    if ( left instanceof  Boolean leftb && right instanceof Boolean  rightb) {
      if (operator.equals("||")) return leftb || rightb;
      if (operator.equals("&&")) return leftb && rightb;
    }
    throw new RuntimeException("Unsupporteed operator '" + operator + "'");
  }

  //TODO mlo: verfify if this is implemented correctly
  public static String translateHaxeStringToJavaString(String value) {
    if (value.isBlank()) {
      return value;
    }
    char[] chars = value.toCharArray();
    int length = chars.length;
    int from = 0;
    int to = 0;

    while (from < length) {
      char ch = chars[from++];
      if (ch == '\\') {
        ch = from < length ? chars[from++] : '\0';
        switch (ch) {
          case 't':
            ch = '\t';
            break;
          case 'n':
            ch = '\n';
            break;
          case 'r':
            ch = '\r';
            break;
          case '\'':
          case '\"':
          case '\\':
            // keep as is, escaped escape char (ex. \\" -> \")
            break;
          // \xNN
          case 'x':
            char hex1 = from < length ? chars[from++] : '\0';
            char hex2 = from < length ? chars[from++] : '\0';
            ch = (char) ((hex(hex1) << 4) | hex(hex2));
            break;
          // \\uNNNN |  \\u{N...}
          case 'u':
            char next = from < length ? chars[from++] : '\0';
            if (next =='{') {
              char[]  values = new char[6];
              int index = 0;
              char digit = from < length ? chars[from++] : '\0';
              while (isHex(digit) && index < 6) {
                values[index++] = digit;
                digit = from < length ? chars[from++] : '\0';
                if(digit == '}' ) break;
              }
              // try to read "}"
              if(isHex(digit) && index != 6) {
                digit = from < length ? chars[from++] : '\0';
              }
              if (digit == '}') {
                int sum = 0;
                  for (int i = 0; i < index; i++) {
                      char c = values[i];
                      sum = (sum << 4) | hex(c);
                  }
                ch = (char) sum;
              }
            } else {
                char x1 = next;
                char x2 = from < length ? chars[from++] : '\0';
                char x3 = from < length ? chars[from++] : '\0';
                char x4 = from < length ? chars[from++] : '\0';
              int sum = (hex(x1) << 12)
                      | (hex(x2) << 8)
                      | (hex(x3) << 4)
                      | hex(x4);
              ch = (char) sum;
                break;
            }
            break;
          //  \\NNN
          case '0': case '1': case '2': case '3':
          case '4': case '5': case '6': case '7':
            int limit = Integer.min(from + (ch <= '3' ? 2 : 1), length);
            int code = ch - '0';
            while (from < limit) {
              ch = chars[from];
              if (ch < '0' || '7' < ch) {
                break;
              }
              from++;
              code = (code << 3) | (ch - '0');
            }
            ch = (char)code;
            break;
        }
      }
      chars[to++] = ch;
    }

    return new String(chars, 0, to);
  }

  private static int hex(char c) {
    return Character.digit(c, 16);
  }

  static boolean isHex(char c) {
    return Character.digit(c, 16) != -1;
  }

}
