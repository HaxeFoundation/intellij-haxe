package com.intellij.plugins.haxe.hashlink;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
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

  int frameId() {
    return frame.getId();
  }

  /**
   * Identifies "the same frame" across steps so the platform
   * ({@code XVariablesViewBase}) restores the previously expanded variable nodes
   * and highlights values that changed — instead of collapsing the whole tree on
   * every stop. Keyed by the function (independent of the current line, so a
   * step within a method restores) plus its source path to disambiguate
   * like-named functions; a step into a different function yields a different
   * key, correctly rebuilding fresh. Null name → no stable identity, let the
   * platform rebuild. Mirrors {@code JavaStackFrame.getEqualityObject()} keying
   * on the method.
   */
  @Override
  public @Nullable Object getEqualityObject() {
    String name = frame.getName();
    if (name == null) {
      return null;
    }
    String path = frame.getSource() != null ? frame.getSource().getPath() : null;
    return path != null ? name + "@" + path : name;
  }

  @Override
  public @Nullable XDebuggerEvaluator getEvaluator() {
    // the frame's own position is the context for qualifying bare class names
    // (imports/scope of the breakpoint file), independent of any expression editor
    return new HashLinkDebuggerEvaluator(process, frame.getId(), getSourcePosition());
  }

  @Override
  public @Nullable XSourcePosition getSourcePosition() {
    String path = frame.getSource() != null ? frame.getSource().getPath() : null;
    try {
      return HashLinkSourceResolver.resolve(process.getSession().getProject(), path, frame);
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
        if ("registers".equals(scope.getPresentationHint())) {
          continue; // shown in the dedicated Registers tab, not the Variables view
        }
        if (first) {
          // the Locals scope: variables straight into the frame node
          for (Variable variable : process.requestVariables(scope.getVariablesReference())) {
            children.add(new HashLinkValue(process, variable, scope.getVariablesReference()));
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
