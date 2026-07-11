package com.intellij.plugins.haxe.hashlink;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One stack frame. Children are the frame's DAP scopes: the Locals scope's
 * variables inline, any further scopes (e.g. "Statics (Class)") as lazy groups.
 */
final class HashLinkStackFrame extends XStackFrame {
  private final HashLinkDebugProcess process;
  private final StackFrame frame;

  HashLinkStackFrame(HashLinkDebugProcess process, StackFrame frame) {
    this.process = process;
    this.frame = frame;
  }

  @Override
  public @Nullable XSourcePosition getSourcePosition() {
    String path = frame.getSource() != null ? frame.getSource().getPath() : null;
    return HashLinkSourceResolver.resolve(process.getSession().getProject(), path, frame.getLine());
  }

  @Override
  public void computeChildren(XCompositeNode node) {
    process.onRequestThread(() -> {
      List<Scope> scopes = process.requestScopes(frame.getId());
      XValueChildrenList children = new XValueChildrenList();
      boolean first = true;
      for (Scope scope : scopes) {
        if (first) {
          // the Locals scope: variables straight into the frame node
          for (Variable variable : process.requestVariables(scope.getVariablesReference())) {
            children.add(new HashLinkValue(process, variable));
          }
          first = false;
        } else {
          children.addTopGroup(new HashLinkScopeGroup(process, scope));
        }
      }
      node.addChildren(children, true);
    });
  }
}
