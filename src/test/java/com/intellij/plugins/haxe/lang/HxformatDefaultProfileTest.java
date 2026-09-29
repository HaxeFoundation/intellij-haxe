package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatDefaultProfile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The settings image {@link HxformatDefaultProfile#apply} installs holds only values the settings UI can show. */
@DisplayName("Formatting: hxformat default profile")
public class HxformatDefaultProfileTest extends HaxeLightFixtureTestCase {

  // the wrap combo values: "chop down if long" is WRAP_ON_EVERY_ITEM|WRAP_AS_NEEDED (5), never bare WRAP_ON_EVERY_ITEM (4)
  private static final List<Integer> UI_WRAP_VALUES = List.of(
    CommonCodeStyleSettings.DO_NOT_WRAP,
    CommonCodeStyleSettings.WRAP_AS_NEEDED,
    CommonCodeStyleSettings.WRAP_ALWAYS,
    CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM | CommonCodeStyleSettings.WRAP_AS_NEEDED);
  private static final List<Integer> UI_BRACE_VALUES = List.of(
    CommonCodeStyleSettings.END_OF_LINE,
    CommonCodeStyleSettings.NEXT_LINE,
    CommonCodeStyleSettings.NEXT_LINE_SHIFTED,
    CommonCodeStyleSettings.NEXT_LINE_SHIFTED2,
    CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED);

  @Override
  protected String getBasePath() {
    return "/formatter/comparison/";
  }

  /** A scheme holding any other value renders as "Invalid option value" in every wrap or brace combo box. */
  @Test
  @DisplayName("wrap and brace values use the settings ui encoding")
  public void testWrapAndBraceValuesUseTheSettingsUiEncoding() throws Exception {
    CodeStyleSettings settings = projectSettingsCopy();

    HxformatDefaultProfile.apply(settings);

    CommonCodeStyleSettings common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    List<String> offenders = new ArrayList<>();
    for (Field field : CommonCodeStyleSettings.class.getFields()) {
      if (field.getType() != int.class) continue;
      String name = field.getName();
      int value = field.getInt(common);
      if (name.endsWith("_WRAP") && !UI_WRAP_VALUES.contains(value)) {
        offenders.add(name + "=" + value + " is not a settings-UI wrap value " + UI_WRAP_VALUES);
      }
      if (name.endsWith("BRACE_STYLE") && !UI_BRACE_VALUES.contains(value)) {
        offenders.add(name + "=" + value + " is not a settings-UI brace value " + UI_BRACE_VALUES);
      }
    }
    assertTrue(offenders.isEmpty(), String.join("\n", offenders));
  }
}
