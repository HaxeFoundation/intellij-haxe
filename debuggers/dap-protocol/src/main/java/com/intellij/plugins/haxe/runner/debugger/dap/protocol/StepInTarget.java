package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * One selectable "step into" target on the stopped line: a call the step can
 * enter. Passing {@code id} as a stepIn request's {@code targetId} enters that
 * specific call instead of the first one reached.
 */
@Data
public class StepInTarget {
  private int id;
  private String label;
}
