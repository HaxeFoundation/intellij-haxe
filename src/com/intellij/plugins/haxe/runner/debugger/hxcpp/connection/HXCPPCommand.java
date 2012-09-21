package com.intellij.plugins.haxe.runner.debugger.hxcpp.connection;

import com.intellij.idea.LoggerFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.socketConnection.AbstractRequest;

import java.io.IOException;
import java.io.Writer;

public class HXCPPCommand implements AbstractRequest {
  private static final Logger LOG = LoggerFactory.getInstance().getLoggerInstance(HXCPPCommand.class.getName());
  private final String[] myArgs;
  private final int myId;

  public HXCPPCommand(int id, String... args) {
    myId = id;
    myArgs = args;
  }

  @Override
  public int getId() {
    return myId;
  }

  public void sendCommand(Writer writer) throws IOException {
    StringBuilder result = new StringBuilder();
    for (String arg : myArgs) {
      if (result.length() > 0) {
        result.append(' ');
      }
      result.append(arg);
    }
    final String message = result.toString();
    LOG.debug("write for " + myId + ": " + message);
    writer.write(message + "\n");
    writer.flush();
  }
}
