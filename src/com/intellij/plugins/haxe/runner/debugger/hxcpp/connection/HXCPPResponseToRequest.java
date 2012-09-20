package com.intellij.plugins.haxe.runner.debugger.hxcpp.connection;

import com.intellij.util.io.socketConnection.ResponseToRequest;

public class HXCPPResponseToRequest extends HXCPPResponse implements ResponseToRequest {

  private final int myId;

  public HXCPPResponseToRequest(int id, String responseString) {
    super(responseString);
    myId = id;
  }

  @Override
  public int getRequestId() {
    return myId;
  }
}
