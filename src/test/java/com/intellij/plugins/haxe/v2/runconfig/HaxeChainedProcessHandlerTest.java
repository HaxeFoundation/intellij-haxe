package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Run configurations: chained process handler")
public class HaxeChainedProcessHandlerTest extends HaxeLightFixtureTestCase {

  private static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("steps run in order and the chain ends with the last exit code")
  public void testStepsRunInOrderAndTheChainEndsWithTheLastExitCode() {
    HaxeChainedProcessHandler chain = new HaxeChainedProcessHandler(List.of(step("first", "-version"), step("second", "-version")));

    String output = run(chain);

    assertEquals(0, chain.getExitCode());
    assertTrue(output.indexOf("first") < output.indexOf("second"), output);
  }

  @Test
  @DisplayName("a failing step stops the chain with its exit code")
  public void testAFailingStepStopsTheChainWithItsExitCode() {
    HaxeChainedProcessHandler chain =
      new HaxeChainedProcessHandler(List.of(step("first", "--no-such-option"), step("second", "-version")));

    String output = run(chain);

    assertNotEquals(0, chain.getExitCode());
    assertTrue(output.contains("first"), output);
    assertFalse(output.contains("second"), output);
  }

  /** The console takes the input stream when it attaches, before any step runs; what it types later must reach the running step. */
  @Test
  @DisplayName("console input taken before the first step reaches the running step")
  public void testConsoleInputTakenBeforeTheFirstStepReachesTheRunningStep() throws IOException {
    RecordingStep step = new RecordingStep();
    HaxeChainedProcessHandler chain = new HaxeChainedProcessHandler(List.of(new HaxeChainedProcessHandler.Step("step", () -> step)));
    OutputStream consoleInput = chain.getProcessInput();
    assertNotNull(consoleInput);

    chain.startNotify();
    consoleInput.write("y\n".getBytes(StandardCharsets.UTF_8));
    consoleInput.flush();
    step.finish();

    assertTrue(chain.waitFor(10_000), "the chain must end");
    assertEquals("y\n", step.input.toString(StandardCharsets.UTF_8));
  }

  private static HaxeChainedProcessHandler.Step step(String header, String argument) {
    return new HaxeChainedProcessHandler.Step(header, () -> startJava(argument));
  }

  private static ProcessHandler startJava(String argument) throws ExecutionException {
    return new OSProcessHandler(new GeneralCommandLine(JAVA, argument));
  }

  /** Starts the chain, waits for it and returns everything it printed. */
  private static String run(HaxeChainedProcessHandler chain) {
    StringBuilder output = new StringBuilder();
    chain.addProcessListener(new ProcessListener() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        synchronized (output) {
          output.append(event.getText());
        }
      }
    });
    chain.startNotify();
    assertTrue(chain.waitFor(60_000), "the chain must end");
    synchronized (output) {
      return output.toString();
    }
  }

  /** A step that records what the console writes into it and ends when told. */
  private static final class RecordingStep extends ProcessHandler {
    final ByteArrayOutputStream input = new ByteArrayOutputStream();

    void finish() {
      notifyProcessTerminated(0);
    }

    @Override
    protected void destroyProcessImpl() {
      notifyProcessTerminated(-1);
    }

    @Override
    protected void detachProcessImpl() {
      notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
      return false;
    }

    @Override
    public @NotNull OutputStream getProcessInput() {
      return input;
    }
  }
}
