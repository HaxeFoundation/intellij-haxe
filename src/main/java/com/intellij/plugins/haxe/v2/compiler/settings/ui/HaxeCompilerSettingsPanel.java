package com.intellij.plugins.haxe.v2.compiler.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Swing panel for the Haxe compiler settings page: the default language
 * level, the completion source, the compiler-diagnostics toggles and a
 * per-module level override table. It holds no reference to project
 * services, so tests can exercise it; the configurable feeds it data and
 * reads it back.
 */
public final class HaxeCompilerSettingsPanel {

  /** One table row: a module and its (optional) language level override. */
  static final class ModuleLevelRow {
    final String moduleName;
    @Nullable HaxeLanguageLevel override;

    ModuleLevelRow(@NotNull String moduleName, @Nullable HaxeLanguageLevel override) {
      this.moduleName = moduleName;
      this.override = override;
    }
  }

  /**
   * Combo item wrapper so the "no explicit level" choice does not need a null
   * entry in the combo model. In the default combo the null choice means
   * "use compiler level"; in a module row it means "project default".
   */
  private record LevelChoice(@Nullable HaxeLanguageLevel level) {
    static final LevelChoice NO_EXPLICIT_LEVEL = new LevelChoice(null);

    static LevelChoice of(@Nullable HaxeLanguageLevel level) {
      return level == null ? NO_EXPLICIT_LEVEL : new LevelChoice(level);
    }
  }

  private final ComboBox<LevelChoice> defaultLevelCombo = createLevelChoiceCombo(this::defaultChoiceText);
  private final ComboBox<HaxeCompletionMode> completionModeCombo = new ComboBox<>(HaxeCompletionMode.values());
  /** Level resolved from the SDK, shown in the "use compiler level" labels; null when no SDK. */
  private @Nullable HaxeLanguageLevel compilerLevel;
  private final JCheckBox useLevelForConditionalsCheckBox =
    new JCheckBox(HaxeBundle.message("haxe.compiler.conditionals.language.level.checkbox"));
  private final JCheckBox compilerIdeFeaturesCheckBox =
    new JCheckBox(HaxeBundle.message("haxe.compiler.ide.features.checkbox"));
  private final JBLabel compilerIdeFeaturesHint =
    new JBLabel(HaxeBundle.message("haxe.compiler.ide.features.hint"), UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
  private final JCheckBox compilerDiagnosticsCheckBox =
    new JCheckBox(HaxeBundle.message("haxe.compiler.diagnostics.checkbox"));
  private final JCheckBox diagnosticsErrorsCheckBox =
    new JCheckBox(HaxeBundle.message("haxe.compiler.diagnostics.errors.checkbox"));
  private final JCheckBox diagnosticsUnusedImportsCheckBox =
    new JCheckBox(HaxeBundle.message("haxe.compiler.diagnostics.unused.imports.checkbox"));
  private final JCheckBox diagnosticsRemovableCodeCheckBox =
    new JCheckBox(HaxeBundle.message("haxe.compiler.diagnostics.removable.code.checkbox"));
  private final JCheckBox diagnosticsOnlyCheckBox =
    new JCheckBox(HaxeBundle.message("haxe.compiler.diagnostics.only.checkbox"));
  private final ListTableModel<ModuleLevelRow> tableModel = new ListTableModel<>(new ModuleColumn(), new LevelColumn());
  private final TableView<ModuleLevelRow> table = new TableView<>(tableModel);
  private final JPanel mainPanel;

  public HaxeCompilerSettingsPanel() {
    // "Project default (x.y)" cells display the selected default, keep them in sync.
    defaultLevelCombo.addActionListener(e -> table.repaint());

    compilerDiagnosticsCheckBox.setToolTipText(HaxeBundle.message("haxe.compiler.diagnostics.tooltip"));
    diagnosticsErrorsCheckBox.setToolTipText(HaxeBundle.message("haxe.compiler.diagnostics.errors.tooltip"));
    diagnosticsUnusedImportsCheckBox.setToolTipText(HaxeBundle.message("haxe.compiler.diagnostics.unused.imports.tooltip"));
    diagnosticsRemovableCodeCheckBox.setToolTipText(HaxeBundle.message("haxe.compiler.diagnostics.removable.code.tooltip"));
    diagnosticsOnlyCheckBox.setToolTipText(HaxeBundle.message("haxe.compiler.diagnostics.only.tooltip"));
    for (JCheckBox child : diagnosticsChildren()) {
      child.setBorder(JBUI.Borders.emptyLeft(24));
    }
    compilerDiagnosticsCheckBox.addItemListener(e -> updateDiagnosticsChildEnablement());
    updateDiagnosticsChildEnablement();
    completionModeCombo.setRenderer(BuilderKt.textListCellRenderer("", HaxeCompletionMode::getPresentableText));
    completionModeCombo.setToolTipText(HaxeBundle.message("haxe.compiler.completion.mode.tooltip"));
    compilerIdeFeaturesCheckBox.setToolTipText(HaxeBundle.message("haxe.compiler.ide.features.tooltip"));
    // the hint sits under the checkbox text, past its box
    compilerIdeFeaturesHint.setBorder(JBUI.Borders.emptyLeft(24));

    useLevelForConditionalsCheckBox.setToolTipText(HaxeBundle.message("haxe.compiler.conditionals.language.level.tooltip"));

    table.setShowGrid(false);
    table.setRowHeight(defaultLevelCombo.getPreferredSize().height);

    mainPanel = FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeBundle.message("haxe.compiler.default.language.level"), defaultLevelCombo)
      .addComponent(useLevelForConditionalsCheckBox)
      .addLabeledComponent(HaxeBundle.message("haxe.compiler.completion.mode"), completionModeCombo)
      .addComponent(compilerIdeFeaturesCheckBox)
      .addComponent(compilerIdeFeaturesHint)
      .addComponent(compilerDiagnosticsCheckBox)
      .addComponent(diagnosticsErrorsCheckBox)
      .addComponent(diagnosticsUnusedImportsCheckBox)
      .addComponent(diagnosticsRemovableCodeCheckBox)
      .addComponent(diagnosticsOnlyCheckBox)
      .addComponentFillVertically(new JBScrollPane(table), 8)
      .getPanel();
  }

