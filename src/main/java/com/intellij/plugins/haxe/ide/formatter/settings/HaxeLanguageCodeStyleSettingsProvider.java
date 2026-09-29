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
import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.codeStyle.*;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.WrappingOrBraceOption.*;
import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.BlankLinesOption.*;
import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.SpacingOption.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  /** A custom option as a tab shows it: the setting it edits, the bundle key of its title, the group it sits in. */
  private record CustomOption(String field, String titleKey, Supplier<String> group) {
  }

  // the platform's group titles are locale-sensitive, so they are read when
  // the tab is built rather than captured into the tables
  private static final Supplier<String> SPACES_AROUND_OPERATORS = platformGroup(groups -> groups.SPACES_AROUND_OPERATORS);
  private static final Supplier<String> SPACES_WITHIN = platformGroup(groups -> groups.SPACES_WITHIN);
  private static final Supplier<String> SPACES_OTHER = platformGroup(groups -> groups.SPACES_OTHER);
  private static final Supplier<String> BLANK_LINES = platformGroup(groups -> groups.BLANK_LINES);
  private static final Supplier<String> BLANK_LINES_KEEP = platformGroup(groups -> groups.BLANK_LINES_KEEP);
  private static final Supplier<String> EXPRESSION_BODY_GROUP = haxeGroup("haxe.codestyle.wrapping.expression.body.group");
  private static final Supplier<String> STRUCTURE_EXTENSION_GROUP = haxeGroup("haxe.codestyle.wrapping.structure.extension.group");
  private static final Supplier<String> RETURN_GROUP = haxeGroup("haxe.codestyle.wrapping.return.group");
  private static final Supplier<String> COMMENTS_GROUP = haxeGroup("haxe.codestyle.wrapping.comments.group");
  private static final Supplier<String> CHAINS_GROUP = haxeGroup("haxe.codestyle.wrapping.chains.group");
  private static final Supplier<String> ARGUMENTS_GROUP = haxeGroup("haxe.codestyle.wrapping.arguments.group");

  // placements and names mirror Java/Kotlin/Groovy: arrow spacing sits with
  // the operators, colon options use Kotlin's phrasing
  private static final List<CustomOption> SPACING_OPTIONS = List.of(
    new CustomOption("SPACE_AROUND_ARROW", "haxe.codestyle.spacing.arrow", SPACES_AROUND_OPERATORS),
    new CustomOption("SPACE_AROUND_FUNCTION_TYPE_ARROW", "haxe.codestyle.spacing.function.type.arrow", SPACES_AROUND_OPERATORS),
    new CustomOption("SPACE_AROUND_OLD_FUNCTION_TYPE_ARROW", "haxe.codestyle.spacing.old.function.type.arrow", SPACES_AROUND_OPERATORS),
    new CustomOption("SPACE_BEFORE_TYPE_REFERENCE_COLON", "haxe.codestyle.spacing.before.type.colon", SPACES_OTHER),
    new CustomOption("SPACE_AFTER_TYPE_REFERENCE_COLON", "haxe.codestyle.spacing.after.type.colon", SPACES_OTHER),
    new CustomOption("SPACE_WITHIN_TYPE_PARAMETERS", "haxe.codestyle.spacing.type.parameters", SPACES_WITHIN),
    new CustomOption("SPACE_WITHIN_STRING_INTERPOLATION", "haxe.codestyle.spacing.string.interpolation", SPACES_WITHIN),
    new CustomOption("SPACE_AROUND_TYPE_CHECK_COLON", "haxe.codestyle.spacing.type.check.colon", SPACES_OTHER),
    new CustomOption("SPACE_WITHIN_METADATA_PARENTHESES", "haxe.codestyle.spacing.metadata.parentheses", SPACES_WITHIN),
    new CustomOption("SPACE_BEFORE_OBJECT_FIELD_COLON", "haxe.codestyle.spacing.before.object.field.colon", SPACES_OTHER),
    new CustomOption("ADD_LINE_COMMENT_SPACE", "haxe.codestyle.spacing.line.comment", SPACES_OTHER),
    new CustomOption("SPACE_AFTER_OBJECT_FIELD_COLON", "haxe.codestyle.spacing.after.object.field.colon", SPACES_OTHER));

  private static final List<CustomOption> BLANK_LINES_OPTIONS = List.of(
    new CustomOption("MINIMUM_BLANK_LINES_AFTER_USING", "haxe.codestyle.blank.lines.after.using", BLANK_LINES),
    new CustomOption("MINIMUM_BLANK_LINES_AFTER_FILE_HEADER", "haxe.codestyle.blank.lines.after.file.header", BLANK_LINES),
    new CustomOption("KEEP_BLANK_LINES_BETWEEN_TYPES", "haxe.codestyle.blank.lines.keep.between.types", BLANK_LINES_KEEP),
    new CustomOption("KEEP_BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES", "haxe.codestyle.blank.lines.between.single.line.types", BLANK_LINES_KEEP),
    new CustomOption("KEEP_BLANK_LINES_AFTER_LBRACE", "haxe.codestyle.blank.lines.keep.after.lbrace", BLANK_LINES_KEEP),
    new CustomOption("KEEP_BLANK_LINES_AFTER_CASE_COLON", "haxe.codestyle.blank.lines.keep.after.case.colon", BLANK_LINES_KEEP),
    new CustomOption("KEEP_BLANK_LINES_BETWEEN_MULTILINE_COMMENTS", "haxe.codestyle.blank.lines.keep.between.multiline.comments", BLANK_LINES_KEEP),
    new CustomOption("BLANK_LINES_BETWEEN_FIELD_GROUPS", "haxe.codestyle.blank.lines.between.field.groups", BLANK_LINES),
    new CustomOption("BLANK_LINES_BEFORE_FIELD_DOC_COMMENT", "haxe.codestyle.blank.lines.before.field.doc", BLANK_LINES),
    new CustomOption("BLANK_LINES_AFTER_DOCUMENTED_FIELD", "haxe.codestyle.blank.lines.after.documented.field", BLANK_LINES));

  // inactive-branch options live in the Conditional Compilation tab, because
  // they affect indentation, spacing and line breaks at once, not just wrapping
  private static final List<CustomOption> WRAPPING_OPTIONS = List.of(
    new CustomOption("FUNCTION_EXPRESSION_BODY_ON_NEXT_LINE", "haxe.codestyle.wrapping.expression.body.on.next.line", EXPRESSION_BODY_GROUP),
    new CustomOption("STRUCTURE_EXTENSION_ON_OWN_LINE", "haxe.codestyle.wrapping.structure.extension.own.line", STRUCTURE_EXTENSION_GROUP),
    new CustomOption("RETURN_VALUE_ON_SAME_LINE", "haxe.codestyle.wrapping.return.value.same.line", RETURN_GROUP),
    new CustomOption("FORMAT_DOC_COMMENTS", "haxe.codestyle.wrapping.format.doc.comments", COMMENTS_GROUP),
    new CustomOption("REINDENT_MULTILINE_COMMENTS", "haxe.codestyle.wrapping.reindent.multiline.comments", COMMENTS_GROUP));

  // the chain and call-argument switches; the numeric rule thresholds live in
  // the Wrap Rules tab (this tab's option table hosts booleans and choices only)
  private static final List<CustomOption> CHAIN_OPTIONS = List.of(
    new CustomOption("INDENT_WRAPPED_OPERATOR_CHAINS", "haxe.codestyle.wrapping.chains.indent", CHAINS_GROUP),
    new CustomOption("FILL_CALL_ARGUMENTS_ON_JOINED_LINE", "haxe.codestyle.wrapping.arguments.fill.joined", ARGUMENTS_GROUP));

  /**
   * The spacing rules keep written line breaks through KEEP_LINE_BREAKS, so
   * the platform's second-reformat flow applies: a reformat that kept custom
   * breaks reports them, and repeating it offers to drop them.
   */
  @Override
  public boolean usesCommonKeepLineBreaks() {
    return true;
  }

  @NotNull
  @Override
  public com.intellij.lang.Language getLanguage() {
    return HaxeLanguage.INSTANCE;
  }

  @Override
  public String getCodeSample(@NotNull SettingsType settingsType) {
    return switch (settingsType) {
      case SPACING_SETTINGS -> SPACING_CODE_SAMPLE;
      case WRAPPING_AND_BRACES_SETTINGS -> WRAPPING_CODE_SAMPLE;
      // the Indents tab (and any tab without a sample of its own) shows the
      // blank-lines sample: its nested bodies walk through every indent step
      case BLANK_LINES_SETTINGS, INDENT_SETTINGS, COMMENTER_SETTINGS, LANGUAGE_SPECIFIC -> BLANK_LINES_CODE_SAMPLE;
    };
  }

  @Override
  public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings baseSettings, @NotNull CodeStyleSettings modelSettings) {
    return new HaxeCodeStyleConfigurable(baseSettings, modelSettings);
  }

  @Override
  public @Nullable CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
    return new HaxeCodeStyleSettings(settings);
  }

  /**
   * Backs the platform's doc-formatting switch, which a reformat may turn
   * off, with the Haxe FORMAT_DOC_COMMENTS setting. Haxe docs are markdown,
   * so there are no leading asterisks and no tags to remove.
   */
  @Override
  public DocCommentSettings getDocCommentSettings(@NotNull CodeStyleSettings rootSettings) {
    return new DocCommentSettings() {
      private final HaxeCodeStyleSettings haxe = rootSettings.getCustomSettings(HaxeCodeStyleSettings.class);

      @Override
      public boolean isDocFormattingEnabled() {
        return haxe.FORMAT_DOC_COMMENTS;
      }

      @Override
      public void setDocFormattingEnabled(boolean formattingEnabled) {
        haxe.FORMAT_DOC_COMMENTS = formattingEnabled;
      }

      @Override
      public boolean isLeadingAsteriskEnabled() {
        return false;
      }

      @Override
      public boolean isRemoveEmptyTags() {
        return false;
      }

      @Override
      public void setRemoveEmptyTags(boolean removeEmptyTags) {
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
                                   SPACE_AROUND_BITWISE_OPERATORS.name(),
                                   SPACE_AROUND_SHIFT_OPERATORS.name(),
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
                                   SPACE_WITHIN_PARENTHESES.name(),
                                   SPACE_BEFORE_QUEST.name(),
                                   SPACE_AFTER_QUEST.name(),
                                   SPACE_BEFORE_COLON.name(),
                                   SPACE_AFTER_COLON.name(),
                                   SPACE_AFTER_COMMA.name(),
                                   SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS.name(),
                                   SPACE_BEFORE_COMMA.name(),
                                   SPACE_AROUND_UNARY_OPERATOR.name(),
                                   SPACE_WITHIN_BRACKETS.name()
      );
      showCustomOptions(consumer, SPACING_OPTIONS);
    }
    else if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
      consumer.showStandardOptions(
        KEEP_BLANK_LINES_IN_CODE.name(),
        KEEP_BLANK_LINES_IN_DECLARATIONS.name(),
        KEEP_BLANK_LINES_BEFORE_RBRACE.name(),
        BLANK_LINES_AFTER_PACKAGE.name(),
        BLANK_LINES_AFTER_IMPORTS.name(),
        BLANK_LINES_AROUND_CLASS.name(),
        BLANK_LINES_AFTER_CLASS_HEADER.name(),
        BLANK_LINES_AROUND_FIELD.name(),
        BLANK_LINES_AROUND_METHOD.name(),
        BLANK_LINES_BEFORE_CLASS_END.name()
      );
      showCustomOptions(consumer, BLANK_LINES_OPTIONS);
    }
    else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
      consumer.showStandardOptions(
        RIGHT_MARGIN.name(),
        WRAP_ON_TYPING.name(),
        KEEP_LINE_BREAKS.name(),
        KEEP_FIRST_COLUMN_COMMENT.name(),
        KEEP_CONTROL_STATEMENT_IN_ONE_LINE.name(),
        KEEP_SIMPLE_BLOCKS_IN_ONE_LINE.name(),
        KEEP_SIMPLE_METHODS_IN_ONE_LINE.name(),
        KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE.name(),
        ARRAY_INITIALIZER_WRAP.name(),
        METHOD_CALL_CHAIN_WRAP.name(),
        EXTENDS_LIST_WRAP.name(),
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
        SPECIAL_ELSE_IF_TREATMENT.name(),
        ASSIGNMENT_WRAP.name(),
        PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE.name()
      );
      // the platform's default label mentions "permits", Java sealed-class
      // syntax that does not exist in Haxe
      consumer.renameStandardOption(EXTENDS_LIST_WRAP.name(), HaxeCodeStyleBundle.message("haxe.codestyle.wrapping.extends.list"));
      showCustomOptions(consumer, WRAPPING_OPTIONS);
      showBodyPlacements(consumer);
      showCustomOptions(consumer, CHAIN_OPTIONS);
    }
  }

  private static Supplier<String> platformGroup(Function<CodeStyleSettingsCustomizableOptions, String> pick) {
    return () -> pick.apply(CodeStyleSettingsCustomizableOptions.getInstance());
  }

  private static Supplier<String> haxeGroup(String titleKey) {
    return () -> HaxeCodeStyleBundle.message(titleKey);
  }

  /** Shows the options in table order, which is the order the tab lists them in. */
  private static void showCustomOptions(@NotNull CodeStyleSettingsCustomizable consumer, List<CustomOption> options) {
    for (CustomOption option : options) {
      String title = HaxeCodeStyleBundle.message(option.titleKey());
      consumer.showCustomOption(HaxeCodeStyleSettings.class, option.field(), title, option.group().get());
    }
  }

  /** Where each construct's non-block body goes (hxformat's sameLine.*Body), a combo box per construct. */
  private static void showBodyPlacements(@NotNull CodeStyleSettingsCustomizable consumer) {
    String group = HaxeCodeStyleBundle.message("haxe.codestyle.wrapping.bodies.group");
    String[] names = {
      HaxeCodeStyleBundle.message("haxe.codestyle.wrapping.body.default"),
      HaxeCodeStyleBundle.message("haxe.codestyle.wrapping.body.next.line"),
      HaxeCodeStyleBundle.message("haxe.codestyle.wrapping.body.same.line"),
      HaxeCodeStyleBundle.message("haxe.codestyle.wrapping.body.keep")};
    int[] values = {
      HaxeCodeStyleSettings.BODY_PLACEMENT_DEFAULT,
      HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE,
      HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE,
      HaxeCodeStyleSettings.BODY_PLACEMENT_KEEP};
    List<String> constructs = List.of("IF", "ELSE", "FOR", "WHILE", "DO_WHILE", "TRY", "CATCH", "CASE", "VALUE_IF", "VALUE_TRY", "VALUE_CASE");
    for (String construct : constructs) {
      String title = HaxeCodeStyleBundle.message("haxe.codestyle.wrapping.body." + construct.toLowerCase(Locale.ROOT));
      consumer.showCustomOption(HaxeCodeStyleSettings.class, construct + "_BODY_PLACEMENT", title, group, names, values);
    }
  }

  @Override
  public IndentOptionsEditor getIndentOptionsEditor() {
    return new IndentOptionsEditor(this);
  }

  @Language("Haxe")
  public static final String SPACING_CODE_SAMPLE = """
    @:native("Foo")
    @:keep
    class Foo<T> {
        var items:Array<T>;
        var lookup:Map<Int, String>;

        function compute(a:Int, b:Int):Int {
            // running total of the sample
            var apply:Int -> Int = v -> v * 2;
            var twice:(Int) -> Int = apply;
            var point = {x: a, y: b};
            var flag = !(a == b) && a <= b || a != 0;
            var bits = (a & 3) ^ (b | 1) << 2 >> 1;
            var sum = a + b * 2 - b % 3;
            var pick = flag ? apply(sum) : -sum;
            var name = 'value ${pick} of ${sum + 1}';
            var head = items[0];
            var asInt = (head : Int);
            for (i in 0...3) {
                sum += i;
            }
            while (sum > 9) {
                sum -= 2;
            }
            do {
                sum++;
            } while (sum < 5);
            try {
                check(sum, name);
            } catch (e:String) {
                sum = 0;
            }
            switch (sum) {
                case 0:
                    sum = 1;
                default:
                    sum = 2;
            }
            if (sum > 1) {
                sum--;
            } else {
                sum++;
            }
            return sum + point.x + twice(1);
        }

        function check(v:Int, label:String) {}
    }
    """;

  @Language("Haxe")
  public static final String WRAPPING_CODE_SAMPLE = """
    // a comment kept at the first column
    /**
     * A component with one long method.
       Documentation lines reindent under the comment.
     */
    class Foo extends BaseComponent implements Drawable implements Resizable implements Serializable implements Comparable implements Observable {
        /* a plain multi-line comment
               whose lines reindent under it */
        function fLong(argumentAlpha:Int, argumentBravo:Int, argumentCharlie:Int, argumentDelta:Int, argumentEcho:Int, argumentFoxtrot:Int):Int {
            var planets = ['mercury', 'venus', 'earth', 'mars', 'jupiter', 'saturn', 'uranus', 'neptune', 'ceres', 'pluto', 'haumea', 'makemake'];
            var shouted = planets.filter(word -> word.length > 4).map(word -> word.toUpperCase()).join(', ') + planets.join('; ') + 'end';
            var total = argumentAlpha + argumentBravo + argumentCharlie + argumentDelta + argumentEcho + argumentFoxtrot + planets.length;
            total = argumentAlpha * argumentBravo + argumentCharlie * argumentDelta + argumentEcho * argumentFoxtrot - shouted.length;
            var label = total > 100 ? 'a rather large total for such a small example' : 'a rather small total for such a large example';
            var grouped = (argumentAlpha + argumentBravo
                           + argumentCharlie);
            var first = 1, second = 2, third = 3;
            var ready = first > second && second > third || total > first && grouped > second || label.length > third;
            if (total > 6) total--;
            else total++;
            for (i in 0...3) total += i;
            while (total > 99) total -= 2;
            do total++ while (total < 5);
            try fLong(1, 2, 3, 4, 5, 6) catch (ignored:String) total = 0;
            switch (total) {
                case 0: total = 1;
                default: total = 2;
            }
            var pick = if (total > 3) 'many' else 'few';
            if (total == 0) {
            }
            var emptyCallback = function() {
            };
            var arrowCallback = () -> {
            };
            if (grouped > 1) {
                total += grouped;
            } else if (label.length > 3) {
                total -= grouped;
            } else {
                total = 0;
            }
            do {
                total--;
            } while (total > 99);
            try {
                fLong(total + 100, total + 200, total + 300, total + 400, total + 500, total + 600);
            } catch (ignored:String) {}
            #if debug
            trace('debug build');
            #else
            trace('release build');
            #end
            return
                total;
        }

        function fEmpty() {
        }

        function fQuick() return 'fast';

        function fMerge(base:{> Iterable<String>,
            var label:String;
        }) {
            return base.label;
        }

        public function new() {}
    }
    """;

  @Language("Haxe")
  public static final String BLANK_LINES_CODE_SAMPLE = """
    /*
     * File header comment.
     */
    package foo.bar;
    import a.b.SomeClass;
    import a.b.SomeWidget;
    using someUtil;
    interface Drawable {}

    interface Resizable {}
    class Foo {


        static var shared:Int = 0;
        var counter:Int = 0;
        public var total:Int = 1;
        /** The label shown next to the total. */
        var label:String = "";
        var flag:Bool = false;
        public function new() {
        }
        public static function main() {

            trace("Hello!");
            switch (shared) {
                case 0:

                    trace("zero");
                default:
                    trace("other");
            }


        }

    }
    class Bar {}
    """;
}
