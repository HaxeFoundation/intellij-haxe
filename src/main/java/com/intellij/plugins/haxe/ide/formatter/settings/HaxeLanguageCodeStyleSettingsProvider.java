/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.codeStyle.*;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.WrappingOrBraceOption.*;
import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.BlankLinesOption.*;
import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.SpacingOption.*;
import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.OptionAnchor;

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
  public DocCommentSettings getDocCommentSettings(@NotNull CodeStyleSettings rootSettings) {
    return new DocCommentSettings() {
      private final JavaCodeStyleSettings mySettings = rootSettings.getCustomSettings(JavaCodeStyleSettings.class);


      @Override
      public boolean isDocFormattingEnabled() {
        return mySettings.ENABLE_JAVADOC_FORMATTING;
      }

      @Override
      public void setDocFormattingEnabled(boolean formattingEnabled) {
        mySettings.ENABLE_JAVADOC_FORMATTING = formattingEnabled;
      }


      @Override
      public boolean isLeadingAsteriskEnabled() {
        return false; // haxe docs are markdown and we do not want it prefixed with Asterisk
      }

      @Override
      public boolean isRemoveEmptyTags() {
        return mySettings.JD_KEEP_EMPTY_EXCEPTION || mySettings.JD_KEEP_EMPTY_PARAMETER || mySettings.JD_KEEP_EMPTY_RETURN;
      }

      @Override
      public void setRemoveEmptyTags(boolean removeEmptyTags) {
        mySettings.JD_KEEP_EMPTY_RETURN = !removeEmptyTags;
        mySettings.JD_KEEP_EMPTY_PARAMETER = !removeEmptyTags;
        mySettings.JD_KEEP_EMPTY_EXCEPTION = !removeEmptyTags;
      }
    };
  }
  @Override
  public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
    if (settingsType == SettingsType.SPACING_SETTINGS) {
      consumer.showStandardOptions(SPACE_BEFORE_METHOD_CALL_PARENTHESES.name(),
                                   SPACE_BEFORE_METHOD_PARENTHESES.name(),
                                   SPACE_BEFORE_IF_PARENTHESES.name(),
                                   SPACE_BEFORE_WHILE_PARENTHESES.name(),
                                   SPACE_BEFORE_FOR_PARENTHESES.name(),
                                   SPACE_BEFORE_CATCH_PARENTHESES.name(),
                                   SPACE_BEFORE_SWITCH_PARENTHESES.name(),
                                   SPACE_AROUND_ASSIGNMENT_OPERATORS.name(),
                                   SPACE_AROUND_LOGICAL_OPERATORS.name(),
                                   SPACE_AROUND_EQUALITY_OPERATORS.name(),
                                   SPACE_AROUND_RELATIONAL_OPERATORS.name(),
                                   SPACE_AROUND_ADDITIVE_OPERATORS.name(),
                                   SPACE_AROUND_MULTIPLICATIVE_OPERATORS.name(),
                                   SPACE_BEFORE_METHOD_LBRACE.name(),
                                   SPACE_BEFORE_IF_LBRACE.name(),
                                   SPACE_BEFORE_ELSE_LBRACE.name(),
                                   SPACE_BEFORE_DO_LBRACE.name(),
                                   SPACE_BEFORE_WHILE_LBRACE.name(),
                                   SPACE_BEFORE_FOR_LBRACE.name(),
                                   SPACE_BEFORE_SWITCH_LBRACE.name(),
                                   SPACE_BEFORE_TRY_LBRACE.name(),
                                   SPACE_BEFORE_CATCH_LBRACE.name(),
                                   SPACE_BEFORE_WHILE_KEYWORD.name(),
                                   SPACE_BEFORE_ELSE_KEYWORD.name(),
                                   SPACE_BEFORE_CATCH_KEYWORD.name(),
                                   SPACE_WITHIN_METHOD_CALL_PARENTHESES.name(),
                                   SPACE_WITHIN_METHOD_PARENTHESES.name(),
                                   SPACE_WITHIN_IF_PARENTHESES.name(),
                                   SPACE_WITHIN_WHILE_PARENTHESES.name(),
                                   SPACE_WITHIN_FOR_PARENTHESES.name(),
                                   SPACE_WITHIN_CATCH_PARENTHESES.name(),
                                   SPACE_WITHIN_SWITCH_PARENTHESES.name(),
                                   SPACE_BEFORE_QUEST.name(),
                                   SPACE_AFTER_QUEST.name(),
                                   SPACE_BEFORE_COLON.name(),
                                   SPACE_AFTER_COLON.name(),
                                   SPACE_AFTER_COMMA.name(),
                                   SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS.name(),
                                   SPACE_BEFORE_COMMA.name(),
                                   SPACE_AROUND_UNARY_OPERATOR.name()
      );
      consumer.showCustomOption(HaxeCodeStyleSettings.class, "SPACE_AROUND_ARROW", "Around ->",
                                CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER, OptionAnchor.NONE);
      consumer.showCustomOption(HaxeCodeStyleSettings.class, "SPACE_BEFORE_TYPE_REFERENCE_COLON", "Space before type reference colon ':'",
                                CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER, OptionAnchor.NONE);
      consumer.showCustomOption(HaxeCodeStyleSettings.class, "SPACE_AFTER_TYPE_REFERENCE_COLON", "Space after type reference colon ':'",
                                CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER, OptionAnchor.NONE);
    }
    else if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
      consumer.showStandardOptions(
        KEEP_BLANK_LINES_IN_CODE.name(),
        BLANK_LINES_AFTER_PACKAGE.name(),
        BLANK_LINES_AFTER_IMPORTS.name()
      );
      consumer.showCustomOption(HaxeCodeStyleSettings.class, "MINIMUM_BLANK_LINES_AFTER_USING", "After using:",
                                CodeStyleSettingsCustomizableOptions.getInstance().BLANK_LINES, OptionAnchor.NONE);
    }
    else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
      consumer.showStandardOptions(
                      KEEP_LINE_BREAKS.name(),
                                   KEEP_FIRST_COLUMN_COMMENT.name(),
                                   BRACE_STYLE.name(),
                                   METHOD_BRACE_STYLE.name(),
                                   CALL_PARAMETERS_WRAP.name(),
                                   CALL_PARAMETERS_LPAREN_ON_NEXT_LINE.name(),
                                   CALL_PARAMETERS_RPAREN_ON_NEXT_LINE.name(),
                                   METHOD_PARAMETERS_WRAP.name(),
                                   METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE.name(),
                                   METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE.name(),
                                   ELSE_ON_NEW_LINE.name(),
                                   WHILE_ON_NEW_LINE.name(),
                                   CATCH_ON_NEW_LINE.name(),
                                   ALIGN_MULTILINE_PARAMETERS.name(),
                                   ALIGN_MULTILINE_PARAMETERS_IN_CALLS.name(),
                                   ALIGN_MULTILINE_BINARY_OPERATION.name(),
                                   BINARY_OPERATION_WRAP.name(),
                                   BINARY_OPERATION_SIGN_ON_NEXT_LINE.name(),
                                   TERNARY_OPERATION_WRAP.name(),
                                   TERNARY_OPERATION_SIGNS_ON_NEXT_LINE.name(),
                                   PARENTHESES_EXPRESSION_LPAREN_WRAP.name(),
                                   PARENTHESES_EXPRESSION_RPAREN_WRAP.name(),
                                   ALIGN_MULTILINE_TERNARY_OPERATION.name(),
                                   SPECIAL_ELSE_IF_TREATMENT.name()
      );
    }
  }

  @Override
  public IndentOptionsEditor getIndentOptionsEditor() {
    return new IndentOptionsEditor(this);
  }

  public static final String SPACING_CODE_SAMPLE = """
    package;
    @author("Penelope")
    @:final
    class Foo {
         public var tmp:Array<Array<Int>>;
        
         public function foo(x:Int, z) {
              new Foo(x, 2);
              function absSum(a:Int, b:Int):Int {
                   var value:Int = a + b;
                   return value > 0 ? value : -value;
              }
              var increment:Int -> Int = function(i:Int) {return ++i;}
              var arr = ["zero", "one"];
              var y = (x ^ 0x123) << 2;
              for (i in 0...10) {
                   y = (y ^ 0x123) << 2;
              }
              var k = x % 2 == 1 ? 0 : 1;
              do {
                   try {
                        if (0 < x&&x < 10) {
                             while (x != y) {
                                  x = absSum(x * 3, 5);
                             }
                             z += 2;
                        } else if (x > 20) {
                             z = x << 1;
                        } else {
                             z = x | 2;
                        }
                        switch (k) {
                             case 0:
                                  var s1 = 'zero';
                             case 2:
                                  var s1 = 'two';
                             default:
                                  var s1 = 'other';
                        }
                   } catch (e:String) {
                        var message = arr[0];
                   }
              } while (x < 0);
         }
        
         public function new(n:Int, m:Int) {
              tmp = new Array<Array<Int>>();
              for (i in 0...n * m) tmp.push(new Array<Int>());
         }
    }
    """;

  public static final String WRAPPING_CODE_SAMPLE = """
    @author("Penelope") @:final
    class Foo {
         // function fBar (x,y);
         function fOne(argA, argB, argC, argD, argE, argF, argG, argH) {
              var numbers:Array<String> = ['one', 'two', 'three', 'four', 'five', 'six'];
              var x = ("" + argA) + argB + argC + argD + argE + argF + argG + argH;
              try {
                   this.fTwo(argA, argB, argC, this.fThree("", argE, argF, argG, argH));
              } catch (ignored:String) {}
              var z = argA == 'Some string' ? 'yes' : 'no';
              var colors = ['red', 'green', 'blue', 'black', 'white', 'gray'];
              for (colorIndex in 0...colors.length) {
                   var colorString = numbers[colorIndex];
              }
              do {
                   colors.pop();
              } while (colors.length > 0);
         }
        
         function fTwo(strA, strB, strC, strD) {
              if (true)
                   return strC;
              if (strA == 'one'||
              strB == 'two') {
                   return strA + strB;
              } else if (true) return strD;
              throw strD;
         }
        
         function fThree(strA, strB, strC, strD, strE) {
              return strA + strB + strC + strD + strE;
         }
        
         public function new() {}
    }
    """;

  public static final String BLANK_LINES_CODE_SAMPLE = """
    package foo.bar;
    import a.b.SomeClass;
    import a.b.SomeOther as ClassAlias;
    using someUtil;
    class Foo {
         public function new() {
         }
        
        
         public static function main() {
              trace("Hello!");
         }
    }
    """;
}
