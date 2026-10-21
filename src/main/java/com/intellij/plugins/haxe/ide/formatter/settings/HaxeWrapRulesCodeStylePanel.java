package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.FormBuilder;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.util.List;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * The Wrap Rules tab of the Haxe code style: the numeric thresholds of the
 * operator chain, multi-var and literal item rules (hxformat's
 * wrapping.opBoolChain, opAddSubChain, multiVar, arrayWrap, mapWrap and
 * objectLiteral). They are integers, and the Wrapping tab's option table
 * shows only booleans and choices.
 */
public class HaxeWrapRulesCodeStylePanel extends HaxeOptionsPreviewPanelBase {

  private static final int MAX_COLUMNS = 999;

  /** One integer option: its field and the setting it edits. */
  private record Option(IntegerField field, ToIntFunction<HaxeCodeStyleSettings> get, ObjIntConsumer<HaxeCodeStyleSettings> set) {
    Option(ToIntFunction<HaxeCodeStyleSettings> get, ObjIntConsumer<HaxeCodeStyleSettings> set) {
      this(new IntegerField(null, 0, MAX_COLUMNS), get, set);
    }
  }

  private final Option boolLineLength = new Option(h -> h.BOOL_CHAIN_SPLIT_LINE_LENGTH, (h, v) -> h.BOOL_CHAIN_SPLIT_LINE_LENGTH = v);
  private final Option boolItemLength = new Option(h -> h.BOOL_CHAIN_SPLIT_ITEM_LENGTH, (h, v) -> h.BOOL_CHAIN_SPLIT_ITEM_LENGTH = v);
  private final Option boolItemCount = new Option(h -> h.BOOL_CHAIN_SPLIT_ITEM_COUNT, (h, v) -> h.BOOL_CHAIN_SPLIT_ITEM_COUNT = v);
  private final Option boolTotalLength = new Option(h -> h.BOOL_CHAIN_SPLIT_TOTAL_LENGTH, (h, v) -> h.BOOL_CHAIN_SPLIT_TOTAL_LENGTH = v);
  private final Option addLineLength = new Option(h -> h.ADD_CHAIN_SPLIT_LINE_LENGTH, (h, v) -> h.ADD_CHAIN_SPLIT_LINE_LENGTH = v);
  private final Option addItemLength = new Option(h -> h.ADD_CHAIN_SPLIT_ITEM_LENGTH, (h, v) -> h.ADD_CHAIN_SPLIT_ITEM_LENGTH = v);
  private final Option addItemCount = new Option(h -> h.ADD_CHAIN_SPLIT_ITEM_COUNT, (h, v) -> h.ADD_CHAIN_SPLIT_ITEM_COUNT = v);
  private final Option addTotalLength = new Option(h -> h.ADD_CHAIN_SPLIT_TOTAL_LENGTH, (h, v) -> h.ADD_CHAIN_SPLIT_TOTAL_LENGTH = v);
  private final Option multiVarSplitWidth = new Option(h -> h.MULTI_VAR_SPLIT_WIDTH, (h, v) -> h.MULTI_VAR_SPLIT_WIDTH = v);
  private final Option multiVarFillItemLength = new Option(h -> h.MULTI_VAR_FILL_ITEM_LENGTH, (h, v) -> h.MULTI_VAR_FILL_ITEM_LENGTH = v);
  private final Option arrayKeepTotalLength = new Option(h -> h.ARRAY_KEEP_TOTAL_LENGTH, (h, v) -> h.ARRAY_KEEP_TOTAL_LENGTH = v);
  private final Option arrayChopItemLength = new Option(h -> h.ARRAY_CHOP_ITEM_LENGTH, (h, v) -> h.ARRAY_CHOP_ITEM_LENGTH = v);
  private final Option arrayChopItemCount = new Option(h -> h.ARRAY_CHOP_ITEM_COUNT, (h, v) -> h.ARRAY_CHOP_ITEM_COUNT = v);
  private final Option arrayFillEqualItemLength = new Option(h -> h.ARRAY_FILL_EQUAL_ITEM_LENGTH, (h, v) -> h.ARRAY_FILL_EQUAL_ITEM_LENGTH = v);
  private final Option arrayFillEqualItemCount = new Option(h -> h.ARRAY_FILL_EQUAL_ITEM_COUNT, (h, v) -> h.ARRAY_FILL_EQUAL_ITEM_COUNT = v);
  private final Option arrayFillItemLength = new Option(h -> h.ARRAY_FILL_ITEM_LENGTH, (h, v) -> h.ARRAY_FILL_ITEM_LENGTH = v);
  private final Option arrayFillItemCount = new Option(h -> h.ARRAY_FILL_ITEM_COUNT, (h, v) -> h.ARRAY_FILL_ITEM_COUNT = v);
  private final Option mapKeepTotalLength = new Option(h -> h.MAP_KEEP_TOTAL_LENGTH, (h, v) -> h.MAP_KEEP_TOTAL_LENGTH = v);
  private final Option mapChopItemLength = new Option(h -> h.MAP_CHOP_ITEM_LENGTH, (h, v) -> h.MAP_CHOP_ITEM_LENGTH = v);
  private final Option mapChopItemCount = new Option(h -> h.MAP_CHOP_ITEM_COUNT, (h, v) -> h.MAP_CHOP_ITEM_COUNT = v);
  private final Option mapFillEqualItemLength = new Option(h -> h.MAP_FILL_EQUAL_ITEM_LENGTH, (h, v) -> h.MAP_FILL_EQUAL_ITEM_LENGTH = v);
  private final Option mapFillEqualItemCount = new Option(h -> h.MAP_FILL_EQUAL_ITEM_COUNT, (h, v) -> h.MAP_FILL_EQUAL_ITEM_COUNT = v);
  private final Option mapFillItemLength = new Option(h -> h.MAP_FILL_ITEM_LENGTH, (h, v) -> h.MAP_FILL_ITEM_LENGTH = v);
  private final Option mapFillItemCount = new Option(h -> h.MAP_FILL_ITEM_COUNT, (h, v) -> h.MAP_FILL_ITEM_COUNT = v);
  private final Option objectKeepItemCount = new Option(h -> h.OBJECT_KEEP_ITEM_COUNT, (h, v) -> h.OBJECT_KEEP_ITEM_COUNT = v);
  private final Option objectChopItemLength = new Option(h -> h.OBJECT_CHOP_ITEM_LENGTH, (h, v) -> h.OBJECT_CHOP_ITEM_LENGTH = v);
  private final Option objectChopTotalLength = new Option(h -> h.OBJECT_CHOP_TOTAL_LENGTH, (h, v) -> h.OBJECT_CHOP_TOTAL_LENGTH = v);
  private final Option objectChopItemCount = new Option(h -> h.OBJECT_CHOP_ITEM_COUNT, (h, v) -> h.OBJECT_CHOP_ITEM_COUNT = v);
  private final List<Option> options = List.of(
    boolLineLength, boolItemLength, boolItemCount, boolTotalLength,
    addLineLength, addItemLength, addItemCount, addTotalLength,
    multiVarSplitWidth, multiVarFillItemLength,
    arrayKeepTotalLength, arrayChopItemLength, arrayChopItemCount,
    arrayFillEqualItemLength, arrayFillEqualItemCount, arrayFillItemLength, arrayFillItemCount,
    mapKeepTotalLength, mapChopItemLength, mapChopItemCount,
    mapFillEqualItemLength, mapFillEqualItemCount, mapFillItemLength, mapFillItemCount,
    objectKeepItemCount, objectChopItemLength, objectChopTotalLength, objectChopItemCount);

