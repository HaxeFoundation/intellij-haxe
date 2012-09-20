package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexIdeBuildConfiguration;
import com.intellij.lang.javascript.flex.run.FlashRunnerParameters;
import com.intellij.lang.javascript.flex.sdk.FlexSdkUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFlashDebuggingUtil {
  public static RunContentDescriptor getDescriptor(HaxeDebugRunner runner, Project project,
                                                   RunContentDescriptor contentToReuse,
                                                   ExecutionEnvironment env,
                                                   String urlToLaunch, String flexSdkName) throws ExecutionException {
    final Sdk flexSdk = FlexSdkUtils.findFlexOrFlexmojosSdk(flexSdkName);
    if (flexSdk == null) {
      throw new ExecutionException(HaxeBundle.message("flex.sdk.not.found", flexSdkName));
    }

    final FlexIdeBuildConfiguration bc = new FakeFlexIdeBuildConfiguration(flexSdk, urlToLaunch);

    final XDebugSession debugSession =
      XDebuggerManager.getInstance(project).startSession(runner, env, contentToReuse, new XDebugProcessStarter() {
        @NotNull
        public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
          try {
            return new HaxeDebugProcess(session, bc, new FlashRunnerParameters());
          }
          catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
          }
        }
      });

    return debugSession.getRunContentDescriptor();
  }
}
