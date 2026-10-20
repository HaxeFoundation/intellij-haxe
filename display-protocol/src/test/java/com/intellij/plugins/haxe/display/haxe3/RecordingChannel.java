package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A channel that records every request's arguments and answers the queued responses in order. */
final class RecordingChannel implements DisplayChannel {

  final List<List<String>> requests = new ArrayList<>();
  private final Deque<DisplayResponse> answers = new ArrayDeque<>();

  RecordingChannel answering(String payload) {
    answers.add(new DisplayResponse(payload, List.of(), false));
    return this;
  }

  RecordingChannel failing(String errorText) {
    answers.add(new DisplayResponse(errorText, List.of(), true));
    return this;
  }

  @Override
  public DisplayResponse request(List<String> args, int readTimeoutMs) {
    requests.add(List.copyOf(args));
    return answers.isEmpty() ? new DisplayResponse("", List.of(), false) : answers.removeFirst();
  }

  /** The {@code --display} value of the last request. */
  String lastDisplayArgument() {
    List<String> last = requests.getLast();
    return last.get(last.indexOf("--display") + 1);
  }
}
