package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.runner.NMERunningState;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPDebugProcess;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebugRunner extends DefaultProgramRunner {
  public static final String HAXE_DEBUG_RUNNER_ID = "HaxeDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return HAXE_DEBUG_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HaxeApplicationConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(Project project,
                                           Executor executor,
                                           RunProfileState state,
                                           RunContentDescriptor contentToReuse,
                                           ExecutionEnvironment env) throws ExecutionException {
    final HaxeApplicationConfiguration configuration = (HaxeApplicationConfiguration)env.getRunProfile();
    final Module module = configuration.getConfigurationModule().getModule();

    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", configuration.getName()));
    }

    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    final boolean notHXCPP = settings.getNmeTarget() != NMETarget.WINDOWS &&
                             settings.getNmeTarget() != NMETarget.LINUX &&
                             settings.getNmeTarget() != NMETarget.LINUX64 &&
                             settings.getNmeTarget() != NMETarget.ANDROID &&
                             settings.getNmeTarget() != NMETarget.IOS;
    if (settings.getHaxeTarget() != HaxeTarget.FLASH && settings.getNmeTarget() != NMETarget.FLASH && notHXCPP) {
      throw new ExecutionException(HaxeBundle.message("haxe.proper.debug.targets"));
    }

    if (settings.getHaxeTarget() != HaxeTarget.FLASH && settings.getNmeTarget() != NMETarget.FLASH) {
      boolean runInTest = settings.getNmeTarget() == NMETarget.ANDROID || settings.getNmeTarget() == NMETarget.IOS;
      return runHXCPP(project, module, env, executor, contentToReuse, runInTest);
    }

    FileDocumentManager.getInstance().saveAllDocuments();
    final CompilerModuleExtension model = CompilerModuleExtension.getInstance(module);
    assert model != null;

    String urlToLaunch = model.getCompilerOutputUrl() + "/" + settings.getOutputFileName();
    if (configuration.isCustomFileToLaunch()) {
      urlToLaunch = configuration.getCustomFileToLaunchPath();
    }

    if (!PluginManager.isPluginInstalled(PluginId.getId("com.intellij.flex"))) {
      throw new ExecutionException(HaxeBundle.message("install.flex.plugin"));
    }

    String flexSdkName = settings.getFlexSdkName();
    if (StringUtil.isEmpty(flexSdkName)) {
      throw new ExecutionException(HaxeBundle.message("flex.sdk.not.specified"));
    }

    return HaxeFlashDebuggingUtil.getDescriptor(this, module, contentToReuse, env, urlToLaunch, flexSdkName);
  }

  private RunContentDescriptor runHXCPP(Project project,
                                        final Module module,
                                        final ExecutionEnvironment env,
                                        final Executor executor,
                                        RunContentDescriptor contentToReuse,
                                        final boolean runInTest) throws ExecutionException {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    Integer port = null;
    try {
      port = Integer.parseInt(settings.getHXCPPPort());
    }
    catch (NumberFormatException e) {
      throw new ExecutionException("Bad HXCPP port \"" + settings.getHXCPPPort() + "\" in module settings");
    }

    final int finalPort = port;
    final XDebugSession debugSession =
      XDebuggerManager.getInstance(project).startSession(this, env, contentToReuse, new XDebugProcessStarter() {
        @NotNull
        public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
          try {
            NMERunningState runningState = new NMERunningState(env, module, runInTest);
            final ExecutionResult executionResult = runningState.execute(executor, HaxeDebugRunner.this);
            return new HXCPPDebugProcess(session, module, finalPort, executionResult);
          }
          catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
          }
        }
      });

    return debugSession.getRunContentDescriptor();
  }
}
