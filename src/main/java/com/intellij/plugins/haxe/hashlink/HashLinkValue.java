package com.intellij.plugins.haxe.hashlink;

import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation;
import org.jetbrains.annotations.NotNull;

/**
 * A variable from the adapter: value + type as reported, expandable when the
 * adapter handed out a variablesReference (objects, arrays, enums, ...).
 */
final class HashLinkValue extends XNamedValue {
  private final HashLinkDebugProcess process;
  private final Variable variable;

  HashLinkValue(HashLinkDebugProcess process, Variable variable) {
    super(variable.getName() != null ? variable.getName() : "?");
    this.process = process;
    this.variable = variable;
  }

  @Override
  public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
    boolean expandable = variable.getVariablesReference() > 0;
    String value = variable.getValue() != null ? variable.getValue() : "";
    node.setPresentation(AllIcons.Debugger.Value,
                         new XRegularValuePresentation(value, variable.getType()),
                         expandable);
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    int reference = variable.getVariablesReference();
    if (reference <= 0) {
      node.addChildren(XValueChildrenList.EMPTY, true);
      return;
    }
    process.onRequestThread(() -> {
      XValueChildrenList children = new XValueChildrenList();
      for (Variable child : process.requestVariables(reference)) {
        children.add(new HashLinkValue(process, child));
      }
      node.addChildren(children, true);
    });
  }
}