  public boolean isUseLanguageLevelForConditionals() {
    return useLevelForConditionalsCheckBox.isSelected();
  }

  public void setUseLanguageLevelForConditionals(boolean enabled) {
    useLevelForConditionalsCheckBox.setSelected(enabled);
  }

  public boolean isCompilerDiagnosticsEnabled() {
    return compilerDiagnosticsCheckBox.isSelected();
  }

  public void setCompilerDiagnosticsEnabled(boolean enabled) {
    compilerDiagnosticsCheckBox.setSelected(enabled);
  }

  public boolean isDiagnosticsErrorsEnabled() {
    return diagnosticsErrorsCheckBox.isSelected();
  }

  public void setDiagnosticsErrorsEnabled(boolean enabled) {
    diagnosticsErrorsCheckBox.setSelected(enabled);
  }

  public boolean isDiagnosticsUnusedImportsEnabled() {
    return diagnosticsUnusedImportsCheckBox.isSelected();
  }

  public void setDiagnosticsUnusedImportsEnabled(boolean enabled) {
    diagnosticsUnusedImportsCheckBox.setSelected(enabled);
  }

  public boolean isDiagnosticsRemovableCodeEnabled() {
    return diagnosticsRemovableCodeCheckBox.isSelected();
  }

  public void setDiagnosticsRemovableCodeEnabled(boolean enabled) {
    diagnosticsRemovableCodeCheckBox.setSelected(enabled);
  }

  public boolean isCompilerDiagnosticsOnly() {
    return diagnosticsOnlyCheckBox.isSelected();
  }

  public void setCompilerDiagnosticsOnly(boolean enabled) {
    diagnosticsOnlyCheckBox.setSelected(enabled);
  }

  private List<JCheckBox> diagnosticsChildren() {
    return List.of(diagnosticsErrorsCheckBox, diagnosticsUnusedImportsCheckBox, diagnosticsRemovableCodeCheckBox,
                   diagnosticsOnlyCheckBox);
  }

  /// The per-feature toggles only apply while the master toggle is on.
  private void updateDiagnosticsChildEnablement() {
    for (JCheckBox child : diagnosticsChildren()) {
      child.setEnabled(compilerDiagnosticsCheckBox.isSelected());
    }
  }

  @NotNull
  public HaxeCompletionMode getCompletionMode() {
    HaxeCompletionMode selected = completionModeCombo.getItem();
    return selected != null ? selected : HaxeCompletionMode.IDE_AND_COMPILER;
  }

  public void setCompletionMode(@NotNull HaxeCompletionMode mode) {
    completionModeCombo.setSelectedItem(mode);
  }

  public boolean isCompilerIdeFeaturesEnabled() {
    return compilerIdeFeaturesCheckBox.isSelected();
  }

  public void setCompilerIdeFeaturesEnabled(boolean enabled) {
    compilerIdeFeaturesCheckBox.setSelected(enabled);
  }

  @NotNull
  public JComponent getComponent() {
    return mainPanel;
  }

