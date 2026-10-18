package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.HaxeModuleVariants;
import com.intellij.plugins.haxe.util.HaxeModuleVariants.CustomTargetProblem;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineOverride;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * "IDE Overrides" dialog for one build file: its define entries and its
 * custom compilation target. The Effect column shows what an entry does
 * against the build file: Add (new name), Override (name exists there) or
 * Remove (unsets the build file's define). The custom target row is hidden
 * for build types that always select a target. The layout lives in the
 * matching .form; the defines table plus its toolbar is the form's
 * custom-created panel (see {@link #createUIComponents}).
 */
public final class HaxeIdeOverridesDialog extends DialogWrapper {

  /** Validates a custom target entry, here and in the tree row's prompt: blank clears it, anything else must be a target name. */
  public static final InputValidatorEx CUSTOM_TARGET_VALIDATOR = HaxeIdeOverridesDialog::customTargetError;

  /** Mutable table row; blank names are dropped on apply. */
  private static final class DefineRow {
    String name = "";
    String value = "";
    DefineEffect effect = DefineEffect.SET;
  }

  private final Project project;
  private final String buildFilePath;
  private final Set<String> buildDefineNames;

  private JPanel panel;
  private JPanel definesPanel;
  private JLabel customTargetLabel;
  private JTextField customTargetField;
  private ListTableModel<DefineRow> tableModel;
  private TableView<DefineRow> table;

  public HaxeIdeOverridesDialog(@NotNull Project project,
                                @NotNull HaxeBuildFile buildFile,
                                @NotNull Set<String> buildDefineNames,
                                boolean customTargetApplicable) {
    super(project);
    this.project = project;
    this.buildFilePath = buildFile.file().getPath();
    this.buildDefineNames = buildDefineNames;

    setTitle(HaxeBundle.message("haxe.ide.overrides.dialog.title", buildFile.file().getName()));
    customTargetLabel.setVisible(customTargetApplicable);
    customTargetField.setVisible(customTargetApplicable);
    fillFromStore();
    init();
  }

  /** Called by the generated form binding for the {@code custom-create} defines panel. */
  private void createUIComponents() {
    tableModel = new ListTableModel<>(new NameColumn(), new ValueColumn(), new EffectColumn());
    table = new TableView<>(tableModel);
    table.setShowGrid(false);
    definesPanel = ToolbarDecorator.createDecorator(table)
      .setAddAction(button -> addRow())
      .setRemoveAction(button -> TableUtil.removeSelectedItems(table))
      .disableUpDownActions()
      .createPanel();
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    panel.setPreferredSize(JBUI.size(560, 360));
    return panel;
  }

  private void fillFromStore() {
    HaxeIdeOverridesStore store = HaxeIdeOverridesStore.getInstance(project);

    List<DefineRow> rows = new ArrayList<>();
    for (DefineOverride define : store.getDefines(buildFilePath)) {
      DefineRow row = new DefineRow();
      row.name = define.name();
      row.value = define.value();
      row.effect = define.effect();
      rows.add(row);
    }
    tableModel.setItems(rows);

    customTargetField.setText(StringUtil.notNullize(store.getCustomTarget(buildFilePath)));
  }

  private void addRow() {
    tableModel.addRow(new DefineRow());
    int newRow = tableModel.getRowCount() - 1;
    table.getSelectionModel().setSelectionInterval(newRow, newRow);
    table.editCellAt(newRow, 0);
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    String error = customTargetError(customTargetField.getText());
    return error == null ? null : new ValidationInfo(error, customTargetField);
  }

  @Override
  protected void doOKAction() {
    TableCellEditor editor = table.getCellEditor();
    if (editor != null) {
      editor.stopCellEditing();
    }

    List<DefineOverride> defines = new ArrayList<>();
    for (DefineRow row : tableModel.getItems()) {
      if (StringUtil.isEmptyOrSpaces(row.name)) continue;
      defines.add(new DefineOverride(row.name.trim(), row.value.trim(), row.effect));
    }

    HaxeIdeOverridesStore store = HaxeIdeOverridesStore.getInstance(project);
    store.setDefines(buildFilePath, defines);
    store.setCustomTarget(buildFilePath, customTargetField.getText());
    super.doOKAction();
  }

  /** The error for an entered custom target; null when it is blank (clears the target) or a name the compiler accepts. */
  @Nullable
  private static String customTargetError(@NotNull String entered) {
    String name = entered.trim();
    CustomTargetProblem problem = name.isEmpty() ? null : HaxeModuleVariants.customTargetProblem(name);
    if (problem == null) return null;
    return switch (problem) {
      case RESERVED -> HaxeBundle.message("haxe.ide.overrides.dialog.custom.target.reserved", name);
      case TOO_LONG -> HaxeBundle.message("haxe.ide.overrides.dialog.custom.target.too.long", HaxeModuleVariants.MAX_CUSTOM_TARGET_LENGTH);
      case NOT_A_TARGET_NAME -> HaxeBundle.message("haxe.ide.overrides.dialog.custom.target.invalid");
    };
  }

  private static final class NameColumn extends ColumnInfo<DefineRow, String> {
    NameColumn() {
      super(HaxeBundle.message("haxe.ide.overrides.column.name"));
    }

    @Override
    public String valueOf(DefineRow row) {
      return row.name;
    }

    @Override
    public void setValue(DefineRow row, String value) {
      row.name = StringUtil.notNullize(value);
    }

    @Override
    public boolean isCellEditable(DefineRow row) {
      return true;
    }
  }

  private static final class ValueColumn extends ColumnInfo<DefineRow, String> {
    ValueColumn() {
      super(HaxeBundle.message("haxe.ide.overrides.column.value"));
    }

    @Override
    public String valueOf(DefineRow row) {
      return row.value;
    }

    @Override
    public void setValue(DefineRow row, String value) {
      row.value = StringUtil.notNullize(value);
    }

    /** A remove entry has no value of its own. */
    @Override
    public boolean isCellEditable(DefineRow row) {
      return row.effect != DefineEffect.REMOVE;
    }
  }

  private final class EffectColumn extends ColumnInfo<DefineRow, DefineEffect> {
    EffectColumn() {
      super(HaxeBundle.message("haxe.ide.overrides.column.action"));
    }

    @Override
    public DefineEffect valueOf(DefineRow row) {
      return row.effect;
    }

    @Override
    public void setValue(DefineRow row, DefineEffect value) {
      row.effect = value;
    }

    @Override
    public boolean isCellEditable(DefineRow row) {
      return true;
    }

    @Override
    public int getWidth(JTable table) {
      return JBUI.scale(90);
    }

    @Override
    public TableCellRenderer getRenderer(DefineRow row) {
      return new DefaultTableCellRenderer() {
        @Override
        protected void setValue(Object value) {
          setText(effectText(row, (DefineEffect)value));
        }
      };
    }

    @Override
    public TableCellEditor getEditor(DefineRow row) {
      ComboBox<DefineEffect> combo = new ComboBox<>(DefineEffect.values());
      combo.setRenderer(BuilderKt.textListCellRenderer("", effect -> effectText(row, effect)));
      return new DefaultCellEditor(combo);
    }
  }

  /** Add / Override / Remove, resolved against the build file's own defines. */
  @NotNull
  private String effectText(@NotNull DefineRow row, @Nullable DefineEffect effect) {
    if (effect == DefineEffect.REMOVE) {
      return HaxeBundle.message("haxe.ide.overrides.effect.remove");
    }
    return buildDefineNames.contains(row.name.trim())
           ? HaxeBundle.message("haxe.ide.overrides.effect.override")
           : HaxeBundle.message("haxe.ide.overrides.effect.add");
  }
}
