package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInTarget;

import java.util.List;
import lombok.Data;

/**
 * Body of a "stepInTargets" response: the calls on the stopped line, in
 * execution order.
 */
@Data
public class StepInTargetsResponseBody {
  private List<StepInTarget> targets;
}
