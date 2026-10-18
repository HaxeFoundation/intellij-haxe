package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The client announces every exchange to its observer before sending it and reports the outcome after, plain compiles included. */
@DisplayName("Display protocol: client request observer")
public class HaxeDisplayClientObserverTest {

  private final List<String> events = new ArrayList<>();
  private final HaxeDisplayClient.RequestObserver observer = new HaxeDisplayClient.RequestObserver() {
    @Override
    public void beforeRequest(String method) {
      events.add("before " + method);
    }

    @Override
    public void afterRequest(String method, long millis, boolean success) {
      events.add("after " + method + " " + success);
    }
  };

  @Test
  @DisplayName("a plain compile is announced and reported under the compile method")
  public void testAPlainCompileIsAnnouncedAndReportedUnderTheCompileMethod() throws DisplayRequestException {
    HaxeDisplayClient client = clientOver(channelAnswering(new DisplayResponse("ok", List.of(), false)));

    client.exchange(List.of("-main", "Main"), 1_000);

    assertEquals(List.of("before compile", "after compile true"), events);
  }

  @Test
  @DisplayName("a display request without a result is reported as failed")
  public void testADisplayRequestWithoutAResultIsReportedAsFailed() {
    HaxeDisplayClient client = clientOver(channelAnswering(new DisplayResponse("", List.of(), true)));

    assertThrows(DisplayRequestException.class, () -> client.hover(List.of(), "Main.hx", 0, null));

    assertEquals(List.of("before display/hover", "after display/hover false"), events);
  }

  private HaxeDisplayClient clientOver(DisplayChannel channel) {
    HaxeDisplayClient client = new HaxeDisplayClient(channel);
    client.setObserver(observer);
    return client;
  }

  private static DisplayChannel channelAnswering(DisplayResponse response) {
    return new DisplayChannel() {
      @Override
      public DisplayResponse request(List<String> args, int readTimeoutMs) {
        return response;
      }

      @Override
      public String describe() {
        return "canned channel";
      }
    };
  }
}
