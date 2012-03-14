package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.debug.FlexDebugProcess;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexIdeBuildConfiguration;
import com.intellij.lang.javascript.flex.run.BCBasedRunnerParameters;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebugProcess extends FlexDebugProcess {
  public HaxeDebugProcess(final XDebugSession session, final FlexIdeBuildConfiguration bc, final BCBasedRunnerParameters params)
    throws IOException {
    super(session, bc, params);

    final XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> flexHandler =
      (XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>>)getBreakpointHandlers()[0];
    getBreakpointHandlers()[0] = new HaxeBreakPointHandler(flexHandler);
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new HaxeDebuggerEditorsProvider();
  }
}
