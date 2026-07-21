package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerValue;
import com.intellij.plugins.haxe.runner.debugger.dap.EvaluationPath;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.VariableKind;
import javax.swing.Icon;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueModifier;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A variable from the debugger: value + type as reported, expandable when the
 * server handed out a variablesReference (objects, arrays, maps, ...).
 * Editable via {@link XValueModifier} when it belongs to a container
 * reference, through the server's setVariable.
 * Evaluate-prefill and Jump to Source come from {@link HaxeDebuggerValue}.
 */
final class DapValue extends HaxeDebuggerValue {
  private final DapDebugProcess process;
  private final Variable variable;
  private final int containerReference;

  /**
   * @param containerReference the reference this variable is a child of (0 = not editable).
   * @param evaluationPath this node's access-path expression, or null when it is
   *                       not expressible (see {@link EvaluationPath}).
   */
  DapValue(DapDebugProcess process, Variable variable, int containerReference, @Nullable String evaluationPath) {
    this(process, variable, containerReference, evaluationPath, null);
  }

  DapValue(DapDebugProcess process, Variable variable, int containerReference,
             @Nullable String evaluationPath, @Nullable String containerTypeName) {
    super(variable.getName() != null ? variable.getName() : "?",
          process.getSession(), evaluationPath, containerTypeName);
    this.process = process;
    this.variable = variable;
    this.containerReference = containerReference;
  }

  @Override
  public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
    boolean expandable = variable.getVariablesReference() > 0;
    String value = variable.getValue() != null ? variable.getValue() : "";
    node.setPresentation(iconFor(variable.getKind()),
                         new XRegularValuePresentation(value, variable.getType()),
                         expandable);
  }

  /** Maps the server's classification to a node icon; a plain value otherwise. */
  private static Icon iconFor(VariableKind kind) {
    return switch (kind) {
      case ARGUMENT -> AllIcons.Nodes.Parameter;
      case LOCAL -> AllIcons.Nodes.Variable;
      case STATIC -> AllIcons.Nodes.Static;
      case FIELD -> AllIcons.Nodes.Field;
      case UNSPECIFIED -> AllIcons.Debugger.Value;
    };
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
        children.add(new DapValue(process, child, reference,
                                    EvaluationPath.child(evaluationPath(), child.getName()),
                                    variable.getType()));
      }
      node.addChildren(children, true);
    }, () -> node.addChildren(XValueChildrenList.EMPTY, true));
  }

  @Override
  public @Nullable XValueModifier getModifier() {
    if (containerReference <= 0) {
      return null; // no container to set this value against
    }
    if ("String".equals(containerTypeName())) {
      // a String's exposed rows (length/byteLength) are derived, not fields;
      // no backend can write them — and the eval VM CRASHES on the attempt
      return null;
    }
    return new XValueModifier() {
      @Override
      public @Nullable String getInitialValueEditorText() {
        return variable.getValue();
      }

      @Override
      public void setValue(@NotNull XExpression expression, @NotNull XModificationCallback callback) {
        String text = expression.getExpression();
        process.onRequestThread(() -> {
          try {
            var updated = process.requestSetVariable(containerReference, variable.getName(), text);
            // the node re-presents THIS instance after the edit: adopt the
            // WHOLE new state. Assigning a container value (an array, an
            // object) creates a fresh variablesReference — keeping the old
            // one makes the expanded row keep showing the old children.
            variable.setValue(updated.getValue());
            if (updated.getType() != null) {
              variable.setType(updated.getType());
            }
            variable.setVariablesReference(updated.getVariablesReference());
            process.backend().afterSetVariable(process);
            callback.valueModified();
          } catch (RuntimeException e) {
            callback.errorOccurred(e.getMessage() != null ? e.getMessage() : "Could not set value");
          }
        }, () -> callback.errorOccurred("Debug session is shutting down"));
      }
    };
  }
}
