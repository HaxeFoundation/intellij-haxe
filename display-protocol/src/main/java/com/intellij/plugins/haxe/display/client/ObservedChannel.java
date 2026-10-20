package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.client.HaxeDisplayClient.RequestObserver;
import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;

/**
 * The channel a client sends through, together with the observer every
 * request is reported to: its method, its duration, and whether it succeeded,
 * which includes decoding the answer.
 */
public final class ObservedChannel {

  /** One request and the decoding of its answer. */
  @FunctionalInterface
  public interface Exchange<T> {
    T send() throws DisplayRequestException;
  }

  private final DisplayChannel channel;
  private RequestObserver observer;

  public ObservedChannel(DisplayChannel channel) {
    this.channel = channel;
  }

  public DisplayChannel channel() {
    return channel;
  }

  public void setObserver(RequestObserver observer) {
    this.observer = observer;
  }

  /** One exchange as the observer sees it: announced before it is sent, timed and judged after. */
  public <T> T observed(String method, Exchange<T> exchange) throws DisplayRequestException {
    if (observer != null) {
      observer.beforeRequest(method);
    }
    long start = System.nanoTime();
    DisplayRequestException failure = null;
    try {
      return exchange.send();
    }
    catch (DisplayRequestException e) {
      failure = e;
      throw e;
    }
    catch (RuntimeException | Error e) {
      // an answer the decoder could not read is a failed request as well
      failure = new DisplayRequestException(e.toString(), e);
      throw e;
    }
    finally {
      if (observer != null) {
        observer.afterRequest(method, (System.nanoTime() - start) / 1_000_000, failure);
      }
    }
  }
}
