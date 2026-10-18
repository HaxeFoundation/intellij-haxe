package com.intellij.plugins.haxe.v2.compiler.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeDisplayConnectionMode;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
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
 * level, the completion source, the connection mode, the compiler-backed IDE
 * features with their per-feature toggles, the compiler-diagnostics toggles
 * and a per-module level override table. The layout lives in the matching
 * .form; the combos and the table panel are the form's custom-created
 * components (see {@link #createUIComponents}). It holds no reference to
 * project services, so tests can exercise it; the configurable feeds it data
 * and reads it back.
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

  private JPanel mainPanel;
  private ComboBox<LevelChoice> defaultLevelCombo;
  private ComboBox<HaxeCompletionMode> completionModeCombo;
  private ComboBox<HaxeDisplayConnectionMode> displayConnectionCombo;
  private JCheckBox useLevelForConditionalsCheckBox;
  private JCheckBox compilerIdeFeaturesCheckBox;
  private JCheckBox featureInlaysCheckBox;
  private JCheckBox featureFindUsagesCheckBox;
  private JCheckBox featureNavigationCheckBox;
  private JCheckBox featureGeneratedMemberPreviewCheckBox;
  private JCheckBox featureCompletionCheckBox;
  private JCheckBox compilerDiagnosticsCheckBox;
  private JCheckBox diagnosticsErrorsCheckBox;
  private JCheckBox diagnosticsUnusedImportsCheckBox;
  private JCheckBox diagnosticsRemovableCodeCheckBox;
  private JCheckBox diagnosticsOnlyCheckBox;
  private JPanel tablePanel;
  // created in createUIComponents: the form's setup runs before the field initializers
  private ListTableModel<ModuleLevelRow> tableModel;
  private TableView<ModuleLevelRow> table;
  /** Level resolved from the SDK, shown in the "use compiler level" labels; null when no SDK. */
  private @Nullable HaxeLanguageLevel compilerLevel;

  public HaxeCompilerSettingsPanel() {
    // "Project default (x.y)" cells display the selected default, keep them in sync.
    defaultLevelCombo.addActionListener(e -> table.repaint());
    compilerIdeFeaturesCheckBox.addItemListener(e -> updateFeatureChildEnablement());
    featureNavigationCheckBox.addItemListener(e -> updateFeatureChildEnablement());
    featureCompletionCheckBox.addItemListener(e -> updateFeatureChildEnablement());
    compilerDiagnosticsCheckBox.addItemListener(e -> updateDiagnosticsChildEnablement());
    updateFeatureChildEnablement();
    updateDiagnosticsChildEnablement();
  }

  private void createUIComponents() {
    defaultLevelCombo = createLevelChoiceCombo(this::defaultChoiceText);
    completionModeCombo = new ComboBox<>(HaxeCompletionMode.values());
    completionModeCombo.setRenderer(BuilderKt.textListCellRenderer("", HaxeCompletionMode::getPresentableText));
    displayConnectionCombo = new ComboBox<>(HaxeDisplayConnectionMode.values());
    displayConnectionCombo.setRenderer(BuilderKt.textListCellRenderer("", HaxeDisplayConnectionMode::getPresentableText));
    tableModel = new ListTableModel<>(new ModuleColumn(), new LevelColumn());
    table = new TableView<>(tableModel);
    table.setShowGrid(false);
    table.setRowHeight(defaultLevelCombo.getPreferredSize().height);
    tablePanel = new JPanel(new BorderLayout());
    tablePanel.add(new JBScrollPane(table), BorderLayout.CENTER);
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

  private List<JCheckBox> featureChildren() {
    return List.of(featureInlaysCheckBox, featureFindUsagesCheckBox, featureNavigationCheckBox, featureCompletionCheckBox);
  }

  /// The per-feature toggles apply only under the master toggle; the generated-member preview and the completion
  /// source only while navigation and completion are among them.
  private void updateFeatureChildEnablement() {
    boolean features = compilerIdeFeaturesCheckBox.isSelected();
    for (JCheckBox child : featureChildren()) {
      child.setEnabled(features);
    }
    featureGeneratedMemberPreviewCheckBox.setEnabled(features && featureNavigationCheckBox.isSelected());
    completionModeCombo.setEnabled(features && featureCompletionCheckBox.isSelected());
  }

  @NotNull
  public HaxeCompletionMode getCompletionMode() {
    HaxeCompletionMode selected = completionModeCombo.getItem();
    return selected != null ? selected : HaxeCompletionMode.IDE_AND_COMPILER;
  }

  public void setCompletionMode(@NotNull HaxeCompletionMode mode) {
    completionModeCombo.setSelectedItem(mode);
  }

  @NotNull
  public HaxeDisplayConnectionMode getDisplayConnectionMode() {
    HaxeDisplayConnectionMode selected = displayConnectionCombo.getItem();
    return selected != null ? selected : HaxeDisplayConnectionMode.SHARED_SERVER;
  }

  public void setDisplayConnectionMode(@NotNull HaxeDisplayConnectionMode mode) {
    displayConnectionCombo.setSelectedItem(mode);
  }

  public boolean isCompilerIdeFeaturesEnabled() {
    return compilerIdeFeaturesCheckBox.isSelected();
  }

  public void setCompilerIdeFeaturesEnabled(boolean enabled) {
    compilerIdeFeaturesCheckBox.setSelected(enabled);
  }

  public boolean isCompilerInlaysEnabled() {
    return featureInlaysCheckBox.isSelected();
  }

  public void setCompilerInlaysEnabled(boolean enabled) {
    featureInlaysCheckBox.setSelected(enabled);
  }

  public boolean isCompilerFindUsagesEnabled() {
    return featureFindUsagesCheckBox.isSelected();
  }

  public void setCompilerFindUsagesEnabled(boolean enabled) {
    featureFindUsagesCheckBox.setSelected(enabled);
  }

  public boolean isCompilerNavigationEnabled() {
    return featureNavigationCheckBox.isSelected();
  }

  public void setCompilerNavigationEnabled(boolean enabled) {
    featureNavigationCheckBox.setSelected(enabled);
  }

  public boolean isGeneratedMemberPreviewEnabled() {
    return featureGeneratedMemberPreviewCheckBox.isSelected();
  }

  public void setGeneratedMemberPreviewEnabled(boolean enabled) {
    featureGeneratedMemberPreviewCheckBox.setSelected(enabled);
  }

  public boolean isCompilerCompletionEnabled() {
    return featureCompletionCheckBox.isSelected();
  }

  public void setCompilerCompletionEnabled(boolean enabled) {
    featureCompletionCheckBox.setSelected(enabled);
  }

  /** Whether the completion source combo takes input: only while completion is a compiler feature. */
  boolean isCompletionModeChoiceEnabled() {
    return completionModeCombo.isEnabled();
  }

  /** Whether the generated-member preview toggle takes input: only while navigation is a compiler feature. */
  boolean isGeneratedMemberPreviewChoiceEnabled() {
    return featureGeneratedMemberPreviewCheckBox.isEnabled();
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
