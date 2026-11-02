package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.execution.configurations.GeneralCommandLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("HashLink debugger: backend")
public class HashLinkBackendTest {

  @Test
  @DisplayName("the debuggee command line holds the program on the backends debug port followed by its arguments")
  public void verifyCommandFromHashLinkBackend() {
    Path path = Path.of("out", "app.hl");
    Path hlExecutable = Path.of("hl");
    HashLinkBackend backend = new HashLinkBackend(hlExecutable, path, 6111, List.of());

    GeneralCommandLine commandLine = backend.debuggeeCommandLine("work", List.of("--level", "two words"));

    List<String> expectedParameters = List.of("--debug", "6111", "--debug-wait", path.toString(), "--level", "two words");
    List<String> actualParameters = commandLine.getParametersList().getList();

    assertEquals(hlExecutable.toString(), commandLine.getExePath());
    assertEquals(expectedParameters, actualParameters);

    assertEquals("work", commandLine.getWorkDirectory().getPath());
  }
}
