package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.plugins.haxe.runner.debugger.dap.EvaluationPath;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueGroup;
import org.jetbrains.annotations.NotNull;

/**
 * A non-Locals DAP scope (e.g. "Members") shown as a collapsible group in the
 * Variables view; its variables are fetched on expand.
 */
final class DapScopeGroup extends XValueGroup {
  private final DapDebugProcess process;
  private final Scope scope;

  DapScopeGroup(DapDebugProcess process, Scope scope) {
    super(scope.getName() != null ? scope.getName() : "Scope");
    this.process = process;
    this.scope = scope;
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    process.onRequestThread(() -> {
      XValueChildrenList children = new XValueChildrenList();
      for (Variable variable : process.requestVariables(scope.getVariablesReference())) {
        children.add(new DapValue(process, variable, scope.getVariablesReference(),
                                    EvaluationPath.root(variable.getName())));
      }
      node.addChildren(children, true);
    }, () -> node.addChildren(XValueChildrenList.EMPTY, true));
  }
}
