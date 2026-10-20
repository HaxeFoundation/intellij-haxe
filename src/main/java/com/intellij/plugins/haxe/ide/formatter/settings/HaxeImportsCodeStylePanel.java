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

/**
 * The Imports tab of the Haxe code style: import grouping and the blank-line
 * cap within the import section. Kept out of Blank Lines because every peer
 * language (Java/Kotlin/Groovy) hosts import behavior in a dedicated tab.
 */
public class HaxeImportsCodeStylePanel extends HaxeOptionsPreviewPanelBase {

  private final IntegerField keepBetweenImports = new IntegerField(null, 0, 99);
  private final IntegerField blanksBetweenGroups = new IntegerField(null, 0, 99);
  private final IntegerField groupPackageDepth = new IntegerField(null, 1, 99);

  protected HaxeImportsCodeStylePanel(CodeStyleSettings settings) {
    super(settings);
    JPanel form = FormBuilder.createFormBuilder()
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.imports.grouping.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.imports.grouping.blanks"), blanksBetweenGroups)
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.imports.grouping.depth"), groupPackageDepth)
      .addComponent(new TitledSeparator(HaxeCodeStyleBundle.message("haxe.codestyle.imports.keep.title")))
      .addLabeledComponent(HaxeCodeStyleBundle.message("haxe.codestyle.imports.keep.between"), keepBetweenImports)
      .getPanel();
    initPanel(form);
    watch(keepBetweenImports);
    watch(blanksBetweenGroups);
    watch(groupPackageDepth);
  }

  @Override
  protected String getTabTitle() {
    return HaxeCodeStyleBundle.message("haxe.codestyle.imports.tab.title");
  }

  @Override
  public void apply(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    haxe.KEEP_BLANK_LINES_BETWEEN_IMPORTS = keepBetweenImports.getValue();
    haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS = blanksBetweenGroups.getValue();
    haxe.IMPORT_GROUP_PACKAGE_DEPTH = groupPackageDepth.getValue();
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    return haxe.KEEP_BLANK_LINES_BETWEEN_IMPORTS != keepBetweenImports.getValue()
           || haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS != blanksBetweenGroups.getValue()
           || haxe.IMPORT_GROUP_PACKAGE_DEPTH != groupPackageDepth.getValue();
  }

  @Override
  protected void resetImpl(@NotNull CodeStyleSettings settings) {
    HaxeCodeStyleSettings haxe = haxeSettings(settings);
    keepBetweenImports.setValue(haxe.KEEP_BLANK_LINES_BETWEEN_IMPORTS);
    blanksBetweenGroups.setValue(haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS);
    groupPackageDepth.setValue(haxe.IMPORT_GROUP_PACKAGE_DEPTH);
  }

  @Override
  protected @Nullable String getPreviewText() {
    return IMPORTS_CODE_SAMPLE;
  }

  @Language("Haxe")
  public static final String IMPORTS_CODE_SAMPLE = """
    package;
    import haxe.ds.StringMap;
    import haxe.io.Bytes;

    import sys.io.File;
    import sys.FileSystem;
    import a.b.Widget;
    import Std;
    using StringTools;

    class Main {
         static function main() {
              var m = new StringMap<Int>();
              trace(m + Std.string(Bytes.alloc(1)));
         }
    }
    """;
}