  protected HaxeWrapRulesCodeStylePanel(CodeStyleSettings settings) {
    super(settings);
    JPanel form = FormBuilder.createFormBuilder()
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.bool.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.line.length"), boolLineLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.item.length"), boolItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.item.count"), boolItemCount.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.total.length"), boolTotalLength.field())
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.add.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.line.length"), addLineLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.item.length"), addItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.item.count"), addItemCount.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.chain.total.length"), addTotalLength.field())
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.multi.var.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.multi.var.split.width"), multiVarSplitWidth.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.multi.var.fill.item"), multiVarFillItemLength.field())
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.keep.total"), arrayKeepTotalLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.chop.item.length"), arrayChopItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.chop.item.count"), arrayChopItemCount.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.equal.item.length"), arrayFillEqualItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.equal.item.count"), arrayFillEqualItemCount.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.item.length"), arrayFillItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.item.count"), arrayFillItemCount.field())
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.map.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.keep.total"), mapKeepTotalLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.chop.item.length"), mapChopItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.chop.item.count"), mapChopItemCount.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.equal.item.length"), mapFillEqualItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.equal.item.count"), mapFillEqualItemCount.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.item.length"), mapFillItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.array.fill.item.count"), mapFillItemCount.field())
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.object.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.object.keep.item.count"), objectKeepItemCount.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.object.chop.item.length"), objectChopItemLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.object.chop.total.length"), objectChopTotalLength.field())
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.object.chop.item.count"), objectChopItemCount.field())
      .getPanel();
    initPanel(form);
    options.forEach(option -> watch(option.field()));
  }

  @Override
  protected String getTabTitle() {
    return HaxeCodeStyleBundle.message("haxe.codestyle.wraprules.tab.title");
  }

  @Override
  public void apply(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    options.forEach(option -> option.set().accept(haxe, option.field().getValue()));
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    return options.stream().anyMatch(option -> option.get().applyAsInt(haxe) != option.field().getValue());
  }

  @Override
  protected void resetImpl(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    options.forEach(option -> option.field().setValue(option.get().applyAsInt(haxe)));
  }

  @Override
  protected @Nullable String getPreviewText() {
    return WRAP_RULES_CODE_SAMPLE;
  }

  // one example per rule, each variable named for the rule it demonstrates
  @Language("Haxe")
  public static final String WRAP_RULES_CODE_SAMPLE = """
    class Main {
        static function main() {
            // multi-var: short declarators fill the line, long ones split one per line
            var short1 = 1, short2 = 2, short3 = 3, short4 = 4, short5 = 5, short6 = 6, short7 = 7, short8 = 8;
            var longDeclaratorOne = 640, longDeclaratorTwo = 480, longDeclaratorThree = 32, longDeclaratorFour = 60;

            // boolean chains: up to three operands stay; many operands split by count or total
            var fewOperands = short1 > short2 && short2 > short3;
            var manyOperands = short1 > short2 && short2 > short3 && short3 > short4 && short4 > short5 && short5 > short6 && short6 > short7;

            // a long line splits one operand per line when an operand is long, else fills
            var longOperand = isLongOperandOnALongLine(longDeclaratorOne, longDeclaratorTwo) && fewOperands && manyOperands;
            var longLine = isLongOperandOnALongLine(longDeclaratorOne, longDeclaratorTwo) && isLongOperandOnALongLine(longDeclaratorThree, longDeclaratorFour) && fewOperands;

            // additive chains follow the same rules with their own thresholds
            var fewTerms = short1 + short2 + short3;
            var manyTerms = short1 + short2 + short3 + short4 + short5 + short6 + short7 + short8 + longDeclaratorOne;
            var longTerm = longTermOnALongLine(longDeclaratorOne, longDeclaratorTwo) + fewTerms + manyTerms;
            var longLineOfTerms = longTermOnALongLine(longDeclaratorOne, longDeclaratorTwo) + longTermOnALongLine(longDeclaratorThree, longDeclaratorFour) + fewTerms;

            // array literals: a short list stays, many or long items go one per line, tiny or equal-length items fill after a leading break
            var shortList = [short1, short2, short3, short4];
            var manyItems = [longDeclaratorOne, longDeclaratorTwo, longDeclaratorThree, longDeclaratorFour, longDeclaratorOne, longDeclaratorTwo];
            var longItem = [isLongOperandOnALongLine(longDeclaratorOne, longDeclaratorTwo), fewOperands, manyOperands];
            var tinyItems = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40];
            var equalItems = ["item number 01", "item number 02", "item number 03", "item number 04", "item number 05", "item number 06", "item number 07", "item number 08", "item number 09", "item number 10"];

            // map literals follow the array rules on their entries
            var shortMap = [1 => short1, 2 => short2];
            var manyEntries = ["alpha" => longDeclaratorOne, "bravo" => longDeclaratorTwo, "charlie" => longDeclaratorThree, "delta" => longDeclaratorFour];

            // object literals: up to three fields on a fitting line stay, four or more go one per line
            var point = {x: short1, y: short2};
            var box = {x: short1, y: short2, width: short3, height: short4};
        }

        static function isLongOperandOnALongLine(width:Int, height:Int):Bool {
            return width > height;
        }

        static function longTermOnALongLine(width:Int, height:Int):Int {
            return width + height;
        }
    }
    """;
}
