/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.lang;

import static com.intellij.plugins.haxe.lang.HaxeCodeStyleTweaks.commonSettings;
import static com.intellij.plugins.haxe.lang.HaxeCodeStyleTweaks.haxeSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Formatting: formatter")
public class HaxeFormatterTest extends HaxeLightFixtureTestCase {
  protected CommonCodeStyleSettings myTestStyleSettings;

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    installTemporarySettings(this::defineStyleSettings);
  }

  protected void defineStyleSettings(CodeStyleSettings tempSettings) {
    myTestStyleSettings = tempSettings.getCommonSettings(HaxeLanguage.INSTANCE);
    myTestStyleSettings.KEEP_BLANK_LINES_IN_CODE = 2;
    myTestStyleSettings.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE;
    myTestStyleSettings.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE;
    myTestStyleSettings.ALIGN_MULTILINE_PARAMETERS = false;
    myTestStyleSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false;
    myTestStyleSettings.KEEP_FIRST_COLUMN_COMMENT = false;
  }

  @Test
  @DisplayName("default")
  public void testDefault() throws Exception {
    doTest();
  }

  @Test
  @DisplayName("statements")
  public void testStatements() throws Exception {
    doTest();
  }

  @Test
  @DisplayName("array utils")
  public void testArrayUtils() throws Exception {
    doTest();
  }

  @Test
  @DisplayName("space before parentheses")
  public void testSpaceBeforeParentheses() throws Exception {
    myTestStyleSettings.KEEP_LINE_BREAKS = false;
    myTestStyleSettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES = true;
    myTestStyleSettings.SPACE_BEFORE_METHOD_PARENTHESES = true;
    myTestStyleSettings.SPACE_BEFORE_IF_PARENTHESES = false;
    myTestStyleSettings.SPACE_BEFORE_FOR_PARENTHESES = false;
    myTestStyleSettings.SPACE_BEFORE_WHILE_PARENTHESES = false;
    myTestStyleSettings.SPACE_BEFORE_SWITCH_PARENTHESES = false;
    myTestStyleSettings.SPACE_BEFORE_CATCH_PARENTHESES = false;
    doTest();
  }

  @Test
  @DisplayName("space around operators")
  public void testSpaceAroundOperators() throws Exception {
    myTestStyleSettings.KEEP_LINE_BREAKS = false;
    myTestStyleSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = false;
    myTestStyleSettings.SPACE_AROUND_LOGICAL_OPERATORS = false;
    myTestStyleSettings.SPACE_AROUND_EQUALITY_OPERATORS = false;
    myTestStyleSettings.SPACE_AROUND_RELATIONAL_OPERATORS = false;
    myTestStyleSettings.SPACE_AROUND_ADDITIVE_OPERATORS = false;
    myTestStyleSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = false;
    doTest();
  }

  @Test
  @DisplayName("space around arrows")
  public void testSpaceAroundArrows() {
    // the three arrow kinds space separately: arrow functions, Haxe 4
    // function types and Haxe 3 function types
    Consumer<HaxeCodeStyleSettings> unspacedArrows = haxe -> {
      haxe.SPACE_AROUND_ARROW = false;
      haxe.SPACE_AROUND_FUNCTION_TYPE_ARROW = false;
      haxe.SPACE_AROUND_OLD_FUNCTION_TYPE_ARROW = false;
    };
    String source = """
      class Main {
      	static function main() {
      		var twice = x -> x * 2;
      		var apply:(Int) -> Int = twice;
      		var legacy:Int -> Int = twice;
      	}
      }
      """;

    String formatted = reformat(haxeSettings(unspacedArrows), source);

    assertEquals("""
      class Main {
          static function main() {
              var twice = x->x * 2;
              var apply:(Int)->Int = twice;
              var legacy:Int->Int = twice;
          }
      }
      """, formatted);
  }

  @Test
  @DisplayName("space left braces")
  public void testSpaceLeftBraces() throws Exception {
    myTestStyleSettings.KEEP_LINE_BREAKS = false;
    myTestStyleSettings.SPACE_BEFORE_METHOD_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_IF_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_ELSE_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_FOR_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_DO_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_WHILE_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_SWITCH_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_TRY_LBRACE = false;
    myTestStyleSettings.SPACE_BEFORE_CATCH_LBRACE = false;
    doTest();
  }

  @Test
  @DisplayName("space within")
  public void testSpaceWithin() throws Exception {
    myTestStyleSettings.KEEP_LINE_BREAKS = false;
    myTestStyleSettings.SPACE_WITHIN_METHOD_CALL_PARENTHESES = true;
    myTestStyleSettings.SPACE_WITHIN_METHOD_PARENTHESES = true;
    myTestStyleSettings.SPACE_WITHIN_IF_PARENTHESES = true;
    myTestStyleSettings.SPACE_WITHIN_FOR_PARENTHESES = true;
    myTestStyleSettings.SPACE_WITHIN_WHILE_PARENTHESES = true;
    myTestStyleSettings.SPACE_WITHIN_SWITCH_PARENTHESES = true;
    myTestStyleSettings.SPACE_WITHIN_CATCH_PARENTHESES = true;
    doTest();
  }

  @Test
  @DisplayName("space others")
  public void testSpaceOthers() throws Exception {
    myTestStyleSettings.KEEP_LINE_BREAKS = false;
    myTestStyleSettings.SPACE_BEFORE_WHILE_KEYWORD = false;
    myTestStyleSettings.SPACE_BEFORE_CATCH_KEYWORD = false;
    myTestStyleSettings.SPACE_BEFORE_ELSE_KEYWORD = false;
    myTestStyleSettings.SPACE_BEFORE_QUEST = false;
    myTestStyleSettings.SPACE_AFTER_QUEST = false;
    myTestStyleSettings.SPACE_BEFORE_COLON = false;
    myTestStyleSettings.SPACE_AFTER_COLON = false;
    myTestStyleSettings.SPACE_BEFORE_COMMA = true;
    myTestStyleSettings.SPACE_AFTER_COMMA = false;
    myTestStyleSettings.SPACE_BEFORE_SEMICOLON = true;
    myTestStyleSettings.SPACE_AFTER_SEMICOLON = false;
    doTest();
  }

  @Test
  @DisplayName("comma settings apply to their own lists")
  public void testCommaSettingsApplyToTheirOwnLists() {
    // the plain setting governs parameter, argument and literal commas; the
    // type-arguments setting governs type arguments and type parameters
    Consumer<CommonCodeStyleSettings> typeArgumentCommasOnly = common -> {
      common.SPACE_AFTER_COMMA = false;
      common.SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS = true;
    };
    Consumer<CommonCodeStyleSettings> plainCommasOnly = common -> {
      common.SPACE_AFTER_COMMA = true;
      common.SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS = false;
    };
    String source = """
      class Box<T, U> {
      	static function pair(first:Int, second:Int):Map<String, Int> {
      		var items = [first, second];
      		return pair(items[0], items[1]);
      	}
      }
      """;

    String typeArgumentsSpaced = reformat(commonSettings(typeArgumentCommasOnly), source);
    String plainSpaced = reformat(commonSettings(plainCommasOnly), source);

    assertEquals("""
      class Box<T, U> {
          static function pair(first:Int,second:Int):Map<String, Int> {
              var items = [first,second];
              return pair(items[0],items[1]);
          }
      }
      """, typeArgumentsSpaced);
    assertEquals("""
      class Box<T,U> {
          static function pair(first:Int, second:Int):Map<String,Int> {
              var items = [first, second];
              return pair(items[0], items[1]);
          }
      }
      """, plainSpaced);
  }

  @Test
  @DisplayName("module level function formats like a method")
  public void testModuleLevelFunctionFormatsLikeAMethod() {
    Consumer<CommonCodeStyleSettings> methodRules = common -> {
      common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE;
      common.SPACE_BEFORE_METHOD_PARENTHESES = true;
      common.ALIGN_MULTILINE_PARAMETERS = true;
    };
    String source = """
      function helper(first:Int,
      second:Int) {
      	return first + second;
      }

      class Main {
      	static function main(first:Int,
      	second:Int) {
      		helper(first, second);
      	}
      }
      """;

    String formatted = reformat(commonSettings(methodRules), source);

    assertEquals("""
      function helper (first:Int,
                       second:Int)
      {
          return first + second;
      }

      class Main {
          static function main (first:Int,
                                second:Int)
          {
              helper(first, second);
          }
      }
      """, formatted);
  }

  @Test
  @DisplayName("indent typedef")
  public void testIndentTypedef() throws Exception {
    doTest();
  }

  @Test
  @DisplayName("wrapping meth")
  public void testWrappingMeth() throws Exception {
    myTestStyleSettings.METHOD_ANNOTATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    myTestStyleSettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE = true;
    myTestStyleSettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE = true;
    myTestStyleSettings.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    myTestStyleSettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE = true;
    myTestStyleSettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE = true;
    myTestStyleSettings.ELSE_ON_NEW_LINE = true;
    myTestStyleSettings.SPECIAL_ELSE_IF_TREATMENT = true;
    myTestStyleSettings.FOR_STATEMENT_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    myTestStyleSettings.FOR_STATEMENT_LPAREN_ON_NEXT_LINE = true;
    myTestStyleSettings.FOR_STATEMENT_RPAREN_ON_NEXT_LINE = true;
    myTestStyleSettings.WHILE_ON_NEW_LINE = true;
    myTestStyleSettings.CATCH_ON_NEW_LINE = true;
    myTestStyleSettings.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    myTestStyleSettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE = true;
    myTestStyleSettings.PARENTHESES_EXPRESSION_LPAREN_WRAP = true;
    myTestStyleSettings.PARENTHESES_EXPRESSION_RPAREN_WRAP = true;
    myTestStyleSettings.ASSIGNMENT_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    myTestStyleSettings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE = true;
    myTestStyleSettings.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    myTestStyleSettings.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true;
    myTestStyleSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = true;
    doTest();
  }

  @Test
  @DisplayName("alignment")
  public void testAlignment() throws Exception {
    myTestStyleSettings.ALIGN_MULTILINE_PARAMETERS = true;
    myTestStyleSettings.ALIGN_MULTILINE_BINARY_OPERATION = true;
    myTestStyleSettings.ALIGN_MULTILINE_TERNARY_OPERATION = true;
    doTest();
  }

  @Test
  @DisplayName("brace placement 1")
  public void testBracePlacement1() throws Exception {
    // next-line shifted braces
    myTestStyleSettings.KEEP_LINE_BREAKS = false;
    myTestStyleSettings.BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED2;
    myTestStyleSettings.METHOD_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE;
    doTest();
  }

  @Test
  @DisplayName("brace placement 2")
  public void testBracePlacement2() throws Exception {
    // end-of-line class braces, next-line methods
    myTestStyleSettings.KEEP_LINE_BREAKS = false;
    myTestStyleSettings.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE;
    myTestStyleSettings.METHOD_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
    doTest();
  }

  @Test
  @DisplayName("comment alignment normal")
  public void testCommentAlignmentNormal() throws Exception {
    myTestStyleSettings.KEEP_LINE_BREAKS = true;
    myTestStyleSettings.KEEP_FIRST_COLUMN_COMMENT = false;
    doTest();
  }

  @Test
  @DisplayName("comment alignment keep left")
  public void testCommentAlignmentKeepLeft() throws Exception {
    myTestStyleSettings.KEEP_LINE_BREAKS = true;
    myTestStyleSettings.KEEP_FIRST_COLUMN_COMMENT = true;
    doTest();
  }

  @Test
  @DisplayName("indent tabs")
  public void testIndentTabs() throws Exception {
    myTestStyleSettings.getIndentOptions().USE_TAB_CHARACTER = true;
    myTestStyleSettings.getIndentOptions().INDENT_SIZE = 1;
    myTestStyleSettings.getIndentOptions().TAB_SIZE = 1;
    doTest();
  }

  @Test
  @DisplayName("indent spaces")
  public void testIndentSpaces() throws Exception {
    myTestStyleSettings.getIndentOptions().USE_TAB_CHARACTER = false;
    myTestStyleSettings.getIndentOptions().INDENT_SIZE = 3;
    doTest();
  }

  @Test
  @DisplayName("line feeds with comments")
  public void testLineFeedsWithComments() throws Exception {
    doTest();
  }

  @Test
  @DisplayName("blank lines between field groups")
  public void testBlankLinesBetweenFieldGroups() {
    // a staticness change splits the var block by the configured count;
    // same-group fields keep the plain around-field gap (none)
    Consumer<HaxeCodeStyleSettings> twoBlankLines = haxe -> haxe.BLANK_LINES_BETWEEN_FIELD_GROUPS = 2;
    String source = """
      class Main {
      	static var first:Int = 1;
      	static var second:Int = 2;
      	var third:Int = 3;
      	var fourth:Int = 4;
      }
      """;

    String formatted = reformat(haxeSettings(twoBlankLines), source);

    assertEquals("""
      class Main {
          static var first:Int = 1;
          static var second:Int = 2;


          var third:Int = 3;
          var fourth:Int = 4;
      }
      """, formatted);
  }

  @Test
  @DisplayName("blank lines between types")
  public void testBlankLinesBetweenTypes() {
    // the around-class count is the minimum between two types and the
    // between-types cap the maximum, independent of the in-code cap
    Consumer<CodeStyleSettings> oneToThree = settings -> {
      settings.getCommonSettings(HaxeLanguage.INSTANCE).BLANK_LINES_AROUND_CLASS = 1;
      settings.getCommonSettings(HaxeLanguage.INSTANCE).KEEP_BLANK_LINES_IN_CODE = 1;
      settings.getCustomSettings(HaxeCodeStyleSettings.class).KEEP_BLANK_LINES_BETWEEN_TYPES = 3;
    };
    String source = """
      class First {
      	var a:Int;
      }
      class Second {
      	var b:Int;
      }



      class Third {
      	var c:Int;
      }
      """;

    String formatted = reformat(oneToThree, source);

    assertEquals("""
      class First {
          var a:Int;
      }

      class Second {
          var b:Int;
      }



      class Third {
          var c:Int;
      }
      """, formatted);
  }

  @Test
  @DisplayName("reformat element covers its range with the text passes")
  public void testReformatElementCoversItsRangeWithTheTextPasses() {
    // the introduce-member intentions reformat the ELEMENT they inserted;
    // a comment pass must reach inside that element and nothing outside it
    Consumer<HaxeCodeStyleSettings> spacedLineComments = haxe -> haxe.ADD_LINE_COMMENT_SPACE = true;
    installTemporarySettings(haxeSettings(spacedLineComments));
    myFixture.configureByText("Main.hx", """
      class Main {
          //outside
          static function main() {
              //inside
          }
      }
      """);
    HaxeMethodDeclaration method = PsiTreeUtil.findChildOfType(myFixture.getFile(), HaxeMethodDeclaration.class);

    PsiElement reformatted = reformatElement(method);

    String text = myFixture.getFile().getText();
    assertTrue(text.contains("// inside"), "the comment inside the element normalizes:\n" + text);
    assertTrue(text.contains("//outside"), "the comment outside the element stays as written:\n" + text);
    assertTrue(reformatted.isValid(), "the returned element is valid");
    HaxeMethodDeclaration returned = assertInstanceOf(HaxeMethodDeclaration.class, reformatted, "the returned element is the method");
    assertEquals("main", returned.getName());
  }

  private PsiElement reformatElement(PsiElement element) {
    Project project = myFixture.getProject();
    ThrowableComputable<PsiElement, RuntimeException> reformat = () -> CodeStyleManager.getInstance(project).reformat(element);
    return WriteCommandAction.writeCommandAction(project).compute(reformat);
  }

  /** Formats the test-named fixture under the settings setUp installed and the test mutated; a missing expectation is written out to be reviewed. */
  private void doTest() throws Exception {
    reformatFile(getTestName(false) + ".hx");
    try {
      myFixture.checkResultByFile(getTestName(false) + ".txt");
    }
    catch (RuntimeException e) {
      // a missing expectation surfaces as FileNotFoundException or, when read through nio, NoSuchFileException
      if (!(e.getCause() instanceof FileNotFoundException || e.getCause() instanceof NoSuchFileException)) {
        throw e;
      }
      Path path = Path.of(getTestDataPath(), getTestName(false) + ".txt");
      Files.writeString(path, myFixture.getFile().getText().trim());
      fail("No output text found. File " + path + " created.");
    }
  }
}
