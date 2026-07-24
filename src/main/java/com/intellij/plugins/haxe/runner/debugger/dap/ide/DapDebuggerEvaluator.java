package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Watches/hover/Evaluate-dialog support: the expression goes to the server's
 * own interpreter, optionally rewritten first by the backend (e.g. bare class
 * names qualified against the frame file's imports).
 */
final class DapDebuggerEvaluator extends XDebuggerEvaluator {
  private final DapDebugProcess process;
  private final int frameId;
  private final @Nullable XSourcePosition framePosition;

  DapDebuggerEvaluator(DapDebugProcess process, int frameId, @Nullable XSourcePosition framePosition) {
    this.process = process;
    this.frameId = frameId;
    this.framePosition = framePosition;
  }

  @Override
  public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback callback,
                       @Nullable XSourcePosition expressionPosition) {
    process.onRequestThread(() -> {
      String qualified = process.backend().qualifyExpression(
        process.getSession().getProject(), framePosition, expression);
      Response response = process.sendRequest(EvaluateRequest.of(frameId, qualified, "watch"));
      if (response instanceof EvaluateResponse evaluated && response.isSuccess()) {
        Variable result = new Variable();
        result.setName(expression);
        result.setValue(evaluated.getBody().getResult());
        result.setType(evaluated.getBody().getType());
        result.setVariablesReference(evaluated.getBody().getVariablesReference());
        // a watch/hover result has no editable container (0); child expansion
        // still uses the result's own reference. Seed the path with the
        // evaluated EXPRESSION verbatim (it already parsed), so children get
        // "expr.field" paths and re-selecting the result prefills what was typed.
        callback.evaluated(new DapValue(process, result, 0,
                                          !expression.isBlank() ? expression : null));
        // NOTE: we do NOT rebuildViews() here even though an assignment changed
        // debuggee state. The evaluate dialog already calls session.rebuildViews()
        // in its own evaluationDone(), so a second one from this (request) thread
        // races the platform's post-evaluation refresh and intermittently doubled
        // the "result" node in the dialog. The platform's refresh covers it.
      } else {
        String message = response != null && response.getMessage() != null
                         ? response.getMessage() : "Cannot evaluate";
        callback.errorOccurred(message);
      }
    }, () -> callback.errorOccurred("Debug session is shutting down"));
  }
}
