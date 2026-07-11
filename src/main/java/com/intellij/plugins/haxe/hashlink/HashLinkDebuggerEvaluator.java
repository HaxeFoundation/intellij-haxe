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
 */
final class HashLinkDebuggerEvaluator extends XDebuggerEvaluator {
  private final HashLinkDebugProcess process;
  private final int frameId;

  HashLinkDebuggerEvaluator(HashLinkDebugProcess process, int frameId) {
    this.process = process;
    this.frameId = frameId;
  }

  @Override
  public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback callback,
                       @Nullable XSourcePosition expressionPosition) {
    process.onRequestThread(() -> {
      EvaluateRequest request = new EvaluateRequest();
      EvaluateArguments arguments = new EvaluateArguments();
      arguments.setExpression(expression);
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
        callback.evaluated(new HashLinkValue(process, result));
      } else {
        String message = response != null && response.getMessage() != null
                         ? response.getMessage() : "Cannot evaluate";
        callback.errorOccurred(message);
      }
    });
  }
}
