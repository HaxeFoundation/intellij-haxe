/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.run.BCBasedRunnerParameters;
import com.intellij.lang.javascript.flex.run.FlashRunnerParameters;
import com.intellij.lang.javascript.flex.run.LauncherParameters;
import com.intellij.lang.javascript.flex.sdk.FlexSdkUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebugProcess;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSessionStartedResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Debug-session entry points for the flash family, built on the Flex plugin's
 * fdb-driven {@link com.intellij.lang.javascript.flex.debug.FlexDebugProcess}.
 * Everything here touches Flex plugin classes, so callers must gate on the
 * plugin's presence BEFORE classloading this type.
 */
public class HaxeFlashDebuggingUtil {

  /**
   * A Flash (swf) debug session: fdb launches the swf itself — with the given
   * standalone player when one resolves, else the OS default handler.
   */
  public static RunContentDescriptor getDescriptor(final Module module,
                                                   ExecutionEnvironment env,
                                                   String urlToLaunch,
                                                   String flexSdkName,
                                                   @Nullable String flashPlayerPath,
                                                   @NotNull List<String> sourceDirectories) throws ExecutionException {
    final FlexBuildConfiguration bc = fakeBuildConfiguration(flexSdkName, urlToLaunch);

    XDebugProcessStarter starter = new XDebugProcessStarter() {
      @NotNull
      public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
        try {
          final FlashRunnerParameters params = new FlashRunnerParameters();
          params.setModuleName(module.getName());
          if (flashPlayerPath != null && !flashPlayerPath.isBlank()) {
            // the 4-arg constructor requires a non-null browser even for the
            // Player type; the default instance + setters avoid it
            LauncherParameters playerLauncher = new LauncherParameters();
            playerLauncher.setLauncherType(LauncherParameters.LauncherType.Player);
            playerLauncher.setPlayerPath(flashPlayerPath);
            params.setLauncherParameters(playerLauncher);
          }
          return new HaxeDebugProcess(session, bc, params, sourceDirectories);
        }
        catch (IOException e) {
          throw new ExecutionException(e.getMessage(), e);
        }
      }
    };
    return startSession(module, env, starter).getRunContentDescriptor();
  }

  /** The split-debugger-safe session start (XDebugSession's own getRunContentDescriptor is deprecated and logs an error). */
  @NotNull
  private static XSessionStartedResult startSession(@NotNull Module module,
                                                    @NotNull ExecutionEnvironment env,
                                                    @NotNull XDebugProcessStarter starter) throws ExecutionException {
    return XDebuggerManager.getInstance(module.getProject())
      .newSessionBuilder(starter)
      .environment(env)
      .startSession();
  }

  /**
   * An AIR debug session: fdb only WAITS (the plain {@link BCBasedRunnerParameters}
   * route the Flex plugin uses for remote debugging), and adl is spawned here with
   * the given command line. The Flex plugin's own desktop path cannot launch a
   * lime-packaged app — it derives the descriptor location and content root from
   * one output path, but lime puts the descriptor at the export root with the
   * content in {@code bin/} beside it.
   */
  public static RunContentDescriptor getAirDescriptor(final Module module,
                                                      ExecutionEnvironment env,
                                                      String flexSdkName,
                                                      @NotNull GeneralCommandLine adlCommandLine,
                                                      @NotNull List<String> sourceDirectories) throws ExecutionException {
    return airDescriptor(module, env, flexSdkName, adlCommandLine, sourceDirectories, null);
  }

  /**
   * A flash-family TEST debug session: the AIR shape ({@link #getAirDescriptor}
   * — fdb waits, adl launches the -debug tests swf) with the session console
   * replaced by a bridge into the given SM test console. fdb relays the app's
   * traces — the TeamCity protocol among them — onto the session console, and
   * the bridge feeds every printed line into the sink the SM view parses (see
   * {@link FdbTestConsoleBridge}). The sink terminates with the session, which
   * finalizes the test tree.
   */
  public static RunContentDescriptor getAirTestDescriptor(final Module module,
                                                          ExecutionEnvironment env,
                                                          String flexSdkName,
                                                          @NotNull GeneralCommandLine adlCommandLine,
                                                          @NotNull List<String> sourceDirectories,
                                                          @NotNull ConsoleView testConsole,
                                                          @NotNull ProcessHandler testOutputSink) throws ExecutionException {
    FdbTestConsoleBridge bridge = new FdbTestConsoleBridge(testConsole, testOutputSink);
    return airDescriptor(module, env, flexSdkName, adlCommandLine, sourceDirectories, bridge);
  }

  /** The shared AIR session shape; a non-null {@code bridge} swaps in the SM test console and finalizes it with the session. */
  private static RunContentDescriptor airDescriptor(final Module module,
                                                    ExecutionEnvironment env,
                                                    String flexSdkName,
                                                    @NotNull GeneralCommandLine adlCommandLine,
                                                    @NotNull List<String> sourceDirectories,
                                                    @Nullable FdbTestConsoleBridge bridge) throws ExecutionException {
    final FlexBuildConfiguration bc = fakeBuildConfiguration(flexSdkName, adlCommandLine.getExePath());

    XDebugProcessStarter starter = new XDebugProcessStarter() {
      @NotNull
      public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
        try {
          BCBasedRunnerParameters params = new BCBasedRunnerParameters();
          params.setModuleName(module.getName());
          if (bridge == null) {
            return new HaxeDebugProcess(session, bc, params, sourceDirectories);
          }
          return new HaxeDebugProcess(session, bc, params, sourceDirectories) {
            @Override
            @NotNull
            public ExecutionConsole createConsole() {
              return bridge;
            }
          };
        }
        catch (IOException e) {
          throw new ExecutionException(e.getMessage(), e);
        }
      }
    };
    XSessionStartedResult started = startSession(module, env, starter);
    if (bridge != null) {
      started.getSession().addSessionListener(new XDebugSessionListener() {
        @Override
        public void sessionStopped() {
          // ends the synthetic test process so the SM tree finalizes
          bridge.endTestOutput();
        }
      });
    }

    // fdb is up and waiting; the -debug swf connects to it as the app starts
    OSProcessHandler adlHandler = new OSProcessHandler(adlCommandLine);
    tieTogether(started.getSession(), adlHandler);
    adlHandler.startNotify();

    return started.getRunContentDescriptor();
  }

  /** The resolved Flex SDK table entry wrapped as the stub build configuration fdb-driven sessions run against. */
  @NotNull
  private static FlexBuildConfiguration fakeBuildConfiguration(String flexSdkName, String launchTarget) throws ExecutionException {
    Sdk flexSdk = FlexSdkUtils.findFlexOrFlexmojosSdk(flexSdkName);
    if (flexSdk == null) {
      throw new ExecutionException(HaxeBundle.message("flex.sdk.not.found", flexSdkName));
    }
    return new FakeFlexBuildConfiguration(flexSdk, launchTarget);
  }

  /**
   * The app closing ends the session; the session stopping kills the app.
   * adl's own output surfaces in the session console - without it an instant
   * adl exit (e.g. AIR's "invocation forwarded to primary instance" when a
   * stray same-id instance survives) silently ends the session with no clue.
   */
  private static void tieTogether(@NotNull XDebugSession debugSession, @NotNull OSProcessHandler adlHandler) {
    adlHandler.addProcessListener(new ProcessListener() {
      // AIR forwards a same-app-id launch to the running instance and exits;
      // an IDE-owned instance triggers the platform's stop-and-rerun dialog
      // instead, so this only fires for a stray instance the IDE never knew
      private boolean forwardedToRunningInstance;

      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (event.getText().contains("invocation forwarded")) {
          forwardedToRunningInstance = true;
        }
        printToSessionConsole(debugSession, event.getText());
      }

      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        printToSessionConsole(debugSession, HaxeDebuggerBundle.message("air.runner.adl.exit.code", event.getExitCode()) + "\n");
        if (forwardedToRunningInstance) {
          printToSessionConsole(debugSession, HaxeDebuggerBundle.message("air.runner.instance.hint") + "\n");
        }
        debugSession.stop();
      }
    });
    debugSession.addSessionListener(new XDebugSessionListener() {
      @Override
      public void sessionStopped() {
        if (!adlHandler.isProcessTerminated()) {
          adlHandler.destroyProcess();
        }
      }
    });
  }

  private static void printToSessionConsole(@NotNull XDebugSession debugSession, @NotNull String text) {
    ConsoleView console = debugSession.getConsoleView();
    if (console != null) {
      console.print(text, ConsoleViewContentType.NORMAL_OUTPUT);
    }
  }
}
