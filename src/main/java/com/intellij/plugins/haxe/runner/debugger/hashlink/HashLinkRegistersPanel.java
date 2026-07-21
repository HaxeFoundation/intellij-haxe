package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapStackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.frame.XStackFrame;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

/**
 * The "Registers" tab of the Debug tool window: a flat name/value/type table
 * of the adapter's Registers scope (HL bytecode registers of the selected
 * frame, plus SP/BP/IP/FLAGS on the top frame). Refreshes on every stop and on
 * frame selection; keeps the last values while the debuggee runs.
 */
final class HashLinkRegistersPanel extends JPanel implements XDebugSessionListener {
  private static final ColumnInfo<Variable, String> REGISTER = new ColumnInfo<>("Register") {
    @Override
    public @Nullable String valueOf(Variable variable) {
      return variable.getName();
    }
  };
  private static final ColumnInfo<Variable, String> VALUE = new ColumnInfo<>("Value") {
    @Override
    public @Nullable String valueOf(Variable variable) {
      return variable.getValue();
    }
  };
  private static final ColumnInfo<Variable, String> TYPE = new ColumnInfo<>("Type") {
    @Override
    public @Nullable String valueOf(Variable variable) {
      return variable.getType();
    }
  };

  private final DapDebugProcess process;
  private final ListTableModel<Variable> model =
    new ListTableModel<>(REGISTER, VALUE, TYPE);

  HashLinkRegistersPanel(DapDebugProcess process) {
    super(new BorderLayout());
    this.process = process;
    TableView<Variable> table = new TableView<>(model);
    table.setShowGrid(false);
    add(new JBScrollPane(table), BorderLayout.CENTER);
  }

  // --- XDebugSessionListener (called on arbitrary threads) ---

  @Override
  public void sessionPaused() {
    refresh();
  }

  @Override
  public void stackFrameChanged() {
    refresh();
  }

  /** Re-reads the current frame's registers (also called after a value write). */
  void refresh() {
    XStackFrame current = process.getSession().getCurrentStackFrame();
    if (!(current instanceof DapStackFrame frame)) {
      return;
    }
    int frameId = frame.frameId();
    process.onRequestThread(() -> {
      Scope registers = null;
      for (Scope scope : process.requestScopes(frameId)) {
        if ("registers".equals(scope.getPresentationHint())) {
          registers = scope;
        }
      }
      List<Variable> rows =
        registers == null ? List.of() : process.requestVariables(registers.getVariablesReference());
      ApplicationManager.getApplication().invokeLater(() -> model.setItems(rows));
    });
  }
}
