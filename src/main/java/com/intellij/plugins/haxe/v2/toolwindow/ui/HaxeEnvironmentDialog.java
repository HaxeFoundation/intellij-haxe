package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeModuleSdkApplier;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.plugins.haxe.v2.buildtools.settings.EnvironmentDefine;
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
 * "Configure Environment" dialog for one container: the Haxe SDK to use, the
 * define entries and the custom compilation target.
 * The Effect column shows what an entry does against the active build
 * file: Add (new name), Override (name exists there) or Remove (unsets the
 * build file's define). The layout lives in the matching
 * .form; the defines table plus its toolbar is the form's custom-created panel
 * (see {@link #createUIComponents}).
 */
public final class HaxeEnvironmentDialog extends DialogWrapper {

  /** Mutable table row; blank names are dropped on apply. */
  private static final class DefineRow {
    String name = "";
    String value = "";
    DefineEffect effect = DefineEffect.SET;
  }

  private final Project project;
  private final String containerId;
  private final Set<String> activeBuildFileDefines;

  private JPanel panel;
  private ComboBox<String> sdkCombo;
  private JPanel definesPanel;
  private JTextField customTargetField;
  private ListTableModel<DefineRow> tableModel;
  private TableView<DefineRow> table;

  public HaxeEnvironmentDialog(@NotNull Project project,
                               @NotNull String containerId,
                               @NotNull String containerDisplayName,
                               @NotNull Set<String> activeBuildFileDefines) {
    super(project);
    this.project = project;
    this.containerId = containerId;
    this.activeBuildFileDefines = activeBuildFileDefines;

    setTitle(HaxeBundle.message("haxe.environment.dialog.title", containerDisplayName));
    sdkCombo.setRenderer(BuilderKt.textListCellRenderer(
      HaxeBundle.message("haxe.toolwindow.environment.sdk.default.choice"), name -> name));
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
    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);

    DefaultComboBoxModel<String> sdkModel = new DefaultComboBoxModel<>();
    sdkModel.addElement(null);
    for (Sdk sdk : ProjectJdkTable.getInstance().getSdksOfType(HaxeSdkType.getInstance())) {
      sdkModel.addElement(sdk.getName());
    }
    String storedSdk = store.getSdkName(containerId);
    if (storedSdk != null && sdkModel.getIndexOf(storedSdk) < 0) {
      sdkModel.addElement(storedSdk);
    }
    sdkCombo.setModel(sdkModel);
    sdkCombo.setSelectedItem(storedSdk);

    List<DefineRow> rows = new ArrayList<>();
    for (EnvironmentDefine define : store.getDefines(containerId)) {
      DefineRow row = new DefineRow();
      row.name = define.name();
      row.value = define.value();
      row.effect = define.effect();
      rows.add(row);
    }
    tableModel.setItems(rows);

    customTargetField.setText(StringUtil.notNullize(store.getCustomTarget(containerId)));
  }

  private void addRow() {
    tableModel.addRow(new DefineRow());
    int newRow = tableModel.getRowCount() - 1;
    table.getSelectionModel().setSelectionInterval(newRow, newRow);
    table.editCellAt(newRow, 0);
  }

  @Override
  protected void doOKAction() {
    TableCellEditor editor = table.getCellEditor();
    if (editor != null) {
      editor.stopCellEditing();
    }

    List<EnvironmentDefine> defines = new ArrayList<>();
    for (DefineRow row : tableModel.getItems()) {
      if (StringUtil.isEmptyOrSpaces(row.name)) continue;
      defines.add(new EnvironmentDefine(row.name.trim(), row.value.trim(), row.effect));
    }

    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);
    String sdkName = (String)sdkCombo.getSelectedItem();
    store.setSdkName(containerId, sdkName);
    HaxeModuleSdkApplier.getInstance(project).applyAsync(containerId, sdkName);
    store.setDefines(containerId, defines);
    store.setCustomTarget(containerId, customTargetField.getText());
    super.doOKAction();
  }

  private static final class NameColumn extends ColumnInfo<DefineRow, String> {
    NameColumn() {
      super(HaxeBundle.message("haxe.environment.column.name"));
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
      super(HaxeBundle.message("haxe.environment.column.value"));
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
      super(HaxeBundle.message("haxe.environment.column.action"));
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

  /** Add / Override / Remove, resolved against the active build file's defines. */
  @NotNull
  private String effectText(@NotNull DefineRow row, @Nullable DefineEffect effect) {
    if (effect == DefineEffect.REMOVE) {
      return HaxeBundle.message("haxe.environment.effect.remove");
    }
    return activeBuildFileDefines.contains(row.name.trim())
           ? HaxeBundle.message("haxe.environment.effect.override")
           : HaxeBundle.message("haxe.environment.effect.add");
  }
}
