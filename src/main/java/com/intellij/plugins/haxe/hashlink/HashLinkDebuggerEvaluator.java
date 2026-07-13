package com.intellij.plugins.haxe.hashlink;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.EvaluateResponse;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Watches/hover/Evaluate-dialog support. The adapter evaluates VARIABLE PATHS
 * (name, obj.field, arr[0], this-implicit fields, statics of the current
 * class) — arbitrary expressions come back with a descriptive error.
 *
 * <p>Bare class references are qualified to their fully-qualified names against
 * the frame's source file (imports/scope) before being sent, so {@code
 * Point.ORIGIN} works without the user typing {@code geom.Point.ORIGIN} — see
 * {@link HashLinkExpressionQualifier}. The user still sees the text they typed.
 */
final class HashLinkDebuggerEvaluator extends XDebuggerEvaluator {
  private final HashLinkDebugProcess process;
  private final int frameId;
  private final XSourcePosition framePosition;

  HashLinkDebuggerEvaluator(HashLinkDebugProcess process, int frameId, @Nullable XSourcePosition framePosition) {
    this.process = process;
    this.frameId = frameId;
    this.framePosition = framePosition;
  }

  @Override
  public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback callback,
                       @Nullable XSourcePosition expressionPosition) {
    process.onRequestThread(() -> {
      String qualified = HashLinkExpressionQualifier.qualify(
        process.getSession().getProject(), framePosition, expression);

      EvaluateRequest request = new EvaluateRequest();
      EvaluateArguments arguments = new EvaluateArguments();
      arguments.setExpression(qualified);
      arguments.setFrameId(frameId);
      arguments.setContext("watch");
      request.setArguments(arguments);

      Response response = process.sendRequest(request);
      if (response instanceof EvaluateResponse evaluated && response.isSuccess()) {
        Variable result = new Variable();
        result.setName(expression);
        result.setValue(evaluated.getBody().getResult());
        result.setType(evaluated.getBody().getType());
        result.setVariablesReference(evaluated.getBody().getVariablesReference());
        // a watch/hover result has no editable container (0); child expansion
        // still uses the result's own reference
        callback.evaluated(new HashLinkValue(process, result, 0));
      } else {
        String message = response != null && response.getMessage() != null
                         ? response.getMessage() : "Cannot evaluate";
        callback.errorOccurred(message);
      }
    });
  }
}
