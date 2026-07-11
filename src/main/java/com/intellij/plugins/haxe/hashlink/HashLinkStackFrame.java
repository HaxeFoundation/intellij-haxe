package com.intellij.plugins.haxe.hashlink;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One stack frame. Children are the frame's DAP scopes: the Locals scope's
 * variables inline, any further scopes (e.g. "Statics (Class)") as lazy groups.
 */
final class HashLinkStackFrame extends XStackFrame {
  private static final Logger LOG = Logger.getInstance(HashLinkStackFrame.class);

  private final HashLinkDebugProcess process;
  private final StackFrame frame;

  HashLinkStackFrame(HashLinkDebugProcess process, StackFrame frame) {
    this.process = process;
    this.frame = frame;
  }

  @Override
  public @Nullable XSourcePosition getSourcePosition() {
    String path = frame.getSource() != null ? frame.getSource().getPath() : null;
    try {
      return HashLinkSourceResolver.resolve(process.getSession().getProject(), path, frame.getLine());
    } catch (RuntimeException e) {
      // one unresolvable frame must never wedge the whole Frames panel
      LOG.warn("Cannot resolve source for frame '" + frame.getName() + "' (" + path + ")", e);
      return null;
    }
  }

  @Override
  public void customizePresentation(@NotNull ColoredTextContainer component) {
    component.append(frame.getName() != null ? frame.getName() : "<unknown>", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    String file = frame.getSource() != null ? frame.getSource().getName() : null;
    String location = file != null ? " (" + file + ":" + frame.getLine() + ")" : " (no source)";
    component.append(location, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    component.setIcon(AllIcons.Debugger.Frame);
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
