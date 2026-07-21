package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XNavigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

/**
 * Base for the Haxe debuggers' Variables-view values: carries the node's
 * access-path EXPRESSION from its frame's local down to itself
 * ("this.someObject.someArray[0].myVar", built as the tree expands) and the
 * RUNTIME type of the object it is a member of, and implements the behavior
 * every debugger flavor shares:
 *
 * <ul>
 *   <li>{@link #calculateEvaluationExpression()} — pre-fills the Evaluate
 *       Expression dialog from a selected tree node, like the Java
 *       debugger;</li>
 *   <li>Jump to Source — resolves the path through the Haxe resolver, falling
 *       back on the container's runtime type (see
 *       {@link HaxeVariableSourceNavigator}).</li>
 * </ul>
 */
public abstract class HaxeDebuggerValue extends XNamedValue {
  private final XDebugSession session;
  private final @Nullable String evaluationPath;
  private final @Nullable String containerTypeName;

  /**
   * @param evaluationPath    this node's access-path expression, or null when
   *                          it is not expressible (see EvaluationPath).
   * @param containerTypeName the RUNTIME type of the object this value is a
   *                          member of (the debuggers report concrete types),
   *                          or null for frame roots/scopes.
   */
  protected HaxeDebuggerValue(@NotNull String name, XDebugSession session,
                              @Nullable String evaluationPath, @Nullable String containerTypeName) {
    super(name);
    this.session = session;
    this.evaluationPath = evaluationPath;
    this.containerTypeName = containerTypeName;
  }

  /** This node's access-path expression — the prefix of its children's paths. */
  protected final @Nullable String evaluationPath() {
    return evaluationPath;
  }

  /** The runtime type of the container this value is a member of, or null. */
  protected final @Nullable String containerTypeName() {
    return containerTypeName;
  }

  // Pre-fills the Evaluate Expression dialog when this node is selected; no
  // prefill (empty dialog) for a node whose path is not expressible.
  @Override
  public @NotNull Promise<XExpression> calculateEvaluationExpression() {
    if (evaluationPath != null) {
      return Promises.resolvedPromise(createExpression(evaluationPath));
    } else {
      return Promises.resolvedPromise(null);
    }
  }

  private @NotNull XExpression createExpression(@NotNull String evaluationPath) {
    return XDebuggerUtil.getInstance().createExpression(evaluationPath, HaxeLanguage.INSTANCE, null, EvaluationMode.EXPRESSION);
  }

  @Override
  public boolean canNavigateToSource() {
    return evaluationPath != null;
  }

  // "Jump to Source": resolve the access path through the Haxe resolver and
  // land on the member's declaration (see HaxeVariableSourceNavigator).
  @Override
  public void computeSourcePosition(@NotNull XNavigatable navigatable) {
    HaxeVariableSourceNavigator.navigate(session, evaluationPath, containerTypeName, getName(), navigatable);
  }
}
