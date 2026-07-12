package com.intellij.plugins.haxe.hashlink;

import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueModifier;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A variable from the adapter: value + type as reported, expandable when the
 * adapter handed out a variablesReference (objects, arrays, enums, ...).
 * Editable via {@link XValueModifier} when it belongs to a container reference
 * (a Locals scope, an object, an array), through the adapter's setVariable.
 */
final class HashLinkValue extends XNamedValue {
  private final HashLinkDebugProcess process;
  private final Variable variable;
  private final int containerReference;

  /** @param containerReference the reference this variable is a child of (0 = not editable). */
  HashLinkValue(HashLinkDebugProcess process, Variable variable, int containerReference) {
    super(variable.getName() != null ? variable.getName() : "?");
    this.process = process;
    this.variable = variable;
    this.containerReference = containerReference;
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
        children.add(new HashLinkValue(process, child, reference));
      }
      node.addChildren(children, true);
    });
  }

  @Override
  public @Nullable XValueModifier getModifier() {
    if (containerReference <= 0) {
      return null; // no container to set this value against
    }
    return new XValueModifier() {
      @Override
      public @Nullable String getInitialValueEditorText() {
        return variable.getValue();
      }

      @Override
      public void setValue(@NotNull String expression, @NotNull XModificationCallback callback) {
        process.onRequestThread(() -> {
          try {
            String newValue = process.requestSetVariable(containerReference, variable.getName(), expression);
            // the node re-presents THIS instance after the edit: update the
            // cached variable or the view keeps showing the old value
            variable.setValue(newValue);
            process.refreshRegistersTab();
            callback.valueModified();
          } catch (RuntimeException e) {
            callback.errorOccurred(e.getMessage() != null ? e.getMessage() : "Could not set value");
          }
        });
      }
    };
  }
}
