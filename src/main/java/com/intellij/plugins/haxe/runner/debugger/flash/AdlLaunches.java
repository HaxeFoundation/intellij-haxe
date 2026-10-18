package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** adl launches, shared by the AIR run configuration and the flash-family test runs. */
public final class AdlLaunches {

  private AdlLaunches() {
  }

  /**
   * {@code adl [options] [-nodebug] <descriptor> <content-root> [-- args]}, run
   * in the content root. Plain Run passes {@code -nodebug}: adl's default
   * debug-launch mode swallows trace output; a debug launch must NOT pass it,
   * or the swf never connects to fdb.
   */
  @NotNull
  public static GeneralCommandLine command(@NotNull Path adl,
                                           @NotNull List<String> options,
                                           @NotNull Path descriptor,
                                           @NotNull Path contentRoot,
                                           boolean debugLaunch,
                                           @NotNull List<String> programArguments) {
    GeneralCommandLine commandLine = new GeneralCommandLine()
      .withExePath(adl.toString())
      .withWorkDirectory(contentRoot.toString());
    commandLine.addParameters(options);
    if (!debugLaunch) {
      commandLine.addParameter("-nodebug");
    }
    commandLine.addParameter(descriptor.toString());
    commandLine.addParameter(contentRoot.toString());
    if (!programArguments.isEmpty()) {
      commandLine.addParameter("--");
      commandLine.addParameters(programArguments);
    }
    return commandLine;
  }

  /**
   * AIR is single-instance per application id (a second same-id launch just
   * forwards to the first and exits; {@code -pubid} is rejected for post-1.5.3
   * namespaces), so a launch that must not be swallowed runs a COPY of the
   * descriptor whose id carries a unique suffix. The content root still
   * points at the original content; the suffixed id also gives the instance
   * its own application storage.
   */
  @NotNull
  public static Path uniqueIdDescriptor(@NotNull Path descriptor) throws ExecutionException {
    try {
      String content = Files.readString(descriptor);
      String suffix = ".ij" + UUID.randomUUID().toString().substring(0, 8);
      // the first <id>...</id> element is the application id; the suffix lands inside it
      String modified = content.replaceFirst("(<id>[^<]*)</id>", "$1" + suffix + "</id>");
      if (modified.equals(content)) {
        throw new ExecutionException(HaxeDebuggerBundle.message("air.runner.descriptor.no.id", descriptor.toString()));
      }
      // deleteOnExit: adl reads the copy at launch only, but its lifetime is
      // unknown here - IDE-exit cleanup bounds the per-launch dirs to one session
      Path directory = FileUtil.createTempDirectory("haxe-air-run", null, true).toPath();
      Path copy = directory.resolve(descriptor.getFileName());
      Files.writeString(copy, modified);
      return copy;
    }
    catch (IOException e) {
      throw new ExecutionException(HaxeDebuggerBundle.message("air.runner.descriptor.copy.failed", e.getMessage()));
    }
  }
}
