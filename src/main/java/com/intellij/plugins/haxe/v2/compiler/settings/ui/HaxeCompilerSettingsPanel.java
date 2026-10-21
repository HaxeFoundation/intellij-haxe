package com.intellij.plugins.haxe.v2.compiler.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeDisplayConnectionMode;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeServerGeneration;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
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
 * level, the completion source, the connection mode per server generation, the compiler-backed IDE
 * features with their per-feature toggles, the compiler-diagnostics toggles
 * and a per-module level override table. The layout lives in the matching
 * .form; the combos and the table panel are the form's custom-created
 * components (see {@link #createUIComponents}). It holds no reference to
 * project services, so tests can exercise it; the configurable feeds it data
 * and reads it back.
 */
public final class HaxeCompilerSettingsPanel {

  /** What the page shows about a module's SDK: its language level, null without an SDK. */
  public record ModuleSdk(@NotNull String moduleName, @Nullable HaxeLanguageLevel level) {
  }

  /** One table row: a module's SDK and the module's (optional) language level override. */
  static final class ModuleLevelRow {
    final ModuleSdk sdk;
    @Nullable HaxeLanguageLevel override;

    ModuleLevelRow(@NotNull ModuleSdk sdk, @Nullable HaxeLanguageLevel override) {
      this.sdk = sdk;
      this.override = override;
    }
  }

  /**
   * Combo item wrapper so the "no explicit level" choice does not need a null
   * entry in the combo model. In the default combo the null choice means
   * "use each module's SDK level"; in a module row it means no override, the
   * default the module takes.
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
  private ComboBox<HaxeDisplayConnectionMode> connectionModeCombo;
  private ComboBox<HaxeDisplayConnectionMode> haxe3ConnectionModeCombo;
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

  public HaxeCompilerSettingsPanel() {
    // a row without an override shows the default the combo gives it
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
    connectionModeCombo = createConnectionModeCombo(HaxeServerGeneration.HAXE_4);
    haxe3ConnectionModeCombo = createConnectionModeCombo(HaxeServerGeneration.HAXE_3);
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
  public HaxeDisplayConnectionMode getDisplayConnectionMode(@NotNull HaxeServerGeneration generation) {
    HaxeDisplayConnectionMode selected = connectionModeComboFor(generation).getItem();
    return selected != null ? selected : HaxeDisplayConnectionMode.DEFAULT;
  }

  public void setDisplayConnectionMode(@NotNull HaxeServerGeneration generation, @NotNull HaxeDisplayConnectionMode mode) {
    connectionModeComboFor(generation).setSelectedItem(mode);
  }

  @NotNull
  private ComboBox<HaxeDisplayConnectionMode> connectionModeComboFor(@NotNull HaxeServerGeneration generation) {
    return generation == HaxeServerGeneration.HAXE_3 ? haxe3ConnectionModeCombo : connectionModeCombo;
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
  @TestOnly
  boolean isCompletionModeChoiceEnabled() {
    return completionModeCombo.isEnabled();
  }

  /** Whether the generated-member preview toggle takes input: only while navigation is a compiler feature. */
  @TestOnly
  boolean isGeneratedMemberPreviewChoiceEnabled() {
    return featureGeneratedMemberPreviewCheckBox.isEnabled();
  }

  @NotNull
  public JComponent getComponent() {
    return mainPanel;
  }

  /** A null {@code explicitDefault} selects "use each module's SDK level". */
  public void reset(@Nullable HaxeLanguageLevel explicitDefault,
                    @NotNull Map<String, HaxeLanguageLevel> overrides,
                    @NotNull List<ModuleSdk> modules) {
    defaultLevelCombo.setSelectedItem(LevelChoice.of(explicitDefault));
    List<ModuleLevelRow> rows = modules.stream()
      .map(module -> new ModuleLevelRow(module, overrides.get(module.moduleName())))
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
        result.put(row.sdk.moduleName(), row.override);
      }
    }
    return result;
  }

  /** The level cell's text in the module's row. */
  @TestOnly
  @NotNull
  String moduleLevelText(@NotNull String moduleName) {
    ModuleLevelRow row = rowOf(moduleName);
    return levelText(row, row.override);
  }

  @NotNull
  private ModuleLevelRow rowOf(@NotNull String moduleName) {
    return tableModel.getItems().stream()
      .filter(row -> row.sdk.moduleName().equals(moduleName))
      .findFirst()
      .orElseThrow();
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
      return row.sdk.moduleName();
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
          setText(levelText(row, (HaxeLanguageLevel)value));
        }
      };
    }

    @Override
    public TableCellEditor getEditor(ModuleLevelRow row) {
      return new LevelCellEditor(row);
    }
  }

  private final class LevelCellEditor extends DefaultCellEditor {
    LevelCellEditor(@NotNull ModuleLevelRow row) {
      super(createLevelChoiceCombo(choice -> levelText(row, choice.level())));
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

  /** The connection modes, named by the flags a server of the generation starts with. */
  @NotNull
  private static ComboBox<HaxeDisplayConnectionMode> createConnectionModeCombo(@NotNull HaxeServerGeneration generation) {
    ComboBox<HaxeDisplayConnectionMode> combo = new ComboBox<>(HaxeDisplayConnectionMode.values());
    combo.setRenderer(BuilderKt.textListCellRenderer("", (HaxeDisplayConnectionMode mode) -> mode.getPresentableText(generation)));
    return combo;
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
    return HaxeBundle.message("haxe.compiler.use.sdk.level");
  }

  /** The row's level as shown: the override, else the default the row's module takes. */
  @NotNull
  private String levelText(@NotNull ModuleLevelRow row, @Nullable HaxeLanguageLevel level) {
    if (level != null) {
      return level.getPresentableText();
    }
    return HaxeLanguageLevelUtil.defaultLevelLabel(getSelectedDefaultLevel(), row.sdk.level());
  }

}
