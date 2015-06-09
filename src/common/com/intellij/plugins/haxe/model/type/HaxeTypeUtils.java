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

public class HaxeTypeUtils {
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
    if (operator.equals("&")) return (int)leftv & (int)rightv;
    if (operator.equals("|")) return (int)leftv | (int)rightv;
    throw new RuntimeException("Unsupporteed operator '" + operator + "'");
  }

}