  /** A null {@code explicitDefault} selects "use compiler level"; {@code compilerLevel} feeds its label. */
  public void reset(@Nullable HaxeLanguageLevel explicitDefault,
                    @Nullable HaxeLanguageLevel compilerLevel,
                    @NotNull Map<String, HaxeLanguageLevel> overrides,
                    @NotNull List<String> moduleNames) {
    this.compilerLevel = compilerLevel;
    defaultLevelCombo.setSelectedItem(LevelChoice.of(explicitDefault));
    List<ModuleLevelRow> rows = moduleNames.stream()
      .map(name -> new ModuleLevelRow(name, overrides.get(name)))
      .toList();
    tableModel.setItems(rows);
  }

  /** The chosen explicit default level, or null for "use compiler level". */
  @Nullable
  public HaxeLanguageLevel getSelectedDefaultLevel() {
    LevelChoice choice = defaultLevelCombo.getItem();
    return choice == null ? null : choice.level();
  }

  /** Only modules with an explicit override are returned. */
  @NotNull
  public Map<String, HaxeLanguageLevel> getModuleOverrides() {
    stopTableEditing();
    Map<String, HaxeLanguageLevel> result = new LinkedHashMap<>();
    for (ModuleLevelRow row : tableModel.getItems()) {
      if (row.override != null) {
        result.put(row.moduleName, row.override);
      }
    }
    return result;
  }

  private void stopTableEditing() {
    TableCellEditor editor = table.getCellEditor();
    if (editor != null) {
      editor.stopCellEditing();
    }
  }

  private static final class ModuleColumn extends ColumnInfo<ModuleLevelRow, String> {
    ModuleColumn() {
      super(HaxeBundle.message("haxe.compiler.module.column"));
    }

    @Override
    public String valueOf(ModuleLevelRow row) {
      return row.moduleName;
    }
  }

  private final class LevelColumn extends ColumnInfo<ModuleLevelRow, HaxeLanguageLevel> {
    LevelColumn() {
      super(HaxeBundle.message("haxe.compiler.language.level.column"));
    }

    @Override
    public @Nullable HaxeLanguageLevel valueOf(ModuleLevelRow row) {
      return row.override;
    }

    @Override
    public void setValue(ModuleLevelRow row, @Nullable HaxeLanguageLevel value) {
      row.override = value;
    }

    @Override
    public boolean isCellEditable(ModuleLevelRow row) {
      return true;
    }

    @Override
    public TableCellRenderer getRenderer(ModuleLevelRow row) {
      return new DefaultTableCellRenderer() {
        @Override
        protected void setValue(Object value) {
          setText(levelText((HaxeLanguageLevel)value));
        }
      };
    }

    @Override
    public TableCellEditor getEditor(ModuleLevelRow row) {
      return new LevelCellEditor();
    }
  }

  private final class LevelCellEditor extends DefaultCellEditor {
    LevelCellEditor() {
      super(createLevelChoiceCombo(choice -> levelText(choice.level())));
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      @SuppressWarnings("unchecked")
      JComboBox<LevelChoice> combo = (JComboBox<LevelChoice>)editorComponent;
      combo.setSelectedItem(LevelChoice.of((HaxeLanguageLevel)value));
      return combo;
    }

    @Override
    public Object getCellEditorValue() {
      LevelChoice choice = (LevelChoice)((JComboBox<?>)editorComponent).getSelectedItem();
      return choice == null ? null : choice.level();
    }
  }

  @NotNull
  private static ComboBox<LevelChoice> createLevelChoiceCombo(@NotNull Function<LevelChoice, String> choiceText) {
    DefaultComboBoxModel<LevelChoice> model = new DefaultComboBoxModel<>();
    model.addElement(LevelChoice.NO_EXPLICIT_LEVEL);
    for (HaxeLanguageLevel level : HaxeLanguageLevel.values()) {
      model.addElement(LevelChoice.of(level));
    }
    ComboBox<LevelChoice> combo = new ComboBox<>(model);
    combo.setRenderer(BuilderKt.textListCellRenderer("", choiceText::apply));
    return combo;
  }

  @NotNull
  private String defaultChoiceText(@NotNull LevelChoice choice) {
    if (choice.level() != null) {
      return choice.level().getPresentableText();
    }
    return compilerLevel != null
           ? HaxeBundle.message("haxe.compiler.use.compiler.level", compilerLevel.getPresentableText())
           : HaxeBundle.message("haxe.compiler.use.compiler.level.no.sdk");
  }

  @NotNull
  private String levelText(@Nullable HaxeLanguageLevel level) {
    if (level != null) {
      return level.getPresentableText();
    }
    return HaxeBundle.message("haxe.compiler.project.default.level", effectiveDefaultLevel().getPresentableText());
  }

  /** Mirrors the settings' fallback: explicit choice, else compiler level, else latest. */
  @NotNull
  private HaxeLanguageLevel effectiveDefaultLevel() {
    HaxeLanguageLevel selected = getSelectedDefaultLevel();
    if (selected != null) return selected;
    return compilerLevel != null ? compilerLevel : HaxeLanguageLevel.latest();
  }
}
