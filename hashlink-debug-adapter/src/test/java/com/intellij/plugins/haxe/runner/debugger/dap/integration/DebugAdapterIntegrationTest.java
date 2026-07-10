package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ErrorResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ThreadsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ThreadsResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Starts the real adapter bytecode with the HashLink executable and drives it
 * over a TCP DAP connection. Skipped (not failed) when the adapter was not
 * built or no HashLink executable can be found — see {@link HlExecutableResolver}.
 */
public class DebugAdapterIntegrationTest {
  private static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";
  private static final long TIMEOUT_MILLIS = 5_000;

  private Process adapterProcess;
  private DapClient client;

  @Before
  public void startAdapter() throws IOException {
    String adapterProperty = System.getProperty("dap.adapter.hl", "");
    Assume.assumeTrue("adapter bytecode not built (haxe compiler unavailable?) - skipping integration test",
                      !adapterProperty.isEmpty() && Files.isRegularFile(Path.of(adapterProperty)));

    Optional<Path> hlExecutable = HlExecutableResolver.resolve();
    Assume.assumeTrue("HashLink executable not found (set -PhashlinkBin / -Dhashlink.executable, "
                      + "HASHLINK_BIN / HASHLINK / HASHLINKPATH, or put hl on PATH) - skipping integration test",
                      hlExecutable.isPresent());

    adapterProcess = new ProcessBuilder(hlExecutable.get().toString(), adapterProperty, "--port", "0")
      .redirectErrorStream(true)
      .start();
    int port = awaitListeningPort();
    client = DapClient.connect("127.0.0.1", port, (int)TIMEOUT_MILLIS);
  }

  @After
  public void stopAdapter() throws Exception {
    if (client != null) {
      try {
        client.close();
      } catch (IOException ignored) {
      }
    }
    if (adapterProcess != null && !adapterProcess.waitFor(2, TimeUnit.SECONDS)) {
      adapterProcess.destroyForcibly();
      adapterProcess.waitFor(5, TimeUnit.SECONDS);
    }
  }

  @Test
  public void initializeRepliesWithCapabilitiesAndInitializedEvent() throws Exception {
    Response response = sendInitialize();

    assertTrue(response instanceof InitializeResponse);
    assertTrue(response.isSuccess());
    assertEquals("initialize", response.getCommand());
    assertEquals(Boolean.TRUE, ((InitializeResponse)response).getBody().getSupportsConfigurationDoneRequest());

    Event event = client.pollEvent(TIMEOUT_MILLIS);
    assertNotNull("expected an event after the initialize response", event);
    assertTrue(event instanceof InitializedEvent);
  }

  @Test
  public void setBreakpointsAreAcceptedAndVerified() throws Exception {
    sendInitialize();

    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    source.setName("Main.hx");
    source.setPath("/project/src/Main.hx");
    arguments.setSource(source);
    arguments.setBreakpoints(List.of(sourceBreakpoint(10), sourceBreakpoint(20), sourceBreakpoint(30)));
    request.setArguments(arguments);

    Response response = client.sendRequest(request, TIMEOUT_MILLIS);

    assertTrue(response instanceof SetBreakpointsResponse);
    assertTrue(response.isSuccess());
    List<Breakpoint> breakpoints = ((SetBreakpointsResponse)response).getBody().getBreakpoints();
    assertEquals(3, breakpoints.size());
    assertEquals(Integer.valueOf(10), breakpoints.get(0).getLine());
    assertEquals(Integer.valueOf(30), breakpoints.get(2).getLine());
    for (Breakpoint breakpoint : breakpoints) {
      assertTrue("breakpoint should be verified", breakpoint.isVerified());
    }
    assertNotEquals(breakpoints.get(0).getId(), breakpoints.get(1).getId());
    assertEquals("/project/src/Main.hx", breakpoints.get(0).getSource().getPath());
  }

  @Test
  public void configurationDoneIsAcknowledged() throws Exception {
    sendInitialize();
    Response response = client.sendRequest(new ConfigurationDoneRequest(), TIMEOUT_MILLIS);
    assertTrue(response.isSuccess());
    assertEquals("configurationDone", response.getCommand());
  }

  @Test
  public void launchStubIsAcknowledged() throws Exception {
    sendInitialize();
    Response response = client.sendRequest(new LaunchRequest(), TIMEOUT_MILLIS);
    assertTrue(response.isSuccess());
  }

  @Test
  public void threadsReturnsStubMainThread() throws Exception {
    sendInitialize();
    Response response = client.sendRequest(new ThreadsRequest(), TIMEOUT_MILLIS);

    assertTrue(response instanceof ThreadsResponse);
    ThreadsResponse threadsResponse = (ThreadsResponse)response;
    assertEquals(1, threadsResponse.getBody().getThreads().size());
    assertEquals(1, threadsResponse.getBody().getThreads().get(0).getId());
    assertEquals("main", threadsResponse.getBody().getThreads().get(0).getName());
  }

  @Test
  public void unknownCommandYieldsErrorResponse() throws Exception {
    Request request = new Request();
    request.setCommand("fooBar");

    Response response = client.sendRequest(request, TIMEOUT_MILLIS);

    assertFalse(response.isSuccess());
    assertTrue(response instanceof ErrorResponse);
    assertEquals("fooBar", response.getCommand());
    assertTrue(response.getMessage().contains("fooBar"));
    assertEquals(1000, ((ErrorResponse)response).getBody().getError().getId());
  }

  @Test
  public void responsesEchoRequestSeqAndAdapterSeqIsMonotonic() throws Exception {
    Response first = sendInitialize();
    Response second = client.sendRequest(new ConfigurationDoneRequest(), TIMEOUT_MILLIS);
    Response third = client.sendRequest(new ThreadsRequest(), TIMEOUT_MILLIS);

    assertEquals(1, first.getRequest_seq());
    assertEquals(2, second.getRequest_seq());
    assertEquals(3, third.getRequest_seq());
    assertTrue("adapter seq must increase", first.getSeq() < second.getSeq());
    assertTrue("adapter seq must increase", second.getSeq() < third.getSeq());
  }

  @Test
  public void disconnectIsAcknowledgedAndAdapterExitsCleanly() throws Exception {
    sendInitialize();
    Response response = client.sendRequest(new DisconnectRequest(), TIMEOUT_MILLIS);
    assertTrue(response.isSuccess());

    assertTrue("adapter should exit after disconnect", adapterProcess.waitFor(5, TimeUnit.SECONDS));
    assertEquals(0, adapterProcess.exitValue());
  }

  private Response sendInitialize() throws Exception {
    InitializeRequest request = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setAdapterID("intellij-haxe-test");
    arguments.setClientID("junit");
    request.setArguments(arguments);
    return client.sendRequest(request, TIMEOUT_MILLIS);
  }

  private static SourceBreakpoint sourceBreakpoint(int line) {
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(line);
    return breakpoint;
  }

  private int awaitListeningPort() throws IOException {
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(adapterProcess.getInputStream(), StandardCharsets.UTF_8));
    String line;
    while ((line = stdout.readLine()) != null) {
      if (line.startsWith(LISTENING_PREFIX)) {
        return Integer.parseInt(line.substring(LISTENING_PREFIX.length()).trim());
      }
    }
    throw new IOException("Adapter exited before announcing its listening port");
  }
}
