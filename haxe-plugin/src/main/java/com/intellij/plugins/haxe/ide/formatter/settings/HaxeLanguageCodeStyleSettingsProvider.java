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
package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.lang.Language;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  @NotNull
  @Override
  public Language getLanguage() {
    return HaxeLanguage.INSTANCE;
  }

  @Override
  public String getCodeSample(@NotNull SettingsType settingsType) {
    if (settingsType == SettingsType.SPACING_SETTINGS) {
      return SPACING_CODE_SAMPLE;
    }
    if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
      return WRAPPING_CODE_SAMPLE;
    }
    return BLANK_LINES_CODE_SAMPLE;
  }

  @Override
  public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
    if (settingsType == SettingsType.SPACING_SETTINGS) {
      consumer.showStandardOptions("SPACE_BEFORE_METHOD_CALL_PARENTHESES",
                                   "SPACE_BEFORE_METHOD_PARENTHESES",
                                   "SPACE_BEFORE_IF_PARENTHESES",
                                   "SPACE_BEFORE_WHILE_PARENTHESES",
                                   "SPACE_BEFORE_FOR_PARENTHESES",
                                   "SPACE_BEFORE_CATCH_PARENTHESES",
                                   "SPACE_BEFORE_SWITCH_PARENTHESES",
                                   "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                                   "SPACE_AROUND_LOGICAL_OPERATORS",
                                   "SPACE_AROUND_EQUALITY_OPERATORS",
                                   "SPACE_AROUND_RELATIONAL_OPERATORS",
                                   "SPACE_AROUND_ADDITIVE_OPERATORS",
                                   "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                                   "SPACE_BEFORE_METHOD_LBRACE",
                                   "SPACE_BEFORE_IF_LBRACE",
                                   "SPACE_BEFORE_ELSE_LBRACE",
                                   "SPACE_BEFORE_WHILE_LBRACE",
                                   "SPACE_BEFORE_FOR_LBRACE",
                                   "SPACE_BEFORE_SWITCH_LBRACE",
                                   "SPACE_BEFORE_TRY_LBRACE",
                                   "SPACE_BEFORE_CATCH_LBRACE",
                                   "SPACE_BEFORE_WHILE_KEYWORD",
                                   "SPACE_BEFORE_ELSE_KEYWORD",
                                   "SPACE_BEFORE_CATCH_KEYWORD",
                                   "SPACE_WITHIN_METHOD_CALL_PARENTHESES",
                                   "SPACE_WITHIN_METHOD_PARENTHESES",
                                   "SPACE_WITHIN_IF_PARENTHESES",
                                   "SPACE_WITHIN_WHILE_PARENTHESES",
                                   "SPACE_WITHIN_FOR_PARENTHESES",
                                   "SPACE_WITHIN_CATCH_PARENTHESES",
                                   "SPACE_WITHIN_SWITCH_PARENTHESES",
                                   "SPACE_BEFORE_QUEST",
                                   "SPACE_AFTER_QUEST",
                                   "SPACE_BEFORE_COLON",
                                   "SPACE_AFTER_COLON",
                                   "SPACE_AFTER_COMMA",
                                   "SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS",
                                   "SPACE_BEFORE_COMMA",
                                   "SPACE_AROUND_UNARY_OPERATOR"
      );
      consumer.showCustomOption(HaxeCodeStyleSettings.class, "SPACE_AROUND_ARROW", "Around ->",
                                CodeStyleSettingsCustomizable.SPACES_OTHER, CodeStyleSettingsCustomizable.OptionAnchor.NONE);
      consumer.showCustomOption(HaxeCodeStyleSettings.class, "SPACE_BEFORE_TYPE_REFERENCE_COLON", "Space before type reference colon ':'",
                                CodeStyleSettingsCustomizable.SPACES_OTHER, CodeStyleSettingsCustomizable.OptionAnchor.NONE);
      consumer.showCustomOption(HaxeCodeStyleSettings.class, "SPACE_AFTER_TYPE_REFERENCE_COLON", "Space after type reference colon ':'",
                                CodeStyleSettingsCustomizable.SPACES_OTHER, CodeStyleSettingsCustomizable.OptionAnchor.NONE);
    }
    else if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
      consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE");
    }
    else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
      consumer.showStandardOptions("KEEP_LINE_BREAKS",
                                   "KEEP_FIRST_COLUMN_COMMENT",
                                   "BRACE_STYLE",
                                   "METHOD_BRACE_STYLE",
                                   "CALL_PARAMETERS_WRAP",
                                   "CALL_PARAMETERS_LPAREN_ON_NEXT_LINE",
                                   "CALL_PARAMETERS_RPAREN_ON_NEXT_LINE",
                                   "METHOD_PARAMETERS_WRAP",
                                   "METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE",
                                   "METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE",
                                   "ELSE_ON_NEW_LINE",
                                   "WHILE_ON_NEW_LINE",
                                   "CATCH_ON_NEW_LINE",
                                   "ALIGN_MULTILINE_PARAMETERS",
                                   "ALIGN_MULTILINE_PARAMETERS_IN_CALLS",
                                   "ALIGN_MULTILINE_BINARY_OPERATION",
                                   "BINARY_OPERATION_WRAP",
                                   "BINARY_OPERATION_SIGN_ON_NEXT_LINE",
                                   "TERNARY_OPERATION_WRAP",
                                   "TERNARY_OPERATION_SIGNS_ON_NEXT_LINE",
                                   "PARENTHESES_EXPRESSION_LPAREN_WRAP",
                                   "PARENTHESES_EXPRESSION_RPAREN_WRAP",
                                   "ALIGN_MULTILINE_TERNARY_OPERATION",
                                   "SPECIAL_ELSE_IF_TREATMENT");
    }
  }

  public static final String SPACING_CODE_SAMPLE = "package;\n" +
                                                   "class Foo {\n" +
                                                   "    public var tmp:Array<Array<Int>>;\n" +
                                                   "    public function foo(x:Int, z) {\n" +
                                                   "        new Foo(x, 2);\n" +
                                                   "        function absSum(a:Int, b:Int):Int {\n" +
                                                   "            var value:Int = a + b;\n" +
                                                   "            return value > 0 ? value : -value;\n" +
                                                   "        }\n" +
                                                   "        var increment:Int->Int = function(i:Int) {return ++i;}\n" +
                                                   "        var arr = [\"zero\", \"one\"];\n" +
                                                   "        var y = (x ^ 0x123) << 2;\n" +
                                                   "        for (i in 0...10) {\n" +
                                                   "            y = (y ^ 0x123) << 2;\n" +
                                                   "        }\n" +
                                                   "        var k = x % 2 == 1 ? 0 : 1;\n" +
                                                   "        do {\n" +
                                                   "            try {\n" +
                                                   "                if (0 < x && x < 10) {\n" +
                                                   "                    while (x != y) {\n" +
                                                   "                        x = absSum(x * 3, 5);\n" +
                                                   "                    }\n" +
                                                   "                    z += 2;\n" +
                                                   "                } else if (x > 20) {\n" +
                                                   "                    z = x << 1;\n" +
                                                   "                } else {\n" +
                                                   "                    z = x | 2;\n" +
                                                   "                }\n" +
                                                   "                switch (k) {\n" +
                                                   "                    case 0:\n" +
                                                   "                    var s1 = 'zero';\n" +
                                                   "                    case 2:\n" +
                                                   "                    var s1 = 'two';\n" +
                                                   "                    default:\n" +
                                                   "                    var s1 = 'other';\n" +
                                                   "                }\n" +
                                                   "            } catch (e:String) {\n" +
                                                   "                var message = arr[0];\n" +
                                                   "            }\n" +
                                                   "        } while (x < 0);\n" +
                                                   "    }\n" +
                                                   "\n" +
                                                   "    public function new(n:Int, m:Int) {\n" +
                                                   "        tmp = new Array<Array<Int>>();\n" +
                                                   "        for (i in 0...n * m) tmp.push(new Array<Int>());\n" +
                                                   "    }\n" +
                                                   "}\n";

  public static final String WRAPPING_CODE_SAMPLE = "class Foo {\n" +
                                                    "    // function fBar (x,y);\n" +
                                                    "    function fOne(argA, argB, argC, argD, argE, argF, argG, argH) {\n" +
                                                    "        var numbers : Array<String> = ['one', 'two', 'three', 'four', 'five', 'six'];\n" +
                                                    "        var x = (\"\" + argA) + argB + argC + argD + argE + argF + argG + argH;\n" +
                                                    "        try {\n" +
                                                    "            this.fTwo(argA, argB, argC, this.fThree(\"\", argE, argF, argG, argH));\n" +
                                                    "        } catch (ignored:String) {}\n" +
                                                    "        var z = argA == 'Some string' ? 'yes' : 'no';\n" +
                                                    "        var colors = ['red', 'green', 'blue', 'black', 'white', 'gray'];\n" +
                                                    "        for (colorIndex in 0...colors.length) {\n" +
                                                    "            var colorString = numbers[colorIndex];\n" +
                                                    "        }\n" +
                                                    "        do {\n" +
                                                    "            colors.pop();\n" +
                                                    "        } while (colors.length > 0);\n" +
                                                    "    }\n" +
                                                    "\n" +
                                                    "    function fTwo(strA, strB, strC, strD) {\n" +
                                                    "        if (true)\n" +
                                                    "        return strC;\n" +
                                                    "        if (strA == 'one' ||\n" +
                                                    "        strB == 'two') {\n" +
                                                    "            return strA + strB;\n" +
                                                    "        } else if (true) return strD;\n" +
                                                    "        throw strD;\n" +
                                                    "    }\n" +
                                                    "\n" +
                                                    "    function fThree(strA, strB, strC, strD, strE) {\n" +
                                                    "        return strA + strB + strC + strD + strE;\n" +
                                                    "    }\n" +
                                                    "    public function new() {}\n" +
                                                    "}";

  public static final String BLANK_LINES_CODE_SAMPLE = "class Foo {\n" +
                                                       "    public function new() {\n" +
                                                       "    }\n" +
                                                       "\n" +
                                                       "\n" +
                                                       "    public static function main() {\n" +
                                                       "        trace(\"Hello!\");\n" +
                                                       "    }\n" +
                                                       "}";
}
