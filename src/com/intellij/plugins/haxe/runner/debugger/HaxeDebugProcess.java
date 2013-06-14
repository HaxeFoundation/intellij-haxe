package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.debug.FlexDebugProcess;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.run.BCBasedRunnerParameters;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebugProcess extends FlexDebugProcess {
  public HaxeDebugProcess(final XDebugSession session,
                          final FlexBuildConfiguration bc,
                          final BCBasedRunnerParameters params) throws IOException {
    super(session, bc, params);
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new HaxeDebuggerEditorsProvider();
  }
}
