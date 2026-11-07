package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.ui.components.JBCheckBox;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;

/**
 * The Documentation tab of the Haxe code style, the counterpart of Java's
 * JavaDoc tab: whether reformat indents doc comment lines, and the layout a
 * new doc comment gets. Layout in the bound .form.
 */
public class HaxeDocCommentCodeStylePanel extends HaxeOptionsPreviewPanelBase {

  // the combo box lists the styles in this order
  private static final int[] STYLES = {HaxeCodeStyleSettings.DOC_COMMENT_STYLE_HAXE, HaxeCodeStyleSettings.DOC_COMMENT_STYLE_JAVADOC};

  private JPanel form;
  private JBCheckBox formatCheckBox;
  private ComboBox<String> styleComboBox;

  protected HaxeDocCommentCodeStylePanel(CodeStyleSettings settings) {
    super(settings);
    styleComboBox.addItem(HaxeCodeStyleBundle.message("haxe.codestyle.docs.style.haxe"));
    styleComboBox.addItem(HaxeCodeStyleBundle.message("haxe.codestyle.docs.style.javadoc"));
    initPanel(form);
    watch(formatCheckBox);
  }

  @Override
  protected String getTabTitle() {
    return HaxeCodeStyleBundle.message("haxe.codestyle.docs.tab.title");
  }

  @Override
  public void apply(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    haxe.FORMAT_DOC_COMMENTS = formatCheckBox.isSelected();
    haxe.DOC_COMMENT_STYLE = selectedStyle();
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    return haxe.FORMAT_DOC_COMMENTS != formatCheckBox.isSelected()
           || haxe.DOC_COMMENT_STYLE != selectedStyle();
  }

  @Override
  protected void resetImpl(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    formatCheckBox.setSelected(haxe.FORMAT_DOC_COMMENTS);
    styleComboBox.setSelectedIndex(Math.max(0, indexOfStyle(haxe.DOC_COMMENT_STYLE)));
  }

  private int selectedStyle() {
    return STYLES[Math.max(0, styleComboBox.getSelectedIndex())];
  }

  private static int indexOfStyle(int style) {
    for (int i = 0; i < STYLES.length; i++) {
      if (STYLES[i] == style) return i;
    }
    return -1;
  }

  @Override
  protected @Nullable String getPreviewText() {
    return DOC_COMMENT_CODE_SAMPLE;
  }

  @Language("Haxe")
  public static final String DOC_COMMENT_CODE_SAMPLE = """
    class Main {
    /**
    Haxe style: bare lines, closed by two stars.

    \t* a markdown bullet keeps its star
    **/
        static function main() {
            trace(sum(1, 2));
        }

    /**
    * Javadoc style: a star on each line.
    *   deeper text keeps its indent after the star
    * @param a the first value
    * @return the sum
    */
        static function sum(a:Int, b:Int):Int {
            return a + b;
        }
    }
    """;
}
