package com.intellij.plugins.haxe.v2.buildtools.settings.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeGeneratedSourceRoots;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.TargetRow;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.table.TableCellEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * One framework's settings page, in two tabs; layout in the bound .form.
 *
 * Targets is the target table: Name / Haxe target / Flags rows that can be added, removed and reordered, the row order
 * being the selector order. The framework's built-in default target stays the default while its row exists, otherwise
 * the first row takes over. The first flag is the tool's target word; removing every row restores the built-in
 * defaults on next use.
 *
 * Generated Sources: whether the generated source folders of the framework's builds, for their selected target, are
 * marked as generated source roots ({@link HaxeGeneratedSourceRoots}).
 */
final class HaxeFrameworkSettingsForm {

  private final Framework framework;
  // created in createUIComponents, which runs before field initializers
  private ListTableModel<TargetRow> model;

  private JPanel panel;
  private JPanel tablePanel;
  private JBLabel targetsHintLabel;
  private JBCheckBox markCheckBox;
  private JBLabel generatedHintLabel;

  HaxeFrameworkSettingsForm(@NotNull Framework framework) {
    this.framework = framework;
    asHint(targetsHintLabel);
    asHint(generatedHintLabel);
    generatedHintLabel.setText(HaxeBundle.message("haxe.frameworks.generated.hint", exampleFolder(framework)));
  }

  /** The table with its add, remove, move and restore-defaults toolbar, which the designer cannot build. */
  private void createUIComponents() {
    model = new ListTableModel<>(new NameColumn(), new TargetColumn(), new FlagsColumn());
    tablePanel = ToolbarDecorator.createDecorator(new TableView<>(model))
      .setAddAction(button -> model.addRow(new TargetRow()))
      .addExtraAction(restoreDefaultsAction())
      .createPanel();
  }

  @NotNull
  JPanel getPanel() {
    return panel;
  }

  boolean isModified() {
    return !rowsEqual(model.getItems(), settings().getTargetRows(framework))
           || markCheckBox.isSelected() != settings().marksGeneratedSources(framework);
  }

  void apply() {
    settings().setTargetRows(framework, new ArrayList<>(model.getItems()));
    // re-read so isModified compares against what the store kept (trimming, dropped blanks)
    model.setItems(settings().getTargetRows(framework));

    if (markCheckBox.isSelected() != settings().marksGeneratedSources(framework)) {
      settings().setMarksGeneratedSources(framework, markCheckBox.isSelected());
      HaxeGeneratedSourceRoots.requestReconcileInOpenProjects();
    }
  }

  void reset() {
    model.setItems(settings().getTargetRows(framework));
    markCheckBox.setSelected(settings().marksGeneratedSources(framework));
  }

  /** Loads the built-in defaults into the table, applied or cancelled like any other edit. */
  @NotNull
  private AnAction restoreDefaultsAction() {
    return new DumbAwareAction(HaxeBundle.message("haxe.frameworks.targets.restore.defaults"), null, AllIcons.General.Reset) {
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

  private static void asHint(@NotNull JBLabel label) {
    label.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    label.setForeground(UIUtil.getContextHelpForeground());
  }

  /** A generated folder of the framework's default layout, which the hint names as an example. */
  @NotNull
  private static String exampleFolder(@NotNull Framework framework) {
    String key = framework == Framework.NME ? "haxe.frameworks.generated.example.nme" : "haxe.frameworks.generated.example.lime";
    return HaxeBundle.message(key);
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

  @NotNull
  private static HaxeFrameworkTargetSettings settings() {
    return HaxeFrameworkTargetSettings.getInstance();
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
