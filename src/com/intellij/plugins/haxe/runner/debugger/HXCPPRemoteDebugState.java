package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPDebugProcess;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;

/**
 * @author: Fedor.Korotkov
 */
public class HXCPPRemoteDebugState extends CommandLineState {
  public HXCPPRemoteDebugState(@NotNull Project project, @NotNull final ExecutionEnvironment env) {
    super(env);
    setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(project));
  }

  @NotNull
  protected ProcessHandler startProcess() throws ExecutionException {
    return new HXCPPRemoteDebugProcessHandler();
  }

  public static class HXCPPRemoteDebugProcessHandler extends ProcessHandler {

    private HXCPPDebugProcess myProcess = null;

    public HXCPPRemoteDebugProcessHandler() {
    }

    @Override
    protected void destroyProcessImpl() {
      if (myProcess != null) {
        myProcess.stop();
      }
      detachProcessImpl();
    }

    @Override
    protected void detachProcessImpl() {
      notifyProcessTerminated(0);
      notifyTextAvailable("Server stopped.\n", ProcessOutputTypes.SYSTEM);
    }

    @Override
    public boolean detachIsDefault() {
      return false;
    }

    @Override
    public OutputStream getProcessInput() {
      return null;
    }

    public void setRemoteDebugProcess(HXCPPDebugProcess process) {
      myProcess = process;
    }
  }
}
