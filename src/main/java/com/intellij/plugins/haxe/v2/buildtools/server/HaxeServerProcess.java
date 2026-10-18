package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.util.Key;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;

/**
 * One haxe server process as every manager runs it: its console output, its
 * process handler and the start/stop/termination mechanics. {@code id}
 * identifies it across managers; {@code port} is the port it serves on or
 * connected through, -1 while it does not run.
 */
class HaxeServerProcess {

  final String id;
  final String executablePath;
  /** The SDK the process serves, null for the project default; its folders lead the process's PATH. */
  @Nullable final String sdkName;
  final String displayName;
  final HaxeServerConsoleOutput output = new HaxeServerConsoleOutput();
  private volatile OSProcessHandler handler;
  int port = -1;

  HaxeServerProcess(@NotNull String id, @NotNull String executablePath, @Nullable String sdkName, @NotNull String displayName) {
    this.id = id;
    this.executablePath = executablePath;
    this.sdkName = sdkName;
    this.displayName = displayName;
  }

  boolean isAlive() {
    OSProcessHandler current = handler;
    return current != null && !current.isProcessTerminated();
  }

  /** The handler of the running process; null after a stop. A termination callback compares its handler with this one. */
  @Nullable
  OSProcessHandler handler() {
    return handler;
  }

  /**
   * Starts the command. Its output goes to the console, the command line as
   * the first line; {@code onTerminated} gets the handler and the exit code
   * when the process ends, whether it died or was stopped.
   */
  void start(@NotNull GeneralCommandLine commandLine,
             @NotNull IntFunction<String> terminatedMessage,
             @NotNull ObjIntConsumer<OSProcessHandler> onTerminated) throws ExecutionException {
    OSProcessHandler started = newHandler(commandLine);
    started.addProcessListener(new Forwarder(started, terminatedMessage, onTerminated));
    // assigned before the start, so a process that dies at once still matches its handler
    handler = started;
    started.startNotify();
  }

  /** Stops the process. The handler is cleared first, so the termination callback recognizes a deliberate stop. */
  void stop() {
    OSProcessHandler current = handler;
    handler = null;
    port = -1;
    if (current != null && !current.isProcessTerminated()) {
      current.destroyProcess();
    }
  }

  @NotNull
  private static OSProcessHandler newHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
    return new OSProcessHandler(commandLine) {
      // a server is a long-running, mostly idle daemon, and the default reader busy-polls and wastes CPU
      @Override
      protected @NotNull BaseOutputReader.Options readerOptions() {
        return BaseOutputReader.Options.forMostlySilentProcess();
      }
    };
  }

  /** Forwards the process output to the console and reports the end of the process. */
  private final class Forwarder implements ProcessListener {
    private final OSProcessHandler started;
    private final IntFunction<String> terminatedMessage;
    private final ObjIntConsumer<OSProcessHandler> onTerminated;

    Forwarder(@NotNull OSProcessHandler started,
              @NotNull IntFunction<String> terminatedMessage,
              @NotNull ObjIntConsumer<OSProcessHandler> onTerminated) {
      this.started = started;
      this.terminatedMessage = terminatedMessage;
      this.onTerminated = onTerminated;
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      output.broadcast(event.getText(), outputType);
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
      output.broadcast(terminatedMessage.apply(event.getExitCode()) + "\n", ProcessOutputType.SYSTEM);
      onTerminated.accept(started, event.getExitCode());
    }
  }
}
