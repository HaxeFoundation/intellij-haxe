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

import com.intellij.psi.PsiElement;

public class HaxeCastUtil {
  static public String getCastText(PsiElement elementToCast, SpecificTypeReference fromType, SpecificTypeReference toType) {
    String elementText = elementToCast.getText();
    // Same type, no cast
    if (toType.toStringWithoutConstant().equals(fromType.toStringWithoutConstant())) {
      return elementText;
    }

    // XXXX -> String
    if (toType.isString()) {
      return "Std.string(" + elementText + ")";
    }

    // String -> XXXX
    if (fromType.isString()) {
      // String -> Int
      if (toType.isInt()) {
        return "Std.parseInt(" + elementText + ")";
      }
      // String -> Float
      if (toType.isFloat()) {
        return "Std.parseFloat(" + elementText + ")";
      }
    }

    // Float -> Int
    if (fromType.isFloat() && toType.isInt()) {
      return "Std.int(" + elementText + ")";
    }

    // (Int|Float) -> Bool
    if (fromType.isNumeric() && toType.isBool()) {
      return "((" + elementText + ") != 0)";
    }

    // Generic cast
    return "cast(" + elementText + ", " + toType.toStringWithoutConstant() + ")";
  }
}
