package com.intellij.plugins.haxe.v2.buildtools.settings.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.TargetRow;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.TableCellEditor;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * One framework's target table (Settings | Haxe | Frameworks | Lime/OpenFL/NME):
 * Name / Haxe target / Flags rows with add, remove and reordering — the row
 * order is the selector order; the framework's built-in default target stays
 * the default while its row exists, else the first row takes over. The first
 * flag is the tool's target word; removing every row restores the built-in
 * defaults on next use.
 */
public abstract class HaxeFrameworkTargetsConfigurable implements SearchableConfigurable {

  private final Framework framework;
  private final String id;
  private final String displayName;
  private final ListTableModel<TargetRow> model =
    new ListTableModel<>(new NameColumn(), new TargetColumn(), new FlagsColumn());

  protected HaxeFrameworkTargetsConfigurable(@NotNull Framework framework,
                                             @NotNull String id,
                                             @NotNull String displayName) {
    this.framework = framework;
    this.id = id;
    this.displayName = displayName;
  }

  @Override
  public @NotNull String getId() {
    return id;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public @Nullable JComponent createComponent() {
    model.setItems(HaxeFrameworkTargetSettings.getInstance().getTargetRows(framework));
    TableView<TargetRow> table = new TableView<>(model);
    JPanel tablePanel = ToolbarDecorator.createDecorator(table)
      .setAddAction(button -> model.addRow(new TargetRow()))
      .addExtraAction(restoreDefaultsAction())
      .createPanel();

    JBLabel hint = new JBLabel(HaxeBundle.message("haxe.frameworks.targets.hint"), UIUtil.ComponentStyle.SMALL);
    hint.setForeground(UIUtil.getContextHelpForeground());
    hint.setBorder(JBUI.Borders.emptyTop(6));

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(tablePanel, BorderLayout.CENTER);
    panel.add(hint, BorderLayout.SOUTH);
    return panel;
  }

  @Override
  public boolean isModified() {
    return !rowsEqual(model.getItems(), HaxeFrameworkTargetSettings.getInstance().getTargetRows(framework));
  }

  @Override
  public void apply() {
    HaxeFrameworkTargetSettings.getInstance().setTargetRows(framework, new ArrayList<>(model.getItems()));
    // re-read so isModified compares against what the store kept (trimming, dropped blanks)
    model.setItems(HaxeFrameworkTargetSettings.getInstance().getTargetRows(framework));
  }

  @Override
  public void reset() {
    model.setItems(HaxeFrameworkTargetSettings.getInstance().getTargetRows(framework));
  }

  /** Loads the built-in defaults into the TABLE — applied (or cancelled) like any other edit. */
  @NotNull
  private AnAction restoreDefaultsAction() {
    return new DumbAwareAction(HaxeBundle.message("haxe.frameworks.targets.restore.defaults"),
                               null, AllIcons.General.Reset) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        model.setItems(HaxeFrameworkTargetSettings.defaultRows(framework));
      }

      @Override
      public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
      }
    };
  }

  private static boolean rowsEqual(@NotNull List<TargetRow> left, @NotNull List<TargetRow> right) {
    if (left.size() != right.size()) return false;
    for (int i = 0; i < left.size(); i++) {
      TargetRow a = left.get(i);
      TargetRow b = right.get(i);
      boolean same = Objects.equals(a.name, b.name)
                     && Objects.equals(a.haxeTarget, b.haxeTarget)
                     && Objects.equals(a.flags, b.flags);
      if (!same) return false;
    }
    return true;
  }

  private static final class NameColumn extends ColumnInfo<TargetRow, String> {
    NameColumn() {
      super(HaxeBundle.message("haxe.frameworks.targets.column.name"));
    }

    @Override
    public String valueOf(TargetRow row) {
      return row.name;
    }

    @Override
    public void setValue(TargetRow row, String value) {
      row.name = value;
    }

    @Override
    public boolean isCellEditable(TargetRow row) {
      return true;
    }
  }

  private static final class TargetColumn extends ColumnInfo<TargetRow, String> {
    // blank first: the backend is informational and may stay unknown
    private static final String[] CHOICES = Stream.concat(Stream.of(""), Arrays.stream(HaxeTarget.values()).map(Enum::name))
      .toArray(String[]::new);

    TargetColumn() {
      super(HaxeBundle.message("haxe.frameworks.targets.column.target"));
    }

    @Override
    public String valueOf(TargetRow row) {
      return row.haxeTarget;
    }

    @Override
    public void setValue(TargetRow row, String value) {
      row.haxeTarget = value;
    }

    @Override
    public boolean isCellEditable(TargetRow row) {
      return true;
    }

    @Override
    public TableCellEditor getEditor(TargetRow row) {
      return new DefaultCellEditor(new ComboBox<>(CHOICES));
    }
  }

  private static final class FlagsColumn extends ColumnInfo<TargetRow, String> {
    FlagsColumn() {
      super(HaxeBundle.message("haxe.frameworks.targets.column.flags"));
    }

    @Override
    public String valueOf(TargetRow row) {
      return row.flags;
    }

    @Override
    public void setValue(TargetRow row, String value) {
      row.flags = value;
    }

    @Override
    public boolean isCellEditable(TargetRow row) {
      return true;
    }
  }
}
