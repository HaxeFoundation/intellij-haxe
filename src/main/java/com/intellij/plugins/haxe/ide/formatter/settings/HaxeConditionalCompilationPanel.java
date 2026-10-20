package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;

/**
 * The Conditional Compilation tab of the Haxe code style: how INACTIVE
 * regions (every branch the current defines exclude - #if, #elseif and #else
 * alike) are treated by reformat. Its options span indentation, spacing and
 * line breaks at once, so they live in their own tab instead of Wrapping.
 */
public class HaxeConditionalCompilationPanel extends HaxeOptionsPreviewPanelBase {

  private final JBCheckBox formatInactive =
    new JBCheckBox(HaxeCodeStyleBundle.message("haxe.codestyle.cc.format.inactive"));
  private final JBCheckBox alignInactive =
    new JBCheckBox(HaxeCodeStyleBundle.message("haxe.codestyle.cc.align.inactive"));

  protected HaxeConditionalCompilationPanel(CodeStyleSettings settings) {
    super(settings);
    JPanel form = FormBuilder.createFormBuilder()
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.cc.inactive.title")))
      .addComponent(formatInactive)
      .addComponent(alignInactive)
      .getPanel();
    initPanel(form);
    watch(formatInactive);
    watch(alignInactive);
  }

  @Override
  protected String getTabTitle() {
    return HaxeCodeStyleBundle.message("haxe.codestyle.cc.tab.title");
  }

  @Override
  public void apply(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    haxe.FORMAT_INACTIVE_BRANCHES = formatInactive.isSelected();
    haxe.ALIGN_INACTIVE_CONDITIONAL_BRANCHES = alignInactive.isSelected();
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    return haxe.FORMAT_INACTIVE_BRANCHES != formatInactive.isSelected()
           || haxe.ALIGN_INACTIVE_CONDITIONAL_BRANCHES != alignInactive.isSelected();
  }

  @Override
  protected void resetImpl(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    formatInactive.setSelected(haxe.FORMAT_INACTIVE_BRANCHES);
    alignInactive.setSelected(haxe.ALIGN_INACTIVE_CONDITIONAL_BRANCHES);
  }

  @Override
  protected @Nullable String getPreviewText() {
    return CONDITIONAL_CODE_SAMPLE;
  }

  // deliberately messy inactive branches: with formatting ON the preview
  // cleans them up, with it OFF they stay exactly like this
  @Language("Haxe")
  public static final String CONDITIONAL_CODE_SAMPLE = """
    class Main {
         #if my_flag
        static   function helper( value:Int ):Void {
        trace(   "helper"  ,value+1 );
        }
         #end

         static function main() {
              #if js
             trace(   "js"  ,1+2 );
              var point=  {x:1,y:2};
              #elseif neko
               trace( "neko" );
              #else
             trace("other target"   );
              #end
              var mode = #if debug "debug" #else "release"   #end;
         }
    }
    """;
}
