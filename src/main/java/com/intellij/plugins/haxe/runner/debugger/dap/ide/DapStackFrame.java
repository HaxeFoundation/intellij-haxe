package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.runner.debugger.dap.EvaluationPath;
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
 * One stack frame. Children are the frame's DAP scopes: the first scope's
 * variables (Locals) inline, any further scopes (e.g. "Members", "Statics")
 * as lazy groups.
 */
public final class DapStackFrame extends XStackFrame {
  private static final Logger LOG = Logger.getInstance(DapStackFrame.class);

  private final DapDebugProcess process;
  private final StackFrame frame;

  // Resolved once and cached: the resolver's index lookups are prohibited slow
  // operations on the EDT, yet platform/plugin listeners may call
  // getSourcePosition there (e.g. sessionPaused -> getCurrentPosition). The
  // debug process prewarms the top frame before reporting a stop and the
  // execution stack prewarms the rest while computing frames, both off the
  // EDT, so EDT callers only ever read the cache. (Benign race: concurrent
  // first calls resolve the same value.)
  private volatile @Nullable XSourcePosition position;
  private volatile boolean positionResolved;

  DapStackFrame(DapDebugProcess process, StackFrame frame) {
    this.process = process;
    this.frame = frame;
  }

  public int frameId() {
    return frame.getId();
  }

  /**
   * Identifies "the same frame" across steps so the platform restores the
   * previously expanded variable nodes and highlights changed values, instead
   * of collapsing the tree on every stop. Keyed by the function plus its
   * source path (independent of the current line, so a step within a method
   * restores). Null name → no stable identity, let the platform rebuild.
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
    // (imports/scope of the frame's file), independent of any expression editor
    return new DapDebuggerEvaluator(process, frame.getId(), getSourcePosition());
  }

  @Override
  public @Nullable XSourcePosition getSourcePosition() {
    if (!positionResolved) {
      position = resolveSourcePosition();
      positionResolved = true;
    }
    return position;
  }

  private @Nullable XSourcePosition resolveSourcePosition() {
    String path = frame.getSource() != null ? frame.getSource().getPath() : null;
    try {
      return process.backend().resolveSource(process.getSession().getProject(), path, frame);
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
          continue; // shown in a dedicated tab when the backend provides one
        }
        if (first) {
          // the Locals scope: variables straight into the frame node
          for (Variable variable : process.requestVariables(scope.getVariablesReference())) {
            children.add(new DapValue(process, variable, scope.getVariablesReference(),
                                        EvaluationPath.root(variable.getName())));
          }
          first = false;
        } else {
          children.addTopGroup(new DapScopeGroup(process, scope));
        }
      }
      node.addChildren(children, true);
    }, () -> node.addChildren(XValueChildrenList.EMPTY, true));
  }
}
